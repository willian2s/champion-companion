package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.database.AppWorkoutSessionEntity
import br.com.mwconsulting.championcompanion.core.database.AssociatedDeviceRecord
import br.com.mwconsulting.championcompanion.core.database.CalendarWorkoutSummaryRecord
import br.com.mwconsulting.championcompanion.core.database.DailySummaryEntity
import br.com.mwconsulting.championcompanion.core.database.DeviceAssociationEntity
import br.com.mwconsulting.championcompanion.core.database.DeviceEntity
import br.com.mwconsulting.championcompanion.core.database.DeviceImportMetadataEntity
import br.com.mwconsulting.championcompanion.core.database.DiagnosticDerivedOutputEntity
import br.com.mwconsulting.championcompanion.core.database.DiagnosticPayloadEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncOperationEntity
import br.com.mwconsulting.championcompanion.core.database.HealthSyncSummaryRecord
import br.com.mwconsulting.championcompanion.core.database.HealthSyncWorkoutCandidateRecord
import br.com.mwconsulting.championcompanion.core.database.HeartRateMeasurementEntity
import br.com.mwconsulting.championcompanion.core.database.PhoneWorkoutCorrelationCandidateRecord
import br.com.mwconsulting.championcompanion.core.database.StoredDeviceModel
import br.com.mwconsulting.championcompanion.core.database.TreadmillCalibrationEntity
import br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportEntity
import br.com.mwconsulting.championcompanion.core.database.WatchWorkoutImportLookupRecord
import br.com.mwconsulting.championcompanion.core.database.WeightMeasurementEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutWorkflowSummaryRecord
import br.com.mwconsulting.championcompanion.core.database.WorkoutActivityEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutHeartRateSampleEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutIntervalEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRouteEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRoutePointEntity
import br.com.mwconsulting.championcompanion.core.model.AbsenceReason
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionAggregates
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.CalibratedWorkoutMetrics
import br.com.mwconsulting.championcompanion.core.model.CalendarWeightMeasurementSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarWorkoutSummary
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DataValidity
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryId
import br.com.mwconsulting.championcompanion.core.model.DailySummaryKey
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceId
import br.com.mwconsulting.championcompanion.core.model.DeviceImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportMetadata
import br.com.mwconsulting.championcompanion.core.model.D1HeartRateImportStatus
import br.com.mwconsulting.championcompanion.core.model.DeviceModel
import br.com.mwconsulting.championcompanion.core.model.DiagnosticCaptureFlow
import br.com.mwconsulting.championcompanion.core.model.DiagnosticDerivedOutputKey
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayload
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadCapture
import br.com.mwconsulting.championcompanion.core.model.DiagnosticPayloadId
import br.com.mwconsulting.championcompanion.core.model.DiagnosticParseResult
import br.com.mwconsulting.championcompanion.core.model.DomainResult
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregateType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.MetricProvenance
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutCorrelationCandidate
import br.com.mwconsulting.championcompanion.core.model.SafeDiagnosticError
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibration
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationId
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationOrigin
import br.com.mwconsulting.championcompanion.core.model.UuidV7
import br.com.mwconsulting.championcompanion.core.model.UserProfileId
import br.com.mwconsulting.championcompanion.core.model.UserProfile
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImport
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportId
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportLookup
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutParserInput
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportStatus
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurementId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.WorkoutMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegmentReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteState
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

internal fun Device.toEntity() = DeviceEntity(
    id = id.value.toString(), platformIdentifier = platformIdentifier, name = name,
    model = StoredDeviceModel.valueOf(model.name), firmware = firmware,
    associatedAtEpochMillis = associatedAt.toEpochMilli(),
    lastSuccessfulConnectionAtEpochMillis = lastSuccessfulConnectionAt?.toEpochMilli(),
    lastImportedAtEpochMillis = importMetadata.lastImportedAt?.toEpochMilli(),
    importParserVersion = importMetadata.parserVersion?.value,
)

internal fun Device.toAssociationEntity(
    updatedAtEpochMillis: Long = associatedAt.toEpochMilli(),
) = DeviceAssociationEntity(
    id = PRIMARY_DEVICE_ASSOCIATION_ID,
    deviceId = id.value.toString(),
    associatedAtEpochMillis = associatedAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun DeviceEntity.toAssociationEntity(
    associatedAtEpochMillis: Long = this.associatedAtEpochMillis,
    updatedAtEpochMillis: Long = associatedAtEpochMillis,
) = DeviceAssociationEntity(
    id = PRIMARY_DEVICE_ASSOCIATION_ID,
    deviceId = id,
    associatedAtEpochMillis = associatedAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun DeviceEntity.toDomain(associatedAtEpochMillis: Long = this.associatedAtEpochMillis): Device? {
    val deviceId = uuid(id)?.let(::DeviceId) ?: return null
    return Device.create(
        id = deviceId, platformIdentifier = platformIdentifier, name = name, model = DeviceModel.valueOf(model.name),
        firmware = firmware, associatedAt = Instant.ofEpochMilli(associatedAtEpochMillis),
        lastSuccessfulConnectionAt = lastSuccessfulConnectionAtEpochMillis?.let(Instant::ofEpochMilli),
        importMetadata = DeviceImportMetadata(lastImportedAtEpochMillis?.let(Instant::ofEpochMilli), importParserVersion?.let(::parserVersion)),
    ).successOrNull()
}

internal fun AssociatedDeviceRecord.toDomain(): Device? = DeviceEntity(
    id = id,
    platformIdentifier = platformIdentifier,
    name = name,
    model = model,
    firmware = firmware,
    associatedAtEpochMillis = associatedAtEpochMillis,
    lastSuccessfulConnectionAtEpochMillis = lastSuccessfulConnectionAtEpochMillis,
    lastImportedAtEpochMillis = lastImportedAtEpochMillis,
    importParserVersion = importParserVersion,
).toDomain(associatedAtEpochMillis = associatedAtEpochMillis)

internal const val PRIMARY_DEVICE_ASSOCIATION_ID: String = "primary"
internal const val WATCH_D1_HISTORY_IMPORT_TYPE: String = "WATCH_D1_HISTORY"
internal const val WATCH_D3_D4_IMPORT_TYPE: String = "WATCH_WORKOUT_D3_D4"

internal fun D1HeartRateImportMetadata.toEntity(
    id: String,
    importType: String = WATCH_D1_HISTORY_IMPORT_TYPE,
) = DeviceImportMetadataEntity(
    id = id,
    deviceId = deviceId.value.toString(),
    importType = importType,
    lastAttemptAtEpochMillis = lastAttemptAt.toEpochMilli(),
    lastSuccessAtEpochMillis = lastSuccessAt?.toEpochMilli(),
    lastResult = lastResult.name,
    parserVersion = parserVersion?.value,
    insertedCount = insertedCount,
    duplicateCount = duplicateCount,
    conflictCount = conflictCount,
    invalidCount = invalidCount,
    missingIndexCount = missingIndexCount,
    timedOut = timedOut,
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun DeviceImportMetadataEntity.toD1ImportMetadata(): D1HeartRateImportMetadata? {
    val mappedDeviceId = uuid(deviceId)?.let(::DeviceId) ?: return null
    val mappedStatus = enum<D1HeartRateImportStatus>(lastResult) ?: return null
    return D1HeartRateImportMetadata(
        deviceId = mappedDeviceId,
        lastAttemptAt = Instant.ofEpochMilli(lastAttemptAtEpochMillis),
        lastSuccessAt = lastSuccessAtEpochMillis?.let(Instant::ofEpochMilli),
        lastResult = mappedStatus,
        parserVersion = parserVersion?.let(::parserVersion),
        insertedCount = insertedCount,
        duplicateCount = duplicateCount,
        conflictCount = conflictCount,
        invalidCount = invalidCount,
        missingIndexCount = missingIndexCount,
        timedOut = timedOut,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    )
}

internal fun WatchWorkoutImportMetadata.toEntity(
    id: String,
    importType: String = WATCH_D3_D4_IMPORT_TYPE,
) = DeviceImportMetadataEntity(
    id = id,
    deviceId = deviceId.value.toString(),
    importType = importType,
    lastAttemptAtEpochMillis = lastAttemptAt.toEpochMilli(),
    lastSuccessAtEpochMillis = lastSuccessAt?.toEpochMilli(),
    lastResult = lastResult.name,
    parserVersion = parserVersion?.value,
    insertedCount = insertedCount,
    duplicateCount = duplicateCount,
    conflictCount = conflictCount,
    invalidCount = invalidCount,
    missingIndexCount = missingIndexCount,
    timedOut = timedOut,
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun DeviceImportMetadataEntity.toWatchWorkoutImportMetadata(): WatchWorkoutImportMetadata? {
    val mappedDeviceId = uuid(deviceId)?.let(::DeviceId) ?: return null
    val mappedStatus = enum<WatchWorkoutImportStatus>(lastResult) ?: return null
    return WatchWorkoutImportMetadata(
        deviceId = mappedDeviceId,
        lastAttemptAt = Instant.ofEpochMilli(lastAttemptAtEpochMillis),
        lastSuccessAt = lastSuccessAtEpochMillis?.let(Instant::ofEpochMilli),
        lastResult = mappedStatus,
        parserVersion = parserVersion?.let(::parserVersion),
        insertedCount = insertedCount,
        duplicateCount = duplicateCount,
        conflictCount = conflictCount,
        invalidCount = invalidCount,
        missingIndexCount = missingIndexCount,
        timedOut = timedOut,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    )
}

internal fun DailySummary.toEntity() = DailySummaryEntity(
    id.value.toString(), key.deviceId?.value?.toString(), key.origin.name, key.localDate.toString(), steps,
    estimatedDistanceMeters, estimatedCaloriesKilocalories, collectedAt.toEpochMilli(), updatedAt.toEpochMilli(), validity.name,
)

internal fun DailySummaryEntity.toDomain(): DailySummary? {
    val summaryId = uuid(id)?.let(::DailySummaryId) ?: return null
    val dataOrigin = enum<DataOrigin>(origin) ?: return null
    val dataValidity = enum<DataValidity>(validity) ?: return null
    return DailySummary.create(
        id = summaryId, key = DailySummaryKey(deviceId?.let(::uuid)?.let(::DeviceId), dataOrigin, LocalDate.parse(localDateIso)),
        steps = steps, estimatedDistanceMeters = estimatedDistanceMeters, estimatedCaloriesKilocalories = estimatedCaloriesKilocalories,
        collectedAt = Instant.ofEpochMilli(collectedAtEpochMillis), updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis), validity = dataValidity,
    ).successOrNull()
}

internal fun br.com.mwconsulting.championcompanion.core.database.WeightMeasurementEntity.toDomain(): WeightMeasurement? {
    val unit = enum<br.com.mwconsulting.championcompanion.core.model.WeightUnit>(unit) ?: return null
    return WeightMeasurement.create(
        id = uuid(id)?.let(::WeightMeasurementId) ?: return null,
        value = value,
        unit = unit,
        measuredAt = Instant.ofEpochMilli(measuredAtEpochMillis),
        origin = enum<DataOrigin>(origin) ?: return null,
    ).successOrNull()
}

internal fun WeightMeasurementEntity.toCalendarSummary(): CalendarWeightMeasurementSummary? =
    toDomain()?.let { measurement ->
        CalendarWeightMeasurementSummary(
            measurementId = measurement.id,
            value = measurement.value,
            unit = measurement.unit,
            measuredAt = measurement.measuredAt,
            origin = measurement.origin,
        )
    }

internal fun CalendarWorkoutSummaryRecord.toCalendarSummary(): CalendarWorkoutSummary? {
    val mappedActivityId = uuid(activityId)?.let(::WorkoutActivityId) ?: return null
    val mappedOrigin = enum<WorkoutActivityOrigin>(origin)?.toDataOrigin()
        ?: enum<DataOrigin>(origin)
        ?: return null
    val mappedSuggestedType = enum<WorkoutType>(suggestedType) ?: return null
    val mappedConfirmedType = confirmedType?.let { enum<WorkoutType>(it) ?: return null }
    val mappedWorkflowState = enum<ActivityWorkflowState>(workflowState) ?: return null
    if (durationNanos < 0) return null

    return CalendarWorkoutSummary(
        activityId = mappedActivityId,
        origin = mappedOrigin,
        title = title,
        suggestedType = mappedSuggestedType,
        confirmedType = mappedConfirmedType,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        duration = Duration.ofNanos(durationNanos),
        distanceMeters = (calibratedDistanceMeters ?: originalDistanceMeters ?: estimatedDistanceMeters)
            .domainValue(),
        caloriesKilocalories = caloriesKilocalories.domainValue(),
        averageBpm = averageBpm.domainValue(),
        workflowState = mappedWorkflowState,
    )
}

internal fun br.com.mwconsulting.championcompanion.core.database.UserProfileEntity.toDomain(): UserProfile? {
    return UserProfile.create(
        id = uuid(id)?.let(::UserProfileId) ?: return null,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        heightCentimeters = heightCentimeters,
    ).successOrNull()
}

internal fun HeartRateMeasurement.toEntity() = HeartRateMeasurementEntity(
    id.value.toString(), source.name, bpm, measuredAt?.toEpochMilli(), observedAt?.localDateTime?.toString(), validity.name,
    deviceId?.value?.toString(), importedAt?.toEpochMilli(), parserVersion?.value,
    appWorkoutSessionId?.value?.toString(), workoutActivityId?.value?.toString(), deduplicationKey.encode(),
)

internal fun HeartRateMeasurementEntity.toDomain(): HeartRateMeasurement? {
    val source = enum<HeartRateSource>(source) ?: return null
    return HeartRateMeasurement.create(
        id = uuid(id)?.let(::HeartRateMeasurementId) ?: return null, source = source, bpm = bpm,
        measuredAt = measuredAtEpochMillis?.let(Instant::ofEpochMilli),
        observedAt = observedLocalDateTime?.let { br.com.mwconsulting.championcompanion.core.model.ObservedWatchTime(LocalDateTime.parse(it)) },
        validity = enum<DataValidity>(validity) ?: return null, deviceId = deviceId?.let(::uuid)?.let(::DeviceId),
        importedAt = importedAtEpochMillis?.let(Instant::ofEpochMilli), parserVersion = parserVersion?.let(::parserVersion),
        appWorkoutSessionId = appWorkoutSessionId?.let(::uuid)?.let(::AppWorkoutSessionId),
        workoutActivityId = workoutActivityId?.let(::uuid)?.let(::WorkoutActivityId),
        deduplicationKey = decodeHeartRateKey(deduplicationKey) ?: return null,
    ).successOrNull()
}

internal fun WorkoutActivity.toEntity(): WorkoutActivityEntity {
    val importContext = watchImportContext
    return WorkoutActivityEntity(
        id = id.value.toString(),
        origin = activityOrigin.name,
        deviceId = associatedDeviceId?.value?.toString(),
        parserValidity = importContext?.parserValidity?.name,
        importedAtEpochMillis = importContext?.importedAt?.toEpochMilli(),
        parserVersion = importContext?.parserVersion?.value,
        diagnosticPayloadId = importContext?.diagnosticPayloadId?.value?.toString(),
        startedAtEpochMillis = metrics.startedAt.toEpochMilli(),
        durationNanos = metrics.duration.toNanos(),
        steps = metrics.steps,
        caloriesKilocalories = metrics.caloriesKilocalories.valueOrNull(),
        originalDistanceMeters = metrics.distances.originalMeters.valueOrNull(),
        estimatedDistanceMeters = metrics.distances.estimatedMeters.valueOrNull(),
        calibratedDistanceMeters = metrics.distances.calibratedMeters.valueOrNull(),
        importedAveragePaceSecondsPerKilometer = metrics.averagePaceSecondsPerKilometer.valueOrNull(),
        importedAverageSpeedMetersPerSecond = metrics.averageSpeedMetersPerSecond.valueOrNull(),
        calibratedAveragePaceSecondsPerKilometer = metrics.calibratedMetrics
            ?.averagePaceSecondsPerKilometer?.valueOrNull(),
        calibratedAverageSpeedMetersPerSecond = metrics.calibratedMetrics
            ?.averageSpeedMetersPerSecond?.valueOrNull(),
        averageBpm = metrics.averageBpm.valueOrNull(),
        minimumBpm = metrics.minimumBpm.valueOrNull(),
        maximumBpm = metrics.maximumBpm.valueOrNull(),
        suggestedType = suggestedType.name,
        title = title,
        notes = notes,
        confirmedType = confirmedType?.name,
        workflowState = workflowState.name,
        reviewedAtEpochMillis = reviewedAt?.toEpochMilli(),
        calibrationId = calibrationId?.value?.toString(),
        appWorkoutSessionId = appWorkoutSessionId?.value?.toString(),
        recordedAtEpochMillis = recordedAt.toEpochMilli(),
        endedAtEpochMillis = metrics.endedAt.toEpochMilli(),
        stepsAvailable = metrics.stepsAvailable,
    )
}

internal fun WatchWorkoutImport.toEntity() = WatchWorkoutImportEntity(
    id.value.toString(), activityId.value.toString(), key.deviceId.value.toString(), key.origin.name, key.crc,
    externalBlockIndex, parserVersion.value, parserValidity.name, diagnosticPayloadId?.value?.toString(), importedAt.toEpochMilli(),
)

internal fun WorkoutActivity.toIntervals(activityId: String, ids: Iterator<UUID>) = intervals.map {
    WorkoutIntervalEntity(ids.next().toString(), activityId, it.index, it.duration.toNanos(), it.steps,
        it.distanceMeters.valueOrNull(), it.caloriesKilocalories.valueOrNull())
}

internal fun WorkoutActivity.toHeartRateSamples(activityId: String, ids: Iterator<UUID>) = heartRateSamples.map {
    WorkoutHeartRateSampleEntity(ids.next().toString(), activityId, it.sampledAt.toEpochMilli(), it.bpm)
}

internal fun WorkoutActivityEntity.toPendingReviewDomain(
    importRecord: WatchWorkoutImportEntity,
    intervals: List<WorkoutIntervalEntity>,
    samples: List<WorkoutHeartRateSampleEntity>,
): WorkoutActivity? = toDomain(importRecord, intervals, samples)

internal fun WorkoutActivityEntity.toDomain(
    importRecord: WatchWorkoutImportEntity?,
    intervals: List<WorkoutIntervalEntity>,
    samples: List<WorkoutHeartRateSampleEntity>,
): WorkoutActivity? {
    val activityId = uuid(id)?.let(::WorkoutActivityId) ?: return null
    val mappedOrigin = enum<WorkoutActivityOrigin>(origin) ?: return null
    val mappedDeviceId = deviceId?.let(::uuid)?.let(::DeviceId)
    val mappedSessionId = appWorkoutSessionId?.let(::uuid)?.let(::AppWorkoutSessionId)
    if (deviceId != null && mappedDeviceId == null) return null
    if (appWorkoutSessionId != null && mappedSessionId == null) return null
    val importContext = importRecord?.toActivityContext(activityId) ?: run {
        if (importRecord != null) return null
        null
    }
    val mappedConfirmedType = confirmedType?.let { enum<WorkoutType>(it) ?: return null }
    val mappedWorkflowState = enum<ActivityWorkflowState>(workflowState) ?: return null
    return WorkoutActivity.restore(
        id = activityId,
        activityOrigin = mappedOrigin,
        associatedDeviceId = mappedDeviceId,
        appWorkoutSessionId = mappedSessionId,
        watchImportContext = importContext,
        recordedAt = Instant.ofEpochMilli(recordedAtEpochMillis),
        metrics = WorkoutMetrics(
            startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
            duration = Duration.ofNanos(durationNanos),
            steps = steps,
            caloriesKilocalories = caloriesKilocalories.domainValue(),
            distances = WorkoutDistanceMetrics(
                originalDistanceMeters.domainValue(),
                estimatedDistanceMeters.domainValue(),
                calibratedDistanceMeters.domainValue(),
            ),
            averagePaceSecondsPerKilometer = importedAveragePaceSecondsPerKilometer.domainValue(),
            averageSpeedMetersPerSecond = importedAverageSpeedMetersPerSecond.domainValue(),
            averageBpm = averageBpm.domainValue(),
            minimumBpm = minimumBpm.domainValue(),
            maximumBpm = maximumBpm.domainValue(),
            calibratedMetrics = calibratedMetricsOrNull(),
            stepsAvailable = stepsAvailable,
            endedAt = Instant.ofEpochMilli(endedAtEpochMillis),
        ),
        intervals = intervals.map {
            WorkoutInterval(
                it.intervalIndex,
                Duration.ofNanos(it.durationNanos),
                it.steps,
                it.distanceMeters.domainValue(),
                it.caloriesKilocalories.domainValue(),
            )
        },
        heartRateSamples = samples.map {
            WorkoutHeartRateSample(Instant.ofEpochMilli(it.sampledAtEpochMillis), it.bpm)
        },
        suggestedType = enum<WorkoutType>(suggestedType) ?: return null,
        title = title,
        notes = notes,
        confirmedType = mappedConfirmedType,
        workflowState = mappedWorkflowState,
        reviewedAt = reviewedAtEpochMillis?.let(Instant::ofEpochMilli),
        calibrationId = calibrationId?.let(::uuid)?.let(::TreadmillCalibrationId),
    ).successOrNull()
}

internal fun HealthSyncWorkoutCandidateRecord.toHealthSyncCandidateDomain(
    samples: List<WorkoutHeartRateSampleEntity>,
): WorkoutActivity? {
    val importRecord = optionalImportEntity() ?: if (importId == null) null else return null
    if (importRecord != null && !importRecord.isStructurallyRelatedTo(activity)) return null
    return activity.toDomain(
        importRecord = importRecord,
        intervals = emptyList(),
        samples = samples,
    )
}

private fun WatchWorkoutImportEntity.isStructurallyRelatedTo(
    activity: WorkoutActivityEntity,
): Boolean {
    val mappedOrigin = enum<DataOrigin>(origin) ?: return false
    val mappedDiagnosticPayloadId = diagnosticPayloadId?.let(::uuid)
    if (diagnosticPayloadId != null && mappedDiagnosticPayloadId == null) return false

    return uuid(id) != null &&
        uuid(activityId) != null &&
        uuid(deviceId) != null &&
        activityId == activity.id &&
        (activity.deviceId == null || deviceId == activity.deviceId) &&
        mappedOrigin == DataOrigin.WATCH_D3_D4 &&
        crc in 1..0xffff &&
        externalBlockIndex >= 0 &&
        parserVersion(parserVersion) != null &&
        enum<ParserValidity>(parserValidity) != null &&
        importedAtEpochMillis >= activity.startedAtEpochMillis
}

private fun WatchWorkoutImportEntity.toActivityContext(
    mappedActivityId: WorkoutActivityId,
): br.com.mwconsulting.championcompanion.core.model.WatchWorkoutActivityImportContext? {
    val import = WatchWorkoutImport.restore(
        id = uuid(id)?.let(::WatchWorkoutImportId) ?: return null,
        activityId = mappedActivityId,
        context = br.com.mwconsulting.championcompanion.core.model.WatchWorkoutActivityImportContext(
            key = br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportKey(
                deviceId = uuid(deviceId)?.let(::DeviceId) ?: return null,
                origin = enum<DataOrigin>(origin) ?: return null,
                crc = crc,
            ),
            externalBlockIndex = externalBlockIndex,
            parserVersion = parserVersion(parserVersion) ?: return null,
            parserValidity = enum<ParserValidity>(parserValidity) ?: return null,
            diagnosticPayloadId = diagnosticPayloadId?.let(::uuid)?.let(::DiagnosticPayloadId),
            importedAt = Instant.ofEpochMilli(importedAtEpochMillis),
        ),
    ).successOrNull() ?: return null
    return import.activityContext
}

private fun HealthSyncWorkoutCandidateRecord.optionalImportEntity(): WatchWorkoutImportEntity? {
    if (listOf(
            importId,
            importActivityId,
            importDeviceId,
            importOrigin,
            importCrc,
            importExternalBlockIndex,
            importParserVersion,
            importParserValidity,
            importImportedAtEpochMillis,
        ).all { it == null }
    ) return null
    return WatchWorkoutImportEntity(
        id = importId ?: return null,
        activityId = importActivityId ?: return null,
        deviceId = importDeviceId ?: return null,
        origin = importOrigin ?: return null,
        crc = importCrc ?: return null,
        externalBlockIndex = importExternalBlockIndex ?: return null,
        parserVersion = importParserVersion ?: return null,
        parserValidity = importParserValidity ?: return null,
        diagnosticPayloadId = importDiagnosticPayloadId,
        importedAtEpochMillis = importImportedAtEpochMillis ?: return null,
    )
}

internal fun PhoneWorkoutCorrelationCandidateRecord.toDomain(): PhoneWorkoutCorrelationCandidate? =
    runCatching {
        PhoneWorkoutCorrelationCandidate(
            activityId = uuid(activityId)?.let(::WorkoutActivityId) ?: return null,
            sessionId = uuid(sessionId)?.let(::AppWorkoutSessionId) ?: return null,
            origin = enum<WorkoutActivityOrigin>(origin) ?: return null,
            startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
            endedAt = Instant.ofEpochMilli(endedAtEpochMillis),
            associatedDeviceId = associatedDeviceId?.let(::uuid)?.let(::DeviceId),
        )
    }.getOrNull()

internal fun WatchWorkoutImportLookupRecord.toDomain(): WatchWorkoutImportLookup? {
    val mappedActivityId = uuid(activityId)?.let(::WorkoutActivityId) ?: return null
    val mappedImportId = uuid(importId)?.let(::WatchWorkoutImportId) ?: return null
    val mappedDeviceId = uuid(deviceId)?.let(::DeviceId) ?: return null
    val mappedParserValidity = enum<ParserValidity>(parserValidity) ?: return null
    val mappedWorkflowState = enum<ActivityWorkflowState>(workflowState) ?: return null
    return WatchWorkoutImportLookup(
        activityId = mappedActivityId,
        importId = mappedImportId,
        deviceId = mappedDeviceId,
        crc = crc,
        parserValidity = mappedParserValidity,
        workflowState = mappedWorkflowState,
        importedAt = Instant.ofEpochMilli(importedAtEpochMillis),
    )
}

internal fun TreadmillCalibration.toEntity(): TreadmillCalibrationEntity? {
    val activityId = (subject as? br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSubject.Activity)?.id ?: return null
    return TreadmillCalibrationEntity(
        id.value.toString(), activityId.value.toString(), key.revision, originalMeters.valueOrNull(), estimatedMeters.valueOrNull(),
        reportedMeters.valueOrNull(), calibratedMeters.valueOrNull(), factor.valueOrNull(),
        (factor as? DomainValue.Absent)?.reason?.name, appliedAt.toEpochMilli(), origin.name,
        derivedMetrics.averagePaceSecondsPerKilometer.valueOrNull(), derivedMetrics.averageSpeedMetersPerSecond.valueOrNull(),
        derivedMetrics.provenance.name,
    )
}

internal fun TreadmillCalibrationEntity.toSnapshot(): TreadmillCalibrationSnapshot? {
    val mappedActivityId = uuid(activityId)?.let(::WorkoutActivityId) ?: return null
    val provenance = enum<MetricProvenance>(metricsProvenance) ?: return null
    return TreadmillCalibrationSnapshot(
        activityId = mappedActivityId,
        revision = revision,
        originalDistanceMeters = originalDistanceMeters.domainValue(),
        estimatedDistanceMeters = estimatedDistanceMeters.domainValue(),
        reportedDistanceMeters = reportedDistanceMeters.domainValue(),
        calibratedDistanceMeters = calibratedDistanceMeters.domainValue(),
        factor = factorDomainValue(),
        appliedAt = Instant.ofEpochMilli(appliedAtEpochMillis),
        averagePaceSecondsPerKilometer = averagePaceSecondsPerKilometer.domainValue(),
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond.domainValue(),
        metricsProvenance = provenance,
    )
}

internal fun WorkoutWorkflowSummaryRecord.toDomain() = WorkoutWorkflowSummary(
    pendingReviewCount = pendingReviewCount,
    pendingCalibrationCount = pendingCalibrationCount,
    readyToSyncCount = readyToSyncCount,
    syncedCount = syncedCount,
    syncFailedCount = syncFailedCount,
    discardedCount = discardedCount,
)

internal fun AppWorkoutSessionEntity.toDomain(): AppWorkoutSession? {
    val mappedAssociatedDeviceId = associatedDeviceId?.let(::uuid)?.let(::DeviceId)
    if (associatedDeviceId != null && mappedAssociatedDeviceId == null) return null
    val mappedActivityId = workoutActivityId?.let(::uuid)?.let(::WorkoutActivityId)
    if (workoutActivityId != null && mappedActivityId == null) return null
    return AppWorkoutSession.restore(
        id = uuid(id)?.let(::AppWorkoutSessionId) ?: return null,
        type = enum<WorkoutType>(type) ?: return null,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        startedAt = startedAtEpochMillis?.let(Instant::ofEpochMilli),
        endedAt = endedAtEpochMillis?.let(Instant::ofEpochMilli),
        activeDuration = Duration.ofNanos(activeDurationNanos),
        pausedDuration = Duration.ofNanos(pausedDurationNanos),
        activeSince = activeSinceEpochMillis?.let(Instant::ofEpochMilli),
        pausedSince = pausedSinceEpochMillis?.let(Instant::ofEpochMilli),
        state = enum<AppWorkoutSessionState>(state) ?: return null,
        aggregates = AppWorkoutSessionAggregates(
            steps = steps,
            distanceMeters = distanceMeters.sessionDomainValue(distanceAbsenceReason) ?: return null,
            caloriesKilocalories = caloriesKilocalories.sessionDomainValue(caloriesAbsenceReason) ?: return null,
            averageBpm = averageBpm.sessionDomainValue(averageBpmAbsenceReason) ?: return null,
            minimumBpm = minimumBpm.sessionDomainValue(minimumBpmAbsenceReason) ?: return null,
            maximumBpm = maximumBpm.sessionDomainValue(maximumBpmAbsenceReason) ?: return null,
            stepsAvailable = stepsAvailable,
        ),
        workoutActivityId = mappedActivityId,
        associatedDeviceId = mappedAssociatedDeviceId,
        revision = revision,
        runtimeGeneration = runtimeGeneration,
        lastTransitionAt = Instant.ofEpochMilli(lastTransitionAtEpochMillis),
    ).successOrNull()
}

internal fun AppWorkoutSession.toEntity() = AppWorkoutSessionEntity(
    id = id.value.toString(),
    type = type.name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    startedAtEpochMillis = startedAt?.toEpochMilli(),
    endedAtEpochMillis = endedAt?.toEpochMilli(),
    activeDurationNanos = activeDuration.toNanos(),
    pausedDurationNanos = pausedDuration.toNanos(),
    activeSinceEpochMillis = activeSince?.toEpochMilli(),
    pausedSinceEpochMillis = pausedSince?.toEpochMilli(),
    state = state.name,
    steps = aggregates.steps,
    distanceMeters = aggregates.distanceMeters.valueOrNull(),
    distanceAbsenceReason = aggregates.distanceMeters.absenceReasonOrNull(),
    caloriesKilocalories = aggregates.caloriesKilocalories.valueOrNull(),
    caloriesAbsenceReason = aggregates.caloriesKilocalories.absenceReasonOrNull(),
    averageBpm = aggregates.averageBpm.valueOrNull(),
    averageBpmAbsenceReason = aggregates.averageBpm.absenceReasonOrNull(),
    minimumBpm = aggregates.minimumBpm.valueOrNull(),
    minimumBpmAbsenceReason = aggregates.minimumBpm.absenceReasonOrNull(),
    maximumBpm = aggregates.maximumBpm.valueOrNull(),
    maximumBpmAbsenceReason = aggregates.maximumBpm.absenceReasonOrNull(),
    workoutActivityId = workoutActivityId?.value?.toString(),
    lastTransitionAtEpochMillis = lastTransitionAt.toEpochMilli(),
    associatedDeviceId = associatedDeviceId?.value?.toString(),
    revision = revision,
    runtimeGeneration = runtimeGeneration,
    stepsAvailable = aggregates.stepsAvailable,
)

internal fun WorkoutRoute.toEntity() = WorkoutRouteEntity(
    id = id.value.toString(),
    sessionId = sessionId.value.toString(),
    workoutType = workoutType.name,
    state = state.name,
    startedAtEpochMillis = startedAt.toEpochMilli(),
    endedAtEpochMillis = endedAt?.toEpochMilli(),
    acceptedPointCount = metrics.acceptedPointCount,
    segmentCount = metrics.segmentCount,
    distanceMeters = metrics.distanceMeters,
    averageHorizontalAccuracyMeters = metrics.averageHorizontalAccuracyMeters,
    revision = revision,
)

internal fun WorkoutRouteEntity.toDomain(): WorkoutRoute? {
    return WorkoutRoute.restore(
        id = uuid(id)?.let(::WorkoutRouteId) ?: return null,
        sessionId = uuid(sessionId)?.let(::AppWorkoutSessionId) ?: return null,
        workoutType = enum<WorkoutType>(workoutType) ?: return null,
        state = enum<WorkoutRouteState>(state) ?: return null,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        endedAt = endedAtEpochMillis?.let(Instant::ofEpochMilli),
        metrics = runCatching {
            WorkoutRouteMetrics(
                acceptedPointCount = acceptedPointCount,
                segmentCount = segmentCount,
                distanceMeters = distanceMeters,
                averageHorizontalAccuracyMeters = averageHorizontalAccuracyMeters,
            )
        }.getOrNull() ?: return null,
        revision = revision,
    ).successOrNull()
}

internal fun WorkoutRoutePoint.toEntity(
    segmentStartReason: WorkoutRouteSegmentReason? = null,
    segmentStartedAt: Instant? = null,
    segmentDistanceMeters: Double? = null,
) = WorkoutRoutePointEntity(
    id = id.value.toString(),
    routeId = routeId.value.toString(),
    sequence = sequence,
    sampledAtEpochMillis = sampledAt.toEpochMilli(),
    latitudeDegrees = latitudeDegrees,
    longitudeDegrees = longitudeDegrees,
    altitudeMeters = altitudeMeters,
    horizontalAccuracyMeters = horizontalAccuracyMeters,
    verticalAccuracyMeters = verticalAccuracyMeters,
    speedMetersPerSecond = speedMetersPerSecond,
    bearingDegrees = bearingDegrees,
    segmentIndex = segmentIndex,
    segmentStartReason = segmentStartReason?.name,
    segmentStartedAtEpochMillis = segmentStartedAt?.toEpochMilli(),
    segmentDistanceMeters = segmentDistanceMeters,
)

internal fun WorkoutRoutePointEntity.toDomain(): WorkoutRoutePoint? {
    return WorkoutRoutePoint.create(
        id = uuid(id)?.let(::WorkoutRoutePointId) ?: return null,
        routeId = uuid(routeId)?.let(::WorkoutRouteId) ?: return null,
        sequence = sequence,
        sampledAt = Instant.ofEpochMilli(sampledAtEpochMillis),
        latitudeDegrees = latitudeDegrees,
        longitudeDegrees = longitudeDegrees,
        altitudeMeters = altitudeMeters,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        verticalAccuracyMeters = verticalAccuracyMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees,
        segmentIndex = segmentIndex,
    ).successOrNull()
}

internal fun HealthSyncOperation.toEntity() = HealthSyncOperationEntity(
    id = id.value.toString(),
    aggregateId = key.aggregate.id.toString(),
    aggregateType = key.aggregate.type.name,
    recordType = key.recordType.name,
    state = state.name,
    attemptCount = attemptCount,
    lastAttemptAtEpochMillis = lastAttemptAt?.toEpochMilli(),
    lastError = lastError?.name,
    clientRecordId = clientRecordId,
    clientRecordVersion = clientRecordVersion,
    syncedAtEpochMillis = syncedAt?.toEpochMilli(),
    timeZoneId = timeZoneId,
    createdAtEpochMillis = createdAt?.toEpochMilli(),
    updatedAtEpochMillis = updatedAt?.toEpochMilli(),
)

internal fun HealthSyncOperationEntity.toDomain(): HealthSyncOperation? {
    val aggregate = toHealthSyncAggregate(aggregateType, aggregateId) ?: return null
    var operation = HealthSyncOperation.create(
        id = uuid(id)?.let(::HealthSyncOperationId) ?: return null,
        key = HealthSyncOperationKey(aggregate, enum<HealthSyncRecordType>(recordType) ?: return null),
        clientRecordId = clientRecordId,
        clientRecordVersion = clientRecordVersion,
        syncedAt = syncedAtEpochMillis?.let(Instant::ofEpochMilli),
        timeZoneId = timeZoneId,
        createdAt = createdAtEpochMillis?.let(Instant::ofEpochMilli),
        updatedAt = updatedAtEpochMillis?.let(Instant::ofEpochMilli),
    ).successOrNull() ?: return null
    if (state != SyncOperationState.PENDING.name) {
        operation = operation.start(lastAttemptAtEpochMillis?.let(Instant::ofEpochMilli) ?: return null).successOrNull() ?: return null
        operation = when (enum<SyncOperationState>(state)) {
            SyncOperationState.SYNCED -> operation.markSynced().successOrNull()
            SyncOperationState.FAILED -> operation.markFailed(enum<SafeSyncError>(lastError) ?: return null).successOrNull()
            SyncOperationState.SYNCING -> operation
            else -> null
        } ?: return null
    }
    return operation
}

internal fun HealthSyncSummaryRecord.toDomain() = HealthSyncSummary(
    pendingCount = pendingCount,
    syncingCount = syncingCount,
    failedCount = failedCount,
    syncedCount = syncedCount,
    lastSyncedAt = lastSyncedAtEpochMillis?.let(Instant::ofEpochMilli),
)

private fun toHealthSyncAggregate(
    aggregateType: String,
    aggregateId: String,
): HealthSyncAggregate? = when (enum<HealthSyncAggregateType>(aggregateType)) {
    HealthSyncAggregateType.DAILY_SUMMARY -> uuid(aggregateId)?.let(::DailySummaryId)?.let(HealthSyncAggregate::DailySummary)
    HealthSyncAggregateType.HEART_RATE_MEASUREMENT -> uuid(aggregateId)?.let(::HeartRateMeasurementId)?.let(HealthSyncAggregate::HeartRateMeasurement)
    HealthSyncAggregateType.WORKOUT_ACTIVITY -> uuid(aggregateId)?.let(::WorkoutActivityId)?.let(HealthSyncAggregate::WorkoutActivity)
    HealthSyncAggregateType.WEIGHT_MEASUREMENT -> uuid(aggregateId)?.let(::WeightMeasurementId)?.let(HealthSyncAggregate::WeightMeasurement)
    HealthSyncAggregateType.USER_PROFILE_HEIGHT -> uuid(aggregateId)?.let(::UserProfileId)?.let(HealthSyncAggregate::UserProfileHeight)
    null -> null
}

internal fun DiagnosticPayload.toEntity() = DiagnosticPayloadEntity(
    id.value.toString(), command, flow.name, bytesForPrivateStorage(), byteCount, deviceId?.value?.toString(), firmware,
    parserVersion.value, result.name, safeError?.name, capturedAt.toEpochMilli(),
)

internal fun DiagnosticPayload.toDerivedOutputEntities(ids: Iterator<UUID>): List<DiagnosticDerivedOutputEntity> =
    derivedOutputs.sortedWith(compareBy<DiagnosticDerivedOutputKey>({ it.type.name }, { it.ordinal })).map {
        DiagnosticDerivedOutputEntity(
            id = ids.next().toString(),
            payloadId = it.payloadId.value.toString(),
            outputType = it.type.name,
            ordinal = it.ordinal,
        )
    }

internal fun DiagnosticPayloadEntity.toDomain(): DiagnosticPayload? {
    val payloadId = uuid(id)?.let(::DiagnosticPayloadId) ?: return null
    val captureFlow = enum<DiagnosticCaptureFlow>(flow) ?: return null
    val parser = parserVersion(parserVersion) ?: return null
    val parseResult = enum<DiagnosticParseResult>(result) ?: return null
    return DiagnosticPayload.create(
        id = payloadId,
        capture = DiagnosticPayloadCapture(command, captureFlow, privateBytes, deviceId?.let(::uuid)?.let(::DeviceId), firmware, parser, Instant.ofEpochMilli(capturedAtEpochMillis)),
        result = parseResult, safeError = safeError?.let { enum<SafeDiagnosticError>(it) },
    ).successOrNull()
}

internal fun HeartRateDeduplicationKey.encode(): String = when (this) {
    is HeartRateDeduplicationKey.D1History -> listOf("D1", deviceId.value, resolvedMeasuredAt?.toEpochMilli(), recordKey?.value?.encode() ?: "").joinToString("|")
    is HeartRateDeduplicationKey.MeasurementSession -> "MEASUREMENT|${sessionId.value}"
    is HeartRateDeduplicationKey.AppWorkoutSample -> "APP|${sessionId.value}|${measuredAt.toEpochMilli()}"
    is HeartRateDeduplicationKey.WatchWorkoutSample -> "WATCH|${activityId.value}|${measuredAt.toEpochMilli()}"
}

private fun decodeHeartRateKey(value: String): HeartRateDeduplicationKey? {
    val parts = value.split('|')
    return when (parts.firstOrNull()) {
        "D1" -> HeartRateDeduplicationKey.D1History(uuid(parts.getOrNull(1))?.let(::DeviceId) ?: return null,
            parts.getOrNull(2)?.toLongOrNull()?.let(Instant::ofEpochMilli), parts.getOrNull(3)?.takeIf(String::isNotEmpty)?.decode()?.let { br.com.mwconsulting.championcompanion.core.model.D1RecordKey.create(it).successOrNull() })
        "MEASUREMENT" -> uuid(parts.getOrNull(1))?.let { HeartRateDeduplicationKey.MeasurementSession(HeartRateMeasurementSessionId(it)) }
        "APP" -> uuid(parts.getOrNull(1))?.let { id -> parts.getOrNull(2)?.toLongOrNull()?.let { HeartRateDeduplicationKey.AppWorkoutSample(AppWorkoutSessionId(id), Instant.ofEpochMilli(it)) } }
        "WATCH" -> uuid(parts.getOrNull(1))?.let { id -> parts.getOrNull(2)?.toLongOrNull()?.let { HeartRateDeduplicationKey.WatchWorkoutSample(WorkoutActivityId(id), Instant.ofEpochMilli(it)) } }
        else -> null
    }
}

private fun String.encode(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())
private fun String.decode(): String? = runCatching { String(Base64.getUrlDecoder().decode(this)) }.getOrNull()
private fun uuid(value: String?): UUID? = value?.let { UuidV7.parse(it).successOrNull() }
private fun parserVersion(value: String): ParserVersion? = ParserVersion.create(value).successOrNull()
private inline fun <reified T : Enum<T>> enum(value: String?): T? = value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
private fun <T> DomainResult<T>.successOrNull(): T? = (this as? DomainResult.Success)?.value
private fun <T> DomainValue<T>.valueOrNull(): T? = (this as? DomainValue.Present)?.value
private fun <T> DomainValue<T>.absenceReasonOrNull(): String? = (this as? DomainValue.Absent)?.reason?.name
private fun Double?.domainValue(): DomainValue<Double> = if (this == null) DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE) else DomainValue.Present(this)
private fun Int?.domainValue(): DomainValue<Int> = if (this == null) DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE) else DomainValue.Present(this)
private fun Double?.sessionDomainValue(absenceReason: String?): DomainValue<Double>? = when {
    this != null && absenceReason == null -> DomainValue.Present(this)
    this == null && absenceReason != null -> enum<AbsenceReason>(absenceReason)?.let { DomainValue.Absent(it) }
    else -> null
}
private fun Int?.sessionDomainValue(absenceReason: String?): DomainValue<Int>? = when {
    this != null && absenceReason == null -> DomainValue.Present(this)
    this == null && absenceReason != null -> enum<AbsenceReason>(absenceReason)?.let { DomainValue.Absent(it) }
    else -> null
}
private fun WorkoutActivityEntity.calibratedMetricsOrNull(): CalibratedWorkoutMetrics? = when {
    calibratedAveragePaceSecondsPerKilometer == null && calibratedAverageSpeedMetersPerSecond == null -> null
    else -> CalibratedWorkoutMetrics(
        averagePaceSecondsPerKilometer = calibratedAveragePaceSecondsPerKilometer.domainValue(),
        averageSpeedMetersPerSecond = calibratedAverageSpeedMetersPerSecond.domainValue(),
    )
}

private fun WorkoutActivityOrigin.toDataOrigin(): DataOrigin = when (this) {
    WorkoutActivityOrigin.WATCH_D3_D4 -> DataOrigin.WATCH_D3_D4
    WorkoutActivityOrigin.PHONE_GPS,
    WorkoutActivityOrigin.PHONE_TREADMILL,
    WorkoutActivityOrigin.PHONE_FREE_WORKOUT,
    -> DataOrigin.APP_WORKOUT_SESSION
}

private fun TreadmillCalibrationEntity.factorDomainValue(): DomainValue<Double> = when {
    factor != null -> DomainValue.Present(factor)
    factorAbsenceReason != null -> DomainValue.Absent(enum<AbsenceReason>(factorAbsenceReason) ?: AbsenceReason.NOT_APPLICABLE)
    else -> DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE)
}
