package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattCharacteristicView
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoverySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattServiceView
import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.RecordedChampionPacketSource
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionTransportSessionTest {
    @Test
    fun `stored discovery capability resolver resolves mapped snapshot and fails when missing`() = runBlocking {
        val sessionId = testSessionId()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        lateinit var queue: ChampionGattQueue
        queue = ChampionGattQueue(
            sessionId = sessionId,
            parentScope = scope,
            operationExecutor = { operation ->
                scope.launch { queue.complete(operation.sessionId, operation.operationId) }
            },
        )
        val resolver = ChampionStoredDiscoveryCapabilityResolver { requestedSessionId ->
            if (requestedSessionId != sessionId) return@ChampionStoredDiscoveryCapabilityResolver null
            AndroidGattDiscoverySnapshot(
                services = listOf(
                    AndroidGattServiceView(
                        uuid = ChampionUuidCatalog.serviceUuid(ChampionServiceAlias.F008),
                        characteristics = listOf(
                            AndroidGattCharacteristicView(
                                uuid = ChampionUuidCatalog.channelUuid(ChampionChannelAlias.F008_NOTIFY),
                                properties = setOf(ChampionChannelProperty.NOTIFY),
                            ),
                            AndroidGattCharacteristicView(
                                uuid = ChampionUuidCatalog.channelUuid(ChampionChannelAlias.F008_WRITE),
                                properties = setOf(ChampionChannelProperty.WRITE),
                            ),
                        ),
                    ),
                ),
            )
        }

        val resolved = resolver.resolve(testSession(), queue)
        val capabilities = (resolved as ChampionBleResult.Success).value
        assertTrue(capabilities.snapshot.isTransportReady())

        val missing = resolver.resolve(testSession(seed = 2), queue)
        assertEquals(ChampionBleError.DiscoveryFailed(), (missing as ChampionBleResult.Failure).error)
        scope.coroutineContext[Job]?.cancel()
        Unit
    }

    @Test
    fun `transport session executes command through queue and awaits response`() = runBlocking {
        val session = testSession()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val packetSource = RecordedChampionPacketSource()
        lateinit var queue: ChampionGattQueue
        val executedOperations = mutableListOf<ChampionGattOperation>()
        queue = ChampionGattQueue(
            sessionId = session.id,
            parentScope = scope,
            operationExecutor = { operation ->
                executedOperations += operation
                scope.launch {
                    queue.complete(operation.sessionId, operation.operationId)
                }
            },
        )
        val notifications = ChampionNotificationHub(
            sessionCapabilities = sessionCapabilities(
                session.id,
                ChampionChannelAlias.F008_NOTIFY,
                ChampionChannelAlias.F008_WRITE,
            ),
            queue = queue,
            sessionEvents = MutableSharedFlow(),
            scope = scope,
        )
        val streams = ChampionTransportStreams(packetSource.packets, scope)
        val transport = ChampionTransportSession(
            session = session,
            capabilities = sessionCapabilities(
                session.id,
                ChampionChannelAlias.F008_NOTIFY,
                ChampionChannelAlias.F008_WRITE,
            ),
            queue = queue,
            notifications = notifications,
            streams = streams,
            internalJobs = emptyList(),
        )
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_WRITE,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 1.seconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = ChampionResponseMatcher { packet ->
                packet.channel == ChampionChannelAlias.F008_NOTIFY && packet.bytesCopy().last() == 0x01.toByte()
            },
            payload = byteArrayOf(0x11, 0x22, 0x33),
        )

        val result = async { transport.executeCommand(command) }
        awaitCondition { executedOperations.isNotEmpty() }
        assertEquals(ChampionOperationType.WRITE_CHARACTERISTIC, executedOperations.single().type)
        assertEquals(ChampionGattTarget.Channel(ChampionChannelAlias.F008_WRITE), executedOperations.single().target)
        assertArrayEquals(byteArrayOf(0x11, 0x22, 0x33), executedOperations.single().payloadCopy())

        val packet = ChampionPacket(
            sessionId = session.id,
            channel = ChampionChannelAlias.F008_NOTIFY,
            direction = ChampionPacketDirection.INBOUND,
            observedAt = Instant.now(),
            commandCode = 0xA1,
            bytes = byteArrayOf(0xA1.toByte(), 0x00, 0x01),
        )
        packetSource.emit(packet)

        assertEquals(ChampionBleResult.Success(packet), result.await())
        scope.coroutineContext[Job]?.cancel()
        Unit
    }

    @Test
    fun `transport session rejects command when channel capability is incompatible`() = runBlocking {
        val session = testSession()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        lateinit var queue: ChampionGattQueue
        queue = ChampionGattQueue(
            sessionId = session.id,
            parentScope = scope,
            operationExecutor = { operation ->
                scope.launch {
                    queue.complete(operation.sessionId, operation.operationId)
                }
            },
        )
        val notifications = ChampionNotificationHub(
            sessionCapabilities = sessionCapabilities(session.id, ChampionChannelAlias.F008_NOTIFY),
            queue = queue,
            sessionEvents = MutableSharedFlow(),
            scope = scope,
        )
        val transport = ChampionTransportSession(
            session = session,
            capabilities = sessionCapabilities(session.id, ChampionChannelAlias.F008_NOTIFY),
            queue = queue,
            notifications = notifications,
            streams = ChampionTransportStreams(RecordedChampionPacketSource().packets, scope),
            internalJobs = emptyList(),
        )
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_WRITE,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 500.milliseconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = null,
            payload = byteArrayOf(0x00),
        )

        val result = transport.sendCommand(command)
        assertEquals(
            ChampionBleResult.Failure(ChampionBleError.MissingRequiredChannel(ChampionChannelAlias.F008_WRITE)),
            result,
        )
        scope.coroutineContext[Job]?.cancel()
        Unit
    }

    @Test
    fun `transport session accepts with response command on write without response channel`() = runBlocking {
        val session = testSession()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        lateinit var queue: ChampionGattQueue
        val executedOperations = mutableListOf<ChampionGattOperation>()
        queue = ChampionGattQueue(
            sessionId = session.id,
            parentScope = scope,
            operationExecutor = { operation ->
                executedOperations += operation
                scope.launch {
                    queue.complete(operation.sessionId, operation.operationId)
                }
            },
        )
        val snapshot = ChampionCapabilitySnapshot(
            requiredChannels = setOf(ChampionChannelAlias.F008_NOTIFY, ChampionChannelAlias.F008_WRITE),
            availableChannels = setOf(ChampionChannelAlias.F008_NOTIFY, ChampionChannelAlias.F008_WRITE),
            channels = mapOf(
                ChampionChannelAlias.F008_NOTIFY to ChampionChannelCapability(
                    channel = ChampionChannelAlias.F008_NOTIFY,
                    properties = setOf(ChampionChannelProperty.NOTIFY),
                ),
                ChampionChannelAlias.F008_WRITE to ChampionChannelCapability(
                    channel = ChampionChannelAlias.F008_WRITE,
                    properties = setOf(ChampionChannelProperty.WRITE_WITHOUT_RESPONSE),
                ),
            ),
        )
        val capabilities = ChampionSessionCapabilities(
            sessionId = session.id,
            snapshot = snapshot,
            discoveredServices = setOf(ChampionServiceAlias.F008),
            optionalChannelsAvailable = emptySet(),
            defaultSubscribedChannels = setOf(ChampionChannelAlias.F008_NOTIFY),
        )
        val transport = ChampionTransportSession(
            session = session,
            capabilities = capabilities,
            queue = queue,
            notifications = ChampionNotificationHub(
                sessionCapabilities = capabilities,
                queue = queue,
                sessionEvents = MutableSharedFlow(),
                scope = scope,
            ),
            streams = ChampionTransportStreams(RecordedChampionPacketSource().packets, scope),
            internalJobs = emptyList(),
        )
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_WRITE,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 500.milliseconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = null,
            payload = byteArrayOf(0x00),
        )

        val result = transport.sendCommand(command)

        assertEquals(ChampionBleResult.Success(Unit), result)
        assertEquals(ChampionOperationType.WRITE_CHARACTERISTIC, executedOperations.single().type)
        scope.coroutineContext[Job]?.cancel()
        Unit
    }
}
