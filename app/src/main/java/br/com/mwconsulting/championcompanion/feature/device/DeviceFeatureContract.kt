package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import java.util.UUID

@JvmInline
value class DevicePocCycleId(val value: UUID) {
    companion object {
        fun random(): DevicePocCycleId = DevicePocCycleId(UUID.randomUUID())
    }
}

sealed interface DevicePocState {
    data object Idle : DevicePocState
    data object PermissionsRequired : DevicePocState
    data object ReadyToScan : DevicePocState
    data object Scanning : DevicePocState
    data object DeviceFound : DevicePocState
    data object Connecting : DevicePocState
    data object TransportReady : DevicePocState
    data object Handshaking : DevicePocState
    data object HandshakeConfirmed : DevicePocState
    data object WaitingForSteps : DevicePocState
    data object StepsObserved : DevicePocState
    data object StartingHeartRate : DevicePocState
    data object MeasuringHeartRate : DevicePocState
    data object StoppingHeartRate : DevicePocState
    data object CycleCompleted : DevicePocState
    data object Disconnecting : DevicePocState
    data class Failed(val error: ChampionBleError) : DevicePocState
}

data class DeviceUiState(
    val cycleId: DevicePocCycleId = DevicePocCycleId.random(),
    val transportState: ChampionBleState = ChampionBleState.Disconnected,
    val pocState: DevicePocState = DevicePocState.Idle,
    val candidate: ChampionDiscoveredDevice? = null,
    val activeSessionId: ChampionSessionId? = null,
    val isStepsCapabilityAvailable: Boolean = false,
    val isStepsCapabilityMissing: Boolean = false,
    val hasObservedSteps: Boolean = false,
    val stepsPacketCount: Int = 0,
    val lastStepCount: Int? = null,
    val lastError: ChampionBleError? = null,
)

interface DeviceFeatureContract : UiStateStore<DeviceUiState>
