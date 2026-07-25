package br.com.mwconsulting.championcompanion.feature.settings

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectWritePermission
import java.time.Instant

interface SettingsFeatureContract : UiStateStore<SettingsUiState> {
    fun onIntent(intent: SettingsIntent)
}

data class SettingsUiState(
    val availability: SettingsAvailability = SettingsAvailability.UNAVAILABLE,
    val healthSyncEnabled: Boolean = false,
    val providerPackageName: String = "com.google.android.apps.healthdata",
    val permissionGroups: List<HealthConnectPermissionGroupUiModel> = emptyList(),
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val syncingCount: Int = 0,
    val syncedCount: Int = 0,
    val lastSyncedAt: Instant? = null,
    val isSyncNowRunning: Boolean = false,
    val syncStatusMessage: String? = null,
)

enum class SettingsAvailability {
    AVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    UNAVAILABLE,
}

data class HealthConnectPermissionGroupUiModel(
    val title: String,
    val permissions: Set<HealthConnectWritePermission>,
    val isGranted: Boolean,
    val isRelevant: Boolean = true,
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data class ToggleHealthSync(val enabled: Boolean) : SettingsIntent
    data object SyncNow : SettingsIntent
    data object RefreshHealthConnectState : SettingsIntent
    data class PermissionsUpdated(val grantedPermissions: Set<String>) : SettingsIntent
    data object DismissStatus : SettingsIntent
}
