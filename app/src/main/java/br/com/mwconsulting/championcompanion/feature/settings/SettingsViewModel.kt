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
import kotlinx.coroutines.CancellationException
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

    fun allRequestedPermissions(): Set<String> = mutableState.value.permissionGroups
        .flatMap { group -> group.permissions.map(HealthConnectWritePermission::rawValue) }
        .toSet()

    fun requestedPermissions(): Set<String> = mutableState.value.permissionGroups
        .filter { !it.isGranted }
        .flatMap { group -> group.permissions.map(HealthConnectWritePermission::rawValue) }
        .toSet()

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
            try {
                syncStatusMessage.value = runExplicitSyncNow()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                syncStatusMessage.value = "Não foi possível concluir a sincronização explícita."
            } finally {
                isSyncNowRunning.value = false
                refreshHealthConnectState()
            }
        }
    }

    private suspend fun runExplicitSyncNow(): String {
        var acceptedWrites = 0
        var syncedOperations = 0
        var failedOperations = 0
        var queuePasses = 0
        var lastQueueOutcome: HealthConnectSyncOutcome? = null

        while (queuePasses < MAX_SYNC_NOW_QUEUE_PASSES) {
            val before = healthSyncRepository.observeSyncSummary().first()
            val outcome = healthConnectSyncCoordinator.syncPending()
            lastQueueOutcome = outcome
            queuePasses += 1
            acceptedWrites += outcome.report.writeAcceptedWithoutReadbackCount
            syncedOperations += outcome.report.syncedCount
            failedOperations += outcome.report.failedCount

            when (outcome) {
                is HealthConnectSyncOutcome.PermissionRequired -> return outcome.toImmediateMessage()
                is HealthConnectSyncOutcome.ProviderUnavailable -> return outcome.toImmediateMessage()
                is HealthConnectSyncOutcome.Disabled -> return outcome.toImmediateMessage()
                is HealthConnectSyncOutcome.Failed -> return outcome.toImmediateMessage()
                is HealthConnectSyncOutcome.PlausibilityAcceptanceRequired,
                is HealthConnectSyncOutcome.PlausibilityBlocked,
                -> break

                is HealthConnectSyncOutcome.Completed,
                is HealthConnectSyncOutcome.CompletedWithFailures,
                is HealthConnectSyncOutcome.NothingToSync,
                -> Unit
            }

            val after = healthSyncRepository.observeSyncSummary().first()
            val hasRemainingQueue = after.pendingCount > 0 || after.failedCount > 0 || after.syncingCount > 0
            val madeProgress = after.syncedCount > before.syncedCount ||
                after.pendingCount < before.pendingCount ||
                after.failedCount < before.failedCount ||
                outcome.report.writeAcceptedWithoutReadbackCount > 0
            val hardFailureWithoutProgress = outcome.report.failedCount > 0 &&
                outcome.report.syncedCount == 0 &&
                outcome.report.writeAcceptedWithoutReadbackCount == 0
            if (!hasRemainingQueue || !madeProgress || hardFailureWithoutProgress) break
        }

        val readyActivities = workoutActivityRepository
            .observeActivities(WorkoutActivityFilter.READY_TO_SYNC)
            .first()
            .filter { it.activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 }
            .sortedBy { it.metrics.startedAt }

        var readyProcessed = 0
        var readyCompleted = 0
        var readyBlocked = 0
        for (activity in readyActivities) {
            val outcome = healthConnectSyncCoordinator.syncActivity(activity.id)
            readyProcessed += 1
            acceptedWrites += outcome.report.writeAcceptedWithoutReadbackCount
            syncedOperations += outcome.report.syncedCount
            failedOperations += outcome.report.failedCount
            when (outcome) {
                is HealthConnectSyncOutcome.Completed,
                is HealthConnectSyncOutcome.NothingToSync,
                -> readyCompleted += 1

                is HealthConnectSyncOutcome.CompletedWithFailures,
                is HealthConnectSyncOutcome.PermissionRequired,
                is HealthConnectSyncOutcome.PlausibilityAcceptanceRequired,
                is HealthConnectSyncOutcome.PlausibilityBlocked,
                is HealthConnectSyncOutcome.ProviderUnavailable,
                is HealthConnectSyncOutcome.Disabled,
                is HealthConnectSyncOutcome.Failed,
                -> readyBlocked += 1
            }
        }

        val remaining = healthSyncRepository.observeSyncSummary().first()
        return buildString {
            if (acceptedWrites > 0) {
                append("Escrita local aceita para $acceptedWrites operação(ões)")
                if (queuePasses > 1) append(" em $queuePasses passes de fila")
                append(". ")
            } else if (syncedOperations > 0) {
                append("Sincronização local concluiu $syncedOperations operação(ões). ")
            } else {
                append(lastQueueOutcome?.toImmediateMessage() ?: "Nenhuma operação de fila exigiu escrita remota. ")
                if (!endsWith(" ")) append(' ')
            }

            if (readyProcessed > 0) {
                append("Workouts READY_TO_SYNC: $readyCompleted concluído(s)")
                if (readyBlocked > 0) append(", $readyBlocked bloqueado(s)/com falha")
                append(". ")
            }
            if (failedOperations > 0 || remaining.failedCount > 0) {
                append("Há ${remaining.failedCount} operação(ões) com falha recuperável. ")
            }
            if (remaining.pendingCount > 0 || remaining.syncingCount > 0) {
                append("Ainda restam ${remaining.pendingCount + remaining.syncingCount} operação(ões) locais para nova tentativa. ")
            }
            append("O provider não foi lido; escritas aceitas não confirmam records visíveis por readback.")
        }
    }

    private fun HealthConnectSyncOutcome.toImmediateMessage(): String = when (this) {
        is HealthConnectSyncOutcome.Completed -> if (report.writeAcceptedWithoutReadbackCount > 0) {
            "Escrita local aceita para ${report.writeAcceptedWithoutReadbackCount} operação(ões)."
        } else {
            "Sincronização concluída na fila local."
        }
        is HealthConnectSyncOutcome.NothingToSync -> "Nenhum dado pendente na fila local."
        is HealthConnectSyncOutcome.CompletedWithFailures -> {
            val failures = failures
                .groupBy { it.recordType }
                .entries
                .joinToString("; ") { (recordType, records) ->
                    "${recordType.toFriendlyLabel()}: ${records.first().failureKind.toFriendlyLabel()}"
                }
            "Sincronização parcial local. Falhas: ${failures.ifBlank { "não classificadas" }}."
        }
        is HealthConnectSyncOutcome.PermissionRequired ->
            "Permissão pendente para: ${missingPermissions.joinToString { it.toFriendlyLabel() }}. Autorize no provider e tente novamente."
        is HealthConnectSyncOutcome.PlausibilityAcceptanceRequired ->
            "Há treino do relógio aguardando aceite explícito de plausibilidade antes da preparação Health Connect."
        is HealthConnectSyncOutcome.PlausibilityBlocked ->
            "Projeção Health Connect bloqueada por indicadores ${indicators.joinToString()}."
        is HealthConnectSyncOutcome.ProviderUnavailable -> if (updateRequired) {
            "Atualize o Health Connect para continuar a sincronização."
        } else {
            "Health Connect indisponível neste dispositivo."
        }
        is HealthConnectSyncOutcome.Disabled -> "Ative a integração para sincronizar agora."
        is HealthConnectSyncOutcome.Failed -> reason
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
        if (speedProjectionEligible || HealthConnectWritePermission.SPEED in granted) {
            add(permissionGroup("Velocidade", setOf(HealthConnectWritePermission.SPEED), granted))
        }
        add(permissionGroup("Calorias", setOf(HealthConnectWritePermission.TOTAL_CALORIES_BURNED), granted))
        if (
            (metrics.weightKilograms != null && metrics.latestWeightMeasuredAt != null) ||
            HealthConnectWritePermission.WEIGHT in granted
        ) {
            add(permissionGroup("Peso", setOf(HealthConnectWritePermission.WEIGHT), granted))
        }
        if (
            metrics.heightCentimeters?.let { it.isFinite() && it > 0.0 } == true ||
            HealthConnectWritePermission.HEIGHT in granted
        ) {
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

    private companion object {
        const val MAX_SYNC_NOW_QUEUE_PASSES: Int = 20
    }
}
