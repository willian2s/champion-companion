package br.com.mwconsulting.championcompanion.feature.device

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val deviceDiagnosticsInstantFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

private fun quoteJson(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

data class DeviceDiagnosticsEnvironmentSnapshot(
    val appVersion: String,
    val androidApiLevel: Int,
    val androidRelease: String,
    val handsetModel: String,
    val bleSupported: Boolean,
    val bluetoothEnabled: Boolean,
    val grantedPermissions: List<String>,
    val missingPermissions: List<String>,
)

data class DeviceDiagnosticsAssociatedDeviceReport(
    val modelName: String,
    val safeIdentifier: String,
    val associatedAt: String,
    val lastSuccessfulConnectionAt: String?,
    val lastImportedAt: String?,
)

data class DeviceDiagnosticsReport(
    val schemaVersion: Int,
    val capturedAt: String,
    val appVersion: String,
    val androidApiLevel: Int,
    val androidRelease: String,
    val handsetModel: String,
    val bleSupported: Boolean,
    val bluetoothEnabled: Boolean,
    val grantedPermissions: List<String>,
    val missingPermissions: List<String>,
    val screenState: String,
    val connectionState: String,
    val activeOperation: String?,
    val retryAction: String?,
    val candidateCount: Int,
    val selectedCandidatePresent: Boolean,
    val associatedDevice: DeviceDiagnosticsAssociatedDeviceReport?,
    val lastErrorCategory: String?,
    val lastErrorRecoverable: Boolean,
) {
    fun asJson(): String = buildString {
        append("{\n")
        field("schema_version", schemaVersion.toString(), quoteValue = false)
        field("captured_at", capturedAt)
        field("app_version", appVersion)
        field("android_api_level", androidApiLevel.toString(), quoteValue = false)
        field("android_release", androidRelease)
        field("handset_model", handsetModel)
        field("ble_supported", bleSupported.toString(), quoteValue = false)
        field("bluetooth_enabled", bluetoothEnabled.toString(), quoteValue = false)
        appendStringList("granted_permissions", grantedPermissions)
        appendStringList("missing_permissions", missingPermissions)
        field("screen_state", screenState)
        field("connection_state", connectionState)
        field("active_operation", activeOperation?.let(::quoteJson) ?: "null", quoteValue = false)
        field("retry_action", retryAction?.let(::quoteJson) ?: "null", quoteValue = false)
        field("candidate_count", candidateCount.toString(), quoteValue = false)
        field("selected_candidate_present", selectedCandidatePresent.toString(), quoteValue = false)
        appendAssociatedDevice(associatedDevice)
        field("last_error_category", lastErrorCategory?.let(::quoteJson) ?: "null", quoteValue = false)
        field("last_error_recoverable", lastErrorRecoverable.toString(), quoteValue = false, last = true)
        append("}\n")
    }

    private fun StringBuilder.appendAssociatedDevice(device: DeviceDiagnosticsAssociatedDeviceReport?) {
        append("  \"associated_device\": ")
        if (device == null) {
            append("null,\n")
            return
        }
        append("{\n")
        append("    \"model_name\": ").append(quoteJson(device.modelName)).append(",\n")
        append("    \"safe_identifier\": ").append(quoteJson(device.safeIdentifier)).append(",\n")
        append("    \"associated_at\": ").append(quoteJson(device.associatedAt)).append(",\n")
        append("    \"last_successful_connection_at\": ")
            .append(device.lastSuccessfulConnectionAt?.let(::quoteJson) ?: "null")
            .append(",\n")
        append("    \"last_imported_at\": ")
            .append(device.lastImportedAt?.let(::quoteJson) ?: "null")
            .append("\n")
        append("  },\n")
    }

    private fun StringBuilder.appendStringList(name: String, values: List<String>) {
        append("  \"").append(name).append("\": [")
        values.joinTo(this, separator = ", ") { quoteJson(it) }
        append("],\n")
    }

    private fun StringBuilder.field(name: String, value: String, quoteValue: Boolean = true, last: Boolean = false) {
        append("  \"").append(name).append("\": ")
        append(if (quoteValue) quoteJson(value) else value)
        append(if (last) "\n" else ",\n")
    }

    companion object {
        const val SCHEMA_VERSION = 1

        fun from(
            state: DeviceManagementUiState,
            environment: DeviceDiagnosticsEnvironmentSnapshot,
            capturedAt: Instant = Instant.now(),
        ): DeviceDiagnosticsReport = DeviceDiagnosticsReport(
            schemaVersion = SCHEMA_VERSION,
            capturedAt = deviceDiagnosticsInstantFormatter.format(capturedAt),
            appVersion = environment.appVersion,
            androidApiLevel = environment.androidApiLevel,
            androidRelease = environment.androidRelease,
            handsetModel = environment.handsetModel,
            bleSupported = environment.bleSupported,
            bluetoothEnabled = environment.bluetoothEnabled,
            grantedPermissions = environment.grantedPermissions,
            missingPermissions = environment.missingPermissions,
            screenState = state.screenState.reportName(),
            connectionState = state.connectionState.reportName(),
            activeOperation = state.activeOperation?.reportName(),
            retryAction = state.retryAction?.reportName(),
            candidateCount = state.candidates.size,
            selectedCandidatePresent = state.selectedCandidateId != null,
            associatedDevice = state.associatedDevice?.toDiagnosticsReport(),
            lastErrorCategory = state.error?.category?.reportName(),
            lastErrorRecoverable = state.error?.isRecoverable == true,
        )
    }
}

private fun DeviceManagementAssociatedDevice.toDiagnosticsReport(): DeviceDiagnosticsAssociatedDeviceReport =
    DeviceDiagnosticsAssociatedDeviceReport(
        modelName = modelName,
        safeIdentifier = safeIdentifier,
        associatedAt = deviceDiagnosticsInstantFormatter.format(associatedAt),
        lastSuccessfulConnectionAt = lastSuccessfulConnectionAt?.let(deviceDiagnosticsInstantFormatter::format),
        lastImportedAt = lastImportedAt?.let(deviceDiagnosticsInstantFormatter::format),
    )

private fun DeviceManagementScreenState.reportName(): String = when (this) {
    DeviceManagementScreenState.Loading -> "loading"
    DeviceManagementScreenState.NoAssociatedDevice -> "no_associated_device"
    DeviceManagementScreenState.PermissionsRequired -> "permissions_required"
    DeviceManagementScreenState.BluetoothDisabled -> "bluetooth_disabled"
    DeviceManagementScreenState.ReadyToScan -> "ready_to_scan"
    DeviceManagementScreenState.Scanning -> "scanning"
    DeviceManagementScreenState.ScanResults -> "scan_results"
    DeviceManagementScreenState.Connecting -> "connecting"
    DeviceManagementScreenState.PreparingConnection -> "preparing_connection"
    DeviceManagementScreenState.Connected -> "connected"
    DeviceManagementScreenState.Disconnecting -> "disconnecting"
    DeviceManagementScreenState.Disconnected -> "disconnected"
    DeviceManagementScreenState.DeviceNotFound -> "device_not_found"
    DeviceManagementScreenState.ConnectionFailed -> "connection_failed"
    DeviceManagementScreenState.Forgetting -> "forgetting"
    DeviceManagementScreenState.Error -> "error"
}

private fun DeviceManagementConnectionState.reportName(): String = when (this) {
    DeviceManagementConnectionState.DISCONNECTED -> "disconnected"
    DeviceManagementConnectionState.SEARCHING_REMEMBERED_DEVICE -> "searching_remembered_device"
    DeviceManagementConnectionState.SCANNING -> "scanning"
    DeviceManagementConnectionState.CONNECTING -> "connecting"
    DeviceManagementConnectionState.PREPARING_CONNECTION -> "preparing_connection"
    DeviceManagementConnectionState.CONNECTED -> "connected"
    DeviceManagementConnectionState.DISCONNECTING -> "disconnecting"
    DeviceManagementConnectionState.FAILED -> "failed"
}

private fun DeviceManagementActiveOperation.reportName(): String = when (this) {
    DeviceManagementActiveOperation.REQUESTING_PERMISSIONS -> "requesting_permissions"
    DeviceManagementActiveOperation.SCANNING -> "scanning"
    DeviceManagementActiveOperation.CONNECTING -> "connecting"
    DeviceManagementActiveOperation.DISCONNECTING -> "disconnecting"
    DeviceManagementActiveOperation.FORGETTING -> "forgetting"
    DeviceManagementActiveOperation.OPENING_DIAGNOSTICS -> "opening_diagnostics"
    DeviceManagementActiveOperation.EXPORTING_DIAGNOSTICS -> "exporting_diagnostics"
}

private fun DeviceManagementRetryAction.reportName(): String = when (this) {
    DeviceManagementRetryAction.RequestPermissions -> "request_permissions"
    DeviceManagementRetryAction.EnableBluetooth -> "enable_bluetooth"
    DeviceManagementRetryAction.RestartScan -> "restart_scan"
    DeviceManagementRetryAction.ReconnectAssociatedDevice -> "reconnect_associated_device"
    DeviceManagementRetryAction.RetrySelectedCandidateConnection -> "retry_selected_candidate_connection"
    DeviceManagementRetryAction.RetryHandshake -> "retry_handshake"
    DeviceManagementRetryAction.RetryDisconnect -> "retry_disconnect"
}

private fun DeviceManagementErrorCategory.reportName(): String = when (this) {
    DeviceManagementErrorCategory.PREREQUISITES -> "prerequisites"
    DeviceManagementErrorCategory.SCAN -> "scan"
    DeviceManagementErrorCategory.CONNECTION -> "connection"
    DeviceManagementErrorCategory.HANDSHAKE -> "handshake"
    DeviceManagementErrorCategory.DISCONNECT -> "disconnect"
    DeviceManagementErrorCategory.FORGET -> "forget"
    DeviceManagementErrorCategory.DIAGNOSTICS -> "diagnostics"
    DeviceManagementErrorCategory.INTERNAL -> "internal"
}
