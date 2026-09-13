package br.com.mwconsulting.championcompanion.feature.health

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD1HistoryProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportResult
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateRepository
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.PersistWatchDailyD1SamplesInput
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SaveD1HeartRateMeasurementsInput
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1PersistenceResult
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1ImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1Repository
import br.com.mwconsulting.championcompanion.core.model.WatchDailyD1SampleId
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

class ChampionD1HeartRateSyncCoordinator(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    private val heartRateRepository: HeartRateRepository,
    private val dailyD1Repository: WatchDailyD1Repository = UnsupportedWatchDailyD1Repository,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
    private val ids: UuidV7Generator = UuidV7Generator(),
    private val parserVersion: ParserVersion = ParserVersion.create("d1-v2").valueOrNull()!!,
    private val historyTimeout: Duration = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts().d1History,
    private val civilNow: () -> Instant = Instant::now,
) : D1HeartRateSyncCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow<D1HeartRateImportSurfaceState>(D1HeartRateImportSurfaceState.Idle)
    private val lifecycleMutex = Mutex()

    private var activeImport: ActiveImport? = null
    private var activeJob: Job? = null
    private var pendingCancellation: D1HeartRateImportCancellationReason? = null

    override val state: StateFlow<D1HeartRateImportSurfaceState> = mutableState.asStateFlow()

    init {
        scope.launch {
            connectionManager.state.collect(::handleBleStateChanged)
        }
    }

    override suspend fun startImport(request: D1HeartRateSyncRequest): HealthD1ImportError? {
        if (activeImport != null) {
            return HealthD1ImportError(
                category = HealthD1ImportErrorCategory.BLE_OPERATION_IN_PROGRESS,
                message = "Ja existe uma importacao D1 em andamento.",
                isRetryable = false,
            )
        }

        val gateResult = foregroundBleOperationGate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.WATCH_D1_HISTORY,
                operationId = request.importAttemptId,
                requestedAt = civilNow(),
            ),
        )
        val permit = when (gateResult) {
            is ForegroundBleOperationAcquireResult.Acquired -> gateResult.permit
            is ForegroundBleOperationAcquireResult.Rejected -> {
                return HealthD1ImportError(
                    category = HealthD1ImportErrorCategory.BLE_OPERATION_IN_PROGRESS,
                    message = gateResult.activeOperation.owner.toHealthForegroundBleBlockMessage(),
                    isRetryable = true,
                ).also { error ->
                    mutableState.value = D1HeartRateImportSurfaceState.Failed(
                        D1HeartRateImportSessionState(
                            importAttemptId = request.importAttemptId,
                            error = error,
                        ),
                    )
                }
            }
        }

        val baseSession = D1HeartRateImportSessionState(importAttemptId = request.importAttemptId)
        mutableState.value = D1HeartRateImportSurfaceState.Preparing(baseSession)
        activeJob?.cancel()
        activeJob = scope.launch {
            runImport(request, permit, baseSession)
        }
        return null
    }

    override suspend fun cancelImport(reason: D1HeartRateImportCancellationReason) {
        val active = lifecycleMutex.withLock {
            activeImport ?: run {
                if (mutableState.value is D1HeartRateImportSurfaceState.Preparing ||
                    mutableState.value is D1HeartRateImportSurfaceState.Connecting ||
                    mutableState.value is D1HeartRateImportSurfaceState.Reading
                ) {
                    pendingCancellation = reason
                }
                null
            }
        }
        if (active == null) return
        requestTermination(active, Termination.Cancelled(reason))
    }

    override fun reset() {
        if (activeImport == null) {
            mutableState.value = D1HeartRateImportSurfaceState.Idle
        }
    }

    private suspend fun runImport(
        request: D1HeartRateSyncRequest,
        permit: ForegroundBleOperationPermit,
        baseSession: D1HeartRateImportSessionState,
    ) {
        var active: ActiveImport? = null
        val attemptStartedAt = civilNow()
        try {
            mutableState.value = D1HeartRateImportSurfaceState.Connecting(baseSession)
            if (completePendingCancellation(request, baseSession, attemptStartedAt)) return
            val transportResult = resolveTransport(request)
            if (completePendingCancellation(request, baseSession, attemptStartedAt)) return
            val transport = when (val result = transportResult) {
                is ChampionBleResult.Success -> result.value
                is ChampionBleResult.Failure -> {
                    val error = result.error.toHealthError(HealthD1ImportErrorCategory.CONNECTION_FAILED)
                    val metadataPersisted = persistDailyTerminalMetadata(
                        deviceId = request.associatedDevice.id,
                        lastAttemptAt = attemptStartedAt,
                        timedOut = result.error is ChampionBleError.Timeout,
                        status = D1HeartRateImportStatus.FAILED,
                    )
                    mutableState.value = D1HeartRateImportSurfaceState.Failed(
                        baseSession.copy(error = if (metadataPersisted) error else storageFailureError()),
                    )
                    return
                }
            }

            val handshakeResult = connectionBootstrap.executeHandshake(transport, ZonedDateTime.now())
            if (completePendingCancellation(request, baseSession, attemptStartedAt, transport.session.id.value.toString())) return
            if (handshakeResult is ChampionBleResult.Failure) {
                val error = handshakeResult.error.toHealthError(HealthD1ImportErrorCategory.HANDSHAKE_FAILED)
                val metadataPersisted = persistDailyTerminalMetadata(
                        deviceId = request.associatedDevice.id,
                        lastAttemptAt = attemptStartedAt,
                        timedOut = handshakeResult.error is ChampionBleError.Timeout,
                        status = D1HeartRateImportStatus.FAILED,
                    )
                mutableState.value = D1HeartRateImportSurfaceState.Failed(
                    baseSession.copy(
                        bleSessionId = transport.session.id.value.toString(),
                        error = if (metadataPersisted) error else storageFailureError(),
                    ),
                )
                return
            }

            val notificationSubscription = transport.notifications.subscribe(ChampionChannelAlias.F008_NOTIFY)
            if (completePendingCancellation(request, baseSession, attemptStartedAt, transport.session.id.value.toString())) return
            if (notificationSubscription is ChampionBleResult.Failure) {
                val error = notificationSubscription.error.toHealthError(HealthD1ImportErrorCategory.F008_UNAVAILABLE)
                val metadataPersisted = persistDailyTerminalMetadata(
                        deviceId = request.associatedDevice.id,
                        lastAttemptAt = attemptStartedAt,
                        timedOut = notificationSubscription.error is ChampionBleError.Timeout,
                        status = D1HeartRateImportStatus.FAILED,
                    )
                mutableState.value = D1HeartRateImportSurfaceState.Failed(
                    baseSession.copy(
                        bleSessionId = transport.session.id.value.toString(),
                        error = if (metadataPersisted) error else storageFailureError(),
                    ),
                )
                return
            }

            val startedAt = civilNow()
            val context = ChampionD1HistoryProtocol.D1ImportContext(
                importAttemptId = request.importAttemptId,
                bleSessionId = transport.session.id,
                deviceId = request.associatedDevice.id,
                startedAt = startedAt,
                baseLocalDate = request.baseLocalDate,
                zoneId = request.zoneId,
                parserVersion = parserVersion,
            )
            val collector = ChampionD1HistoryProtocol.MultipartCollector(context) {
                HeartRateMeasurementId(ids.next())
            }
            val readingSession = baseSession.copy(
                bleSessionId = transport.session.id.value.toString(),
                startedAt = startedAt,
            )
            mutableState.value = D1HeartRateImportSurfaceState.Reading(readingSession)

            active = ActiveImport(
                permit = permit,
                importAttemptId = request.importAttemptId,
                bleSessionId = transport.session.id.value.toString(),
                context = context,
                collector = collector,
                completion = CompletableDeferred(),
            )
            lifecycleMutex.withLock {
                activeImport = active
            }

            val cancellationBeforeReading = lifecycleMutex.withLock {
                pendingCancellation.also { pendingCancellation = null }
            }
            if (cancellationBeforeReading != null) {
                requestTermination(active, Termination.Cancelled(cancellationBeforeReading))
                finalizeCollectedImport(active, active.collector.cancel(civilNow()))
                return
            }

            active.readingJob = scope.launch {
                try {
                    (notificationSubscription as ChampionBleResult.Success).value.collect { packet ->
                        if (!isActiveImport(active, packet.sessionId.value.toString())) return@collect
                        when (val update = collector.onPacket(packet)) {
                            ChampionD1HistoryProtocol.D1CollectionUpdate.Ignored -> Unit
                            is ChampionD1HistoryProtocol.D1CollectionUpdate.Progress -> {
                                mutableState.value = D1HeartRateImportSurfaceState.Reading(
                                    currentSessionState(active).copy(progress = update.progress),
                                )
                            }

                            is ChampionD1HistoryProtocol.D1CollectionUpdate.Finished -> {
                                requestTermination(active, Termination.Collected(update.outcome))
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    Unit
                } catch (error: RuntimeException) {
                    if (!error.isExpectedObserverShutdown()) throw error
                }
            }

            val sendResult = transport.sendCommand(ChampionD1HistoryProtocol.createHistoryCommand(historyTimeout))
            if (sendResult is ChampionBleResult.Failure) {
                requestTermination(
                    active,
                    Termination.Failed(sendResult.error.toHealthError(HealthD1ImportErrorCategory.SEND_COMMAND_FAILED)),
                )
            } else {
                active.timeoutJob = scope.launch {
                    delay(historyTimeout)
                    if (isActiveImport(active, active.bleSessionId)) {
                        requestTermination(active, Termination.Collected(collector.onTimeout(civilNow())))
                    }
                }
            }

            when (val termination = active.completion.await()) {
                is Termination.Cancelled -> {
                    finalizeCollectedImport(active, active.collector.cancel(civilNow()))
                }

                is Termination.Failed -> {
                    val cancelledOutcome = active.collector.cancel(civilNow())
                    finalizeCollectedImport(
                        active = active,
                        outcome = cancelledOutcome.copy(
                            result = D1HeartRateImportResult.Failed(cancelledOutcome.result.summary),
                        ),
                        failureError = termination.error,
                    )
                }

                is Termination.Collected -> finalizeCollectedImport(active, termination.outcome)
            }
        } finally {
            active?.let { cleanupImport(it) }
            foregroundBleOperationGate.release(permit)
            lifecycleMutex.withLock {
                if (activeImport?.importAttemptId == request.importAttemptId) {
                    activeImport = null
                }
            }
        }
    }

    private suspend fun finalizeCollectedImport(
        active: ActiveImport,
        outcome: ChampionD1HistoryProtocol.D1CollectionOutcome,
        failureError: HealthD1ImportError? = null,
    ) {
        val session = currentSessionState(active).copy(
            progress = outcome.progress,
            result = outcome.result,
        )
        when (outcome.result) {
            is D1HeartRateImportResult.Cancelled -> {
                mutableState.value = D1HeartRateImportSurfaceState.Persisting(session)
                val persistence = persistDailyOutcome(active, outcome)
                mutableState.value = D1HeartRateImportSurfaceState.Cancelled(
                    session.copy(error = if (persistence.storageFailed) storageFailureError() else cancellationError()),
                )
            }

            is D1HeartRateImportResult.Failed -> {
                mutableState.value = D1HeartRateImportSurfaceState.Persisting(session)
                val persistence = persistDailyOutcome(active, outcome)
                val error = if (outcome.result.summary?.timedOut == true) {
                    HealthD1ImportError(
                        category = HealthD1ImportErrorCategory.TIMEOUT_EMPTY,
                        message = "Tempo limite atingido antes de receber um registro D1 valido.",
                        isRetryable = true,
                    )
                } else {
                    HealthD1ImportError(
                        category = HealthD1ImportErrorCategory.NO_VALID_RECORDS,
                        message = "Nenhum registro D1 valido foi encontrado para importar.",
                        isRetryable = true,
                    )
                }
                mutableState.value = D1HeartRateImportSurfaceState.Failed(
                    session.copy(error = if (persistence.storageFailed) storageFailureError() else failureError ?: error),
                )
            }

            is D1HeartRateImportResult.Completed,
            is D1HeartRateImportResult.CompletedWithGaps,
            is D1HeartRateImportResult.Partial,
            -> {
                mutableState.value = D1HeartRateImportSurfaceState.Persisting(session)
                val dailyPersistence = persistDailyOutcome(active, outcome)
                if (dailyPersistence.storageFailed) {
                    mutableState.value = D1HeartRateImportSurfaceState.Failed(
                        session.copy(error = storageFailureError()),
                    )
                    return
                }

                if (outcome.measurements.isEmpty()) {
                    val structuralOnlySession = session.copy(
                        error = if (outcome.result is D1HeartRateImportResult.Partial &&
                            outcome.result.summary?.timedOut == true
                        ) {
                            timeoutPartialError()
                        } else {
                            null
                        },
                    )
                    mutableState.value = when (outcome.result) {
                        is D1HeartRateImportResult.Completed ->
                            D1HeartRateImportSurfaceState.Completed(structuralOnlySession)
                        is D1HeartRateImportResult.CompletedWithGaps ->
                            D1HeartRateImportSurfaceState.CompletedWithGaps(structuralOnlySession)
                        is D1HeartRateImportResult.Partial ->
                            D1HeartRateImportSurfaceState.Partial(structuralOnlySession)
                        is D1HeartRateImportResult.Failed ->
                            D1HeartRateImportSurfaceState.Failed(structuralOnlySession)
                        is D1HeartRateImportResult.Cancelled ->
                            D1HeartRateImportSurfaceState.Cancelled(structuralOnlySession)
                    }
                    return
                }

                val placeholderMetadata = buildMetadata(
                    outcome = outcome,
                    deviceId = active.context.deviceId,
                    insertedCount = 0,
                    duplicateCount = 0,
                    conflictCount = 0,
                )
                when (val persisted = heartRateRepository.importD1Measurements(
                    SaveD1HeartRateMeasurementsInput(
                        measurements = outcome.measurements,
                        metadata = placeholderMetadata,
                    ),
                )) {
                    is RepositoryResult.Success -> {
                        val metadata = buildMetadata(
                            outcome = outcome,
                            deviceId = active.context.deviceId,
                            insertedCount = persisted.value.insertedCount,
                            duplicateCount = persisted.value.duplicateCount,
                            conflictCount = persisted.value.conflictCount,
                        )
                        if (outcome.measurements.isNotEmpty()) {
                            when (heartRateRepository.importD1Measurements(
                                SaveD1HeartRateMeasurementsInput(
                                    measurements = emptyList(),
                                    metadata = metadata,
                                ),
                            )) {
                                is RepositoryResult.Success -> Unit
                                RepositoryResult.StorageUnavailable,
                                RepositoryResult.Conflict,
                                RepositoryResult.NotFound,
                                RepositoryResult.UnexpectedState,
                                -> {
                                    mutableState.value = D1HeartRateImportSurfaceState.Failed(
                                        session.copy(error = storageFailureError()),
                                    )
                                    return
                                }
                            }
                        }
                        val finalResult = updateResultSummary(outcome.result, persisted.value)
                        val completedSession = session.copy(result = finalResult, metadata = metadata)
                        mutableState.value = when (finalResult.status) {
                            D1HeartRateImportStatus.COMPLETED -> D1HeartRateImportSurfaceState.Completed(completedSession)
                            D1HeartRateImportStatus.COMPLETED_WITH_GAPS -> D1HeartRateImportSurfaceState.CompletedWithGaps(completedSession)
                            D1HeartRateImportStatus.PARTIAL -> D1HeartRateImportSurfaceState.Partial(
                                completedSession.copy(
                                    error = if (outcome.result.summary?.timedOut == true) {
                                        HealthD1ImportError(
                                            category = HealthD1ImportErrorCategory.TIMEOUT_PARTIAL,
                                            message = "Importacao parcial concluida apos timeout com pontos validos.",
                                            isRetryable = true,
                                        )
                                    } else {
                                        null
                                    },
                                ),
                            )
                            D1HeartRateImportStatus.FAILED -> D1HeartRateImportSurfaceState.Failed(completedSession)
                            D1HeartRateImportStatus.CANCELLED -> D1HeartRateImportSurfaceState.Cancelled(completedSession)
                        }
                    }

                    RepositoryResult.StorageUnavailable,
                    RepositoryResult.Conflict,
                    RepositoryResult.NotFound,
                    RepositoryResult.UnexpectedState,
                    -> {
                        mutableState.value = D1HeartRateImportSurfaceState.Failed(
                            session.copy(
                                error = HealthD1ImportError(
                                    category = HealthD1ImportErrorCategory.STORAGE_FAILED,
                                    message = "Nao foi possivel salvar registros D1 importados.",
                                    isRetryable = true,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun storageFailureError() = HealthD1ImportError(
        category = HealthD1ImportErrorCategory.STORAGE_FAILED,
        message = "Nao foi possivel salvar registros D1 importados.",
        isRetryable = true,
    )

    private fun cancellationError() = HealthD1ImportError(
        category = HealthD1ImportErrorCategory.CANCELLED,
        message = "Importacao cancelada antes da conclusao.",
        isRetryable = true,
    )

    private fun timeoutPartialError() = HealthD1ImportError(
        category = HealthD1ImportErrorCategory.TIMEOUT_PARTIAL,
        message = "Importacao parcial concluida apos timeout com pontos validos.",
        isRetryable = true,
    )

    private suspend fun persistDailyOutcome(
        active: ActiveImport,
        outcome: ChampionD1HistoryProtocol.D1CollectionOutcome,
    ): DailyPersistence {
        val importedAt = outcome.result.summary?.finishedAt ?: civilNow()
        val dailySamples = outcome.packets.mapNotNull { packet ->
            ChampionD1HistoryProtocol.toDailySample(
                packet = packet,
                context = active.context,
                sampleId = WatchDailyD1SampleId(ids.next()),
                importedAt = importedAt,
            )
        }
        val conversionInvalidCount = outcome.packets.size - dailySamples.size
        var factsPersisted = true
        val persistedFacts = if (dailySamples.isEmpty()) {
            WatchDailyD1PersistenceResult()
        } else {
            when (val result = dailyD1Repository.persist(PersistWatchDailyD1SamplesInput(dailySamples))) {
                is RepositoryResult.Success -> result.value
                RepositoryResult.StorageUnavailable,
                RepositoryResult.Conflict,
                RepositoryResult.NotFound,
                RepositoryResult.UnexpectedState,
                -> {
                    factsPersisted = false
                    WatchDailyD1PersistenceResult()
                }
            }
        }
        val metadataPersisted = persistDailyMetadata(
            buildDailyMetadata(
                outcome = outcome,
                deviceId = active.context.deviceId,
                persisted = persistedFacts,
                invalidCount = outcome.invalidPacketCount + conversionInvalidCount,
                persistenceSucceeded = factsPersisted && conversionInvalidCount == 0,
            ),
        )
        return DailyPersistence(
            facts = persistedFacts,
            storageFailed = !factsPersisted || !metadataPersisted || conversionInvalidCount > 0,
        )
    }

    private suspend fun persistDailyTerminalMetadata(
        deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        lastAttemptAt: Instant,
        timedOut: Boolean,
        status: D1HeartRateImportStatus,
    ): Boolean = persistDailyMetadata(
        WatchDailyD1ImportMetadata(
            deviceId = deviceId,
            lastAttemptAt = lastAttemptAt,
            lastResult = status,
            parserVersion = parserVersion,
            timedOut = timedOut,
            updatedAt = civilNow(),
        ),
    )

    private suspend fun completePendingCancellation(
        request: D1HeartRateSyncRequest,
        session: D1HeartRateImportSessionState,
        attemptStartedAt: Instant,
        bleSessionId: String? = null,
    ): Boolean {
        val cancellation = lifecycleMutex.withLock {
            pendingCancellation.also { pendingCancellation = null }
        } ?: return false
        val metadataPersisted = persistDailyTerminalMetadata(
            deviceId = request.associatedDevice.id,
            lastAttemptAt = attemptStartedAt,
            timedOut = false,
            status = D1HeartRateImportStatus.CANCELLED,
        )
        mutableState.value = D1HeartRateImportSurfaceState.Cancelled(
            session.copy(
                bleSessionId = bleSessionId,
                error = if (metadataPersisted) cancellationError() else storageFailureError(),
            ),
        )
        return true
    }

    private suspend fun persistDailyMetadata(metadata: WatchDailyD1ImportMetadata): Boolean =
        when (dailyD1Repository.persistImportMetadata(metadata)) {
            is RepositoryResult.Success -> true
            RepositoryResult.StorageUnavailable,
            RepositoryResult.Conflict,
            RepositoryResult.NotFound,
            RepositoryResult.UnexpectedState,
            -> false
        }

    private data class DailyPersistence(
        val facts: WatchDailyD1PersistenceResult,
        val storageFailed: Boolean,
    )

    private fun buildMetadata(
        outcome: ChampionD1HistoryProtocol.D1CollectionOutcome,
        deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        insertedCount: Int,
        duplicateCount: Int,
        conflictCount: Int,
    ): D1HeartRateImportMetadata {
        val summary = outcome.result.summary
        return D1HeartRateImportMetadata(
            deviceId = deviceId,
            lastAttemptAt = summary?.startedAt ?: civilNow(),
            lastSuccessAt = when (outcome.result.status) {
                D1HeartRateImportStatus.COMPLETED,
                D1HeartRateImportStatus.COMPLETED_WITH_GAPS,
                D1HeartRateImportStatus.PARTIAL,
                -> summary?.finishedAt
                D1HeartRateImportStatus.FAILED,
                D1HeartRateImportStatus.CANCELLED,
                -> null
            },
            lastResult = outcome.result.status,
            parserVersion = parserVersion,
            insertedCount = insertedCount,
            duplicateCount = duplicateCount,
            conflictCount = conflictCount,
            invalidCount = outcome.invalidPacketCount,
            missingIndexCount = outcome.missingPacketIndices.size,
            timedOut = summary?.timedOut == true,
            updatedAt = summary?.finishedAt ?: civilNow(),
        )
    }

    private fun buildDailyMetadata(
        outcome: ChampionD1HistoryProtocol.D1CollectionOutcome,
        deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        persisted: WatchDailyD1PersistenceResult,
        invalidCount: Int = outcome.invalidPacketCount,
        persistenceSucceeded: Boolean = true,
    ): WatchDailyD1ImportMetadata {
        val summary = outcome.result.summary
        val resultStatus = if (persistenceSucceeded) {
            outcome.result.status
        } else {
            D1HeartRateImportStatus.FAILED
        }
        return WatchDailyD1ImportMetadata(
            deviceId = deviceId,
            lastAttemptAt = summary?.startedAt ?: civilNow(),
            lastSuccessAt = when (resultStatus) {
                D1HeartRateImportStatus.COMPLETED,
                D1HeartRateImportStatus.COMPLETED_WITH_GAPS,
                D1HeartRateImportStatus.PARTIAL,
                -> summary?.finishedAt
                D1HeartRateImportStatus.FAILED,
                D1HeartRateImportStatus.CANCELLED,
                -> null
            },
            lastResult = resultStatus,
            parserVersion = parserVersion,
            insertedCount = persisted.insertedCount,
            updatedCount = persisted.updatedCount,
            duplicateCount = persisted.duplicateCount,
            conflictCount = persisted.conflictCount,
            invalidCount = invalidCount,
            missingIndexCount = outcome.missingPacketIndices.size,
            timedOut = summary?.timedOut == true,
            updatedAt = summary?.finishedAt ?: civilNow(),
        )
    }

    private fun updateResultSummary(
        result: D1HeartRateImportResult,
        persisted: br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportPersistenceResult,
    ): D1HeartRateImportResult {
        val summary = result.summary ?: return result
        val updatedSummary = summary.copy(
            insertedCount = persisted.insertedCount,
            duplicateCount = persisted.duplicateCount,
            conflictCount = persisted.conflictCount,
            pendingCreatedCount = persisted.pendingCreatedCount,
            pendingExistingCount = persisted.pendingExistingCount,
        )
        return when (result) {
            is D1HeartRateImportResult.Completed -> D1HeartRateImportResult.Completed(updatedSummary)
            is D1HeartRateImportResult.CompletedWithGaps -> D1HeartRateImportResult.CompletedWithGaps(updatedSummary)
            is D1HeartRateImportResult.Partial -> D1HeartRateImportResult.Partial(updatedSummary)
            is D1HeartRateImportResult.Failed -> D1HeartRateImportResult.Failed(updatedSummary)
            is D1HeartRateImportResult.Cancelled -> D1HeartRateImportResult.Cancelled(updatedSummary)
        }
    }

    private suspend fun cleanupImport(active: ActiveImport) {
        active.timeoutJob?.cancel()
        active.readingJob?.cancel()
    }

    private suspend fun resolveTransport(
        request: D1HeartRateSyncRequest,
    ): ChampionBleResult<ChampionTransportSession> {
        val currentSession = connectionManager.currentSession()
        if (currentSession != null && currentSession.session.transportKey == request.associatedDevice.platformIdentifier) {
            return ChampionBleResult.Success(currentSession)
        }
        return when (val result = connectionBootstrap.connectAssociatedDevice(request.associatedDevice.toDiscoveredDevice())) {
            is ChampionBleResult.Success -> ChampionBleResult.Success(result.value.transport)
            is ChampionBleResult.Failure -> ChampionBleResult.Failure(result.error)
        }
    }

    private fun currentSessionState(active: ActiveImport): D1HeartRateImportSessionState = when (val state = mutableState.value) {
        is D1HeartRateImportSurfaceState.Preparing -> state.session
        is D1HeartRateImportSurfaceState.Connecting -> state.session
        is D1HeartRateImportSurfaceState.Reading -> state.session
        is D1HeartRateImportSurfaceState.Persisting -> state.session
        is D1HeartRateImportSurfaceState.Completed -> state.session
        is D1HeartRateImportSurfaceState.CompletedWithGaps -> state.session
        is D1HeartRateImportSurfaceState.Partial -> state.session
        is D1HeartRateImportSurfaceState.Failed -> state.session
        is D1HeartRateImportSurfaceState.Cancelled -> state.session
        D1HeartRateImportSurfaceState.Idle -> D1HeartRateImportSessionState(
            importAttemptId = active.importAttemptId,
            bleSessionId = active.bleSessionId,
        )
    }

    private suspend fun requestTermination(active: ActiveImport, termination: Termination) {
        lifecycleMutex.withLock {
            if (activeImport !== active || active.isFinishing || active.completion.isCompleted) return
            active.isFinishing = true
            active.completion.complete(termination)
        }
    }

    private suspend fun handleBleStateChanged(state: ChampionBleState) {
        val active = activeImport ?: return
        when (state) {
            ChampionBleState.Disconnected -> {
                requestTermination(
                    active,
                    Termination.Failed(
                        HealthD1ImportError(
                            category = HealthD1ImportErrorCategory.UNEXPECTED_DISCONNECT,
                            message = "Conexao com relogio foi encerrada durante a importacao.",
                            isRetryable = true,
                        ),
                    ),
                )
            }

            is ChampionBleState.Disconnecting -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(
                        active,
                        Termination.Failed(
                            HealthD1ImportError(
                                category = HealthD1ImportErrorCategory.UNEXPECTED_DISCONNECT,
                                message = "Conexao com relogio foi encerrada durante a importacao.",
                                isRetryable = true,
                            ),
                        ),
                    )
                }
            }

            is ChampionBleState.Failed -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(active, Termination.Failed(state.error.toHealthError()))
                }
            }

            is ChampionBleState.Connecting,
            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.ConfiguringSubscriptions,
            is ChampionBleState.TransportReady,
            is ChampionBleState.Scanning,
            -> Unit
        }
    }

    private fun isActiveImport(active: ActiveImport, bleSessionId: String): Boolean =
        activeImport === active && active.bleSessionId == bleSessionId && !active.completion.isCompleted

    private fun br.com.mwconsulting.championcompanion.core.model.Device.toDiscoveredDevice() =
        br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice(
            transportKey = platformIdentifier,
            name = name,
            localName = name,
            observedAt = civilNow(),
        )

    private fun ChampionBleError.toHealthError(
        defaultCategory: HealthD1ImportErrorCategory = HealthD1ImportErrorCategory.CONNECTION_FAILED,
    ): HealthD1ImportError = when (this) {
        ChampionBleError.BluetoothDisabled -> HealthD1ImportError(
            category = HealthD1ImportErrorCategory.BLUETOOTH_DISABLED,
            message = "Ative o Bluetooth para buscar dados do relogio.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> HealthD1ImportError(
            category = HealthD1ImportErrorCategory.PERMISSIONS_REQUIRED,
            message = "Permissoes de Bluetooth sao necessarias para buscar dados do relogio.",
            isRetryable = true,
        )

        ChampionBleError.DeviceNotFound -> HealthD1ImportError(
            category = HealthD1ImportErrorCategory.DEVICE_NOT_FOUND,
            message = "Nao foi possivel localizar relogio associado.",
            isRetryable = true,
        )

        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.SubscriptionFailed,
        -> HealthD1ImportError(
            category = if (
                (this is ChampionBleError.MissingRequiredChannel && channel == ChampionChannelAlias.F008_NOTIFY) ||
                (this is ChampionBleError.SubscriptionFailed && channel == ChampionChannelAlias.F008_NOTIFY)
            ) {
                HealthD1ImportErrorCategory.F008_UNAVAILABLE
            } else {
                defaultCategory
            },
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )

        is ChampionBleError.OperationCancelled,
        is ChampionBleError.QueueClosed,
        ChampionBleError.CleanupInProgress,
        -> HealthD1ImportError(
            category = HealthD1ImportErrorCategory.CANCELLED,
            message = "Importacao cancelada antes da conclusao.",
            isRetryable = true,
        )

        is ChampionBleError.Timeout,
        is ChampionBleError.ConnectionFailed,
        is ChampionBleError.DiscoveryFailed,
        is ChampionBleError.UnexpectedDisconnect,
        is ChampionBleError.InvalidPayload,
        is ChampionBleError.OperationRejected,
        is ChampionBleError.CallbackFailure,
        is ChampionBleError.IncompatibleChannelProperties,
        is ChampionBleError.MissingOptionalChannel,
        is ChampionBleError.MissingRequiredService,
        ChampionBleError.OperationUnavailable,
        ChampionBleError.UnexpectedInternal,
        ChampionBleError.ScanAlreadyInProgress,
        ChampionBleError.BleNotSupported,
        is ChampionBleError.StaleSession,
        is ChampionBleError.ScanFailed,
        -> HealthD1ImportError(
            category = defaultCategory,
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )
    }

    private fun Throwable.isExpectedObserverShutdown(): Boolean =
        message?.startsWith("notification_hub_closed:") == true

    private fun ForegroundBleOperationOwner.toHealthForegroundBleBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Uma sincronização do relógio está em andamento na tela Início."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Uma medição manual está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Uma importação de histórico do relógio já está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Uma importação de treinos do relógio está em andamento na tela Exercícios."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "A participação opcional do relógio está em andamento na sessão de treino."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento na tela Dispositivo."
    }

    private data class ActiveImport(
        val permit: ForegroundBleOperationPermit,
        val importAttemptId: String,
        val bleSessionId: String,
        val context: ChampionD1HistoryProtocol.D1ImportContext,
        val collector: ChampionD1HistoryProtocol.MultipartCollector,
        val completion: CompletableDeferred<Termination>,
        var isFinishing: Boolean = false,
        var timeoutJob: Job? = null,
        var readingJob: Job? = null,
    )

    private sealed interface Termination {
        data class Cancelled(val reason: D1HeartRateImportCancellationReason) : Termination
        data class Failed(val error: HealthD1ImportError) : Termination
        data class Collected(val outcome: ChampionD1HistoryProtocol.D1CollectionOutcome) : Termination
    }
}

private object UnsupportedWatchDailyD1Repository : WatchDailyD1Repository {
    override suspend fun persist(
        input: PersistWatchDailyD1SamplesInput,
    ): RepositoryResult<WatchDailyD1PersistenceResult> = RepositoryResult.UnexpectedState
}
