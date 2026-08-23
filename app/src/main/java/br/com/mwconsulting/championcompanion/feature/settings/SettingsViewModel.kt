package br.com.mwconsulting.championcompanion.feature.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectAvailability
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectGateway
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectFailureKind
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectPlatform
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncOutcome
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectPermissionResult
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectWritePermission
import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val personalMetricsRepository: PersonalMetricsRepository,
    private val healthSyncRepository: HealthSyncRepository,
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
    private val healthConnectPlatform: HealthConnectPlatform,
    private val healthConnectGateway: HealthConnectGateway,
    private val workoutActivityRepository: WorkoutActivityRepository,
) : ViewModel(), SettingsFeatureContract {
    private val grantedPermissions = MutableStateFlow<Set<HealthConnectWritePermission>>(emptySet())
    private val availability = MutableStateFlow(SettingsAvailability.UNAVAILABLE)
    private val syncStatusMessage = MutableStateFlow<String?>(null)
    private val isSyncNowRunning = MutableStateFlow(false)
    private val mutableState = MutableStateFlow(SettingsUiState(providerPackageName = healthConnectPlatform.providerPackageName))

    override val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasEligibleSpeedProjection = workoutActivityRepository
                .observeActivities(WorkoutActivityFilter.ALL)
                .map { activities ->
                    activities.any { activity ->
                        val snapshot = activity.derivationSnapshot
                        activity.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
                            activity.confirmedType != WorkoutType.CYCLING &&
                            snapshot?.isEligibleForHealthProjection == true &&
                            snapshot.canonicalSeries?.speedSamples?.isNotEmpty() == true
                    }
                }
                .distinctUntilChanged()
            val coreFlow = combine(
                appPreferencesRepository.observe(),
                personalMetricsRepository.observeSnapshot(),
                healthSyncRepository.observeSyncSummary(),
                hasEligibleSpeedProjection,
            ) { preferences: AppPreferences, metrics: PersonalMetricsSnapshot, summary: HealthSyncSummary, speedProjectionEligible: Boolean ->
                SettingsCore(preferences, metrics, summary, speedProjectionEligible)
            }
            combine(coreFlow, grantedPermissions, availability, syncStatusMessage, isSyncNowRunning) { core, granted, currentAvailability, status, syncRunning ->
                val (preferences, metrics, summary, speedProjectionEligible) = core
                SettingsUiState(
                    availability = currentAvailability,
                    healthSyncEnabled = preferences.healthSyncEnabled,
                    providerPackageName = healthConnectPlatform.providerPackageName,
                    permissionGroups = buildPermissionGroups(metrics, granted, speedProjectionEligible),
                    pendingCount = summary.pendingCount,
                    failedCount = summary.failedCount,
                    syncingCount = summary.syncingCount,
                    syncedCount = summary.syncedCount,
                    lastSyncedAt = summary.lastSyncedAt,
                    isSyncNowRunning = syncRunning,
                    syncStatusMessage = status,
                )
            }.collect { mutableState.value = it }
        }
        refreshHealthConnectState()
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load,
            SettingsIntent.RefreshHealthConnectState,
            -> refreshHealthConnectState()

            is SettingsIntent.ToggleHealthSync -> toggleHealthSync(intent.enabled)
            SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.PermissionsUpdated -> permissionsUpdated(intent.grantedPermissions)
            SettingsIntent.DismissStatus -> syncStatusMessage.value = null
        }
    }

    fun requestedPermissions(): Set<String> = mutableState.value.permissionGroups
        .filter { !it.isGranted }
        .flatMap { group -> group.permissions.map(HealthConnectWritePermission::rawValue) }
        .toSet()

    fun createManageAccessIntent(): Intent = healthConnectPlatform.createManageDataIntent()

    fun createProviderUpdateIntent(): Intent = healthConnectPlatform.createProviderUpdateIntent()

    private fun toggleHealthSync(enabled: Boolean) {
        viewModelScope.launch {
            val current = appPreferencesRepository.observe().first()
            when (appPreferencesRepository.update(current.copy(healthSyncEnabled = enabled))) {
                is RepositoryResult.Success -> {
                    syncStatusMessage.value = if (enabled) {
                        "Integração com Health Connect ativada."
                    } else {
                        "Sincronização com Health Connect pausada."
                    }
                }

                else -> syncStatusMessage.value = "Não foi possível atualizar a preferência de sincronização."
            }
        }
    }

    private fun syncNow() {
        if (isSyncNowRunning.value) return
        viewModelScope.launch {
            isSyncNowRunning.value = true
            syncStatusMessage.value = null
            val message = when (val outcome = healthConnectSyncCoordinator.syncPending()) {
                is HealthConnectSyncOutcome.Completed -> if (outcome.report.writeAcceptedWithoutReadbackCount > 0) {
                    "Escrita local aceita para ${outcome.report.writeAcceptedWithoutReadbackCount} operação(ões). O provider não foi lido; isso não confirma records visíveis no Health Connect."
                } else {
                    "Sincronização concluída na fila local. Nenhum record remoto foi lido para confirmação."
                }
                is HealthConnectSyncOutcome.NothingToSync -> "Nenhum dado pendente na fila local. A integração write-only não confirma records por leitura."
                is HealthConnectSyncOutcome.CompletedWithFailures -> {
                    val failures = outcome.failures
                        .groupBy { it.recordType }
                        .entries
                        .joinToString("; ") { (recordType, records) ->
                            "${recordType.toFriendlyLabel()}: ${records.first().failureKind.toFriendlyLabel()}"
                        }
                    buildString {
                        append("Sincronização parcial local. Falhas: ")
                        append(failures.ifBlank { "não classificadas" })
                        append(". A atividade com filho falho permanece recuperável.")
                        if (outcome.report.writeAcceptedWithoutReadbackCount > 0) {
                            append(" Escritas aceitas não foram confirmadas por leitura do provider.")
                        }
                    }
                }
                is HealthConnectSyncOutcome.PermissionRequired -> "Permissão pendente para: ${outcome.missingPermissions.joinToString { it.toFriendlyLabel() }}. Autorize no provider e tente novamente."
                is HealthConnectSyncOutcome.PlausibilityAcceptanceRequired ->
                    "Há treino do relógio aguardando aceite explícito de plausibilidade antes da preparação Health Connect."
                is HealthConnectSyncOutcome.PlausibilityBlocked ->
                    "Projeção Health Connect bloqueada por indicadores ${outcome.indicators.joinToString()}."
                is HealthConnectSyncOutcome.ProviderUnavailable -> if (outcome.updateRequired) {
                    "Atualize o Health Connect para continuar a sincronização."
                } else {
                    "Health Connect indisponível neste dispositivo."
                }

                is HealthConnectSyncOutcome.Disabled -> "Ative a integração para sincronizar agora."
                is HealthConnectSyncOutcome.Failed -> outcome.reason
            }
            syncStatusMessage.value = message
            isSyncNowRunning.value = false
            refreshHealthConnectState()
        }
    }

    private fun permissionsUpdated(grantedPermissions: Set<String>) {
        this.grantedPermissions.value = grantedPermissions.mapNotNull(HealthConnectWritePermission::fromRaw).toSet()
        syncStatusMessage.value = "Permissões recebidas. Reconsultando o estado real do provider Health Connect."
        refreshHealthConnectState()
    }

    private fun refreshHealthConnectState() {
        viewModelScope.launch {
            val currentAvailability = runCatching { healthConnectPlatform.getAvailability() }.getOrNull()
            availability.value = when (currentAvailability) {
                HealthConnectAvailability.Available -> SettingsAvailability.AVAILABLE
                HealthConnectAvailability.ProviderUpdateRequired -> SettingsAvailability.PROVIDER_UPDATE_REQUIRED
                HealthConnectAvailability.Unavailable,
                null,
                -> SettingsAvailability.UNAVAILABLE
            }
            if (availability.value != SettingsAvailability.AVAILABLE) {
                grantedPermissions.value = emptySet()
                return@launch
            }
            when (val result = healthConnectGateway.getGrantedPermissions()) {
                is HealthConnectPermissionResult.Granted -> {
                    grantedPermissions.value = result.permissions
                }

                is HealthConnectPermissionResult.Failed -> {
                    grantedPermissions.value = emptySet()
                    availability.value = when (result.failure.kind) {
                        HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED -> SettingsAvailability.PROVIDER_UPDATE_REQUIRED
                        else -> SettingsAvailability.UNAVAILABLE
                    }
                    syncStatusMessage.value = result.failure.reason
                }
            }
        }
    }

    private fun buildPermissionGroups(
        metrics: PersonalMetricsSnapshot,
        granted: Set<HealthConnectWritePermission>,
        speedProjectionEligible: Boolean,
    ): List<HealthConnectPermissionGroupUiModel> = buildList {
        add(permissionGroup("Passos", setOf(HealthConnectWritePermission.STEPS), granted))
        add(permissionGroup("Frequência cardíaca", setOf(HealthConnectWritePermission.HEART_RATE), granted))
        add(permissionGroup("Exercícios", setOf(HealthConnectWritePermission.EXERCISE), granted))
        add(permissionGroup("Distância", setOf(HealthConnectWritePermission.DISTANCE), granted))
        if (speedProjectionEligible) {
            add(permissionGroup("Velocidade", setOf(HealthConnectWritePermission.SPEED), granted))
        }
        add(permissionGroup("Calorias", setOf(HealthConnectWritePermission.TOTAL_CALORIES_BURNED), granted))
        if (metrics.weightKilograms != null && metrics.latestWeightMeasuredAt != null) {
            add(permissionGroup("Peso", setOf(HealthConnectWritePermission.WEIGHT), granted))
        }
        if (metrics.heightCentimeters?.let { it.isFinite() && it > 0.0 } == true) {
            add(permissionGroup("Altura", setOf(HealthConnectWritePermission.HEIGHT), granted))
        }
    }

    private fun permissionGroup(
        title: String,
        permissions: Set<HealthConnectWritePermission>,
        granted: Set<HealthConnectWritePermission>,
    ) = HealthConnectPermissionGroupUiModel(
        title = title,
        permissions = permissions,
        isGranted = permissions.all { it in granted },
    )

    private fun HealthConnectWritePermission.toFriendlyLabel(): String = when (this) {
        HealthConnectWritePermission.STEPS -> "passos"
        HealthConnectWritePermission.HEART_RATE -> "frequência cardíaca"
        HealthConnectWritePermission.EXERCISE -> "exercícios"
        HealthConnectWritePermission.DISTANCE -> "distância"
        HealthConnectWritePermission.SPEED -> "velocidade"
        HealthConnectWritePermission.TOTAL_CALORIES_BURNED -> "calorias"
        HealthConnectWritePermission.WEIGHT -> "peso"
        HealthConnectWritePermission.HEIGHT -> "altura"
    }

    private fun br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.toFriendlyLabel(): String = when (this) {
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.STEPS -> "Passos"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.STEPS_CADENCE -> "Cadência de passos"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.HEART_RATE -> "Frequência cardíaca"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.EXERCISE_SESSION -> "Exercícios"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.DISTANCE -> "Distância"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.SPEED -> "Velocidade"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.TOTAL_CALORIES_BURNED -> "Calorias"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.WEIGHT -> "Peso"
        br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType.HEIGHT -> "Altura"
    }

    private fun HealthConnectFailureKind.toFriendlyLabel(): String = when (this) {
        HealthConnectFailureKind.PERMISSION_REQUIRED -> "permissão pendente"
        HealthConnectFailureKind.PROVIDER_UNAVAILABLE -> "provider indisponível"
        HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED -> "atualização do provider necessária"
        HealthConnectFailureKind.WRITE_REJECTED -> "escrita rejeitada pelo provider"
        HealthConnectFailureKind.INVALID_LOCAL_DATA -> "dados locais inválidos"
        HealthConnectFailureKind.INTERNAL -> "falha interna"
    }

    private data class SettingsCore(
        val preferences: AppPreferences,
        val metrics: PersonalMetricsSnapshot,
        val summary: HealthSyncSummary,
        val speedProjectionEligible: Boolean,
    )
}
