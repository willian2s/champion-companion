package br.com.mwconsulting.championcompanion.feature.device

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceDiagnosticsReportTest {
    @Test
    fun `report exports only sanitized device management diagnostics`() {
        val report = DeviceDiagnosticsReport.from(
            state = DeviceManagementUiState(
                screenState = DeviceManagementScreenState.Connected,
                associatedDevice = DeviceManagementAssociatedDevice(
                    deviceId = DeviceManagementDeviceId("device-1"),
                    displayName = "Champion C001 Personal",
                    modelName = "Champion C001",
                    safeIdentifier = "**** 12AB",
                    firmware = "1.0.0",
                    associatedAt = Instant.parse("2026-07-22T10:00:00Z"),
                    lastSuccessfulConnectionAt = Instant.parse("2026-07-22T11:00:00Z"),
                    lastImportedAt = Instant.parse("2026-07-22T11:30:00Z"),
                ),
                selectedCandidateId = DeviceManagementCandidateId("raw-transport-key"),
                candidates = listOf(
                    DeviceManagementScanCandidate(
                        candidateId = DeviceManagementCandidateId("raw-transport-key"),
                        displayName = "Champion nearby",
                        safeIdentifier = "**** 12AB",
                        signalLabel = null,
                        isSelected = true,
                    ),
                ),
                connectionState = DeviceManagementConnectionState.CONNECTED,
                activeOperation = null,
                error = DeviceManagementError(
                    category = DeviceManagementErrorCategory.CONNECTION,
                    message = "Nao foi possivel conectar.",
                    isRecoverable = true,
                ),
                retryAction = DeviceManagementRetryAction.ReconnectAssociatedDevice,
            ),
            environment = DeviceDiagnosticsEnvironmentSnapshot(
                appVersion = "1.0-dev",
                androidApiLevel = 36,
                androidRelease = "16",
                handsetModel = "Google Pixel",
                bleSupported = true,
                bluetoothEnabled = true,
                grantedPermissions = listOf("BLUETOOTH_SCAN", "BLUETOOTH_CONNECT"),
                missingPermissions = emptyList(),
            ),
            capturedAt = Instant.parse("2026-07-22T12:00:00Z"),
        )

        val json = report.asJson().lowercase()

        assertTrue(json.contains("\"safe_identifier\": \"**** 12ab\""))
        assertTrue(json.contains("\"screen_state\": \"connected\""))
        assertFalse(json.contains("champion c001 personal"))
        assertFalse(json.contains("raw-transport-key"))
        listOf("payload", "bytes", "hex", "stacktrace", "mac", "bpm", "steps", "latitude", "longitude").forEach {
            assertFalse("forbidden term: $it", json.contains(it))
        }
    }
}
