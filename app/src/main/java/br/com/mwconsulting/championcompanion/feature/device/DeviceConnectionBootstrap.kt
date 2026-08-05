package br.com.mwconsulting.championcompanion.feature.device

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DeviceConnectionBootstrap @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
) {
    suspend fun connectAssociatedDevice(
        device: ChampionDiscoveredDevice,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        timeout: Duration = DevicePocHandshakeA1.defaultTimeout,
        fallbackScanRequest: ChampionScanRequest = ChampionScanRequest(timeout = 5.seconds),
    ): ChampionBleResult<DeviceConnectionBootstrapResult> {
        val directResult = connectAndHandshake(device = device, dateTime = dateTime, timeout = timeout)
        if (directResult is ChampionBleResult.Success) return directResult

        val directError = (directResult as ChampionBleResult.Failure).error
        if (!directError.shouldFallbackToAssociatedScan()) return directResult

        val resolvedCandidate = when (val scanResult = connectionManager.scan(fallbackScanRequest)) {
            is ChampionBleResult.Failure -> return ChampionBleResult.Failure(scanResult.error)
            is ChampionBleResult.Success -> scanResult.value.firstOrNull { it.transportKey == device.transportKey }
                ?: return ChampionBleResult.Failure(ChampionBleError.DeviceNotFound)
        }

        return connectAndHandshake(device = resolvedCandidate, dateTime = dateTime, timeout = timeout)
    }

    suspend fun connectAndHandshake(
        device: ChampionDiscoveredDevice,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        timeout: Duration = DevicePocHandshakeA1.defaultTimeout,
    ): ChampionBleResult<DeviceConnectionBootstrapResult> {
        val transport = when (val result = connectionManager.connect(device)) {
            is ChampionBleResult.Failure -> return ChampionBleResult.Failure(result.error)
            is ChampionBleResult.Success -> result.value
        }

        return try {
            when (val result = executeHandshake(transport, dateTime, timeout)) {
                is ChampionBleResult.Failure -> {
                    cleanupFailedBootstrap(transport)
                    ChampionBleResult.Failure(result.error)
                }

                is ChampionBleResult.Success -> ChampionBleResult.Success(
                    DeviceConnectionBootstrapResult(
                        transport = transport,
                        protocolState = result.value,
                    ),
                )
            }
        } catch (error: CancellationException) {
            cleanupFailedBootstrap(transport)
            throw error
        } catch (error: Exception) {
            cleanupFailedBootstrap(transport)
            throw error
        }
    }

    suspend fun executeHandshake(
        transport: ChampionTransportSession,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        timeout: Duration = DevicePocHandshakeA1.defaultTimeout,
    ): ChampionBleResult<DeviceConnectionProtocolState> = supervisorScope {
        val handshakeSubscription = transport.notifications.subscribe(ChampionChannelAlias.F008_NOTIFY)
        if (handshakeSubscription is ChampionBleResult.Failure) {
            return@supervisorScope ChampionBleResult.Failure(handshakeSubscription.error)
        }

        val confirmed = CompletableDeferred<Unit>()
        val observationJob = launch {
            try {
                (handshakeSubscription as ChampionBleResult.Success).value.collect { packet ->
                    if (DevicePocHandshakeA1.isConfirmationPacket(packet)) {
                        confirmed.complete(Unit)
                        return@collect
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (error.message?.startsWith("notification_hub_closed:") != true) throw error
            }
        }

        try {
            delay(DevicePocHandshakeA1.initialDelay)
            var attempt = 1
            var commandResult: ChampionBleResult<*> = ChampionBleResult.Failure(ChampionBleError.OperationUnavailable)
            while (true) {
                commandResult = transport.sendCommand(
                    DevicePocHandshakeA1.createCommand(
                        dateTime = dateTime,
                        timeout = timeout,
                    ),
                )
                val error = (commandResult as? ChampionBleResult.Failure)?.error ?: break
                if (!DevicePocHandshakeA1.shouldRetry(error, attempt)) break
                delay(DevicePocHandshakeA1.retryDelay)
                attempt += 1
            }

            when (commandResult) {
                is ChampionBleResult.Failure -> ChampionBleResult.Failure(commandResult.error)
                is ChampionBleResult.Success -> {
                    val explicitAck = withTimeoutOrNull(DevicePocHandshakeA1.assumedSuccessDelay) {
                        confirmed.await()
                        true
                    } ?: false
                    ChampionBleResult.Success(
                        if (explicitAck) DeviceConnectionProtocolState.HANDSHAKE_CONFIRMED
                        else DeviceConnectionProtocolState.HANDSHAKE_ASSUMED,
                    )
                }
            }
        } finally {
            observationJob.cancel()
        }
    }

    private suspend fun cleanupFailedBootstrap(transport: ChampionTransportSession) {
        withContext(NonCancellable) {
            connectionManager.disconnectExpectedTransport(
                expectedTransport = transport,
                reason = ChampionDisconnectReason.CLEANUP,
            )
        }
    }

    private fun ChampionBleError.shouldFallbackToAssociatedScan(): Boolean = when (this) {
        ChampionBleError.DeviceNotFound,
        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.UnexpectedDisconnect,
        -> true

        else -> false
    }
}
