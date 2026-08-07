package br.com.mwconsulting.championcompanion.feature.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectAvailability
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectGateway
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectPlatform
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthRecordMapper
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncOutcome
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectWritePermission
import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRepository
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsSnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalMetricsRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.isSpeedSeries
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
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
    private val healthRecordMapper: HealthRecordMapper,
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
                        healthRecordMapper.findEligibleWorkoutProjectionChildren(activity)
                            .any { child -> child.isSpeedSeries }
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
                is HealthConnectSyncOutcome.Completed -> "Sincronização concluída com sucesso."
                is HealthConnectSyncOutcome.NothingToSync -> "Nenhum dado pendente para sincronizar."
                is HealthConnectSyncOutcome.CompletedWithFailures -> "Sincronização parcial concluída. ${outcome.failures.size} operação(ões) ainda exigem atenção."
                is HealthConnectSyncOutcome.PermissionRequired -> "Permissões do Health Connect ainda faltam para concluir a sincronização."
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
        syncStatusMessage.value = "Permissões do Health Connect atualizadas."
        refreshAvailability()
    }

    private fun refreshHealthConnectState() {
        viewModelScope.launch {
            refreshAvailability()
            grantedPermissions.value = healthConnectGateway.getGrantedPermissions()
        }
    }

    private fun refreshAvailability() {
        availability.value = when (healthConnectPlatform.getAvailability()) {
            HealthConnectAvailability.Available -> SettingsAvailability.AVAILABLE
            HealthConnectAvailability.ProviderUpdateRequired -> SettingsAvailability.PROVIDER_UPDATE_REQUIRED
            HealthConnectAvailability.Unavailable -> SettingsAvailability.UNAVAILABLE
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

    private data class SettingsCore(
        val preferences: AppPreferences,
        val metrics: PersonalMetricsSnapshot,
        val summary: HealthSyncSummary,
        val speedProjectionEligible: Boolean,
    )
}
