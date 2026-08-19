package br.com.mwconsulting.championcompanion.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.StepLengthSuggestionPolicy
import br.com.mwconsulting.championcompanion.core.model.UpdateHeightInput
import br.com.mwconsulting.championcompanion.core.model.UpdateStepLengthInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
) : ViewModel(), ProfileFeatureContract {
    private val mutableState = MutableStateFlow(ProfileUiState())
    private var hasLocalHeightInput = false
    private var hasLocalDailyStepsGoalInput = false
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
                            heightInput = if (hasLocalHeightInput) {
                                current.heightInput
                            } else {
                                snapshot.heightCentimeters?.let(::formatHeightCentimeters).orEmpty()
                            },
                            stepLengthMeters = profile?.stepLengthMeters,
                            stepLengthSource = profile?.source,
                            stepLengthUpdatedAt = profile?.updatedAt,
                            stepLengthInput = if (hasLocalStepLengthInput) {
                                current.stepLengthInput
                            } else {
                                profile?.stepLengthMeters?.let(::formatStepLengthInput).orEmpty()
                            },
                            heightSuggestionMeters = StepLengthSuggestionPolicy.fromHeight(
                                snapshot.heightCentimeters,
                            ),
                        )
                    }
                }
        }
        viewModelScope.launch {
            appPreferencesRepository.observeDailyStepsGoal()
                .catch {
                    mutableState.update { current ->
                        current.copy(message = ProfileMessage.PROFILE_UNAVAILABLE)
                    }
                }
                .collect { goal ->
                    mutableState.update { current ->
                        current.copy(
                            dailyStepsGoal = goal,
                            dailyStepsGoalInput = if (hasLocalDailyStepsGoalInput) {
                                current.dailyStepsGoalInput
                            } else {
                                goal.toString()
                            },
                        )
                    }
                }
        }
        viewModelScope.launch {
            personalMetricsRepository.observeLearnedStepLengthProfiles()
                .catch {
                    mutableState.update { current ->
                        current.copy(learnedStepLengthProfiles = LearnedStepLengthProfilesState.Unavailable)
                    }
                }
                .collect { profiles ->
                    mutableState.update { current ->
                        current.copy(learnedStepLengthProfiles = LearnedStepLengthProfilesState.Available(profiles))
                    }
                }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                personalMetricsRepository.observeStepLengthCalibrations(br.com.mwconsulting.championcompanion.core.model.WorkoutType.RUNNING),
                personalMetricsRepository.observeStepLengthCalibrations(br.com.mwconsulting.championcompanion.core.model.WorkoutType.WALKING),
            ) { running, walking -> (running + walking).sortedByDescending { it.updatedAt } }
                .catch { mutableState.update { it.copy(calibrationHistory = emptyList()) } }
                .collect { history -> mutableState.update { it.copy(calibrationHistory = history) } }
        }
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> Unit
            is ProfileIntent.HeightChanged -> {
                hasLocalHeightInput = true
                mutableState.update { current ->
                    current.copy(heightInput = intent.value, message = null)
                }
            }

            ProfileIntent.SaveHeight -> saveHeight()
            is ProfileIntent.DailyStepsGoalChanged -> {
                hasLocalDailyStepsGoalInput = true
                mutableState.update { current ->
                    current.copy(dailyStepsGoalInput = intent.value, message = null)
                }
            }

            ProfileIntent.SaveDailyStepsGoal -> saveDailyStepsGoal()
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
            is ProfileIntent.RemoveCalibration -> removeCalibration(intent.activityId)
            is ProfileIntent.StartCalibrationCorrection -> mutableState.update {
                it.copy(
                    correctionActivityId = intent.calibration.activityId,
                    correctionDistanceInput = (intent.calibration.referenceDistanceMeters / 1_000.0).toString(),
                )
            }
            is ProfileIntent.CalibrationCorrectionChanged -> mutableState.update { it.copy(correctionDistanceInput = intent.value) }
            ProfileIntent.SaveCalibrationCorrection -> saveCalibrationCorrection()
            ProfileIntent.CancelCalibrationCorrection -> mutableState.update {
                it.copy(correctionActivityId = null, correctionDistanceInput = "")
            }
        }
    }

    private fun saveCalibrationCorrection() {
        val current = mutableState.value
        val activityId = current.correctionActivityId ?: return
        val meters = current.correctionDistanceInput.trim().replace(',', '.').toDoubleOrNull()
            ?.let { it * 1_000.0 }?.takeIf { it.isFinite() && it > 0.0 } ?: run {
                mutableState.update { it.copy(message = ProfileMessage.INVALID_STEP_LENGTH) }
                return
            }
        val calibration = current.calibrationHistory.firstOrNull { it.activityId == activityId } ?: return
        viewModelScope.launch {
            when (personalMetricsRepository.updateStepLengthCalibrationEvidence(
                br.com.mwconsulting.championcompanion.core.model.UpdateStepLengthCalibrationEvidenceInput(
                    activityId = activityId,
                    referenceDistanceMeters = meters,
                    decodedSteps = calibration.decodedSteps,
                ),
            )) {
                is RepositoryResult.Success -> mutableState.update {
                    it.copy(correctionActivityId = null, correctionDistanceInput = "")
                }
                else -> mutableState.update { it.copy(message = ProfileMessage.SAVE_FAILED) }
            }
        }
    }

    private fun removeCalibration(activityId: br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId) {
        viewModelScope.launch {
            when (personalMetricsRepository.removeStepLengthCalibration(activityId)) {
                is RepositoryResult.Success -> Unit
                else -> mutableState.update { it.copy(message = ProfileMessage.SAVE_FAILED) }
            }
        }
    }

    private fun saveHeight() {
        if (mutableState.value.isSaving) return
        val rawInput = mutableState.value.heightInput.trim()
        val heightCentimeters = parseOptionalHeightCentimeters(rawInput)
        if (rawInput.isNotEmpty() && heightCentimeters == null) {
            mutableState.update { current ->
                current.copy(message = ProfileMessage.INVALID_HEIGHT)
            }
            return
        }
        saveHeight(heightCentimeters)
    }

    private fun saveHeight(heightCentimeters: Double?) {
        viewModelScope.launch {
            mutableState.update { current -> current.copy(isSaving = true, message = null) }
            val result = try {
                personalMetricsRepository.updateHeight(
                    UpdateHeightInput(
                        heightCentimeters = heightCentimeters,
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
                    hasLocalHeightInput = false
                    mutableState.update { current ->
                        current.copy(
                            isSaving = false,
                            heightCentimeters = result.value.heightCentimeters,
                            heightInput = result.value.heightCentimeters?.let(::formatHeightCentimeters).orEmpty(),
                            heightSuggestionMeters = StepLengthSuggestionPolicy.fromHeight(
                                result.value.heightCentimeters,
                            ),
                            message = ProfileMessage.HEIGHT_SAVED,
                        )
                    }
                    triggerHealthConnectSyncIfEnabled()
                }

                RepositoryResult.NotFound -> showSaveFailure(ProfileMessage.PROFILE_UNAVAILABLE)
                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                RepositoryResult.StorageUnavailable,
                -> showSaveFailure(ProfileMessage.HEIGHT_SAVE_FAILED)
            }
        }
    }

    private fun saveDailyStepsGoal() {
        if (mutableState.value.isSaving) return
        val goal = parseDailyStepsGoal(mutableState.value.dailyStepsGoalInput)
        if (goal == null) {
            mutableState.update { current ->
                current.copy(message = ProfileMessage.INVALID_DAILY_STEPS_GOAL)
            }
            return
        }
        viewModelScope.launch {
            mutableState.update { current -> current.copy(isSaving = true, message = null) }
            val result = try {
                appPreferencesRepository.updateDailyStepsGoal(goal)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RepositoryResult.StorageUnavailable
            }
            when (result) {
                is RepositoryResult.Success -> {
                    hasLocalDailyStepsGoalInput = false
                    mutableState.update { current ->
                        current.copy(
                            isSaving = false,
                            dailyStepsGoal = goal,
                            dailyStepsGoalInput = goal.toString(),
                            message = ProfileMessage.DAILY_STEPS_GOAL_SAVED,
                        )
                    }
                }

                else -> showSaveFailure(ProfileMessage.DAILY_STEPS_GOAL_SAVE_FAILED)
            }
        }
    }

    private suspend fun triggerHealthConnectSyncIfEnabled() {
        try {
            if (appPreferencesRepository.observe().first().healthSyncEnabled) {
                healthConnectSyncCoordinator.syncPending()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Falha remota ou do provider não desfaz sucesso local do perfil.
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
                                ?.let(::formatStepLengthInput)
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

internal fun parseOptionalHeightCentimeters(
    input: String,
    locale: Locale = Locale.getDefault(),
): Double? = input.trim().takeIf(String::isNotEmpty)?.let { parseStepLengthMeters(it, locale) }

internal fun parseDailyStepsGoal(input: String): Int? = input.trim()
    .takeIf { it.matches(Regex("[+]?[0-9]+")) }
    ?.toLongOrNull()
    ?.takeIf { it in 1..Int.MAX_VALUE }
    ?.toInt()
    ?.let(DailySummaryPolicy::sanitizeDailyStepsGoal)

internal fun formatHeightCentimeters(
    value: Double,
    locale: Locale = Locale.getDefault(),
): String = formatDecimal(value, locale)

internal fun formatStepLengthInput(
    value: Double,
    locale: Locale = Locale.getDefault(),
): String = formatDecimal(value, locale)

internal fun formatStepLengthMeters(
    value: Double,
    locale: Locale = Locale.getDefault(),
): String {
    val plain = BigDecimal.valueOf(value)
        .setScale(4, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (decimalSeparator == '.') plain else plain.replace('.', decimalSeparator)
}

private fun formatDecimal(
    value: Double,
    locale: Locale,
): String {
    val plain = BigDecimal.valueOf(value)
        .stripTrailingZeros()
        .toPlainString()
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (decimalSeparator == '.') plain else plain.replace('.', decimalSeparator)
}
