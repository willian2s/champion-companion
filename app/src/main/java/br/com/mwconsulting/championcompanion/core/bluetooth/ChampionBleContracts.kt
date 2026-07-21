package br.com.mwconsulting.championcompanion.core.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

enum class ChampionServiceAlias {
    F008,
    F002,
    FEE7,
}

enum class ChampionChannelAlias(
    val service: ChampionServiceAlias,
    val isRequiredForTransportReady: Boolean,
) {
    F008_NOTIFY(ChampionServiceAlias.F008, true),
    F008_WRITE(ChampionServiceAlias.F008, true),
    F002_NOTIFY(ChampionServiceAlias.F002, false),
    F002_WRITE(ChampionServiceAlias.F002, false),
    FEA1_NOTIFY(ChampionServiceAlias.FEE7, false),
    FEA2_INDICATE(ChampionServiceAlias.FEE7, false),
    FEC8_INDICATE(ChampionServiceAlias.FEE7, false),
}

enum class ChampionChannelProperty {
    READ,
    WRITE,
    WRITE_WITHOUT_RESPONSE,
    NOTIFY,
    INDICATE,
}

enum class ChampionOperationType {
    SCAN,
    CONNECT,
    DISCOVER_SERVICES,
    READ_CHARACTERISTIC,
    WRITE_CHARACTERISTIC,
    WRITE_DESCRIPTOR,
    REQUEST_MTU,
    CONFIGURE_NOTIFICATION,
    CONFIGURE_INDICATION,
    WAIT_FOR_RESPONSE,
    DISCONNECT,
}

data class ChampionPrerequisiteSnapshot(
    val isBleSupported: Boolean,
    val isBluetoothEnabled: Boolean,
    val isScanPermissionGranted: Boolean,
    val isConnectPermissionGranted: Boolean,
    val isLegacyPermissionGranted: Boolean,
    val isLocationPermissionGranted: Boolean,
    val isLocationPermissionRequired: Boolean,
)

data class ChampionChannelCapability(
    val channel: ChampionChannelAlias,
    val properties: Set<ChampionChannelProperty>,
)

data class ChampionCapabilitySnapshot(
    val requiredChannels: Set<ChampionChannelAlias>,
    val availableChannels: Set<ChampionChannelAlias>,
    val channels: Map<ChampionChannelAlias, ChampionChannelCapability>,
) {
    init {
        require(requiredChannels.all(channels::containsKey)) { "requiredChannels must exist in channels map" }
        require(availableChannels.all(channels::containsKey)) { "availableChannels must exist in channels map" }
    }

    fun hasChannel(channel: ChampionChannelAlias): Boolean = availableChannels.contains(channel)

    fun capabilityOf(channel: ChampionChannelAlias): ChampionChannelCapability? = channels[channel]

    fun isTransportReady(): Boolean = requiredChannels.all(availableChannels::contains)
}

data class ChampionScanRequest(
    val timeout: Duration,
    val namePrefix: String = "champion",
) {
    init {
        require(timeout.isPositive()) { "timeout must be positive" }
        require(namePrefix.isNotBlank()) { "namePrefix must not be blank" }
    }
}

data class ChampionAwaitPacketRequest(
    val sessionId: ChampionSessionId,
    val timeout: Duration,
    val matcher: ChampionResponseMatcher,
) {
    init {
        require(timeout.isPositive()) { "timeout must be positive" }
    }
}

sealed interface ChampionBleResult<out T> {
    data class Success<T>(val value: T) : ChampionBleResult<T>
    data class Failure(val error: ChampionBleError) : ChampionBleResult<Nothing>
}

interface ChampionBleTransport {
    val state: StateFlow<ChampionBleState>
    val packets: Flow<ChampionPacket>

    suspend fun scan(request: ChampionScanRequest): ChampionBleResult<List<ChampionDiscoveredDevice>>

    suspend fun connect(device: ChampionDiscoveredDevice): ChampionBleResult<ChampionSession>

    suspend fun send(sessionId: ChampionSessionId, command: ChampionCommand): ChampionBleResult<Unit>

    fun observeChannel(sessionId: ChampionSessionId, channel: ChampionChannelAlias): Flow<ChampionPacket>

    suspend fun awaitPacket(request: ChampionAwaitPacketRequest): ChampionBleResult<ChampionPacket>

    suspend fun disconnect(reason: ChampionDisconnectReason = ChampionDisconnectReason.USER_REQUESTED): ChampionBleResult<Unit>
}
