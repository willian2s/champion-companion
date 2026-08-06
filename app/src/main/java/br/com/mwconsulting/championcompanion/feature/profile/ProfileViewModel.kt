package br.com.mwconsulting.championcompanion.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.StepLengthSuggestionPolicy
import br.com.mwconsulting.championcompanion.core.model.UpdateStepLengthInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val personalMetricsRepository: PersonalMetricsRepository,
) : ViewModel(), ProfileFeatureContract {
    private val mutableState = MutableStateFlow(ProfileUiState())
    private var hasLocalStepLengthInput = false

    override val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            personalMetricsRepository.observeSnapshot()
                .catch {
                    mutableState.update { current ->
                        current.copy(isLoading = false, message = ProfileMessage.PROFILE_UNAVAILABLE)
                    }
                }
                .collect { snapshot ->
                    val profile = snapshot.stepLengthProfile
                    mutableState.update { current ->
                        current.copy(
                            isLoading = false,
                            heightCentimeters = snapshot.heightCentimeters,
                            stepLengthMeters = profile?.stepLengthMeters,
                            stepLengthSource = profile?.source,
                            stepLengthUpdatedAt = profile?.updatedAt,
                            stepLengthInput = if (hasLocalStepLengthInput) {
                                current.stepLengthInput
                            } else {
                                profile?.stepLengthMeters?.let(::formatStepLengthMeters).orEmpty()
                            },
                            heightSuggestionMeters = StepLengthSuggestionPolicy.fromHeight(
                                snapshot.heightCentimeters,
                            ),
                        )
                    }
                }
        }
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> Unit
            is ProfileIntent.StepLengthChanged -> {
                hasLocalStepLengthInput = true
                mutableState.update { current ->
                    current.copy(stepLengthInput = intent.value, message = null)
                }
            }

            ProfileIntent.SaveManualStepLength -> saveManualStepLength()
            ProfileIntent.AcceptHeightSuggestion -> mutableState.update {
                it.copy(isConfirmingHeightSuggestion = it.heightSuggestionMeters != null)
            }
            ProfileIntent.ConfirmHeightSuggestion -> {
                mutableState.update { it.copy(isConfirmingHeightSuggestion = false) }
                acceptHeightSuggestion()
            }
            ProfileIntent.CancelHeightSuggestion -> mutableState.update {
                it.copy(isConfirmingHeightSuggestion = false)
            }
            ProfileIntent.DismissMessage -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun saveManualStepLength() {
        if (mutableState.value.isSaving) return
        val stepLengthMeters = parseStepLengthMeters(mutableState.value.stepLengthInput)
        if (stepLengthMeters == null) {
            mutableState.update { current ->
                current.copy(message = ProfileMessage.INVALID_STEP_LENGTH)
            }
            return
        }
        saveStepLength(
            stepLengthMeters = stepLengthMeters,
            source = StepLengthSource.MANUAL,
            successMessage = ProfileMessage.MANUAL_SAVED,
        )
    }

    private fun acceptHeightSuggestion() {
        if (mutableState.value.isSaving) return
        val suggestion = mutableState.value.heightSuggestionMeters
        if (suggestion == null) {
            mutableState.update { current ->
                current.copy(message = ProfileMessage.SUGGESTION_UNAVAILABLE)
            }
            return
        }
        saveStepLength(
            stepLengthMeters = suggestion,
            source = StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED,
            successMessage = ProfileMessage.HEIGHT_SUGGESTION_SAVED,
        )
    }

    private fun saveStepLength(
        stepLengthMeters: Double,
        source: StepLengthSource,
        successMessage: ProfileMessage,
    ) {
        viewModelScope.launch {
            mutableState.update { current -> current.copy(isSaving = true, message = null) }
            val result = try {
                personalMetricsRepository.updateStepLength(
                    UpdateStepLengthInput(
                        stepLengthMeters = stepLengthMeters,
                        source = source,
                        updatedAt = Instant.now(),
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RepositoryResult.StorageUnavailable
            }
            when (result) {
                is RepositoryResult.Success -> {
                    hasLocalStepLengthInput = false
                    mutableState.update { current ->
                        current.copy(
                            isSaving = false,
                            isConfirmingHeightSuggestion = false,
                            stepLengthMeters = result.value.stepLengthProfile?.stepLengthMeters,
                            stepLengthSource = result.value.stepLengthProfile?.source,
                            stepLengthUpdatedAt = result.value.stepLengthProfile?.updatedAt,
                            stepLengthInput = result.value.stepLengthProfile?.stepLengthMeters
                                ?.let(::formatStepLengthMeters)
                                .orEmpty(),
                            message = successMessage,
                        )
                    }
                }

                RepositoryResult.NotFound -> showSaveFailure(ProfileMessage.PROFILE_UNAVAILABLE)
                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                RepositoryResult.StorageUnavailable,
                -> showSaveFailure(ProfileMessage.SAVE_FAILED)
            }
        }
    }

    private fun showSaveFailure(message: ProfileMessage) {
        mutableState.update { current -> current.copy(isSaving = false, message = message) }
    }
}

internal fun parseStepLengthMeters(
    input: String,
    locale: Locale = Locale.getDefault(),
): Double? {
    val compact = input.trim()
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val separators = compact.filter { it == ',' || it == '.' || it == decimalSeparator }.toSet()
    if (separators.size > 1) return null
    val separator = separators.singleOrNull()
    if (separator != null && compact.count { it == separator } > 1) return null
    val normalized = separator?.let { compact.replace(it, '.') } ?: compact
    if (!normalized.matches(Regex("[+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"))) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
}

internal fun formatStepLengthMeters(
    value: Double,
    locale: Locale = Locale.getDefault(),
): String {
    val plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (decimalSeparator == '.') plain else plain.replace('.', decimalSeparator)
}
