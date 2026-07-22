package br.com.mwconsulting.championcompanion.feature.home

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionFea1StepsParser
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionOperationType
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionSessionId
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshFailure
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshOutcome
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshRequest
import br.com.mwconsulting.championcompanion.core.model.DailyStepsRefreshResult
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncCoordinator
import br.com.mwconsulting.championcompanion.core.model.DailySyncAttempt
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.toLocalDateIn
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ChampionDailyStepsSyncCoordinator @Inject constructor(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
) : DailyStepsSyncCoordinator {
    private val refreshMutex = Mutex()

    override suspend fun refresh(request: DailyStepsRefreshRequest): DailyStepsRefreshOutcome {
        val parserVersion = when (val version = ParserVersion.create(ChampionFea1StepsParser.parserVersion)) {
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Success -> version.value
            is br.com.mwconsulting.championcompanion.core.model.DomainResult.Failure -> return DailyStepsRefreshOutcome.Failure(
                DailyStepsRefreshFailure.INTERNAL,
            )
        }
        return when (
            val result = awaitFreshSteps(
                associatedDevice = ChampionDiscoveredDevice(
                    transportKey = request.associatedDevicePlatformIdentifier,
                    name = "Champion C001",
                    localName = "Champion C001",
                    observedAt = request.attempt.startedAt,
                ),
                attempt = request.attempt,
            )
        ) {
            is ChampionBleResult.Failure -> DailyStepsRefreshOutcome.Failure(result.error.toRefreshFailure())
            is ChampionBleResult.Success -> DailyStepsRefreshOutcome.Success(
                DailyStepsRefreshResult(
                    attempt = result.value.attempt,
                    deviceId = request.associatedDeviceId,
                    localDate = result.value.collectedAt.toLocalDateIn(request.zoneId),
                    steps = result.value.steps.toLong(),
                    collectedAt = result.value.collectedAt,
                    parserVersion = parserVersion,
                ),
            )
        }
    }

    suspend fun awaitFreshSteps(
        associatedDevice: ChampionDiscoveredDevice,
        attempt: DailySyncAttempt,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        connectTimeout: Duration = defaultConnectTimeout,
        packetTimeout: Duration = defaultPacketTimeout,
    ): ChampionBleResult<FreshStepsObservation> = refreshMutex.withLock {
        val transport = when (val transportResult = resolveTransport(associatedDevice, dateTime, connectTimeout)) {
            is ChampionBleResult.Failure -> return@withLock ChampionBleResult.Failure(transportResult.error)
            is ChampionBleResult.Success -> transportResult.value
        }
        val activeAttempt = attempt.copy(sessionId = transport.session.id.value.toString())
        awaitFreshPacket(transport, activeAttempt, packetTimeout)
    }

    internal suspend fun resolveTransport(
        associatedDevice: ChampionDiscoveredDevice,
        dateTime: ZonedDateTime,
        connectTimeout: Duration,
    ): ChampionBleResult<ChampionTransportSession> {
        val current = connectionManager.currentSession()
        if (current != null && current.session.transportKey == associatedDevice.transportKey) {
            return ChampionBleResult.Success(current)
        }
        return when (val result = connectionBootstrap.connectAssociatedDevice(
            device = associatedDevice,
            dateTime = dateTime,
            timeout = connectTimeout,
        )) {
            is ChampionBleResult.Failure -> ChampionBleResult.Failure(result.error)
            is ChampionBleResult.Success -> ChampionBleResult.Success(result.value.transport)
        }
    }

    internal suspend fun awaitFreshPacket(
        transport: ChampionTransportSession,
        attempt: DailySyncAttempt,
        timeout: Duration,
    ): ChampionBleResult<FreshStepsObservation> {
        if (!transport.capabilities.snapshot.hasChannel(ChampionChannelAlias.FEA1_NOTIFY)) {
            return ChampionBleResult.Failure(ChampionBleError.MissingOptionalChannel(ChampionChannelAlias.FEA1_NOTIFY))
        }
        val ensureResult = transport.notifications.ensureSubscribed(ChampionChannelAlias.FEA1_NOTIFY)
        if (ensureResult is ChampionBleResult.Failure) return ChampionBleResult.Failure(ensureResult.error)

        val subscription = transport.notifications.subscribe(ChampionChannelAlias.FEA1_NOTIFY)
        if (subscription is ChampionBleResult.Failure) return ChampionBleResult.Failure(subscription.error)

        return try {
            val packet = withTimeoutOrNull(timeout) {
                (subscription as ChampionBleResult.Success).value.first { candidate ->
                    accepts(candidate, attempt, transport.session.id)
                }
            } ?: return ChampionBleResult.Failure(
                ChampionBleError.Timeout(
                    operation = ChampionOperationType.WAIT_FOR_RESPONSE,
                    after = timeout,
                ),
            )

            when (val parsed = ChampionFea1StepsParser.parse(packet)) {
                is ChampionFea1StepsParser.Result.Valid -> ChampionBleResult.Success(
                    FreshStepsObservation(
                        attempt = attempt,
                        steps = parsed.steps,
                        collectedAt = packet.observedAt,
                    ),
                )

                is ChampionFea1StepsParser.Result.Invalid -> ChampionBleResult.Failure(parsed.error)
            }
        } catch (_: CancellationException) {
            ChampionBleResult.Failure(ChampionBleError.OperationCancelled(br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason.CLEANUP))
        } catch (error: RuntimeException) {
            if (error.isExpectedObserverShutdown()) {
                ChampionBleResult.Failure(ChampionBleError.OperationCancelled(br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCancellationReason.CLEANUP))
            } else {
                throw error
            }
        }
    }

    internal fun accepts(
        packet: ChampionPacket,
        attempt: DailySyncAttempt,
        activeSessionId: ChampionSessionId,
    ): Boolean =
        packet.sessionId == activeSessionId &&
            packet.channel == ChampionChannelAlias.FEA1_NOTIFY &&
            !packet.observedAt.isBefore(attempt.startedAt)

    data class FreshStepsObservation(
        val attempt: DailySyncAttempt,
        val steps: Int,
        val collectedAt: Instant,
    )

    private fun ChampionBleError.toRefreshFailure(): DailyStepsRefreshFailure = when (this) {
        ChampionBleError.DeviceNotFound -> DailyStepsRefreshFailure.DEVICE_NOT_FOUND
        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.UnexpectedDisconnect,
        is ChampionBleError.DiscoveryFailed,
        is ChampionBleError.SubscriptionFailed,
        is ChampionBleError.MissingOptionalChannel,
        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.MissingRequiredService,
        is ChampionBleError.OperationRejected,
        is ChampionBleError.CallbackFailure,
        -> DailyStepsRefreshFailure.CONNECTION_FAILED

        is ChampionBleError.Timeout -> DailyStepsRefreshFailure.TIMEOUT
        is ChampionBleError.InvalidPayload -> DailyStepsRefreshFailure.INVALID_PACKET
        is ChampionBleError.OperationCancelled,
        ChampionBleError.CleanupInProgress,
        is ChampionBleError.QueueClosed,
        -> DailyStepsRefreshFailure.CANCELLED

        else -> DailyStepsRefreshFailure.INTERNAL
    }

    private fun Throwable.isExpectedObserverShutdown(): Boolean =
        message?.startsWith("notification_hub_closed:") == true

    companion object {
        val defaultPacketTimeout: Duration = 5.seconds
        val defaultConnectTimeout: Duration = 8.seconds
    }
}
