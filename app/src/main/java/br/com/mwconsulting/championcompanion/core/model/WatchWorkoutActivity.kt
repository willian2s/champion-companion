package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant

enum class ParserValidity {
    VALID,
    PARTIAL,
    INVALID,
}

internal const val WATCH_WORKOUT_D4_V1_PARSER_VERSION = "d4-v1"
internal const val WATCH_WORKOUT_D4_V2_PARSER_VERSION = "d4-v2"

internal fun isSupportedWatchWorkoutD4ParserVersion(parserVersion: ParserVersion?): Boolean =
    parserVersion?.value == WATCH_WORKOUT_D4_V1_PARSER_VERSION ||
        parserVersion?.value == WATCH_WORKOUT_D4_V2_PARSER_VERSION

enum class WorkoutType {
    WALKING,
    RUNNING,
    FREE_WORKOUT,
    TREADMILL,
    UNKNOWN,
}

/** Proveniência primária; importação D3/D4 pode enriquecer atividade phone. */
enum class WorkoutActivityOrigin {
    WATCH_D3_D4,
    PHONE_GPS,
    PHONE_TREADMILL,
    PHONE_FREE_WORKOUT,
}

enum class ActivityWorkflowState {
    PENDING_REVIEW,
    PENDING_CALIBRATION,
    READY_TO_SYNC,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
    DISCARDED,
}

data class WatchWorkoutImportKey(
    val deviceId: DeviceId,
    val origin: DataOrigin,
    val crc: Int,
)

data class WorkoutDistanceMetrics(
    val originalMeters: DomainValue<Double>,
    val estimatedMeters: DomainValue<Double>,
    val calibratedMeters: DomainValue<Double>,
)

data class WorkoutMetrics(
    val startedAt: Instant,
    val duration: Duration,
    val steps: Long,
    val caloriesKilocalories: DomainValue<Double>,
    val distances: WorkoutDistanceMetrics,
    val averagePaceSecondsPerKilometer: DomainValue<Double>,
    val averageSpeedMetersPerSecond: DomainValue<Double>,
    val averageBpm: DomainValue<Int>,
    val minimumBpm: DomainValue<Int>,
    val maximumBpm: DomainValue<Int>,
    /** Valores calibrados vivem ao lado das métricas de origem e nunca as substituem. */
    val calibratedMetrics: CalibratedWorkoutMetrics? = null,
    val stepsAvailable: Boolean = true,
    /** Fim temporal efetivo; pode exceder duração ativa quando houve pausas. */
    val endedAt: Instant = startedAt.plus(duration),
)

data class WorkoutInterval(
    val index: Int,
    val duration: Duration,
    val steps: Long,
    val distanceMeters: DomainValue<Double>,
    val caloriesKilocalories: DomainValue<Double>,
    /** Movimento D4 integral, sem promover os bits inferiores a uma métrica. */
    val rawMotionValue: Long? = null,
    /** Distância temporal D4 preservada sem unidade semântica confirmada. */
    val rawDistanceValue: Long? = null,
    /** Calorias temporais D4 preservadas sem unidade semântica confirmada. */
    val rawCaloriesValue: Long? = null,
    /** Passos medidos após decodificação explícita da versão do parser. */
    val decodedSteps: Long? = null,
)

data class WorkoutHeartRateSample(
    val sampledAt: Instant,
    val bpm: Int,
)

/** Valores interpretados cruzam o parser sem reter pacotes binários D3/D4. */
data class WatchWorkoutParserInput(
    val deviceId: DeviceId,
    val crc: Int,
    val externalBlockIndex: Int,
    val parserVersion: ParserVersion,
    val parserValidity: ParserValidity,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val metrics: WorkoutMetrics,
    val intervals: List<WorkoutInterval>,
    val heartRateSamples: List<WorkoutHeartRateSample>,
)

/**
 * Snapshot de compatibilidade da proveniência D3/D4 na atividade. Metadados
 * persistentes e identidade da importação pertencem a [WatchWorkoutImport].
 */
data class WatchWorkoutActivityImportContext(
    val key: WatchWorkoutImportKey,
    val externalBlockIndex: Int,
    val parserVersion: ParserVersion,
    val parserValidity: ParserValidity,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val importedAt: Instant,
) {
    companion object {
        fun from(input: WatchWorkoutParserInput, importedAt: Instant): DomainResult<WatchWorkoutActivityImportContext> =
            if (input.crc !in VALID_CRC_RANGE || input.externalBlockIndex < 0 ||
                importedAt.isBefore(input.metrics.startedAt) ||
                !isSupportedWatchWorkoutD4ParserVersion(input.parserVersion)
            ) {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            } else {
                DomainResult.Success(WatchWorkoutActivityImportContext(
                    key = WatchWorkoutImportKey(input.deviceId, DataOrigin.WATCH_D3_D4, input.crc),
                    externalBlockIndex = input.externalBlockIndex,
                    parserVersion = input.parserVersion,
                    parserValidity = input.parserValidity,
                    diagnosticPayloadId = input.diagnosticPayloadId,
                    importedAt = importedAt,
                ))
            }

        private val VALID_CRC_RANGE = 1..0xffff
    }
}

@ConsistentCopyVisibility
data class WatchWorkoutImport private constructor(
    val id: WatchWorkoutImportId,
    val activityId: WorkoutActivityId,
    val key: WatchWorkoutImportKey,
    val externalBlockIndex: Int,
    val parserVersion: ParserVersion,
    val parserValidity: ParserValidity,
    val diagnosticPayloadId: DiagnosticPayloadId?,
    val importedAt: Instant,
) {
    val activityContext: WatchWorkoutActivityImportContext
        get() = WatchWorkoutActivityImportContext(
            key = key,
            externalBlockIndex = externalBlockIndex,
            parserVersion = parserVersion,
            parserValidity = parserValidity,
            diagnosticPayloadId = diagnosticPayloadId,
            importedAt = importedAt,
        )

    companion object {
        fun create(
            id: WatchWorkoutImportId,
            activityId: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
        ): DomainResult<WatchWorkoutImport> = when (val context = WatchWorkoutActivityImportContext.from(input, importedAt)) {
            is DomainResult.Success -> DomainResult.Success(fromContext(id, activityId, context.value))
            is DomainResult.Failure -> context
        }

        fun restore(
            id: WatchWorkoutImportId,
            activityId: WorkoutActivityId,
            context: WatchWorkoutActivityImportContext,
        ): DomainResult<WatchWorkoutImport> =
            if (context.key.origin == DataOrigin.WATCH_D3_D4 && context.key.crc in 1..0xffff &&
                context.externalBlockIndex >= 0 &&
                isSupportedWatchWorkoutD4ParserVersion(context.parserVersion)
            ) {
                DomainResult.Success(fromContext(id, activityId, context))
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }

        private fun fromContext(
            id: WatchWorkoutImportId,
            activityId: WorkoutActivityId,
            context: WatchWorkoutActivityImportContext,
        ) = WatchWorkoutImport(
            id = id,
            activityId = activityId,
            key = context.key,
            externalBlockIndex = context.externalBlockIndex,
            parserVersion = context.parserVersion,
            parserValidity = context.parserValidity,
            diagnosticPayloadId = context.diagnosticPayloadId,
            importedAt = context.importedAt,
        )
    }
}

data class PhoneWorkoutActivityInput(
    val session: AppWorkoutSession,
    val intervals: List<WorkoutInterval> = emptyList(),
    val heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
)

@ConsistentCopyVisibility
data class WorkoutActivity private constructor(
    val id: WorkoutActivityId,
    val activityOrigin: WorkoutActivityOrigin,
    /** Dispositivo associado é opcional para sessões phone. */
    val associatedDeviceId: DeviceId?,
    val appWorkoutSessionId: AppWorkoutSessionId?,
    val watchImportContext: WatchWorkoutActivityImportContext?,
    val recordedAt: Instant,
    val metrics: WorkoutMetrics,
    val intervals: List<WorkoutInterval>,
    val heartRateSamples: List<WorkoutHeartRateSample>,
    val suggestedType: WorkoutType,
    val title: String?,
    val notes: String?,
    val confirmedType: WorkoutType?,
    val workflowState: ActivityWorkflowState,
    val reviewedAt: Instant?,
    val calibrationId: TreadmillCalibrationId?,
    /** Série derivada local carregada separadamente dos fatos raw/measured. */
    val derivationSnapshot: WorkoutDerivationSnapshot? = null,
) {
    /** Compatibilidade para consumidores anteriores à generalização. */
    val origin: DataOrigin
        get() = if (activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4) {
            DataOrigin.WATCH_D3_D4
        } else {
            DataOrigin.APP_WORKOUT_SESSION
        }
    val hasPhoneSession: Boolean get() = appWorkoutSessionId != null
    val hasWatchImport: Boolean get() = watchImportContext != null
    val hasCombinedProvenance: Boolean get() = hasPhoneSession && hasWatchImport
    val recordsRoute: Boolean get() = activityOrigin == WorkoutActivityOrigin.PHONE_GPS &&
        suggestedType in setOf(WorkoutType.WALKING, WorkoutType.RUNNING)

    /** Ponte interna somente para mapper D3/D4 legado; removida do contrato público. */
    internal val deviceId: DeviceId get() = legacyImportContext().key.deviceId
    val importKey: WatchWorkoutImportKey? get() = watchImportContext?.key
    val importParserValidity: ParserValidity? get() = watchImportContext?.parserValidity
    /** Bridge fail-closed temporária para consumidores D3/D4 ainda não generalizados. */
    val parserValidity: ParserValidity get() = importParserValidity ?: ParserValidity.INVALID
    internal val importedAt: Instant get() = legacyImportContext().importedAt
    internal val parserVersion: ParserVersion get() = legacyImportContext().parserVersion
    val diagnosticPayloadId: DiagnosticPayloadId? get() = watchImportContext?.diagnosticPayloadId
    val canonicalDistanceMeters: Double?
        get() = derivationSnapshot?.canonicalDistanceMeters
    val canonicalSeries: CanonicalWorkoutSeries?
        get() = derivationSnapshot?.canonicalSeries

    private fun legacyImportContext(): WatchWorkoutActivityImportContext = checkNotNull(watchImportContext) {
        "Mapper legado D3/D4 não aceita atividade phone; use proveniência opcional generalizada"
    }

    fun submitReview(
        input: SubmitWorkoutReviewInput,
        derivationSnapshot: WorkoutDerivationSnapshot? = this.derivationSnapshot,
    ): DomainResult<WorkoutActivity> {
        val normalizedTitle = normalizeTitle(input.title)
        val normalizedNotes = normalizeNotes(input.notes)
        if (normalizedTitle.isInvalidNormalizedText() || normalizedNotes.isInvalidNormalizedText()) {
            return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
        if (input.activityId != id || input.confirmedType == WorkoutType.UNKNOWN) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }

        return confirmReview(input.confirmedType, input.reviewedAt, derivationSnapshot).mapSuccess { reviewed ->
            reviewed.copy(title = normalizedTitle, notes = normalizedNotes)
        }
    }

    fun confirmReview(
        type: WorkoutType,
        reviewedAt: Instant,
        derivationSnapshot: WorkoutDerivationSnapshot? = this.derivationSnapshot,
    ): DomainResult<WorkoutActivity> {
        if (workflowState != ActivityWorkflowState.PENDING_REVIEW ||
            reviewedAt.isBefore(recordedAt) || type == WorkoutType.UNKNOWN
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        return DomainResult.Success(copy(
            confirmedType = type,
            workflowState = WatchWorkoutWorkflowPolicy.stateAfterReview(
                origin = activityOrigin,
                confirmedType = type,
                derivationSnapshot = derivationSnapshot,
            ),
            reviewedAt = reviewedAt,
        ))
    }

    fun markCalibrated(): DomainResult<WorkoutActivity> =
        if (workflowState == ActivityWorkflowState.PENDING_CALIBRATION &&
            metrics.distances.calibratedMeters is DomainValue.Present
        ) {
            DomainResult.Success(copy(workflowState = ActivityWorkflowState.READY_TO_SYNC))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

    fun applyCalibration(calibration: TreadmillCalibration): DomainResult<WorkoutActivity> {
        if (workflowState != ActivityWorkflowState.PENDING_CALIBRATION ||
            calibration.subject != TreadmillCalibrationSubject.Activity(id) ||
            reviewedAt == null || calibration.appliedAt.isBefore(reviewedAt)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }

        return DomainResult.Success(copy(
            calibrationId = calibration.id,
            workflowState = ActivityWorkflowState.READY_TO_SYNC,
            metrics = metrics.copy(
                distances = metrics.distances.copy(calibratedMeters = calibration.calibratedMeters),
                calibratedMetrics = calibration.derivedMetrics,
            ),
        ))
    }

    fun startSync(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED),
        to = ActivityWorkflowState.SYNCING,
    )

    fun markSynced(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.SYNCING),
        to = ActivityWorkflowState.SYNCED,
    )

    fun markSyncFailed(): DomainResult<WorkoutActivity> = transition(
        from = setOf(ActivityWorkflowState.SYNCING),
        to = ActivityWorkflowState.SYNC_FAILED,
    )

    fun discard(): DomainResult<WorkoutActivity> = transition(
        from = setOf(
            ActivityWorkflowState.PENDING_REVIEW,
            ActivityWorkflowState.PENDING_CALIBRATION,
            ActivityWorkflowState.READY_TO_SYNC,
        ),
        to = ActivityWorkflowState.DISCARDED,
    )

    fun reimport(
        input: WatchWorkoutParserInput,
        importedAt: Instant,
    ): DomainResult<WorkoutActivity> {
        val currentImport = watchImportContext ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        val nextImport = when (val result = WatchWorkoutActivityImportContext.from(input, importedAt)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return result
        }
        if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 || input.deviceId != associatedDeviceId ||
            input.crc != currentImport.key.crc || importedAt.isBefore(currentImport.importedAt) ||
            recordedAt.isBefore(input.metrics.startedAt) || !isValidInput(input)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        }

        return DomainResult.Success(copy(
            watchImportContext = nextImport,
            metrics = input.metrics.copy(
                distances = input.metrics.distances.copy(
                    calibratedMeters = metrics.distances.calibratedMeters,
                ),
                calibratedMetrics = metrics.calibratedMetrics,
            ),
            intervals = input.intervals,
            heartRateSamples = input.heartRateSamples,
            suggestedType = suggestType(input.metrics.steps),
            derivationSnapshot = null,
        ))
    }

    internal fun mergeCorrelatedWatchImport(
        input: WatchWorkoutParserInput,
        importedAt: Instant,
    ): DomainResult<WorkoutActivity> {
        if (activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 || appWorkoutSessionId == null || hasWatchImport ||
            workflowState !in setOf(
                ActivityWorkflowState.PENDING_REVIEW,
                ActivityWorkflowState.PENDING_CALIBRATION,
                ActivityWorkflowState.READY_TO_SYNC,
            ) ||
            (associatedDeviceId != null && associatedDeviceId != input.deviceId) ||
            input.parserValidity == ParserValidity.INVALID || !isValidInput(input)
        ) {
            return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        }
        val context = when (val result = WatchWorkoutActivityImportContext.from(input, importedAt)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return result
        }
        val eligibleWatchSamples = input.heartRateSamples.filter {
            !it.sampledAt.isBefore(metrics.startedAt) && !it.sampledAt.isAfter(metrics.endedAt)
        }
        val eligibleSamples = if (eligibleWatchSamples.isEmpty()) {
            heartRateSamples
        } else {
            (heartRateSamples + eligibleWatchSamples)
                .associateBy { it.sampledAt }
                .values
                .sortedBy { it.sampledAt }
        }
        val watchBpm = if (eligibleWatchSamples.isEmpty()) {
            Triple(metrics.averageBpm, metrics.minimumBpm, metrics.maximumBpm)
        } else {
            bpmMetrics(eligibleSamples)
        }
        return DomainResult.Success(copy(
            associatedDeviceId = associatedDeviceId ?: input.deviceId,
            watchImportContext = context,
            metrics = metrics.copy(
                steps = if (input.metrics.stepsAvailable) input.metrics.steps else metrics.steps,
                stepsAvailable = input.metrics.stepsAvailable || metrics.stepsAvailable,
                caloriesKilocalories = preferValid(input.metrics.caloriesKilocalories, metrics.caloriesKilocalories),
                averageBpm = watchBpm.first,
                minimumBpm = watchBpm.second,
                maximumBpm = watchBpm.third,
            ),
            heartRateSamples = eligibleSamples,
        ))
    }

    private fun transition(
        from: Set<ActivityWorkflowState>,
        to: ActivityWorkflowState,
    ): DomainResult<WorkoutActivity> =
        if (workflowState in from) DomainResult.Success(copy(workflowState = to))
        else DomainResult.Failure(DomainValidationError.INVALID_STATE)

    companion object {
        fun createFromImport(
            id: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
        ): DomainResult<WorkoutActivity> = createImported(
            id = id,
            input = input,
            importedAt = importedAt,
            title = null,
            notes = null,
            confirmedType = null,
            workflowState = ActivityWorkflowState.PENDING_REVIEW,
            reviewedAt = null,
            calibrationId = null,
        )

        fun createPersistedImported(
            id: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
            title: String?,
            notes: String?,
            confirmedType: WorkoutType?,
            workflowState: ActivityWorkflowState,
            reviewedAt: Instant?,
            calibrationId: TreadmillCalibrationId?,
        ): DomainResult<WorkoutActivity> = createImported(
            id, input, importedAt, title, notes, confirmedType, workflowState, reviewedAt, calibrationId,
        )

        fun createFromPhoneSession(
            id: WorkoutActivityId,
            input: PhoneWorkoutActivityInput,
        ): DomainResult<WorkoutActivity> {
            val session = input.session
            val startedAt = session.startedAt
            val endedAt = session.endedAt
            if (session.state !in setOf(AppWorkoutSessionState.FINISHING, AppWorkoutSessionState.FINISHED) ||
                (session.workoutActivityId != null && session.workoutActivityId != id) ||
                (session.state == AppWorkoutSessionState.FINISHED && session.workoutActivityId != id) ||
                startedAt == null || endedAt == null ||
                session.type == WorkoutType.UNKNOWN
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
            val metrics = metricsFromSession(session, startedAt)
            if (!areValid(metrics) || !areValidIntervals(input.intervals) ||
                !areValidHeartRateSamples(input.heartRateSamples) ||
                !areValidTemporalOrder(input.heartRateSamples) ||
                input.heartRateSamples.any { it.sampledAt.isBefore(startedAt) || it.sampledAt.isAfter(endedAt) }
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            return DomainResult.Success(WorkoutActivity(
                id = id,
                activityOrigin = phoneOrigin(session.type),
                associatedDeviceId = session.associatedDeviceId,
                appWorkoutSessionId = session.id,
                watchImportContext = null,
                recordedAt = endedAt,
                metrics = metrics,
                intervals = input.intervals,
                heartRateSamples = input.heartRateSamples,
                suggestedType = session.type,
                title = null,
                notes = null,
                confirmedType = null,
                workflowState = ActivityWorkflowState.PENDING_REVIEW,
                reviewedAt = null,
                calibrationId = null,
            ))
        }

        @Suppress("LongParameterList")
        fun restore(
            id: WorkoutActivityId,
            activityOrigin: WorkoutActivityOrigin,
            associatedDeviceId: DeviceId?,
            appWorkoutSessionId: AppWorkoutSessionId?,
            watchImportContext: WatchWorkoutActivityImportContext?,
            recordedAt: Instant,
            metrics: WorkoutMetrics,
            intervals: List<WorkoutInterval>,
            heartRateSamples: List<WorkoutHeartRateSample>,
            suggestedType: WorkoutType,
            title: String?,
            notes: String?,
            confirmedType: WorkoutType?,
            workflowState: ActivityWorkflowState,
            reviewedAt: Instant?,
            calibrationId: TreadmillCalibrationId?,
            derivationSnapshot: WorkoutDerivationSnapshot? = null,
        ): DomainResult<WorkoutActivity> {
            val normalizedTitle = normalizeTitle(title)
            val normalizedNotes = normalizeNotes(notes)
            val candidate = WorkoutActivity(
                id, activityOrigin, associatedDeviceId, appWorkoutSessionId, watchImportContext, recordedAt,
                metrics, intervals, heartRateSamples, suggestedType, normalizedTitle,
                normalizedNotes, confirmedType, workflowState, reviewedAt, calibrationId, derivationSnapshot,
            )
            return if (normalizedTitle.isInvalidNormalizedText() || normalizedNotes.isInvalidNormalizedText() ||
                !candidate.hasValidProvenance() || !isValidCommon(candidate)
            ) {
                DomainResult.Failure(DomainValidationError.INVALID_STATE)
            } else {
                DomainResult.Success(candidate)
            }
        }

        fun suggestType(steps: Long): WorkoutType =
            if (steps < STEPS_FOR_RUNNING_SUGGESTION) WorkoutType.FREE_WORKOUT else WorkoutType.RUNNING

        private fun createImported(
            id: WorkoutActivityId,
            input: WatchWorkoutParserInput,
            importedAt: Instant,
            title: String?,
            notes: String?,
            confirmedType: WorkoutType?,
            workflowState: ActivityWorkflowState,
            reviewedAt: Instant?,
            calibrationId: TreadmillCalibrationId?,
        ): DomainResult<WorkoutActivity> {
            val context = when (val result = WatchWorkoutActivityImportContext.from(input, importedAt)) {
                is DomainResult.Success -> result.value
                is DomainResult.Failure -> return result
            }
            val normalizedTitle = normalizeTitle(title)
            val normalizedNotes = normalizeNotes(notes)
            if (!isValidInput(input) || normalizedTitle.isInvalidNormalizedText() ||
                normalizedNotes.isInvalidNormalizedText()
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            return DomainResult.Success(WorkoutActivity(
                id = id,
                activityOrigin = WorkoutActivityOrigin.WATCH_D3_D4,
                associatedDeviceId = input.deviceId,
                appWorkoutSessionId = null,
                watchImportContext = context,
                recordedAt = importedAt,
                metrics = input.metrics,
                intervals = input.intervals,
                heartRateSamples = input.heartRateSamples,
                suggestedType = suggestType(input.metrics.steps),
                title = normalizedTitle,
                notes = normalizedNotes,
                confirmedType = confirmedType,
                workflowState = workflowState,
                reviewedAt = reviewedAt,
                calibrationId = calibrationId,
            ))
        }

        private fun metricsFromSession(session: AppWorkoutSession, startedAt: Instant): WorkoutMetrics {
            val distance = session.aggregates.distanceMeters
            val durationSeconds = session.activeDuration.toNanos().toDouble() / 1_000_000_000.0
            val distanceValue = (distance as? DomainValue.Present)?.value
            val speed = if (distanceValue != null && distanceValue > 0.0 && durationSeconds > 0.0) {
                DomainValue.Present(distanceValue / durationSeconds)
            } else {
                DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED)
            }
            val pace = if (distanceValue != null && distanceValue > 0.0 && durationSeconds > 0.0) {
                DomainValue.Present(durationSeconds * 1_000.0 / distanceValue)
            } else {
                DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED)
            }
            return WorkoutMetrics(
                startedAt = startedAt,
                duration = session.activeDuration,
                steps = session.aggregates.steps,
                caloriesKilocalories = session.aggregates.caloriesKilocalories,
                distances = WorkoutDistanceMetrics(
                    originalMeters = distance,
                    estimatedMeters = DomainValue.Absent(AbsenceReason.NOT_APPLICABLE),
                    calibratedMeters = DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED),
                ),
                averagePaceSecondsPerKilometer = pace,
                averageSpeedMetersPerSecond = speed,
                averageBpm = session.aggregates.averageBpm,
                minimumBpm = session.aggregates.minimumBpm,
                maximumBpm = session.aggregates.maximumBpm,
                stepsAvailable = session.aggregates.stepsAvailable,
                endedAt = requireNotNull(session.endedAt),
            )
        }

        private fun phoneOrigin(type: WorkoutType): WorkoutActivityOrigin = when (type) {
            WorkoutType.WALKING, WorkoutType.RUNNING -> WorkoutActivityOrigin.PHONE_GPS
            WorkoutType.TREADMILL -> WorkoutActivityOrigin.PHONE_TREADMILL
            WorkoutType.FREE_WORKOUT -> WorkoutActivityOrigin.PHONE_FREE_WORKOUT
            WorkoutType.UNKNOWN -> error("Tipo UNKNOWN validado antes da conversão")
        }

        private fun WorkoutActivity.hasValidProvenance(): Boolean = when (activityOrigin) {
            WorkoutActivityOrigin.WATCH_D3_D4 -> appWorkoutSessionId == null && associatedDeviceId != null &&
                watchImportContext != null && watchImportContext.key.deviceId == associatedDeviceId
            WorkoutActivityOrigin.PHONE_GPS -> appWorkoutSessionId != null && hasValidOptionalWatchContext() &&
                suggestedType in setOf(WorkoutType.WALKING, WorkoutType.RUNNING)
            WorkoutActivityOrigin.PHONE_TREADMILL -> appWorkoutSessionId != null && hasValidOptionalWatchContext() &&
                suggestedType == WorkoutType.TREADMILL
            WorkoutActivityOrigin.PHONE_FREE_WORKOUT -> appWorkoutSessionId != null && hasValidOptionalWatchContext() &&
                suggestedType == WorkoutType.FREE_WORKOUT
        }

        private fun WorkoutActivity.hasValidOptionalWatchContext(): Boolean =
            watchImportContext == null || associatedDeviceId == watchImportContext.key.deviceId

        private fun isValidCommon(activity: WorkoutActivity): Boolean =
            activity.suggestedType != WorkoutType.UNKNOWN && !activity.recordedAt.isBefore(activity.metrics.startedAt) &&
                areValid(activity.metrics) && areValidIntervals(activity.intervals, activity.watchImportContext?.parserVersion) &&
                areValidHeartRateSamples(activity.heartRateSamples) && areValidTemporalOrder(activity.heartRateSamples) &&
                (activity.reviewedAt == null || !activity.reviewedAt.isBefore(activity.recordedAt))

        private fun isValidInput(input: WatchWorkoutParserInput): Boolean =
            input.crc in 1..0xffff && input.externalBlockIndex >= 0 &&
                input.metrics.duration >= Duration.ZERO && input.metrics.steps >= 0 &&
                areValid(input.metrics) && areValidIntervals(input.intervals, input.parserVersion) &&
                areValidHeartRateSamples(input.heartRateSamples) && areValidTemporalOrder(input.heartRateSamples)

        private fun areValid(metrics: WorkoutMetrics): Boolean =
            metrics.duration >= Duration.ZERO && metrics.steps >= 0 &&
                (metrics.stepsAvailable || metrics.steps == 0L) &&
                !metrics.endedAt.isBefore(metrics.startedAt) &&
                metrics.duration <= Duration.between(metrics.startedAt, metrics.endedAt) &&
                isNonNegative(metrics.caloriesKilocalories) &&
                isNonNegative(metrics.distances.originalMeters) &&
                isNonNegative(metrics.distances.estimatedMeters) &&
                isNonNegative(metrics.distances.calibratedMeters) &&
                isNonNegative(metrics.averagePaceSecondsPerKilometer) &&
                isNonNegative(metrics.averageSpeedMetersPerSecond) &&
                isValidBpm(metrics.averageBpm) && isValidBpm(metrics.minimumBpm) &&
                isValidBpm(metrics.maximumBpm)

        private fun isNonNegative(value: DomainValue<Double>): Boolean =
            value !is DomainValue.Present || (value.value.isFinite() && value.value >= 0.0)

        private fun isValidBpm(value: DomainValue<Int>): Boolean =
            value !is DomainValue.Present || value.value in 30..220

        private fun areValidIntervals(
            intervals: List<WorkoutInterval>,
            parserVersion: ParserVersion? = null,
        ): Boolean {
            var previousIndex = Int.MIN_VALUE
            return intervals.all { interval ->
                val isOrdered = interval.index >= 0 && interval.index > previousIndex
                previousIndex = interval.index
                isOrdered && interval.duration >= Duration.ZERO && interval.steps >= 0 &&
                    isNonNegative(interval.distanceMeters) && isNonNegative(interval.caloriesKilocalories) &&
                    isValidRawInterval(interval, parserVersion)
            }
        }

        private fun isValidRawInterval(interval: WorkoutInterval, parserVersion: ParserVersion?): Boolean {
            val isD4V1 = parserVersion?.value == WATCH_WORKOUT_D4_V1_PARSER_VERSION
            val isD4V2 = parserVersion?.value == WATCH_WORKOUT_D4_V2_PARSER_VERSION
            val rawMotionValid = interval.rawMotionValue == null ||
                interval.rawMotionValue in 0L..MeasuredWorkoutInterval.MAX_RAW_MOTION_VALUE
            val rawDistanceValid = interval.rawDistanceValue == null || interval.rawDistanceValue in 0L..0xFF_FFFFL
            val rawCaloriesValid = interval.rawCaloriesValue == null || interval.rawCaloriesValue in 0L..0xFFFFL
            val decodedStepsValid = interval.decodedSteps == null || (
                interval.rawMotionValue != null &&
                    interval.decodedSteps >= 0L &&
                    interval.decodedSteps == (interval.rawMotionValue ushr 16) &&
                    interval.steps == interval.decodedSteps
                )
            val versionedRawValid = when {
                isD4V2 -> interval.rawMotionValue != null && interval.rawDistanceValue != null &&
                    interval.rawCaloriesValue != null && interval.decodedSteps != null
                isD4V1 -> interval.rawMotionValue != null && interval.decodedSteps == null
                else -> true
            }
            return rawMotionValid && rawDistanceValid && rawCaloriesValid && decodedStepsValid && versionedRawValid
        }

        private fun areValidHeartRateSamples(samples: List<WorkoutHeartRateSample>): Boolean =
            samples.all { it.bpm in 30..220 }

        private fun areValidTemporalOrder(samples: List<WorkoutHeartRateSample>): Boolean {
            var previous: Instant? = null
            return samples.all { sample ->
                val isOrdered = previous == null || sample.sampledAt.isAfter(previous)
                previous = sample.sampledAt
                isOrdered
            }
        }

        private fun bpmMetrics(samples: List<WorkoutHeartRateSample>): Triple<DomainValue<Int>, DomainValue<Int>, DomainValue<Int>> {
            if (samples.isEmpty()) {
                val absent = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE)
                return Triple(absent, absent, absent)
            }
            val values = samples.map { it.bpm }
            return Triple(
                DomainValue.Present(values.average().toInt()),
                DomainValue.Present(values.min()),
                DomainValue.Present(values.max()),
            )
        }

        private fun preferValid(
            preferred: DomainValue<Double>,
            fallback: DomainValue<Double>,
        ): DomainValue<Double> = if (preferred is DomainValue.Present && preferred.value.isFinite() && preferred.value >= 0.0) {
            preferred
        } else {
            fallback
        }

        private fun normalizeTitle(value: String?): String? {
            val trimmed = value?.trim() ?: return null
            if (trimmed.isEmpty()) return null
            if (trimmed.length > TITLE_MAX_LENGTH) return INVALID_NORMALIZED_TEXT
            return trimmed
        }

        private fun normalizeNotes(value: String?): String? {
            val trimmed = value?.trim() ?: return null
            if (trimmed.isEmpty()) return null
            if (trimmed.length > NOTES_MAX_LENGTH) return INVALID_NORMALIZED_TEXT
            return trimmed
        }

        private const val STEPS_FOR_RUNNING_SUGGESTION = 1_000L
        private const val TITLE_MAX_LENGTH = 120
        private const val NOTES_MAX_LENGTH = 2_000
        private const val INVALID_NORMALIZED_TEXT = "\u0000"
    }
}

private fun String?.isInvalidNormalizedText(): Boolean = this == "\u0000"

private inline fun DomainResult<WorkoutActivity>.mapSuccess(
    transform: (WorkoutActivity) -> WorkoutActivity,
): DomainResult<WorkoutActivity> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}
