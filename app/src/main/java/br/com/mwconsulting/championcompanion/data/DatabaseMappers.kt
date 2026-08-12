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
import br.com.mwconsulting.championcompanion.core.database.DerivedWorkoutIntervalEntity
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
import br.com.mwconsulting.championcompanion.core.database.WorkoutDerivationRecord
import br.com.mwconsulting.championcompanion.core.database.WorkoutDerivationSnapshotEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutHeartRateSampleEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutIntervalEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRouteEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutRoutePointEntity
import br.com.mwconsulting.championcompanion.core.database.WorkoutSplitEntity
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
import br.com.mwconsulting.championcompanion.core.model.DerivedWorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.DailySummary
import br.com.mwconsulting.championcompanion.core.model.DailySummaryId
import br.com.mwconsulting.championcompanion.core.model.DailySummaryKey
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncPayload
import br.com.mwconsulting.championcompanion.core.model.DailyStepsSyncPayloadKind
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
import br.com.mwconsulting.championcompanion.core.model.DomainValidationError
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregate
import br.com.mwconsulting.championcompanion.core.model.HealthSyncAggregateType
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionChildKey
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionOrigin
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionReconciliationCheckpoint
import br.com.mwconsulting.championcompanion.core.model.HealthProjectionReconciliationState
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperation
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationId
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationKey
import br.com.mwconsulting.championcompanion.core.model.HealthSyncOperationAction
import br.com.mwconsulting.championcompanion.core.model.HealthSyncRecordType
import br.com.mwconsulting.championcompanion.core.model.HealthSyncSummary
import br.com.mwconsulting.championcompanion.core.model.HealthSyncFrozenPayload
import br.com.mwconsulting.championcompanion.core.model.HeartRateDeduplicationKey
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementId
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurementSessionId
import br.com.mwconsulting.championcompanion.core.model.HeartRateSource
import br.com.mwconsulting.championcompanion.core.model.MetricProvenance
import br.com.mwconsulting.championcompanion.core.model.ParserValidity
import br.com.mwconsulting.championcompanion.core.model.ParserVersion
import br.com.mwconsulting.championcompanion.core.model.MeasuredWorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutCorrelationCandidate
import br.com.mwconsulting.championcompanion.core.model.SafeDiagnosticError
import br.com.mwconsulting.championcompanion.core.model.SafeSyncError
import br.com.mwconsulting.championcompanion.core.model.StepLengthProfile
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.SyncOperationState
import br.com.mwconsulting.championcompanion.core.model.TEMPORAL_ORDER_VALIDATED_MARKER
import br.com.mwconsulting.championcompanion.core.model.TemporalSeriesEligibility
import br.com.mwconsulting.championcompanion.core.model.TemporalSeriesValidationReason
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
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V1_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V2_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_D4_V3_PARSER_VERSION
import br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_LEGACY_DERIVATION_VERSION
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurement
import br.com.mwconsulting.championcompanion.core.model.WeightMeasurementId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityOrigin
import br.com.mwconsulting.championcompanion.core.model.WorkoutCanonicalReadStatus
import br.com.mwconsulting.championcompanion.core.model.WorkoutDerivationMethod
import br.com.mwconsulting.championcompanion.core.model.WorkoutDerivationSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutDistanceMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutDurationCase
import br.com.mwconsulting.championcompanion.core.model.WorkoutDurationResolution
import br.com.mwconsulting.championcompanion.core.model.WorkoutDurationResolver
import br.com.mwconsulting.championcompanion.core.model.WorkoutHeartRateSample
import br.com.mwconsulting.championcompanion.core.model.WorkoutInterval
import br.com.mwconsulting.championcompanion.core.model.WorkoutMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutPlausibilityIndicator
import br.com.mwconsulting.championcompanion.core.model.WorkoutSeriesPlausibility
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteMetrics
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePointId
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteSegmentReason
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteState
import br.com.mwconsulting.championcompanion.core.model.WorkoutSplit
import br.com.mwconsulting.championcompanion.core.model.WorkoutSplitCalculator
import br.com.mwconsulting.championcompanion.core.model.WorkoutIntervalWindow
import br.com.mwconsulting.championcompanion.core.model.WorkoutPlausibilityPolicy
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

    val mappedStepLengthSource = if (mappedOrigin == DataOrigin.WATCH_D3_D4) {
        canonicalStepLengthSource?.let {
            enum<StepLengthSource>(it) ?: return null
        }
    } else {
        null
    }
    val mappedPlausibilityIndicators = if (mappedOrigin == DataOrigin.WATCH_D3_D4) {
        decodeEnumSet<WorkoutPlausibilityIndicator>(canonicalPlausibilityIndicators) ?: return null
    } else {
        emptySet()
    }
    val canonicalReadStatus = if (mappedOrigin == DataOrigin.WATCH_D3_D4) {
        if (mappedConfirmedType == WorkoutType.CYCLING) {
            WorkoutCanonicalReadStatus.NOT_APPLICABLE
        } else {
            when {
                canonicalDerivationVersion == null -> WorkoutCanonicalReadStatus.NOT_MATERIALIZED
                enum<TemporalSeriesEligibility>(canonicalTemporalEligibility) != TemporalSeriesEligibility.ELIGIBLE ||
                    canonicalDistanceMeters == null -> WorkoutCanonicalReadStatus.SERIES_INELIGIBLE
                mappedPlausibilityIndicators.isNotEmpty() -> WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED
                else -> WorkoutCanonicalReadStatus.AVAILABLE
            }
        }
    } else {
        WorkoutCanonicalReadStatus.NOT_APPLICABLE
    }
    val mappedDistance = when {
        mappedConfirmedType == WorkoutType.CYCLING -> manualDistanceMeters?.takeIf {
            it.isFinite() && it > 0.0
        }
            .domainValue()

        mappedOrigin == DataOrigin.WATCH_D3_D4 -> when (canonicalReadStatus) {
            WorkoutCanonicalReadStatus.AVAILABLE,
            WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED,
            -> canonicalDistanceMeters.domainValue()

            else -> DomainValue.Absent(AbsenceReason.NOT_YET_RESOLVED)
        }

        else -> (calibratedDistanceMeters ?: originalDistanceMeters ?: estimatedDistanceMeters).domainValue()
    }

    return CalendarWorkoutSummary(
        activityId = mappedActivityId,
        origin = mappedOrigin,
        title = title,
        suggestedType = mappedSuggestedType,
        confirmedType = mappedConfirmedType,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        duration = Duration.ofNanos(durationNanos),
        distanceMeters = mappedDistance,
        caloriesKilocalories = caloriesKilocalories.domainValue(),
        averageBpm = averageBpm.domainValue(),
        workflowState = mappedWorkflowState,
        canonicalReadStatus = canonicalReadStatus,
        derivationVersion = canonicalDerivationVersion,
        stepLengthSource = mappedStepLengthSource,
        plausibilityIndicators = mappedPlausibilityIndicators,
    )
}

internal fun UserProfile.toEntity() = br.com.mwconsulting.championcompanion.core.database.UserProfileEntity(
    id = id.value.toString(),
    heightCentimeters = heightCentimeters,
    stepLengthMeters = stepLengthProfile?.stepLengthMeters,
    stepLengthSource = stepLengthProfile?.source?.name,
    stepLengthUpdatedAtEpochMillis = stepLengthProfile?.updatedAt?.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun br.com.mwconsulting.championcompanion.core.database.UserProfileEntity.toDomain(): UserProfile? {
    val stepLengthProfile = when {
        stepLengthMeters == null && stepLengthSource == null && stepLengthUpdatedAtEpochMillis == null -> null
        stepLengthMeters == null || stepLengthSource == null || stepLengthUpdatedAtEpochMillis == null -> return null
        else -> StepLengthProfile.create(
            stepLengthMeters = stepLengthMeters,
            source = enum<StepLengthSource>(stepLengthSource) ?: return null,
            updatedAt = Instant.ofEpochMilli(stepLengthUpdatedAtEpochMillis),
        ).successOrNull() ?: return null
    }
    return UserProfile.create(
        id = uuid(id)?.let(::UserProfileId) ?: return null,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        heightCentimeters = heightCentimeters,
        stepLengthProfile = stepLengthProfile,
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
        manualDistanceMeters = metrics.distances.manualDistanceMeters.valueOrNull()
            ?.takeIf { confirmedType == WorkoutType.CYCLING && it.isFinite() && it > 0.0 },
    )
}

internal fun WatchWorkoutImport.toEntity() = WatchWorkoutImportEntity(
    id.value.toString(), activityId.value.toString(), key.deviceId.value.toString(), key.origin.name, key.crc,
    externalBlockIndex, parserVersion.value, parserValidity.name, diagnosticPayloadId?.value?.toString(), importedAt.toEpochMilli(),
)

internal fun WorkoutActivity.toIntervals(activityId: String, ids: Iterator<UUID>) = intervals.map {
    val parserVersion = watchImportContext?.parserVersion?.value
    val isWatchD4 = activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4
    val isD4V1 = isWatchD4 && parserVersion == WATCH_WORKOUT_D4_V1_PARSER_VERSION
    val isD4V2OrV3 = isWatchD4 && parserVersion in setOf(
        WATCH_WORKOUT_D4_V2_PARSER_VERSION,
        WATCH_WORKOUT_D4_V3_PARSER_VERSION,
    )
    WorkoutIntervalEntity(
        id = ids.next().toString(),
        activityId = activityId,
        intervalIndex = it.index,
        durationNanos = it.duration.toNanos(),
        steps = it.steps,
        distanceMeters = it.distanceMeters.valueOrNull().takeUnless { isWatchD4 },
        caloriesKilocalories = it.caloriesKilocalories.valueOrNull().takeUnless { isWatchD4 },
        rawMotionValue = when {
            isD4V2OrV3 -> it.rawMotionValue
            isD4V1 -> it.rawMotionValue ?: it.steps
            else -> null
        },
        rawDistanceValue = it.rawDistanceValue.takeIf { isD4V1 || isD4V2OrV3 },
        rawCaloriesValue = it.rawCaloriesValue.takeIf { isD4V1 || isD4V2OrV3 },
        decodedSteps = it.decodedSteps.takeIf { isD4V2OrV3 },
    )
}

/** Mapeia somente fatos raw/measured já persistidos; não interpreta coluna legada. */
internal fun WorkoutIntervalEntity.toMeasuredWorkoutInterval(
    effectiveWindowStart: Instant? = null,
): MeasuredWorkoutInterval? {
    val rawMotion = rawMotionValue ?: return null
    val effectiveDuration = effectiveDurationNanos?.let { nanos ->
        if (nanos <= 0L) return null
        runCatching { Duration.ofNanos(nanos) }.getOrNull() ?: return null
    }
    val effectiveWindow = when {
        effectiveDuration == null -> null
        effectiveWindowStart == null -> return null
        else -> runCatching {
            WorkoutIntervalWindow.create(effectiveWindowStart, effectiveWindowStart.plus(effectiveDuration))
        }.getOrNull()?.successOrNull() ?: return null
    }
    return MeasuredWorkoutInterval.create(
        index = intervalIndex,
        rawMotionValue = rawMotion,
        decodedSteps = decodedSteps,
        rawTemporalDistance = rawDistanceValue,
        rawTemporalCalories = rawCaloriesValue,
        effectiveDuration = effectiveDuration,
        effectiveWindow = effectiveWindow,
    ).successOrNull()
}

/** Persiste somente duração resolvida; fatos raw/measured permanecem inalterados. */
internal fun WorkoutIntervalEntity.withEffectiveDuration(
    effectiveDuration: Duration?,
): WorkoutIntervalEntity = copy(
    effectiveDurationNanos = effectiveDuration?.toNanos(),
)

internal fun WorkoutDerivationSnapshot.toEntity(
    id: String,
    activityId: String,
    isActive: Boolean = true,
): WorkoutDerivationSnapshotEntity? {
    val derivedAtParts = derivedAt.toPersistedInstantPartsOrNull() ?: return null
    return runCatching {
        WorkoutDerivationSnapshotEntity(
            id = id,
            activityId = activityId,
            stepLengthMetersUsed = stepLengthMetersUsed,
            stepLengthSource = stepLengthSource.name,
            derivationMethod = derivationMethod.name,
            derivationVersion = derivationVersion,
            derivedAtEpochMillis = derivedAtParts.epochMillis,
            durationCase = temporalResolution.durationCase.name,
            totalDurationNanos = temporalResolution.totalDuration.toNanos(),
            nominalIntervalDurationNanos = temporalResolution.nominalIntervalDuration.toNanos(),
            tailUncoveredNanos = temporalResolution.tailUncovered.toNanos(),
            temporalEligibility = temporalResolution.eligibility.name,
            temporalEligibilityReasons = temporalResolution.reasons.encodeEnumNames(),
            plausibilityPolicy = plausibility.policy.name,
            plausibilityIndicators = plausibility.indicators.encodeEnumNames(),
            canonicalDistanceMeters = canonicalSeries?.canonicalDistanceMeters,
            canonicalCoveredDurationNanos = canonicalSeries?.coveredDuration?.toNanos(),
            diagnosticAggregateTotalSteps = diagnosticAggregateStepEstimate?.totalSteps,
            diagnosticAggregateStepLengthMeters = diagnosticAggregateStepEstimate?.stepLengthMetersUsed,
            diagnosticAggregateDistanceMeters = diagnosticAggregateStepEstimate?.estimatedDistanceMeters,
            derivedAtEpochSecond = derivedAtParts.epochSecond,
            derivedAtNano = derivedAtParts.nano,
            activeActivityId = activityId.takeIf { isActive },
            isActive = isActive,
        )
    }.getOrNull()
}

internal fun WorkoutDerivationSnapshot.toDerivedIntervalEntities(
    snapshotId: String,
    ids: Iterator<UUID>,
): List<DerivedWorkoutIntervalEntity>? = canonicalSeries?.intervals?.map { interval ->
    interval.toEntity(snapshotId, ids.next().toString()) ?: return null
} ?: emptyList()

internal fun WorkoutDerivationSnapshot.toSplitEntities(
    snapshotId: String,
    splits: List<WorkoutSplit>,
    ids: Iterator<UUID>,
): List<WorkoutSplitEntity>? = if (!isEligibleForFunctionalSplits && splits.isNotEmpty()) {
    null
} else {
    splits.map { split ->
        split.toEntity(ids.next().toString(), snapshotId) ?: return null
    }
}

private fun DerivedWorkoutInterval.toEntity(
    snapshotId: String,
    id: String,
): DerivedWorkoutIntervalEntity? {
    val startParts = effectiveWindow.start.toPersistedInstantPartsOrNull() ?: return null
    val speedSampledAtParts = speedSampledAt.toPersistedInstantPartsOrNull() ?: return null
    return runCatching {
        DerivedWorkoutIntervalEntity(
            id = id,
            snapshotId = snapshotId,
            intervalIndex = index,
            effectiveStartEpochMillis = startParts.epochMillis,
            effectiveStartEpochSecond = startParts.epochSecond,
            effectiveStartNano = startParts.nano,
            effectiveDurationNanos = effectiveDuration.toNanos(),
            decodedSteps = decodedSteps,
            canonicalIntervalDistanceMeters = canonicalIntervalDistanceMeters,
            cadenceStepsPerMinute = metrics.cadenceStepsPerMinute,
            speedMetersPerSecond = metrics.speedMetersPerSecond,
            paceSecondsPerKilometer = metrics.paceSecondsPerKilometer,
            cumulativeSteps = cumulativeSteps,
            cumulativeDistanceMeters = cumulativeDistanceMeters,
            cumulativeDurationNanos = cumulativeDuration.toNanos(),
            cumulativeCadenceStepsPerMinute = cumulativeMetrics.cadenceStepsPerMinute,
            cumulativeSpeedMetersPerSecond = cumulativeMetrics.speedMetersPerSecond,
            cumulativePaceSecondsPerKilometer = cumulativeMetrics.paceSecondsPerKilometer,
            speedSampledAtEpochMillis = speedSampledAtParts.epochMillis,
            speedSampledAtEpochSecond = speedSampledAtParts.epochSecond,
            speedSampledAtNano = speedSampledAtParts.nano,
        )
    }.getOrNull()
}

internal fun WorkoutSplit.toEntity(
    id: String,
    snapshotId: String,
): WorkoutSplitEntity? {
    val startParts = window.start.toPersistedInstantPartsOrNull() ?: return null
    val endParts = window.end.toPersistedInstantPartsOrNull() ?: return null
    return runCatching {
        WorkoutSplitEntity(
            id = id,
            snapshotId = snapshotId,
            splitNumber = number,
            targetDistanceMeters = targetDistanceMeters,
            windowStartEpochMillis = startParts.epochMillis,
            windowStartEpochSecond = startParts.epochSecond,
            windowStartNano = startParts.nano,
            windowEndEpochMillis = endParts.epochMillis,
            windowEndEpochSecond = endParts.epochSecond,
            windowEndNano = endParts.nano,
            distanceMeters = distanceMeters,
            durationNanos = duration.toNanos(),
            averageCadenceStepsPerMinute = averageCadenceStepsPerMinute,
            averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
            averagePaceSecondsPerKilometer = averagePaceSecondsPerKilometer,
            averageBpm = averageBpm,
            bpmCoveredDurationNanos = bpmCoveredDuration.toNanos(),
        )
    }.getOrNull()
}

internal fun DerivedWorkoutIntervalEntity.toDomain(
    rawInterval: WorkoutIntervalEntity,
    stepLengthMeters: Double,
): DerivedWorkoutInterval? {
    if (rawInterval.intervalIndex != intervalIndex || rawInterval.rawMotionValue == null ||
        rawInterval.decodedSteps != decodedSteps
    ) return null
    val duration = runCatching { Duration.ofNanos(effectiveDurationNanos) }.getOrNull()
        ?.takeUnless { it.isNegative || it.isZero } ?: return null
    val start = exactPersistedInstant(
        epochMillis = effectiveStartEpochMillis,
        epochSecond = effectiveStartEpochSecond,
        nano = effectiveStartNano,
    ) ?: return null
    val end = runCatching { start.plus(duration) }.getOrNull() ?: return null
    val window = WorkoutIntervalWindow.create(start, end).successOrNull() ?: return null
    val measured = MeasuredWorkoutInterval.create(
        index = intervalIndex,
        rawMotionValue = rawInterval.rawMotionValue,
        decodedSteps = decodedSteps,
        rawTemporalDistance = rawInterval.rawDistanceValue,
        rawTemporalCalories = rawInterval.rawCaloriesValue,
        effectiveDuration = duration,
        effectiveWindow = window,
    ).successOrNull() ?: return null
    val cumulativeDuration = runCatching { Duration.ofNanos(cumulativeDurationNanos) }.getOrNull()
        ?.takeUnless { it.isNegative || it.isZero } ?: return null
    val mapped = DerivedWorkoutInterval.create(
        measured = measured,
        stepLengthMeters = stepLengthMeters,
        cumulativeSteps = cumulativeSteps,
        cumulativeDistanceMeters = cumulativeDistanceMeters,
        cumulativeDuration = cumulativeDuration,
    ).successOrNull() ?: return null
    return mapped.takeIf {
        it.canonicalIntervalDistanceMeters == canonicalIntervalDistanceMeters &&
            it.metrics.cadenceStepsPerMinute == cadenceStepsPerMinute &&
            it.metrics.speedMetersPerSecond == speedMetersPerSecond &&
            it.metrics.paceSecondsPerKilometer == paceSecondsPerKilometer &&
            it.cumulativeSteps == cumulativeSteps &&
            it.cumulativeDistanceMeters == cumulativeDistanceMeters &&
            it.cumulativeMetrics.cadenceStepsPerMinute == cumulativeCadenceStepsPerMinute &&
            it.cumulativeMetrics.speedMetersPerSecond == cumulativeSpeedMetersPerSecond &&
            it.cumulativeMetrics.paceSecondsPerKilometer == cumulativePaceSecondsPerKilometer &&
            exactPersistedInstant(
                epochMillis = speedSampledAtEpochMillis,
                epochSecond = speedSampledAtEpochSecond,
                nano = speedSampledAtNano,
            ) == it.speedSampledAt
    }
}

internal fun WorkoutSplitEntity.toDomain(): WorkoutSplit? {
    if (splitNumber <= 0 || !targetDistanceMeters.isFinite() || targetDistanceMeters <= 0.0 ||
        !distanceMeters.isFinite() || distanceMeters <= 0.0 ||
        !averageCadenceStepsPerMinute.isFinite() || averageCadenceStepsPerMinute < 0.0 ||
        !averageSpeedMetersPerSecond.isFinite() || averageSpeedMetersPerSecond < 0.0 ||
        averagePaceSecondsPerKilometer?.let { !it.isFinite() || it <= 0.0 } == true ||
        averageBpm?.let { !it.isFinite() || it <= 0.0 } == true ||
        bpmCoveredDurationNanos < 0L
    ) return null
    val duration = runCatching { Duration.ofNanos(durationNanos) }.getOrNull()
        ?.takeUnless { it.isNegative || it.isZero } ?: return null
    val bpmCoveredDuration = runCatching { Duration.ofNanos(bpmCoveredDurationNanos) }.getOrNull()
        ?.takeUnless { it.isNegative } ?: return null
    if (bpmCoveredDuration > duration ||
        (bpmCoveredDuration.isZero && averageBpm != null) ||
        (!bpmCoveredDuration.isZero && averageBpm == null)
    ) return null
    val window = WorkoutIntervalWindow.create(
        exactPersistedInstant(windowStartEpochMillis, windowStartEpochSecond, windowStartNano) ?: return null,
        exactPersistedInstant(windowEndEpochMillis, windowEndEpochSecond, windowEndNano) ?: return null,
    ).successOrNull() ?: return null
    if (window.duration != duration) return null
    return WorkoutSplit(
        number = splitNumber,
        targetDistanceMeters = targetDistanceMeters,
        window = window,
        distanceMeters = distanceMeters,
        duration = duration,
        averageCadenceStepsPerMinute = averageCadenceStepsPerMinute,
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
        averagePaceSecondsPerKilometer = averagePaceSecondsPerKilometer,
        averageBpm = averageBpm,
        bpmCoveredDuration = bpmCoveredDuration,
    )
}

internal fun WorkoutDerivationRecord.toDomain(
    activityOrigin: WorkoutActivityOrigin,
    activityStartedAt: Instant,
    activityDurationNanos: Long,
    rawIntervals: List<WorkoutIntervalEntity>,
    heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
): WorkoutDerivationSnapshot? = snapshot.toDomain(
    activityOrigin = activityOrigin,
    activityStartedAt = activityStartedAt,
    activityDurationNanos = activityDurationNanos,
    rawIntervals = rawIntervals,
    derivedIntervals = intervals,
    splits = splits,
    heartRateSamples = heartRateSamples,
)

internal fun WorkoutDerivationSnapshotEntity.toDomain(
    activityOrigin: WorkoutActivityOrigin,
    activityStartedAt: Instant,
    activityDurationNanos: Long,
    rawIntervals: List<WorkoutIntervalEntity>,
    derivedIntervals: List<DerivedWorkoutIntervalEntity>,
    splits: List<WorkoutSplitEntity>,
    heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
): WorkoutDerivationSnapshot? {
    if (activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 ||
        activityDurationNanos != totalDurationNanos
    ) return null
    if ((isActive && activeActivityId != activityId) || (!isActive && activeActivityId != null)) return null
    val source = enum<StepLengthSource>(stepLengthSource) ?: return null
    val method = enum<WorkoutDerivationMethod>(derivationMethod) ?: return null
    val durationCase = enum<WorkoutDurationCase>(durationCase) ?: return null
    val eligibility = enum<TemporalSeriesEligibility>(temporalEligibility) ?: return null
    val policy = enum<WorkoutPlausibilityPolicy>(plausibilityPolicy) ?: return null
    val reasons = decodeEnumSet<TemporalSeriesValidationReason>(temporalEligibilityReasons) ?: return null
    val indicators = decodeEnumSet<WorkoutPlausibilityIndicator>(plausibilityIndicators) ?: return null
    if (nominalIntervalDurationNanos != WorkoutDurationResolver.DEFAULT_INTERVAL_DURATION.toNanos()) return null
    val persistedDerivedAt = exactPersistedInstant(derivedAtEpochMillis, derivedAtEpochSecond, derivedAtNano)
        ?: return null
    if (rawIntervals.any { it.activityId != activityId } ||
        derivedIntervals.any { it.snapshotId != id } ||
        splits.any { it.snapshotId != id }
    ) return null
    if (rawIntervals.any { it.temporalEligibilityReason != TEMPORAL_ORDER_VALIDATED_MARKER }) return null

    var nextWindowStart = activityStartedAt
    val measuredIntervals = rawIntervals.map { entity ->
        val measured = entity.toMeasuredWorkoutInterval(effectiveWindowStart = nextWindowStart)
            ?: return null
        entity.effectiveDurationNanos?.let { nanos ->
            nextWindowStart = runCatching { nextWindowStart.plusNanos(nanos) }.getOrNull() ?: return null
        }
        measured
    }
    val totalSteps = when {
        diagnosticAggregateTotalSteps == null && diagnosticAggregateStepLengthMeters == null &&
            diagnosticAggregateDistanceMeters == null -> null
        diagnosticAggregateTotalSteps == null || diagnosticAggregateStepLengthMeters == null ||
            diagnosticAggregateDistanceMeters == null -> return null
        else -> diagnosticAggregateTotalSteps
    }
    val totalDuration = runCatching { Duration.ofNanos(totalDurationNanos) }.getOrNull() ?: return null
    val snapshot = when (derivationVersion) {
        WATCH_WORKOUT_LEGACY_DERIVATION_VERSION -> WorkoutDerivationSnapshot.deriveLegacy(
            intervals = measuredIntervals,
            totalDuration = totalDuration,
            sessionStartedAt = activityStartedAt,
            stepLengthMeters = stepLengthMetersUsed,
            stepLengthSource = source,
            derivationVersion = derivationVersion,
            derivedAt = persistedDerivedAt,
            totalSteps = totalSteps,
            plausibility = WorkoutSeriesPlausibility(indicators = indicators, policy = policy),
        )

        br.com.mwconsulting.championcompanion.core.model.WATCH_WORKOUT_DERIVATION_VERSION ->
            WorkoutDerivationSnapshot.derive(
                intervals = measuredIntervals,
                totalDuration = totalDuration,
                sessionStartedAt = activityStartedAt,
                stepLengthMeters = stepLengthMetersUsed,
                stepLengthSource = source,
                derivationVersion = derivationVersion,
                derivedAt = persistedDerivedAt,
                totalSteps = totalSteps,
                plausibility = WorkoutSeriesPlausibility(indicators = indicators, policy = policy),
            )

        else -> return null
    }.successOrNull() ?: return null
    val temporal = snapshot.temporalResolution
    if (rawIntervals.size != temporal.intervals.size || rawIntervals.zip(temporal.intervals).any { (entity, resolved) ->
            entity.effectiveDurationNanos != null &&
                entity.effectiveDurationNanos != resolved.effectiveDuration?.toNanos()
        }
    ) return null
    if (temporal.isEligible && rawIntervals.any { it.effectiveDurationNanos == null }) return null
    if (snapshot.derivationMethod != method || temporal.durationCase != durationCase ||
        temporal.eligibility != eligibility || temporal.reasons != reasons ||
        temporal.tailUncovered.toNanos() != tailUncoveredNanos ||
        snapshot.canonicalDistanceMeters != canonicalDistanceMeters ||
        snapshot.canonicalSeries?.coveredDuration?.toNanos() != canonicalCoveredDurationNanos
    ) return null
    val diagnostic = snapshot.diagnosticAggregateStepEstimate
    if (diagnostic?.totalSteps != diagnosticAggregateTotalSteps ||
        diagnostic?.stepLengthMetersUsed != diagnosticAggregateStepLengthMeters ||
        diagnostic?.estimatedDistanceMeters != diagnosticAggregateDistanceMeters
    ) return null

    val canonicalSeries = snapshot.canonicalSeries
    if (canonicalSeries == null) {
        if (derivedIntervals.isNotEmpty() || splits.isNotEmpty()) return null
        return snapshot
    }
    val canonicalIntervals = canonicalSeries.intervals
    if (derivedIntervals.size != canonicalIntervals.size) return null
    val rawByIndex = rawIntervals.associateBy { it.intervalIndex }
    val mappedIntervals = derivedIntervals.map { entity ->
        entity.toDomain(rawByIndex[entity.intervalIndex] ?: return null, stepLengthMetersUsed)
            ?: return null
    }
    if (mappedIntervals.map { it.index } != canonicalIntervals.map { it.index }) return null
    if (mappedIntervals.zip(canonicalIntervals).any { (mapped, expected) -> mapped != expected }) return null

    val mappedSplits = splits.map { entity ->
        entity.toDomain() ?: return null
    }
    if (mappedSplits.isNotEmpty()) {
        if (!snapshot.isEligibleForFunctionalSplits) return null
        val expectedSplits = WorkoutSplitCalculator.calculateOrNull(canonicalSeries, heartRateSamples)
            ?: return null
        if (mappedSplits != expectedSplits) return null
    }
    // Snapshot legado de 014-07 pode não ter projeção de splits materializada.
    // Ausência não sintetiza linhas no read path; rederivação explícita faz isso.
    return snapshot.withPersistedSplits(mappedSplits)
}

internal fun WorkoutActivity.toHeartRateSamples(activityId: String, ids: Iterator<UUID>) = heartRateSamples.map {
    WorkoutHeartRateSampleEntity(
        id = ids.next().toString(),
        activityId = activityId,
        sampledAtEpochMillis = it.sampledAt.toEpochMilli(),
        bpm = it.bpm,
        sourceSampleIndex = it.sourceSampleIndex,
        sampledAtEpochSecond = it.sampledAt.epochSecond,
        sampledAtNano = it.sampledAt.nano,
    )
}

internal fun WorkoutHeartRateSampleEntity.toDomain(): WorkoutHeartRateSample? {
    val sampledAt = when {
        sampledAtEpochSecond == null && sampledAtNano == null -> Instant.ofEpochMilli(sampledAtEpochMillis)
        sampledAtEpochSecond == null || sampledAtNano == null -> return null
        else -> exactPersistedInstant(
            epochMillis = sampledAtEpochMillis,
            epochSecond = sampledAtEpochSecond,
            nano = sampledAtNano,
        ) ?: return null
    }
    return WorkoutHeartRateSample(
        sampledAt = sampledAt,
        bpm = bpm,
        sourceSampleIndex = sourceSampleIndex,
    )
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
    derivationSnapshot: WorkoutDerivationSnapshot? = null,
    canonicalReadStatusOverride: WorkoutCanonicalReadStatus? = null,
): WorkoutActivity? {
    val activityId = uuid(id)?.let(::WorkoutActivityId) ?: return null
    val mappedOrigin = enum<WorkoutActivityOrigin>(origin) ?: return null
    val isWatchD4V1 = mappedOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        importRecord?.parserVersion == WATCH_WORKOUT_D4_V1_PARSER_VERSION
    val isWatchD4V2OrV3 = mappedOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        importRecord?.parserVersion in setOf(
            WATCH_WORKOUT_D4_V2_PARSER_VERSION,
            WATCH_WORKOUT_D4_V3_PARSER_VERSION,
        )
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
    val mappedManualDistance = if (mappedConfirmedType == WorkoutType.CYCLING) {
        manualDistanceMeters.domainValue()
    } else {
        DomainValue.Absent(AbsenceReason.NOT_APPLICABLE)
    }
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
                mappedManualDistance,
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
            if (isWatchD4V2OrV3 && (
                    it.rawMotionValue == null ||
                    it.decodedSteps == null ||
                    it.steps != it.decodedSteps ||
                        it.decodedSteps != (it.rawMotionValue ushr 16)
                    )
            ) return null
            if (isWatchD4V1 && it.decodedSteps != null) return null
            WorkoutInterval(
                it.intervalIndex,
                Duration.ofNanos(it.durationNanos),
                it.steps,
                if (isWatchD4V2OrV3) DomainValue.Absent(AbsenceReason.UNKNOWN) else it.distanceMeters.domainValue(),
                if (isWatchD4V2OrV3) DomainValue.Absent(AbsenceReason.UNKNOWN) else it.caloriesKilocalories.domainValue(),
                rawMotionValue = it.rawMotionValue.takeIf { isWatchD4V1 || isWatchD4V2OrV3 },
                rawDistanceValue = it.rawDistanceValue.takeIf { isWatchD4V1 || isWatchD4V2OrV3 },
                rawCaloriesValue = it.rawCaloriesValue.takeIf { isWatchD4V1 || isWatchD4V2OrV3 },
                decodedSteps = it.decodedSteps.takeIf { isWatchD4V2OrV3 },
            )
        },
        heartRateSamples = samples.map { it.toDomain() ?: return null },
        suggestedType = enum<WorkoutType>(suggestedType) ?: return null,
        title = title,
        notes = notes,
        confirmedType = mappedConfirmedType,
        workflowState = mappedWorkflowState,
        reviewedAt = reviewedAtEpochMillis?.let(Instant::ofEpochMilli),
        calibrationId = calibrationId?.let(::uuid)?.let(::TreadmillCalibrationId),
        derivationSnapshot = derivationSnapshot,
        canonicalReadStatusOverride = canonicalReadStatusOverride,
    ).successOrNull()
}

internal fun HealthSyncWorkoutCandidateRecord.toHealthSyncCandidateDomain(
    intervals: List<WorkoutIntervalEntity>,
    samples: List<WorkoutHeartRateSampleEntity>,
    derivation: WorkoutDerivationRecord?,
    derivationIntervals: List<WorkoutIntervalEntity> = intervals,
): WorkoutActivity? {
    val importRecord = optionalImportEntity() ?: if (importId == null) null else return null
    if (importRecord != null && !importRecord.isStructurallyRelatedTo(activity)) return null
    val derivationSnapshot = derivation?.toDomain(
        activityOrigin = enum<WorkoutActivityOrigin>(activity.origin) ?: return null,
        activityStartedAt = Instant.ofEpochMilli(activity.startedAtEpochMillis),
        activityDurationNanos = activity.durationNanos,
        rawIntervals = derivationIntervals,
        heartRateSamples = samples.map { it.toDomain() ?: return null },
    )
    return activity.toDomain(
        importRecord = importRecord,
        intervals = intervals,
        samples = samples,
        derivationSnapshot = derivationSnapshot,
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
    val mappedParserVersion = parserVersion?.let(::parserVersion)
    val mappedParserValidity = enum<ParserValidity>(parserValidity) ?: return null
    val mappedWorkflowState = enum<ActivityWorkflowState>(workflowState) ?: return null
    return WatchWorkoutImportLookup(
        activityId = mappedActivityId,
        importId = mappedImportId,
        deviceId = mappedDeviceId,
        crc = crc,
        parserVersion = mappedParserVersion,
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
    operationSequence = dailyStepsPayload?.sequence ?: 0L,
    dailyStepsPayloadKind = dailyStepsPayload?.kind?.name,
    previousTotalSteps = dailyStepsPayload?.previousTotalSteps,
    currentTotalSteps = dailyStepsPayload?.currentTotalSteps,
    stepDelta = dailyStepsPayload?.stepDelta,
    stepStartTimeEpochMillis = dailyStepsPayload?.startTime?.toEpochMilli(),
    stepEndTimeEpochMillis = dailyStepsPayload?.endTime?.toEpochMilli(),
    projectionChildKey = key.projectionChildKey?.stableValue
        ?: HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY,
    action = action.name,
    frozenPayload = frozenPayload?.serializedPayload,
    frozenPayloadVersion = frozenPayload?.schemaVersion,
    reconciliationState = reconciliationState.name,
    reconciliationCheckpoint = reconciliationCheckpoint.name,
    projectionOriginId = projectionOrigin?.originId,
    projectionOriginVersion = projectionOrigin?.originVersion,
)

internal fun HealthSyncOperationEntity.toDomain(): HealthSyncOperation? {
    val aggregate = toHealthSyncAggregate(aggregateType, aggregateId) ?: return null
    val projectionChildKey = this.projectionChildKey
        .takeUnless { it == HealthSyncOperationEntity.LEGACY_PROJECTION_CHILD_KEY }
        ?.let { HealthProjectionChildKey.fromStableValue(it) ?: return null }
    val dailyStepsPayload = toDailyStepsSyncPayload()
    if (dailyStepsPayload == null && hasAnyDailyStepsPayloadField()) return null
    val persistedFrozenPayload = this.frozenPayload
    val frozenPayload = if (persistedFrozenPayload == null && frozenPayloadVersion == null) {
        null
    } else {
        val payloadVersion = frozenPayloadVersion ?: return null
        val payload = persistedFrozenPayload ?: return null
        runCatching {
            HealthSyncFrozenPayload(
                schemaVersion = payloadVersion,
                serializedPayload = payload,
            )
        }.getOrNull() ?: return null
    }
    val projectionOrigin = if (projectionOriginId == null && projectionOriginVersion == null) {
        null
    } else {
        val originId = projectionOriginId ?: return null
        val originVersion = projectionOriginVersion ?: return null
        runCatching {
            HealthProjectionOrigin(
                originId = originId,
                originVersion = originVersion,
            )
        }.getOrNull() ?: return null
    }
    val operationState = enum<SyncOperationState>(state) ?: return null
    val operationAction = enum<HealthSyncOperationAction>(action) ?: return null
    val operationReconciliationState =
        enum<HealthProjectionReconciliationState>(reconciliationState) ?: return null
    val operationReconciliationCheckpoint =
        enum<HealthProjectionReconciliationCheckpoint>(reconciliationCheckpoint) ?: return null
    val operationLastError = lastError?.let { enum<SafeSyncError>(it) ?: return null }
    return HealthSyncOperation.create(
        id = uuid(id)?.let(::HealthSyncOperationId) ?: return null,
        key = HealthSyncOperationKey(
            aggregate = aggregate,
            recordType = enum<HealthSyncRecordType>(recordType) ?: return null,
            projectionChildKey = projectionChildKey,
        ),
        clientRecordId = clientRecordId,
        clientRecordVersion = clientRecordVersion,
        syncedAt = syncedAtEpochMillis?.let(Instant::ofEpochMilli),
        timeZoneId = timeZoneId,
        dailyStepsPayload = dailyStepsPayload,
        createdAt = createdAtEpochMillis?.let(Instant::ofEpochMilli),
        updatedAt = updatedAtEpochMillis?.let(Instant::ofEpochMilli),
        action = operationAction,
        frozenPayload = frozenPayload,
        reconciliationState = operationReconciliationState,
        reconciliationCheckpoint = operationReconciliationCheckpoint,
        projectionOrigin = projectionOrigin,
        state = operationState,
        attemptCount = attemptCount,
        lastAttemptAt = lastAttemptAtEpochMillis?.let(Instant::ofEpochMilli),
        lastError = operationLastError,
    ).successOrNull()
}

/** Variante explícita: operação não reconstituível nunca vira ausência silenciosa. */
internal fun HealthSyncOperationEntity.toDomainStrict(): DomainResult<HealthSyncOperation> =
    toDomain()?.let { DomainResult.Success(it) }
        ?: DomainResult.Failure(DomainValidationError.INVALID_STATE)

internal fun HealthSyncOperationEntity.toAggregateOrNull(): HealthSyncAggregate? =
    toHealthSyncAggregate(aggregateType, aggregateId)

private fun HealthSyncOperationEntity.toDailyStepsSyncPayload(): DailyStepsSyncPayload? {
    if (!hasAnyDailyStepsPayloadField()) return null
    return runCatching {
        DailyStepsSyncPayload(
            kind = enum<DailyStepsSyncPayloadKind>(dailyStepsPayloadKind) ?: return null,
            sequence = operationSequence,
            previousTotalSteps = previousTotalSteps ?: return null,
            currentTotalSteps = currentTotalSteps ?: return null,
            stepDelta = stepDelta ?: return null,
            startTime = stepStartTimeEpochMillis?.let(Instant::ofEpochMilli) ?: return null,
            endTime = stepEndTimeEpochMillis?.let(Instant::ofEpochMilli) ?: return null,
            timeZoneId = timeZoneId ?: return null,
        )
    }.getOrNull()
}

private fun HealthSyncOperationEntity.hasAnyDailyStepsPayloadField(): Boolean =
    dailyStepsPayloadKind != null ||
        previousTotalSteps != null ||
        currentTotalSteps != null ||
        stepDelta != null ||
        stepStartTimeEpochMillis != null ||
        stepEndTimeEpochMillis != null

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

private fun <T : Enum<T>> Set<T>.encodeEnumNames(): String? = map { it.name }
    .sorted()
    .takeIf { it.isNotEmpty() }
    ?.joinToString("|")

private inline fun <reified T : Enum<T>> decodeEnumSet(value: String?): Set<T>? {
    if (value.isNullOrEmpty()) return emptySet()
    val names = value.split('|')
    if (names.any(String::isEmpty) || names != names.distinct() || names != names.sorted()) return null
    return names.map { enum<T>(it) ?: return null }.toSet()
}

private data class PersistedInstantParts(
    val epochMillis: Long,
    val epochSecond: Long,
    val nano: Int,
)

private fun Instant.toPersistedInstantPartsOrNull(): PersistedInstantParts? = runCatching {
    PersistedInstantParts(
        epochMillis = toEpochMilli(),
        epochSecond = epochSecond,
        nano = nano,
    )
}.getOrNull()

private fun exactPersistedInstant(epochMillis: Long, epochSecond: Long, nano: Int): Instant? {
    if (nano !in 0 until 1_000_000_000) return null
    return runCatching { Instant.ofEpochSecond(epochSecond, nano.toLong()) }
        .getOrNull()
        ?.takeIf { it.toEpochMilli() == epochMillis }
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
