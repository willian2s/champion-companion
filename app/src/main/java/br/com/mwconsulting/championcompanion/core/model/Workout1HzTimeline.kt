package br.com.mwconsulting.championcompanion.core.model

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor

const val WATCH_WORKOUT_BPM_1HZ_ALGORITHM_VERSION = "watch-d3-d4-1hz-v1"
const val WORKOUT_1HZ_BPM_PARAMETER_SET = "workout-1hz-params-v1"
const val WORKOUT_1HZ_CADENCE_PARAMETER_SET = "workout-1hz-params-v1"

enum class Workout1HzLayer {
    RESAMPLED_1HZ,
}

enum class Workout1HzProvenance {
    OBSERVED_DERIVED,
    INTERPOLATED,
    ABSENT,
    UNSUPPORTED,
}

/** Parâmetros congelados da primeira série derivada de BPM em 1 Hz. */
data class Workout1HzBpmParameterSet(
    val validBpmRange: IntRange = 30..220,
    val hampelRadius: Int = 2,
    val hampelMultiplier: Int = 3,
    val hampelScale: Double = 1.4826,
    val hampelFloorBpm: Int = 12,
    val emaTauSeconds: Double = 5.0,
    val fallbackGapSeconds: Int = 90,
    val version: String = WORKOUT_1HZ_BPM_PARAMETER_SET,
) {
    init {
        require(validBpmRange.first >= 0 && validBpmRange.last >= validBpmRange.first)
        require(hampelRadius >= 1)
        require(hampelMultiplier >= 1)
        require(hampelScale.isFinite() && hampelScale > 0.0)
        require(hampelFloorBpm >= 0)
        require(emaTauSeconds.isFinite() && emaTauSeconds > 0.0)
        require(fallbackGapSeconds > 0)
        require(version.matches(Regex("[a-z0-9._-]+")))
    }

    /** Manifesto sem BOM e sem linha final, usado para identidade da série. */
    val manifest: String
        get() = mapOf(
            "algorithm-version" to WATCH_WORKOUT_BPM_1HZ_ALGORITHM_VERSION,
            "ema.tau-seconds" to canonicalDecimal(emaTauSeconds),
            "hampel.floor-bpm" to hampelFloorBpm.toString(),
            "hampel.multiplier" to hampelMultiplier.toString(),
            "hampel.radius" to hampelRadius.toString(),
            "hampel.scale" to canonicalDecimal(hampelScale),
            "parameter-set" to version,
            "bpm.valid-max" to validBpmRange.last.toString(),
            "bpm.valid-min" to validBpmRange.first.toString(),
            "continuity.fallback-gap-seconds" to fallbackGapSeconds.toString(),
        ).entries
            .sortedWith { left, right -> compareAsciiBytes(left.key, right.key) }
            .joinToString("\n") { (key, value) -> "$key=$value" }

    val fingerprint: String
        get() = MessageDigest.getInstance("SHA-256")
            .digest(manifest.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte ->
                val value = byte.toInt() and 0xff
                HEX[value ushr 4].toString() + HEX[value and 0x0f]
            }

    companion object {
        val CURRENT = Workout1HzBpmParameterSet()
    }
}

/** Parâmetros congelados da primeira série derivada de cadência em 1 Hz. */
data class Workout1HzCadenceParameterSet(
    val hampelRadius: Int = 2,
    val hampelMultiplier: Int = 3,
    val hampelScale: Double = 1.4826,
    val hampelFloorSpm: Int = 20,
    val emaTauSeconds: Double = 5.0,
    val fallbackGapSeconds: Int = 90,
    val version: String = WORKOUT_1HZ_CADENCE_PARAMETER_SET,
) {
    init {
        require(hampelRadius >= 1)
        require(hampelMultiplier >= 1)
        require(hampelScale.isFinite() && hampelScale > 0.0)
        require(hampelFloorSpm >= 0)
        require(emaTauSeconds.isFinite() && emaTauSeconds > 0.0)
        require(fallbackGapSeconds in 1..MAX_FALLBACK_GAP_SECONDS)
        require(version.matches(Regex("[a-z0-9._-]+")))
    }

    /** Manifesto sem BOM e sem linha final, usado para identidade da série. */
    val manifest: String
        get() = mapOf(
            "algorithm-version" to WATCH_WORKOUT_BPM_1HZ_ALGORITHM_VERSION,
            "cadence.valid-min-spm" to "0",
            "ema.tau-seconds" to canonicalDecimal(emaTauSeconds),
            "hampel.floor-spm" to hampelFloorSpm.toString(),
            "hampel.multiplier" to hampelMultiplier.toString(),
            "hampel.radius" to hampelRadius.toString(),
            "hampel.scale" to canonicalDecimal(hampelScale),
            "parameter-set" to version,
            "continuity.fallback-gap-seconds" to fallbackGapSeconds.toString(),
        ).entries
            .sortedWith { left, right -> compareAsciiBytes(left.key, right.key) }
            .joinToString("\n") { (key, value) -> "$key=$value" }

    val fingerprint: String
        get() = MessageDigest.getInstance("SHA-256")
            .digest(manifest.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte ->
                val value = byte.toInt() and 0xff
                HEX[value ushr 4].toString() + HEX[value and 0x0f]
            }

    companion object {
        val CURRENT = Workout1HzCadenceParameterSet()

        private const val MAX_FALLBACK_GAP_SECONDS = 90
    }
}

data class Workout1HzSeriesIdentity(
    val algorithmVersion: String,
    val sourceParserVersion: String,
    val sourceDerivationVersion: String,
    val parameterSet: String,
    val parameterFingerprint: String,
    val parameterManifest: String,
)

data class WorkoutBpm1HzPoint(
    val offsetSeconds: Int,
    val instant: Instant,
    val bucketDuration: Duration,
    val bpm: Double?,
    val provenance: Workout1HzProvenance,
    val layer: Workout1HzLayer = Workout1HzLayer.RESAMPLED_1HZ,
) {
    /** Arredondamento half-up somente para consumidores que exigem BPM inteiro. */
    val roundedBpm: Int?
        get() = bpm?.takeIf { it.isFinite() }?.let { floor(it + 0.5).toInt() }
}

data class WorkoutBpm1HzTimeline(
    val sessionWindow: WorkoutIntervalWindow,
    val identity: Workout1HzSeriesIdentity,
    val points: List<WorkoutBpm1HzPoint>,
) {
    init {
        require(points.zipWithNext().all { (previous, current) ->
            current.offsetSeconds == previous.offsetSeconds + 1 && current.instant.isAfter(previous.instant)
        })
        require(points.all { it.layer == Workout1HzLayer.RESAMPLED_1HZ })
    }
}

data class WorkoutCadence1HzPoint(
    val offsetSeconds: Int,
    val instant: Instant,
    val bucketDuration: Duration,
    val cadenceSpm: Double?,
    val provenance: Workout1HzProvenance,
    val layer: Workout1HzLayer = Workout1HzLayer.RESAMPLED_1HZ,
)

data class WorkoutCadence1HzTimeline(
    val sessionWindow: WorkoutIntervalWindow,
    val identity: Workout1HzSeriesIdentity,
    val points: List<WorkoutCadence1HzPoint>,
) {
    init {
        require(points.zipWithNext().all { (previous, current) ->
            current.offsetSeconds == previous.offsetSeconds + 1 && current.instant.isAfter(previous.instant)
        })
        require(points.all { it.layer == Workout1HzLayer.RESAMPLED_1HZ })
    }
}

/** Grade comum local que compõe canais independentes sem alterar SOURCE. */
data class Workout1HzTimelinePoint(
    val offsetSeconds: Int,
    val instant: Instant,
    val bucketDuration: Duration,
    val bpm: Double?,
    val bpmProvenance: Workout1HzProvenance,
    val cadenceSpm: Double?,
    val cadenceProvenance: Workout1HzProvenance,
    val layer: Workout1HzLayer = Workout1HzLayer.RESAMPLED_1HZ,
)

data class Workout1HzTimeline(
    val sessionWindow: WorkoutIntervalWindow,
    val bpmIdentity: Workout1HzSeriesIdentity?,
    val cadenceIdentity: Workout1HzSeriesIdentity?,
    val points: List<Workout1HzTimelinePoint>,
) {
    init {
        require(points.isNotEmpty())
        require(points.zipWithNext().all { (previous, current) ->
            current.offsetSeconds == previous.offsetSeconds + 1 && current.instant.isAfter(previous.instant)
        })
        require(points.all { it.layer == Workout1HzLayer.RESAMPLED_1HZ })
    }

    /** Reduz custo de desenho sem conectar pontos através de uma ausência. */
    fun downsampleForRendering(maxPoints: Int = 720): List<Workout1HzTimelinePoint> {
        require(maxPoints >= 2)
        if (points.size <= maxPoints) return points

        return List(maxPoints) { bucketIndex ->
            val startIndex = bucketIndex * points.size / maxPoints
            val endIndex = ((bucketIndex + 1) * points.size / maxPoints).coerceAtLeast(startIndex + 1)
            val representative = points[(startIndex + endIndex - 1) / 2]
            var bpmHasGap = false
            var bpmHasInterpolation = false
            var cadenceHasGap = false
            var cadenceHasInterpolation = false
            for (index in startIndex until endIndex.coerceAtMost(points.size)) {
                val point = points[index]
                if (bpmIdentity != null) {
                    if (point.bpm == null) bpmHasGap = true
                    if (point.bpmProvenance == Workout1HzProvenance.INTERPOLATED) bpmHasInterpolation = true
                }
                if (cadenceIdentity != null) {
                    if (point.cadenceSpm == null) cadenceHasGap = true
                    if (point.cadenceProvenance == Workout1HzProvenance.INTERPOLATED) cadenceHasInterpolation = true
                }
            }
            representative.copy(
                bpm = representative.bpm.takeUnless { bpmIdentity != null && bpmHasGap },
                bpmProvenance = if (bpmIdentity == null) {
                    Workout1HzProvenance.UNSUPPORTED
                } else if (bpmHasGap) {
                    Workout1HzProvenance.ABSENT
                } else if (bpmHasInterpolation) {
                    Workout1HzProvenance.INTERPOLATED
                } else {
                    representative.bpmProvenance
                },
                cadenceSpm = representative.cadenceSpm.takeUnless { cadenceIdentity != null && cadenceHasGap },
                cadenceProvenance = if (cadenceIdentity == null) {
                    Workout1HzProvenance.UNSUPPORTED
                } else if (cadenceHasGap) {
                    Workout1HzProvenance.ABSENT
                } else if (cadenceHasInterpolation) {
                    Workout1HzProvenance.INTERPOLATED
                } else {
                    representative.cadenceProvenance
                },
            )
        }
    }
}

/** Deriva canais e os compõe uma única vez sobre a mesma grade temporal. */
fun WorkoutActivity.deriveWorkout1HzTimeline(
    bpmParameters: Workout1HzBpmParameterSet = Workout1HzBpmParameterSet.CURRENT,
    cadenceParameters: Workout1HzCadenceParameterSet = Workout1HzCadenceParameterSet.CURRENT,
    cancellationCheck: (() -> Unit)? = null,
): DomainResult<Workout1HzTimeline> {
    cancellationCheck?.invoke()
    val window = when (val result = WorkoutIntervalWindow.create(metrics.startedAt, metrics.endedAt)) {
        is DomainResult.Success -> result.value
        is DomainResult.Failure -> return result
    }
    val bpmTimeline = deriveBpm1Hz(
        parameters = bpmParameters,
        cancellationCheck = cancellationCheck,
    ).valueOrNull()
    cancellationCheck?.invoke()
    val cadenceTimeline = deriveCadence1Hz(
        parameters = cadenceParameters,
        cancellationCheck = cancellationCheck,
    ).valueOrNull()
    cancellationCheck?.invoke()
    val pointCount = maxOf(bpmTimeline?.points?.size ?: 0, cadenceTimeline?.points?.size ?: 0)
    if (pointCount == 0) return DomainResult.Failure(DomainValidationError.INVALID_RANGE)

    val points = List(pointCount) { index ->
        cancellationCheck?.invoke()
        val bpmPoint = bpmTimeline?.points?.getOrNull(index)
        val cadencePoint = cadenceTimeline?.points?.getOrNull(index)
        val baseOffset = bpmPoint?.offsetSeconds ?: cadencePoint?.offsetSeconds ?: return@List null
        val baseInstant = bpmPoint?.instant ?: cadencePoint?.instant ?: return@List null
        val baseDuration = bpmPoint?.bucketDuration ?: cadencePoint?.bucketDuration ?: return@List null
        Workout1HzTimelinePoint(
            offsetSeconds = baseOffset,
            instant = baseInstant,
            bucketDuration = baseDuration,
            bpm = bpmPoint?.bpm,
            bpmProvenance = bpmPoint?.provenance ?: Workout1HzProvenance.UNSUPPORTED,
            cadenceSpm = cadencePoint?.cadenceSpm,
            cadenceProvenance = cadencePoint?.provenance ?: Workout1HzProvenance.UNSUPPORTED,
            layer = Workout1HzLayer.RESAMPLED_1HZ,
        )
    }.filterNotNull()

    return DomainResult.Success(
        Workout1HzTimeline(
            sessionWindow = window,
            bpmIdentity = bpmTimeline?.identity,
            cadenceIdentity = cadenceTimeline?.identity,
            points = points,
        ),
    )
}

/** Deriva BPM sob demanda sem alterar samples, snapshots ou métricas existentes. */
fun WorkoutActivity.deriveBpm1Hz(
    sourceParserVersion: String? = null,
    sourceDerivationVersion: String? = null,
    parameters: Workout1HzBpmParameterSet = Workout1HzBpmParameterSet.CURRENT,
    cancellationCheck: (() -> Unit)? = null,
): DomainResult<WorkoutBpm1HzTimeline> {
    cancellationCheck?.invoke()
    val importContext = watchImportContext ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
    val actualParserVersion = importContext.parserVersion.value
    val actualDerivationVersion = derivationSnapshot?.derivationVersion
    val sessionWindow = WorkoutIntervalWindow.create(metrics.startedAt, metrics.endedAt)

    if (
        activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 ||
        actualParserVersion != WATCH_WORKOUT_D4_V3_PARSER_VERSION ||
        sourceParserVersion != null && sourceParserVersion != actualParserVersion ||
        actualDerivationVersion == null ||
        sourceDerivationVersion != null && sourceDerivationVersion != actualDerivationVersion ||
        parameters.validBpmRange != MIN_SUPPORTED_BPM..MAX_SUPPORTED_BPM ||
        actualDerivationVersion !in SUPPORTED_SOURCE_DERIVATION_VERSIONS ||
        sessionWindow !is DomainResult.Success
    ) {
        return DomainResult.Failure(
            if (sessionWindow is DomainResult.Failure) sessionWindow.error else DomainValidationError.INVALID_STATE,
        )
    }

    val derivationVersion = actualDerivationVersion
    val window = (sessionWindow as DomainResult.Success).value
    val identity = Workout1HzSeriesIdentity(
        algorithmVersion = WATCH_WORKOUT_BPM_1HZ_ALGORITHM_VERSION,
        sourceParserVersion = actualParserVersion,
        sourceDerivationVersion = derivationVersion,
        parameterSet = parameters.version,
        parameterFingerprint = parameters.fingerprint,
        parameterManifest = parameters.manifest,
    )
    val segments = buildSourceSegments(
        samples = heartRateSamples,
        window = window,
        fallbackGapSeconds = parameters.fallbackGapSeconds,
        cancellationCheck = cancellationCheck,
    )
    cancellationCheck?.invoke()
    val supports = deriveSupports(segments, parameters, cancellationCheck)
    val bucketCount = bucketCount(window)
        ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
    val points = ArrayList<WorkoutBpm1HzPoint>(bucketCount.toInt())
    var supportIndex = 0
    repeat(bucketCount.toInt()) { offset ->
        cancellationCheck?.invoke()
        val bucketStart = window.start.plusSeconds(offset.toLong())
        val bucketEnd = minInstant(window.end, bucketStart.plusSeconds(1L))
        while (supportIndex + 1 < supports.size && !supports[supportIndex + 1].instant.isAfter(bucketStart)) {
            cancellationCheck?.invoke()
            supportIndex++
        }
        val previous = supports.getOrNull(supportIndex)
        val next = supports.getOrNull(supportIndex + 1)
        val value = when {
            previous?.instant == bucketStart -> previous.value to Workout1HzProvenance.OBSERVED_DERIVED
            previous != null && next != null &&
                previous.segmentId == next.segmentId &&
                previous.instant.isBefore(bucketStart) && bucketStart.isBefore(next.instant) -> {
                val totalSeconds = secondsBetween(previous.instant, next.instant)
                val elapsedSeconds = secondsBetween(previous.instant, bucketStart)
                (previous.value + (next.value - previous.value) * (elapsedSeconds / totalSeconds)) to
                    Workout1HzProvenance.INTERPOLATED
            }
            else -> null to Workout1HzProvenance.ABSENT
        }
        points += WorkoutBpm1HzPoint(
            offsetSeconds = offset,
            instant = bucketStart,
            bucketDuration = Duration.between(bucketStart, bucketEnd),
            bpm = value.first,
            provenance = value.second,
        )
    }
    return DomainResult.Success(WorkoutBpm1HzTimeline(window, identity, points.toList()))
}

/** Deriva cadência sob demanda sem alterar intervalos, snapshots ou métricas existentes. */
fun WorkoutActivity.deriveCadence1Hz(
    sourceParserVersion: String? = null,
    sourceDerivationVersion: String? = null,
    parameters: Workout1HzCadenceParameterSet = Workout1HzCadenceParameterSet.CURRENT,
    cancellationCheck: (() -> Unit)? = null,
): DomainResult<WorkoutCadence1HzTimeline> {
    cancellationCheck?.invoke()
    val importContext = watchImportContext ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
    val actualParserVersion = importContext.parserVersion.value
    val actualDerivationVersion = derivationSnapshot?.derivationVersion
    val temporalResolution = derivationSnapshot?.temporalResolution
    val stepBasedType = confirmedType ?: suggestedType
    val sessionWindow = WorkoutIntervalWindow.create(metrics.startedAt, metrics.endedAt)

    if (
        activityOrigin != WorkoutActivityOrigin.WATCH_D3_D4 ||
        stepBasedType !in STEP_SEMANTIC_WORKOUT_TYPES ||
        actualParserVersion != WATCH_WORKOUT_D4_V3_PARSER_VERSION ||
        importContext.parserValidity != ParserValidity.VALID ||
        sourceParserVersion != null && sourceParserVersion != actualParserVersion ||
        actualDerivationVersion == null ||
        sourceDerivationVersion != null && sourceDerivationVersion != actualDerivationVersion ||
        actualDerivationVersion !in SUPPORTED_SOURCE_DERIVATION_VERSIONS ||
        temporalResolution == null ||
        temporalResolution.intervals.isEmpty() ||
        temporalResolution.intervals.any { interval ->
            val intervalWindow = interval.effectiveWindow
            intervalWindow == null ||
                !interval.effectiveDuration.orZeroIsPositiveAndMatches(intervalWindow)
        } ||
        sessionWindow !is DomainResult.Success
    ) {
        return DomainResult.Failure(
            if (sessionWindow is DomainResult.Failure) sessionWindow.error else DomainValidationError.INVALID_STATE,
        )
    }

    val derivationVersion = actualDerivationVersion
    val window = (sessionWindow as DomainResult.Success).value
    val identity = Workout1HzSeriesIdentity(
        algorithmVersion = WATCH_WORKOUT_BPM_1HZ_ALGORITHM_VERSION,
        sourceParserVersion = actualParserVersion,
        sourceDerivationVersion = derivationVersion,
        parameterSet = parameters.version,
        parameterFingerprint = parameters.fingerprint,
        parameterManifest = parameters.manifest,
    )
    val segments = buildCadenceSourceSegments(
        intervals = temporalResolution.intervals,
        window = window,
        fallbackGapSeconds = parameters.fallbackGapSeconds,
        cancellationCheck = cancellationCheck,
    )
    cancellationCheck?.invoke()
    val supports = deriveCadenceSupports(segments, parameters, cancellationCheck)
    val bucketCount = bucketCount(window)
        ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
    val points = ArrayList<WorkoutCadence1HzPoint>(bucketCount.toInt())
    var supportIndex = 0
    repeat(bucketCount.toInt()) { offset ->
        cancellationCheck?.invoke()
        val bucketStart = window.start.plusSeconds(offset.toLong())
        val bucketEnd = minInstant(window.end, bucketStart.plusSeconds(1L))
        while (supportIndex + 1 < supports.size && !supports[supportIndex + 1].instant.isAfter(bucketStart)) {
            cancellationCheck?.invoke()
            supportIndex++
        }
        val previous = supports.getOrNull(supportIndex)
        val next = supports.getOrNull(supportIndex + 1)
        val value = when {
            previous?.instant == bucketStart -> previous.value to Workout1HzProvenance.OBSERVED_DERIVED
            previous != null && next != null &&
                previous.segmentId == next.segmentId &&
                previous.instant.isBefore(bucketStart) && bucketStart.isBefore(next.instant) -> {
                val totalSeconds = secondsBetween(previous.instant, next.instant)
                val elapsedSeconds = secondsBetween(previous.instant, bucketStart)
                (previous.value + (next.value - previous.value) * (elapsedSeconds / totalSeconds)) to
                    Workout1HzProvenance.INTERPOLATED
            }
            else -> null to Workout1HzProvenance.ABSENT
        }
        points += WorkoutCadence1HzPoint(
            offsetSeconds = offset,
            instant = bucketStart,
            bucketDuration = Duration.between(bucketStart, bucketEnd),
            cadenceSpm = value.first,
            provenance = value.second,
        )
    }
    return DomainResult.Success(WorkoutCadence1HzTimeline(window, identity, points.toList()))
}

private const val MIN_SUPPORTED_BPM = 30
private const val MAX_SUPPORTED_BPM = 220
private val SUPPORTED_SOURCE_DERIVATION_VERSIONS = setOf(
    WATCH_WORKOUT_DERIVATION_VERSION,
    WATCH_WORKOUT_REFERENCE_CALIBRATION_DERIVATION_VERSION,
)
private val STEP_SEMANTIC_WORKOUT_TYPES = setOf(
    WorkoutType.WALKING,
    WorkoutType.RUNNING,
    WorkoutType.FREE_WORKOUT,
    WorkoutType.TREADMILL,
)
private val HEX = "0123456789abcdef".toCharArray()

private data class SourceRecord(
    val sample: WorkoutHeartRateSample,
    val sourceOrder: Int,
    val segmentId: Int = -1,
    val structurallyInvalid: Boolean = false,
) {
    val isValidBpm: Boolean
        get() = sample.bpm in MIN_SUPPORTED_BPM..MAX_SUPPORTED_BPM
}

private data class DerivedSupport(
    val instant: Instant,
    val value: Double,
    val segmentId: Int,
)

private data class CadenceSourceRecord(
    val interval: MeasuredWorkoutInterval,
    val sourceIndex: Int?,
    val sourceOrder: Int,
    val segmentId: Int = -1,
    val structurallyInvalid: Boolean = false,
) {
    val midpoint: Instant?
        get() {
            val window = interval.effectiveWindow ?: return null
            val duration = interval.effectiveDuration ?: return null
            return runCatching { window.start.plus(duration.dividedBy(2)) }.getOrNull()
        }

    val cadenceSpm: Double?
        get() {
            val steps = interval.decodedSteps ?: return null
            val duration = interval.effectiveDuration ?: return null
            if (steps < 0L || !duration.isPositive()) return null
            val durationSeconds = duration.seconds.toDouble() + duration.nano.toDouble() / 1_000_000_000.0
            if (!durationSeconds.isFinite() || durationSeconds <= 0.0) return null
            return (steps.toDouble() / durationSeconds * 60.0).takeIf { it.isFinite() && it >= 0.0 }
    }
}

private fun buildCadenceSourceSegments(
    intervals: List<MeasuredWorkoutInterval>,
    window: WorkoutIntervalWindow,
    fallbackGapSeconds: Int,
    cancellationCheck: (() -> Unit)?,
): List<List<CadenceSourceRecord>> {
    val inSession = intervals.mapIndexedNotNull { sourceOrder, interval ->
        cancellationCheck?.invoke()
        val intervalWindow = interval.effectiveWindow ?: return@mapIndexedNotNull null
        val intervalDuration = interval.effectiveDuration
        if (
            intervalWindow.start.isBefore(window.start) ||
            intervalWindow.end.isAfter(window.end)
        ) {
            return@mapIndexedNotNull null
        }
        CadenceSourceRecord(
            interval = interval,
            sourceIndex = interval.index.takeIf { it >= 0 },
            sourceOrder = sourceOrder,
            structurallyInvalid = intervalDuration == null ||
                !intervalDuration.isPositive() ||
                intervalDuration != intervalWindow.duration ||
                interval.decodedSteps == null ||
                interval.decodedSteps < 0L ||
                runCatching { intervalWindow.start.plus(intervalDuration.dividedBy(2)) }.getOrNull() == null,
        )
    }
    if (inSession.isEmpty()) return emptyList()

    val duplicateIndexes = inSession.mapNotNull { it.sourceIndex }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    val segments = mutableListOf<MutableList<CadenceSourceRecord>>()
    var current = mutableListOf<CadenceSourceRecord>()
    var previous: CadenceSourceRecord? = null
    inSession.forEach { originalRecord ->
        cancellationCheck?.invoke()
        val record = originalRecord.copy(
            structurallyInvalid = originalRecord.structurallyInvalid ||
                originalRecord.sourceIndex in duplicateIndexes,
        )
        val continuous = previous != null &&
            !previous.structurallyInvalid &&
            !record.structurallyInvalid &&
            isCadenceContinuous(previous, record, fallbackGapSeconds)
        when {
            record.structurallyInvalid -> {
                if (current.isNotEmpty()) segments += current
                current = mutableListOf()
            }
            previous != null && !continuous -> {
                if (current.isNotEmpty()) segments += current
                current = mutableListOf(record)
            }
            else -> current += record
        }
        previous = record
    }
    if (current.isNotEmpty()) segments += current
    return segments.mapIndexed { segmentId, segment ->
        cancellationCheck?.invoke()
        segment.map { it.copy(segmentId = segmentId) }
    }
}

private fun isCadenceContinuous(
    previous: CadenceSourceRecord,
    current: CadenceSourceRecord,
    fallbackGapSeconds: Int,
): Boolean {
    val previousMidpoint = previous.midpoint ?: return false
    val currentMidpoint = current.midpoint ?: return false
    if (!currentMidpoint.isAfter(previousMidpoint)) return false
    if (previous.interval.effectiveWindow?.end != current.interval.effectiveWindow?.start) return false
    return when {
        previous.sourceIndex != null || current.sourceIndex != null ->
            previous.sourceIndex != null && current.sourceIndex != null &&
                previous.sourceIndex < Int.MAX_VALUE &&
                current.sourceIndex == previous.sourceIndex + 1
        else -> secondsBetween(previousMidpoint, currentMidpoint) <= fallbackGapSeconds.toDouble()
    }
}

private fun Duration?.orZeroIsPositiveAndMatches(window: WorkoutIntervalWindow): Boolean =
    this != null && this.isPositive() && this == window.duration

private fun deriveCadenceSupports(
    segments: List<List<CadenceSourceRecord>>,
    parameters: Workout1HzCadenceParameterSet,
    cancellationCheck: (() -> Unit)?,
): List<DerivedSupport> {
    val supports = mutableListOf<DerivedSupport>()
    segments.forEach { segment ->
        cancellationCheck?.invoke()
        val outliers = segment.mapIndexed { index, candidate ->
            cancellationCheck?.invoke()
            index to isCadenceHampelOutlier(segment, index, candidate, parameters, cancellationCheck)
        }.toMap()
        var previousValue: Double? = null
        var previousInstant: Instant? = null
        segment.forEachIndexed { index, record ->
            cancellationCheck?.invoke()
            val currentValue = record.cadenceSpm ?: return@forEachIndexed
            val currentInstant = record.midpoint ?: return@forEachIndexed
            if (outliers[index] == true) return@forEachIndexed
            val value = if (previousValue == null || previousInstant == null) {
                currentValue
            } else {
                val deltaSeconds = secondsBetween(previousInstant, currentInstant)
                if (deltaSeconds <= 0.0) return@forEachIndexed
                val alpha = 1.0 - exp(-deltaSeconds / parameters.emaTauSeconds)
                alpha * currentValue + (1.0 - alpha) * previousValue
            }
            supports += DerivedSupport(currentInstant, value, record.segmentId)
            previousValue = value
            previousInstant = currentInstant
        }
    }
    return supports.sortedBy { it.instant }
}

private fun isCadenceHampelOutlier(
    segment: List<CadenceSourceRecord>,
    candidateIndex: Int,
    candidate: CadenceSourceRecord,
    parameters: Workout1HzCadenceParameterSet,
    cancellationCheck: (() -> Unit)?,
): Boolean {
    val candidateValue = candidate.cadenceSpm ?: return false
    if (candidate.structurallyInvalid) return false
    val validIndexes = segment.mapIndexedNotNull { index, record ->
        cancellationCheck?.invoke()
        if (!record.structurallyInvalid && record.cadenceSpm != null) index else null
    }
    val neighbors = validIndexes
        .filter { it < candidateIndex }
        .takeLast(parameters.hampelRadius)
        .mapNotNull {
            cancellationCheck?.invoke()
            segment[it].cadenceSpm
        } +
        validIndexes
            .filter { it > candidateIndex }
            .take(parameters.hampelRadius)
            .mapNotNull {
                cancellationCheck?.invoke()
                segment[it].cadenceSpm
            }
    if (neighbors.size < 2) return false
    val median = median(neighbors)
    val mad = median(neighbors.map { abs(it - median) })
    val threshold = maxOf(
        parameters.hampelFloorSpm.toDouble(),
        parameters.hampelScale * parameters.hampelMultiplier * mad,
    )
    return abs(candidateValue - median) > threshold
}

private fun buildSourceSegments(
    samples: List<WorkoutHeartRateSample>,
    window: WorkoutIntervalWindow,
    fallbackGapSeconds: Int,
    cancellationCheck: (() -> Unit)?,
): List<List<SourceRecord>> {
    val inSession = samples
        .filter {
            cancellationCheck?.invoke()
            window.contains(it.sampledAt)
        }
        .mapIndexed { index, sample -> SourceRecord(sample = sample, sourceOrder = index) }
        .sortedWith(compareBy<SourceRecord> { it.sample.sampledAt }.thenBy { it.sourceOrder })
    if (inSession.isEmpty()) return emptyList()

    val duplicateIndexes = inSession.mapNotNull { it.sample.sourceSampleIndex }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    val segments = mutableListOf<MutableList<SourceRecord>>()
    var current = mutableListOf<SourceRecord>()
    inSession.forEachIndexed { index, record ->
        cancellationCheck?.invoke()
        val previous = inSession.getOrNull(index - 1)
        val recordDuplicate = record.sample.sourceSampleIndex in duplicateIndexes
        val previousDuplicate = previous?.sample?.sourceSampleIndex in duplicateIndexes
        val recordInvalidIndex = record.sample.sourceSampleIndex?.let { it < 0 } == true
        val previousInvalidIndex = previous?.sample?.sourceSampleIndex?.let { it < 0 } == true
        val duplicateMarker = recordDuplicate || previousDuplicate
        val invalidIndex = recordInvalidIndex || previousInvalidIndex
        val invalidStructure = duplicateMarker || invalidIndex || previous?.let {
            record.sourceOrder != it.sourceOrder + 1
        } == true
        val continuous = previous != null && !invalidStructure &&
            isContinuous(previous.sample, record.sample, fallbackGapSeconds)
        if (previous != null && !continuous) {
            if (current.isNotEmpty()) segments += current
            current = mutableListOf()
        }
        current += record.copy(structurallyInvalid = recordDuplicate || recordInvalidIndex)
    }
    if (current.isNotEmpty()) segments += current
    return segments.mapIndexed { segmentId, segment ->
        cancellationCheck?.invoke()
        segment.map { it.copy(segmentId = segmentId) }
    }
}

private fun isContinuous(
    previous: WorkoutHeartRateSample,
    current: WorkoutHeartRateSample,
    fallbackGapSeconds: Int,
): Boolean {
    if (!current.sampledAt.isAfter(previous.sampledAt)) return false
    if (previous.sourceSampleIndex?.let { it < 0 } == true || current.sourceSampleIndex?.let { it < 0 } == true) {
        return false
    }
    return when {
        previous.sourceSampleIndex != null || current.sourceSampleIndex != null ->
            previous.sourceSampleIndex != null && current.sourceSampleIndex != null &&
                previous.sourceSampleIndex < Int.MAX_VALUE &&
                current.sourceSampleIndex == previous.sourceSampleIndex + 1
        else -> secondsBetween(previous.sampledAt, current.sampledAt) <= fallbackGapSeconds.toDouble()
    }
}

private fun deriveSupports(
    segments: List<List<SourceRecord>>,
    parameters: Workout1HzBpmParameterSet,
    cancellationCheck: (() -> Unit)?,
): List<DerivedSupport> {
    val supports = mutableListOf<DerivedSupport>()
    segments.forEach { segment ->
        cancellationCheck?.invoke()
        val outliers = segment.mapIndexed { index, candidate ->
            cancellationCheck?.invoke()
            index to isHampelOutlier(segment, index, candidate, parameters, cancellationCheck)
        }.toMap()
        var previousValue: Double? = null
        var previousInstant: Instant? = null
        segment.forEachIndexed { index, record ->
            cancellationCheck?.invoke()
            if (record.structurallyInvalid || !record.isValidBpm || outliers[index] == true) return@forEachIndexed
            val currentValue = record.sample.bpm.toDouble()
            val value = if (previousValue == null || previousInstant == null) {
                currentValue
            } else {
                val deltaSeconds = secondsBetween(previousInstant, record.sample.sampledAt)
                if (deltaSeconds <= 0.0) return@forEachIndexed
                val alpha = 1.0 - exp(-deltaSeconds / parameters.emaTauSeconds)
                alpha * currentValue + (1.0 - alpha) * previousValue
            }
            supports += DerivedSupport(record.sample.sampledAt, value, record.segmentId)
            previousValue = value
            previousInstant = record.sample.sampledAt
        }
    }
    return supports.sortedBy { it.instant }
}

private fun isHampelOutlier(
    segment: List<SourceRecord>,
    candidateIndex: Int,
    candidate: SourceRecord,
    parameters: Workout1HzBpmParameterSet,
    cancellationCheck: (() -> Unit)?,
): Boolean {
    if (candidate.structurallyInvalid || !candidate.isValidBpm) return false
    val validIndexes = segment.mapIndexedNotNull { index, record ->
        cancellationCheck?.invoke()
        if (!record.structurallyInvalid && record.isValidBpm) index else null
    }
    val neighbors = validIndexes
        .filter { it < candidateIndex }
        .takeLast(parameters.hampelRadius)
        .map {
            cancellationCheck?.invoke()
            segment[it].sample.bpm.toDouble()
        } +
        validIndexes
            .filter { it > candidateIndex }
            .take(parameters.hampelRadius)
            .map {
                cancellationCheck?.invoke()
                segment[it].sample.bpm.toDouble()
            }
    if (neighbors.size < 2) return false
    val median = median(neighbors)
    val mad = median(neighbors.map { abs(it - median) })
    val threshold = maxOf(
        parameters.hampelFloorBpm.toDouble(),
        parameters.hampelScale * parameters.hampelMultiplier * mad,
    )
    return abs(candidate.sample.bpm.toDouble() - median) > threshold
}

private fun median(values: List<Double>): Double {
    val ordered = values.sorted()
    val middle = ordered.size / 2
    return if (ordered.size % 2 == 1) {
        ordered[middle]
    } else {
        (ordered[middle - 1] + ordered[middle]) / 2.0
    }
}

private fun bucketCount(window: WorkoutIntervalWindow): Long? {
    val duration = window.duration
    val count = duration.seconds + if (duration.nano > 0) 1L else 0L
    return count.takeIf { it > 0L && it <= Int.MAX_VALUE.toLong() }
}

private fun secondsBetween(start: Instant, end: Instant): Double =
    Duration.between(start, end).seconds.toDouble() +
        Duration.between(start, end).nano.toDouble() / 1_000_000_000.0

private fun minInstant(first: Instant, second: Instant): Instant = if (first.isBefore(second)) first else second

private fun canonicalDecimal(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

private fun compareAsciiBytes(left: String, right: String): Int =
    left.toByteArray(StandardCharsets.US_ASCII).let { leftBytes ->
        right.toByteArray(StandardCharsets.US_ASCII).let { rightBytes ->
            val commonLength = minOf(leftBytes.size, rightBytes.size)
            for (index in 0 until commonLength) {
                val comparison = leftBytes[index].toInt().compareTo(rightBytes[index].toInt())
                if (comparison != 0) return comparison
            }
            leftBytes.size.compareTo(rightBytes.size)
        }
    }
