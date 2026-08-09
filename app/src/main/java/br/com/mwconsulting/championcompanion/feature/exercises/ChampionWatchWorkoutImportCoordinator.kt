package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleResult
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionChannelAlias
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD3WorkoutIndexProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD4WorkoutBlockProtocol
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionD4WorkoutParser
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionTransportSession
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage
import br.com.mwconsulting.championcompanion.core.common.BleDispatcher
import br.com.mwconsulting.championcompanion.core.model.DiagnosticCaptureFlow
import br.com.mwconsulting.championcompanion.core.model.DiagnosticParseResult
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadCapture
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadRepository
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationAcquireResult
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationGate
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationPermit
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationRequest
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportSummary
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportId
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V3_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.feature.device.DeviceConnectionBootstrap
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

class ChampionWatchWorkoutImportCoordinator(
    private val connectionManager: ChampionConnectionManager,
    private val connectionBootstrap: DeviceConnectionBootstrap,
    private val diagnosticPayloadRepository: DiagnosticPayloadRepository,
    private val foregroundBleOperationGate: ForegroundBleOperationGate,
    private val workoutActivityRepository: WorkoutActivityRepository,
    @BleDispatcher bleDispatcher: CoroutineDispatcher,
    private val ids: UuidV7Generator = UuidV7Generator(),
    private val parserVersion: ParserVersion = ParserVersion.create(WATCH_WORKOUT_D4_V3_PARSER_VERSION).valueOrNull()!!,
    private val d3Timeout: Duration = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts().d3WorkoutIndex,
    private val d4Timeout: Duration = br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleTimeouts().d4WorkoutBlock,
    private val civilNow: () -> Instant = Instant::now,
    private val isDebugBuild: Boolean,
) : WatchWorkoutImportCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + bleDispatcher)
    private val mutableState = MutableStateFlow<WatchWorkoutImportSurfaceState>(WatchWorkoutImportSurfaceState.Idle)
    private val lifecycleMutex = Mutex()

    private var activeImport: ActiveImport? = null
    private var activeJob: Job? = null

    override val state: StateFlow<WatchWorkoutImportSurfaceState> = mutableState.asStateFlow()

    init {
        scope.launch {
            connectionManager.state.collect(::handleBleStateChanged)
        }
    }

    override suspend fun startImport(request: WatchWorkoutImportRequest): WatchWorkoutImportError? {
        if (activeImport != null) {
            return WatchWorkoutImportError(
                category = WatchWorkoutImportErrorCategory.BLE_OPERATION_IN_PROGRESS,
                message = "Ja existe uma importacao de treinos em andamento.",
                isRetryable = false,
            )
        }

        val gateResult = foregroundBleOperationGate.tryAcquire(
            ForegroundBleOperationRequest(
                owner = ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT,
                operationId = request.importAttemptId,
                requestedAt = civilNow(),
            ),
        )
        val permit = when (gateResult) {
            is ForegroundBleOperationAcquireResult.Acquired -> gateResult.permit
            is ForegroundBleOperationAcquireResult.Rejected -> {
                return WatchWorkoutImportError(
                    category = WatchWorkoutImportErrorCategory.BLE_OPERATION_IN_PROGRESS,
                    message = gateResult.activeOperation.owner.toExercisesForegroundBleBlockMessage(),
                    isRetryable = true,
                ).also { error ->
                    mutableState.value = WatchWorkoutImportSurfaceState.Failed(
                        WatchWorkoutImportSessionState(
                            importAttemptId = request.importAttemptId,
                            error = error,
                        ),
                    )
                }
            }
        }

        val baseSession = WatchWorkoutImportSessionState(importAttemptId = request.importAttemptId)
        mutableState.value = WatchWorkoutImportSurfaceState.Preparing(baseSession)
        activeJob?.cancel()
        activeJob = scope.launch {
            runImport(request, permit, baseSession)
        }
        return null
    }

    override suspend fun cancelImport(reason: WatchWorkoutImportCancellationReason) {
        val active = activeImport ?: return
        requestTermination(active, Termination.Cancelled(reason))
    }

    override fun reset() {
        if (activeImport == null) {
            mutableState.value = WatchWorkoutImportSurfaceState.Idle
        }
    }

    private suspend fun runImport(
        request: WatchWorkoutImportRequest,
        permit: ForegroundBleOperationPermit,
        baseSession: WatchWorkoutImportSessionState,
    ) {
        var active: ActiveImport? = null
        try {
            val startedAt = civilNow()
            mutableState.value = WatchWorkoutImportSurfaceState.Connecting(baseSession.copy(startedAt = startedAt))
            val transport = when (val result = resolveTransport(request)) {
                is ChampionBleResult.Success -> result.value
                is ChampionBleResult.Failure -> {
                    mutableState.value = WatchWorkoutImportSurfaceState.Failed(
                        baseSession.copy(
                            startedAt = startedAt,
                            error = result.error.toImportError(WatchWorkoutImportErrorCategory.CONNECTION_FAILED),
                        ),
                    )
                    persistMetadata(
                        deviceId = request.associatedDevice.id,
                        result = WatchWorkoutImportResult.Failed(),
                        summary = null,
                    )
                    return
                }
            }

            val bleSessionId = transport.session.id.value.toString()
            val activeSession = baseSession.copy(
                bleSessionId = bleSessionId,
                startedAt = startedAt,
            )

            val handshakeResult = connectionBootstrap.executeHandshake(transport, ZonedDateTime.now())
            if (handshakeResult is ChampionBleResult.Failure) {
                val error = handshakeResult.error.toImportError(WatchWorkoutImportErrorCategory.HANDSHAKE_FAILED)
                mutableState.value = WatchWorkoutImportSurfaceState.Failed(activeSession.copy(error = error))
                persistMetadata(
                    deviceId = request.associatedDevice.id,
                    result = WatchWorkoutImportResult.Failed(),
                    summary = null,
                )
                return
            }

            val notificationSubscription = transport.notifications.subscribe(ChampionChannelAlias.F008_NOTIFY)
            if (notificationSubscription is ChampionBleResult.Failure) {
                val error = notificationSubscription.error.toImportError(WatchWorkoutImportErrorCategory.F008_UNAVAILABLE)
                mutableState.value = WatchWorkoutImportSurfaceState.Failed(activeSession.copy(error = error))
                persistMetadata(
                    deviceId = request.associatedDevice.id,
                    result = WatchWorkoutImportResult.Failed(),
                    summary = null,
                )
                return
            }

            active = ActiveImport(
                permit = permit,
                importAttemptId = request.importAttemptId,
                bleSessionId = bleSessionId,
                deviceId = request.associatedDevice.id,
                startedAt = startedAt,
                transport = transport,
            )
            lifecycleMutex.withLock {
                activeImport = active
            }

            val packets = (notificationSubscription as ChampionBleResult.Success).value
            mutableState.value = WatchWorkoutImportSurfaceState.ReadingIndex(activeSession)
            val d3Outcome = awaitD3Outcome(active, packets)
            active.summary.discoveredBlockCount = d3Outcome.entries.size
            active.summary.timedOut = d3Outcome.timedOut

            if (d3Outcome.entries.isEmpty()) {
                val summary = buildSummary(active)
                val result = if (d3Outcome.timedOut) {
                    WatchWorkoutImportResult.Failed(summary)
                } else {
                    WatchWorkoutImportResult.Empty(summary)
                }
                val terminalState = if (d3Outcome.timedOut) {
                    WatchWorkoutImportSurfaceState.Failed(
                        activeSession.copy(
                            progress = progressOf(active),
                            result = result,
                            metadata = metadataOf(request.associatedDevice.id, result, summary),
                            error = WatchWorkoutImportError(
                                category = WatchWorkoutImportErrorCategory.D3_TIMEOUT,
                                message = "Tempo limite atingido antes de receber indice D3 valido.",
                                isRetryable = true,
                            ),
                        ),
                    )
                } else {
                    WatchWorkoutImportSurfaceState.Empty(
                        activeSession.copy(
                            progress = progressOf(active),
                            result = result,
                            metadata = metadataOf(request.associatedDevice.id, result, summary),
                        ),
                    )
                }
                mutableState.value = terminalState
                persistMetadata(request.associatedDevice.id, result, summary)
                return
            }

            for (entry in d3Outcome.entries) {
                ensureStillActive(active)
                val lookup = when (val result = workoutActivityRepository.findWatchWorkoutImport(request.associatedDevice.id, entry.crc)) {
                    is RepositoryResult.Success -> result.value
                    RepositoryResult.Conflict,
                    RepositoryResult.NotFound,
                    RepositoryResult.StorageUnavailable,
                    RepositoryResult.UnexpectedState,
                    -> throw ImportFailure(
                        WatchWorkoutImportError(
                            category = WatchWorkoutImportErrorCategory.STORAGE_FAILED,
                            message = "Nao foi possivel consultar importacoes anteriores do treino.",
                            isRetryable = true,
                        ),
                    )
                }
                if (lookup?.canSkipOnResume == true) {
                    active.summary.skippedValidCount += 1
                    active.summary.processedBlockCount += 1
                    mutableState.value = WatchWorkoutImportSurfaceState.ReadingBlocks(
                        activeSession.copy(progress = progressOf(active, currentBlockNumber = entry.blockIndex + 1)),
                    )
                    continue
                }

                mutableState.value = WatchWorkoutImportSurfaceState.ReadingBlocks(
                    activeSession.copy(progress = progressOf(active, currentBlockNumber = entry.blockIndex + 1)),
                )
                val d4Outcome = awaitD4Outcome(active, packets, entry)
                active.summary.missingPacketCount += d4Outcome.missingPacketIndices.size
                if (d4Outcome.timedOut) {
                    active.summary.timedOut = true
                    active.summary.timedOutBlockCount += 1
                }

                mutableState.value = WatchWorkoutImportSurfaceState.Parsing(
                    activeSession.copy(progress = progressOf(active, currentBlockNumber = entry.blockIndex + 1)),
                )
                val parsed = ChampionD4WorkoutParser.parse(
                    ChampionD4WorkoutParser.Request(
                        deviceId = request.associatedDevice.id,
                        crc = entry.crc,
                        externalBlockIndex = entry.blockIndex,
                        parserVersion = parserVersion,
                        block = d4Outcome,
                    ),
                )
                val diagnosticPayloadId = storeDiagnosticPayloadIfNeeded(request, d4Outcome, parsed)

                when (parsed) {
                    is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Invalid -> {
                        active.summary.invalidCount += 1
                    }

                    is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Partial -> {
                        active.summary.partialCount += 1
                        persistParsedBlock(active, activeSession, parsed.input.copy(diagnosticPayloadId = diagnosticPayloadId))
                    }

                    is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Valid -> {
                        persistParsedBlock(active, activeSession, parsed.input.copy(diagnosticPayloadId = diagnosticPayloadId))
                    }
                }
                active.summary.processedBlockCount += 1
            }

            val summary = buildSummary(active)
            val result = if (
                summary.timedOut ||
                summary.partialCount > 0 ||
                summary.invalidCount > 0 ||
                summary.timedOutBlockCount > 0
            ) {
                WatchWorkoutImportResult.Partial(summary)
            } else {
                WatchWorkoutImportResult.Completed(summary)
            }
            val metadata = metadataOf(request.associatedDevice.id, result, summary)
            mutableState.value = when (result) {
                is WatchWorkoutImportResult.Completed -> WatchWorkoutImportSurfaceState.Completed(
                    activeSession.copy(progress = progressOf(active), result = result, metadata = metadata),
                )

                is WatchWorkoutImportResult.Partial -> WatchWorkoutImportSurfaceState.Partial(
                    activeSession.copy(progress = progressOf(active), result = result, metadata = metadata),
                )

                else -> WatchWorkoutImportSurfaceState.Failed(
                    activeSession.copy(progress = progressOf(active), result = result, metadata = metadata),
                )
            }
            persistMetadata(request.associatedDevice.id, result, summary)
        } catch (_: CancellationException) {
            val current = active ?: return
            when (val termination = current.termination) {
                is Termination.Cancelled -> {
                    val summary = buildSummary(current)
                    val result = WatchWorkoutImportResult.Cancelled(summary)
                    val metadata = metadataOf(request.associatedDevice.id, result, summary)
                    mutableState.value = WatchWorkoutImportSurfaceState.Cancelled(
                        WatchWorkoutImportSessionState(
                            importAttemptId = request.importAttemptId,
                            bleSessionId = current.bleSessionId,
                            startedAt = current.startedAt,
                            progress = progressOf(current),
                            result = result,
                            metadata = metadata,
                            error = WatchWorkoutImportError(
                                category = WatchWorkoutImportErrorCategory.CANCELLED,
                                message = "Importacao cancelada antes da conclusao.",
                                isRetryable = true,
                            ),
                        ),
                    )
                    persistMetadata(request.associatedDevice.id, result, summary)
                }

                is Termination.Failed -> {
                    val summary = buildSummary(current)
                    val result = WatchWorkoutImportResult.Failed(summary)
                    val metadata = metadataOf(request.associatedDevice.id, result, summary)
                    mutableState.value = WatchWorkoutImportSurfaceState.Failed(
                        WatchWorkoutImportSessionState(
                            importAttemptId = request.importAttemptId,
                            bleSessionId = current.bleSessionId,
                            startedAt = current.startedAt,
                            progress = progressOf(current),
                            result = result,
                            metadata = metadata,
                            error = termination.error,
                        ),
                    )
                    persistMetadata(request.associatedDevice.id, result, summary)
                }

                null -> throw CancellationException("watch_workout_import_cancelled_without_termination")
            }
        } catch (failure: ImportFailure) {
            val current = active
            val summary = current?.let(::buildSummary)
            val result = WatchWorkoutImportResult.Failed(summary)
            val metadata = summary?.let { metadataOf(request.associatedDevice.id, result, it) }
            mutableState.value = WatchWorkoutImportSurfaceState.Failed(
                WatchWorkoutImportSessionState(
                    importAttemptId = request.importAttemptId,
                    bleSessionId = current?.bleSessionId,
                    startedAt = current?.startedAt,
                    progress = current?.let(::progressOf) ?: WatchWorkoutImportProgress(),
                    result = result,
                    metadata = metadata,
                    error = failure.error,
                ),
            )
            persistMetadata(request.associatedDevice.id, result, summary)
        } finally {
            active?.phaseJob?.cancel()
            foregroundBleOperationGate.release(permit)
            lifecycleMutex.withLock {
                if (activeImport?.importAttemptId == request.importAttemptId) {
                    activeImport = null
                }
            }
        }
    }

    private suspend fun persistParsedBlock(
        active: ActiveImport,
        session: WatchWorkoutImportSessionState,
        input: br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserInput,
    ) {
        mutableState.value = WatchWorkoutImportSurfaceState.Persisting(
            session.copy(progress = progressOf(active, currentBlockNumber = input.externalBlockIndex + 1)),
        )
        val importedAt = civilNow()
        val activity = WorkoutActivity.createFromImport(
            id = WorkoutActivityId(ids.next()),
            input = input,
            importedAt = importedAt,
        ).valueOrNull() ?: throw ImportFailure(
            WatchWorkoutImportError(
                category = WatchWorkoutImportErrorCategory.INTERNAL,
                message = "Nao foi possivel montar atividade importada do treino.",
                isRetryable = false,
            ),
        )
        val importRecord = WatchWorkoutImport.create(
            id = WatchWorkoutImportId(ids.next()),
            activityId = activity.id,
            input = input,
            importedAt = importedAt,
        ).valueOrNull() ?: throw ImportFailure(
            WatchWorkoutImportError(
                category = WatchWorkoutImportErrorCategory.INTERNAL,
                message = "Nao foi possivel montar registro de importacao do treino.",
                isRetryable = false,
            ),
        )
        when (workoutActivityRepository.persistImported(activity, importRecord)) {
            is RepositoryResult.Success -> active.summary.insertedCount += 1
            RepositoryResult.Conflict,
            RepositoryResult.NotFound,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> throw ImportFailure(
                WatchWorkoutImportError(
                    category = WatchWorkoutImportErrorCategory.STORAGE_FAILED,
                    message = "Nao foi possivel salvar treino importado.",
                    isRetryable = true,
                ),
            )
        }
    }

    private suspend fun storeDiagnosticPayloadIfNeeded(
        request: WatchWorkoutImportRequest,
        outcome: ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome,
        parsed: br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult,
    ): DiagnosticPayloadId? {
        if (!isDebugBuild) return null
        val parseResult = when (parsed) {
            is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Valid -> DiagnosticParseResult.COMPLETED
            is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Partial -> DiagnosticParseResult.PARTIAL
            is br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserResult.Invalid -> DiagnosticParseResult.INVALID
        }
        val payloadId = DiagnosticPayloadId(ids.next())
        val payload = DiagnosticPayload.create(
            id = payloadId,
            capture = DiagnosticPayloadCapture(
                command = "WATCH_WORKOUT_D4_BLOCK",
                flow = DiagnosticCaptureFlow.WATCH_WORKOUT,
                privateBytes = outcome.toDiagnosticBytes(),
                deviceId = request.associatedDevice.id,
                firmware = request.associatedDevice.firmware,
                parserVersion = parserVersion,
                capturedAt = civilNow(),
            ),
            result = parseResult,
        ).valueOrNull() ?: return null
        return when (diagnosticPayloadRepository.store(payload)) {
            is RepositoryResult.Success -> payloadId
            RepositoryResult.Conflict,
            RepositoryResult.NotFound,
            RepositoryResult.StorageUnavailable,
            RepositoryResult.UnexpectedState,
            -> null
        }
    }

    private suspend fun awaitD3Outcome(
        active: ActiveImport,
        packets: Flow<br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket>,
    ): ChampionD3WorkoutIndexProtocol.D3IndexCollectionOutcome {
        val phaseStartedAt = civilNow()
        val collector = ChampionD3WorkoutIndexProtocol.IndexCollector(
            ChampionD3WorkoutIndexProtocol.D3IndexContext(
                importAttemptId = active.importAttemptId,
                bleSessionId = active.transport.session.id,
                startedAt = phaseStartedAt,
            ),
        )
        val completion = CompletableDeferred<ChampionD3WorkoutIndexProtocol.D3IndexCollectionOutcome>()
        active.phaseJob = scope.launch {
            try {
                packets.collect { packet ->
                    if (!isActiveImport(active, packet.sessionId.value.toString())) return@collect
                    when (val update = collector.onPacket(packet)) {
                        ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Ignored -> Unit
                        is ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Progress -> {
                            mutableState.value = WatchWorkoutImportSurfaceState.ReadingIndex(
                                WatchWorkoutImportSessionState(
                                    importAttemptId = active.importAttemptId,
                                    bleSessionId = active.bleSessionId,
                                    startedAt = active.startedAt,
                                    progress = progressOf(active),
                                ),
                            )
                        }

                        is ChampionD3WorkoutIndexProtocol.D3IndexCollectionUpdate.Finished -> {
                            completion.complete(update.outcome)
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (!error.isExpectedObserverShutdown()) throw error
            }
        }

        val sendResult = active.transport.sendCommand(ChampionD3WorkoutIndexProtocol.createIndexCommand(d3Timeout))
        if (sendResult is ChampionBleResult.Failure) {
            throw ImportFailure(sendResult.error.toImportError(WatchWorkoutImportErrorCategory.SEND_D3_FAILED))
        }

        return try {
            withTimeoutOrNull(d3Timeout) {
                completion.await()
            } ?: collector.onTimeout()
        } finally {
            active.phaseJob?.cancel()
            active.phaseJob = null
        }
    }

    private suspend fun awaitD4Outcome(
        active: ActiveImport,
        packets: Flow<br.com.mwconsulting.championcompanion.core.bluetooth.ChampionPacket>,
        entry: ChampionD3WorkoutIndexProtocol.D3WorkoutBlockEntry,
    ): ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome {
        val phaseStartedAt = civilNow()
        val collector = ChampionD4WorkoutBlockProtocol.BlockCollector(
            ChampionD4WorkoutBlockProtocol.D4BlockContext(
                importAttemptId = active.importAttemptId,
                bleSessionId = active.transport.session.id,
                startedAt = phaseStartedAt,
                blockIndex = entry.blockIndex,
            ),
        )
        val completion = CompletableDeferred<ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome>()
        active.phaseJob = scope.launch {
            try {
                packets.collect { packet ->
                    if (!isActiveImport(active, packet.sessionId.value.toString())) return@collect
                    when (val update = collector.onPacket(packet)) {
                        ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Ignored -> Unit
                        is ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Progress -> {
                            mutableState.value = WatchWorkoutImportSurfaceState.ReadingBlocks(
                                WatchWorkoutImportSessionState(
                                    importAttemptId = active.importAttemptId,
                                    bleSessionId = active.bleSessionId,
                                    startedAt = active.startedAt,
                                    progress = progressOf(
                                        active,
                                        currentBlockNumber = entry.blockIndex + 1,
                                        currentPacketIndex = update.progress.highestPacketIndex,
                                        totalPacketsInBlock = update.progress.totalPackets.takeIf { it > 0 },
                                    ),
                                ),
                            )
                        }

                        is ChampionD4WorkoutBlockProtocol.D4BlockCollectionUpdate.Finished -> {
                            completion.complete(update.outcome)
                        }
                    }
                }
            } catch (_: CancellationException) {
                Unit
            } catch (error: RuntimeException) {
                if (!error.isExpectedObserverShutdown()) throw error
            }
        }

        val sendResult = active.transport.sendCommand(
            ChampionD4WorkoutBlockProtocol.createBlockCommand(entry.blockIndex, d4Timeout),
        )
        if (sendResult is ChampionBleResult.Failure) {
            throw ImportFailure(sendResult.error.toImportError(WatchWorkoutImportErrorCategory.SEND_D4_FAILED))
        }

        return try {
            withTimeoutOrNull(d4Timeout) {
                completion.await()
            } ?: collector.onTimeout()
        } finally {
            active.phaseJob?.cancel()
            active.phaseJob = null
        }
    }

    private suspend fun resolveTransport(
        request: WatchWorkoutImportRequest,
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

    private suspend fun persistMetadata(
        deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        result: WatchWorkoutImportResult,
        summary: WatchWorkoutImportSummary?,
    ) {
        val metadata = summary?.let { metadataOf(deviceId, result, it) } ?: WatchWorkoutImportMetadata(
            deviceId = deviceId,
            lastAttemptAt = civilNow(),
            lastSuccessAt = null,
            lastResult = result.status,
            parserVersion = parserVersion,
            updatedAt = civilNow(),
        )
        workoutActivityRepository.updateWatchWorkoutImportMetadata(metadata)
    }

    private fun metadataOf(
        deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        result: WatchWorkoutImportResult,
        summary: WatchWorkoutImportSummary,
    ): WatchWorkoutImportMetadata = WatchWorkoutImportMetadata(
        deviceId = deviceId,
        lastAttemptAt = summary.startedAt,
        lastSuccessAt = when (result.status) {
            WatchWorkoutImportStatus.COMPLETED,
            WatchWorkoutImportStatus.PARTIAL,
            WatchWorkoutImportStatus.EMPTY,
            -> summary.finishedAt

            WatchWorkoutImportStatus.CANCELLED,
            WatchWorkoutImportStatus.FAILED,
            -> null
        },
        lastResult = result.status,
        parserVersion = parserVersion,
        insertedCount = summary.insertedCount,
        duplicateCount = summary.duplicateCount,
        invalidCount = summary.invalidCount,
        missingIndexCount = summary.missingPacketCount,
        timedOut = summary.timedOut,
        updatedAt = summary.finishedAt,
    )

    private fun buildSummary(active: ActiveImport): WatchWorkoutImportSummary = WatchWorkoutImportSummary(
        discoveredBlockCount = active.summary.discoveredBlockCount,
        processedBlockCount = active.summary.processedBlockCount,
        insertedCount = active.summary.insertedCount,
        duplicateCount = active.summary.duplicateCount,
        partialCount = active.summary.partialCount,
        invalidCount = active.summary.invalidCount,
        timedOutBlockCount = active.summary.timedOutBlockCount,
        skippedValidCount = active.summary.skippedValidCount,
        missingPacketCount = active.summary.missingPacketCount,
        startedAt = active.startedAt,
        finishedAt = civilNow(),
        timedOut = active.summary.timedOut,
    )

    private fun progressOf(
        active: ActiveImport,
        currentBlockNumber: Int? = null,
        currentPacketIndex: Int? = null,
        totalPacketsInBlock: Int? = null,
    ): WatchWorkoutImportProgress = WatchWorkoutImportProgress(
        discoveredBlockCount = active.summary.discoveredBlockCount,
        processedBlockCount = active.summary.processedBlockCount,
        currentBlockNumber = currentBlockNumber,
        currentPacketIndex = currentPacketIndex,
        totalPacketsInBlock = totalPacketsInBlock,
        missingPacketCount = active.summary.missingPacketCount,
    )

    // Binary envelope keeps packet boundaries for later local SQL hex inspection.
    private fun ChampionD4WorkoutBlockProtocol.D4BlockCollectionOutcome.toDiagnosticBytes(): ByteArray =
        ByteArrayOutputStream().use { output ->
            output.write(byteArrayOf('D'.code.toByte(), '4'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte()))
            output.writeUInt16LittleEndian(blockIndex)
            output.writeUInt16LittleEndian(packets.size)
            packets.forEach { packet ->
                val rawBytes = packet.rawBytesCopy()
                output.writeUInt16LittleEndian(packet.packetIndex)
                output.writeUInt16LittleEndian(packet.totalPackets)
                output.writeUInt16LittleEndian(rawBytes.size)
                output.write(rawBytes)
            }
            output.toByteArray()
        }

    private fun ByteArrayOutputStream.writeUInt16LittleEndian(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private suspend fun ensureStillActive(active: ActiveImport) {
        lifecycleMutex.withLock {
            when {
                activeImport !== active -> throw CancellationException("watch_workout_attempt_replaced")
                active.termination != null -> throw CancellationException("watch_workout_attempt_terminated")
                else -> Unit
            }
        }
    }

    private suspend fun requestTermination(active: ActiveImport, termination: Termination) {
        lifecycleMutex.withLock {
            if (activeImport !== active || active.termination != null) return
            active.termination = termination
            active.phaseJob?.cancel()
            activeJob?.cancel()
        }
    }

    private suspend fun handleBleStateChanged(state: ChampionBleState) {
        val active = activeImport ?: return
        when (state) {
            ChampionBleState.Disconnected -> requestTermination(
                active,
                Termination.Failed(
                    WatchWorkoutImportError(
                        category = WatchWorkoutImportErrorCategory.UNEXPECTED_DISCONNECT,
                        message = "Conexao com relogio foi encerrada durante a importacao.",
                        isRetryable = true,
                    ),
                ),
            )

            is ChampionBleState.Disconnecting -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(
                        active,
                        Termination.Failed(
                            WatchWorkoutImportError(
                                category = WatchWorkoutImportErrorCategory.UNEXPECTED_DISCONNECT,
                                message = "Conexao com relogio foi encerrada durante a importacao.",
                                isRetryable = true,
                            ),
                        ),
                    )
                }
            }

            is ChampionBleState.Failed -> {
                if (state.session?.id?.value?.toString() == active.bleSessionId) {
                    requestTermination(active, Termination.Failed(state.error.toImportError()))
                }
            }

            is ChampionBleState.Connecting,
            is ChampionBleState.ConfiguringSubscriptions,
            is ChampionBleState.DiscoveringServices,
            is ChampionBleState.ResolvingChannels,
            is ChampionBleState.Scanning,
            is ChampionBleState.TransportReady,
            -> Unit
        }
    }

    private fun isActiveImport(active: ActiveImport, bleSessionId: String): Boolean =
        activeImport === active && active.bleSessionId == bleSessionId && active.termination == null

    private fun br.com.mwconsulting.championcompanion.core.model.Device.toDiscoveredDevice() =
        br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDiscoveredDevice(
            transportKey = platformIdentifier,
            name = name,
            localName = name,
            observedAt = civilNow(),
        )

    private fun ChampionBleError.toImportError(
        defaultCategory: WatchWorkoutImportErrorCategory = WatchWorkoutImportErrorCategory.CONNECTION_FAILED,
    ): WatchWorkoutImportError = when (this) {
        ChampionBleError.BluetoothDisabled -> WatchWorkoutImportError(
            category = WatchWorkoutImportErrorCategory.BLUETOOTH_DISABLED,
            message = "Ative o Bluetooth para buscar treinos do relogio.",
            isRetryable = true,
        )

        ChampionBleError.ScanPermissionMissing,
        ChampionBleError.ConnectPermissionMissing,
        ChampionBleError.LegacyPermissionMissing,
        ChampionBleError.LocationPermissionRequired,
        -> WatchWorkoutImportError(
            category = WatchWorkoutImportErrorCategory.PERMISSIONS_REQUIRED,
            message = "Permissoes de Bluetooth sao necessarias para buscar treinos do relogio.",
            isRetryable = true,
        )

        ChampionBleError.DeviceNotFound -> WatchWorkoutImportError(
            category = WatchWorkoutImportErrorCategory.NO_ASSOCIATED_DEVICE,
            message = "Nao foi possivel localizar relogio associado.",
            isRetryable = true,
        )

        is ChampionBleError.MissingOptionalChannel,
        is ChampionBleError.MissingRequiredChannel,
        is ChampionBleError.SubscriptionFailed,
        -> WatchWorkoutImportError(
            category = if (
                (this is ChampionBleError.MissingOptionalChannel && channel == ChampionChannelAlias.F008_NOTIFY) ||
                (this is ChampionBleError.MissingRequiredChannel && channel == ChampionChannelAlias.F008_NOTIFY) ||
                (this is ChampionBleError.SubscriptionFailed && channel == ChampionChannelAlias.F008_NOTIFY)
            ) {
                WatchWorkoutImportErrorCategory.F008_UNAVAILABLE
            } else {
                defaultCategory
            },
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )

        is ChampionBleError.OperationCancelled,
        is ChampionBleError.QueueClosed,
        ChampionBleError.CleanupInProgress,
        -> WatchWorkoutImportError(
            category = WatchWorkoutImportErrorCategory.CANCELLED,
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
        is ChampionBleError.MissingRequiredService,
        ChampionBleError.OperationUnavailable,
        ChampionBleError.UnexpectedInternal,
        ChampionBleError.ScanAlreadyInProgress,
        ChampionBleError.BleNotSupported,
        is ChampionBleError.StaleSession,
        is ChampionBleError.ScanFailed,
        -> WatchWorkoutImportError(
            category = defaultCategory,
            message = toSafeMessage(),
            isRetryable = isRecoverable,
        )
    }

    private fun Throwable.isExpectedObserverShutdown(): Boolean =
        message?.startsWith("notification_hub_closed:") == true

    private fun ForegroundBleOperationOwner.toExercisesForegroundBleBlockMessage(): String = when (this) {
        ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Uma sincronização do relógio está em andamento na tela Início."
        ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Uma medição manual está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Uma importação de histórico do relógio está em andamento na tela Saúde."
        ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Uma importação de treinos do relógio já está em andamento na tela Exercícios."
        ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "A participação opcional do relógio está em andamento na sessão de treino."
        ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento na tela Dispositivo."
    }

    private data class ActiveImport(
        val permit: ForegroundBleOperationPermit,
        val importAttemptId: String,
        val bleSessionId: String,
        val deviceId: br.com.mwconsulting.championcompanion.core.model.DeviceId,
        val startedAt: Instant,
        val transport: ChampionTransportSession,
        val summary: MutableSummary = MutableSummary(),
        var phaseJob: Job? = null,
        var termination: Termination? = null,
    )

    private data class MutableSummary(
        var discoveredBlockCount: Int = 0,
        var processedBlockCount: Int = 0,
        var insertedCount: Int = 0,
        var duplicateCount: Int = 0,
        var partialCount: Int = 0,
        var invalidCount: Int = 0,
        var timedOutBlockCount: Int = 0,
        var skippedValidCount: Int = 0,
        var missingPacketCount: Int = 0,
        var timedOut: Boolean = false,
    )

    private sealed interface Termination {
        data class Cancelled(val reason: WatchWorkoutImportCancellationReason) : Termination
        data class Failed(val error: WatchWorkoutImportError) : Termination
    }

    private class ImportFailure(
        val error: WatchWorkoutImportError,
    ) : RuntimeException(error.message)
}
