package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import java.time.Instant
import java.util.UUID

@JvmInline
value class DevicePocCycleId(val value: UUID) {
    companion object {
        fun random(): DevicePocCycleId = DevicePocCycleId(UUID.randomUUID())
    }
}

@JvmInline
value class DevicePocHeartRateSessionId(val value: UUID) {
    companion object {
        fun random(): DevicePocHeartRateSessionId = DevicePocHeartRateSessionId(UUID.randomUUID())
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

enum class DevicePocHeartRateSignalQuality {
    GOOD,
    MEDIUM,
    LOW,
}

enum class DevicePocHeartRateFinalSource {
    D0_CURRENT,
    F002_ESTIMATE,
    UNAVAILABLE,
}

data class DeviceUiState(
    val cycleId: DevicePocCycleId = DevicePocCycleId.random(),
    val transportState: ChampionBleState = ChampionBleState.Disconnected,
    val pocState: DevicePocState = DevicePocState.Idle,
    val candidate: ChampionDiscoveredDevice? = null,
    val activeSessionId: ChampionSessionId? = null,
    val isStepsCapabilityAvailable: Boolean = false,
    val isStepsCapabilityMissing: Boolean = false,
    val isHeartRateCapabilityAvailable: Boolean = false,
    val isHeartRateCapabilityMissing: Boolean = false,
    val hasObservedSteps: Boolean = false,
    val stepsPacketCount: Int = 0,
    val lastStepCount: Int? = null,
    val activeHeartRateSessionId: DevicePocHeartRateSessionId? = null,
    val heartRateStartedAt: Instant? = null,
    val d0PacketCount: Int = 0,
    val f002PacketCount: Int = 0,
    val f002BufferedSampleCount: Int = 0,
    val estimatedF002Bpm: Int? = null,
    val estimatedF002Quality: DevicePocHeartRateSignalQuality? = null,
    val estimatedF002QualityScore: Int = 0,
    val estimatedF002SampleRateHz: Double? = null,
    val estimatedF002PeakCount: Int = 0,
    val lastD0Bpm: Int? = null,
    val lastD0ObservedAt: Instant? = null,
    val finalHeartRateBpm: Int? = null,
    val finalHeartRateSource: DevicePocHeartRateFinalSource = DevicePocHeartRateFinalSource.UNAVAILABLE,
    val lastError: ChampionBleError? = null,
)

interface DeviceFeatureContract : UiStateStore<DeviceUiState>
