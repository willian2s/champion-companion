package br.com.mwconsulting.championcompanion.core.bluetooth

import br.com.mwconsulting.championcompanion.core.bluetooth.fakes.RecordedChampionPacketSource
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionBlePacketRouterTest {
    @Test
    fun `await command matches only same session and channel`() = runBlocking {
        val sessionId = testSessionId()
        val source = RecordedChampionPacketSource()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val streams = ChampionTransportStreams(source.packets, scope)
        val command = ChampionCommand(
            commandCode = 0xA1,
            channel = ChampionChannelAlias.F008_NOTIFY,
            writeType = ChampionWriteType.WITH_RESPONSE,
            timeout = 1.seconds,
            retryPolicy = ChampionRetryPolicy.None,
            responseMatcher = null,
            payload = byteArrayOf(0x00),
        )

        val awaited = async { streams.awaitCommand(sessionId, command) }
        yield()
        source.emit(testPacket(sessionId = testSessionId(2), channel = ChampionChannelAlias.F008_NOTIFY))
        source.emit(testPacket(sessionId = sessionId, channel = ChampionChannelAlias.F002_NOTIFY))
        val expected = testPacket(sessionId = sessionId, channel = ChampionChannelAlias.F008_NOTIFY)
        source.emit(expected)

        assertEquals(ChampionBleResult.Success(expected), awaited.await())
        scope.cancel()
    }

    @Test
    fun `observe channel emits packet and await packet times out or closes safely`() = runBlocking {
        val sessionId = testSessionId()
        val source = RecordedChampionPacketSource()
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val router = ChampionPacketRouter(source.packets, scope)
        val observed = async { router.observeChannel(sessionId, ChampionChannelAlias.FEA1_NOTIFY).first() }
        yield()
        val packet = testPacket(sessionId = sessionId, channel = ChampionChannelAlias.FEA1_NOTIFY)

        source.emit(packet)
        assertEquals(packet, observed.await())

        val awaited = async {
            router.awaitPacket(
                ChampionAwaitPacketRequest(
                    sessionId = sessionId,
                    timeout = 1.seconds,
                    matcher = ChampionResponseMatchers.byChannel(sessionId, ChampionChannelAlias.F008_NOTIFY),
                ),
            )
        }
        yield()
        val matched = testPacket(sessionId = sessionId, channel = ChampionChannelAlias.F008_NOTIFY)
        source.emit(matched)
        assertEquals(ChampionBleResult.Success(matched), awaited.await())

        val pending = async {
            router.awaitPacket(
                ChampionAwaitPacketRequest(
                    sessionId = sessionId,
                    timeout = 1.seconds,
                    matcher = ChampionResponseMatchers.bySession(sessionId),
                ),
            )
        }
        yield()
        launch { router.close(ChampionDisconnectReason.CLEANUP) }
        assertTrue(pending.await() is ChampionBleResult.Failure)
        assertTrue(source.recorded().contains(packet))
        scope.cancel()
    }
}
