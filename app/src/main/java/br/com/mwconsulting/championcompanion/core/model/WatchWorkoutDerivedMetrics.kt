package br.com.mwconsulting.championcompanion.core.model

import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val U16_MASK = 0xFFFFL

/** Versão da derivação normalizada de série canônica de treinos do relógio. */
const val WATCH_WORKOUT_DERIVATION_VERSION = "watch-d3-d4-v2"

/** Versão usada pela derivação contextual e pela calibração externa da atividade. */
const val WATCH_WORKOUT_REFERENCE_CALIBRATION_DERIVATION_VERSION = "watch-d3-d4-v3"

/** Versão da derivação que usa a série nativa D4 v4 como fonte canônica. */
const val WATCH_WORKOUT_NATIVE_DERIVATION_VERSION = "watch-d3-d4-v4"

/** Versão legada preservada somente para leitura de snapshots já persistidos. */
internal const val WATCH_WORKOUT_LEGACY_DERIVATION_VERSION = "watch-d3-d4-v1"

/** Origem explícita do comprimento de um passo individual. */
enum class StepLengthSource {
    MANUAL,
    HEIGHT_SUGGESTION_ACCEPTED,
    REFERENCE_DISTANCE_CALIBRATION,
    CONTEXTUAL_LEARNED_PROFILE,
}

/** Estado seguro da leitura canônica para consumidores locais. */
enum class WorkoutCanonicalReadStatus {
    NOT_APPLICABLE,
    MISSING_STEP_LENGTH,
    NOT_MATERIALIZED,
    SERIES_INELIGIBLE,
    PLAUSIBILITY_BLOCKED,
    SPLITS_NOT_MATERIALIZED,
    AVAILABLE,
}

/**
 * Valor pessoal imutável usado como comprimento de um passo individual.
 *
 * O valor não é default global e não pode ser confundido com distância de uma
 * passada histórica ou com uma distância agregada.
 */
@ConsistentCopyVisibility
data class StepLengthProfile private constructor(
    val stepLengthMeters: Double,
    val source: StepLengthSource,
    val updatedAt: Instant,
) {
    val recordedAt: Instant
        get() = updatedAt

    companion object {
        fun create(
            stepLengthMeters: Double,
            source: StepLengthSource,
            updatedAt: Instant,
        ): DomainResult<StepLengthProfile> = if (
            stepLengthMeters.isFinite() && stepLengthMeters > 0.0
        ) {
            DomainResult.Success(StepLengthProfile(stepLengthMeters, source, updatedAt))
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
    }
}

/** Sugestão inicial de comprimento individual derivada somente da altura válida. */
object StepLengthSuggestionPolicy {
    private const val HEIGHT_TO_STEP_LENGTH_FACTOR = 0.415

    fun fromHeight(heightCentimeters: Double?): Double? = heightCentimeters
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?.let { (it / 100.0) * HEIGHT_TO_STEP_LENGTH_FACTOR }
        ?.takeIf { it.isFinite() && it > 0.0 }
}

/** Adapters de nomenclatura para consumidores que ainda usam perfil pessoal. */
typealias PersonalStepLength = StepLengthProfile
typealias StepLengthSnapshot = StepLengthProfile

/**
 * Única política semântica para resolver o perfil de passo de um treino.
 *
 * A ausência de perfil aprendido sempre retorna ao perfil base. Bicicleta não
 * possui resolução por passos; resumo diário não chama esta política e mantém
 * [DailySummaryPolicy].
 */
object StepLengthResolutionPolicy {
    fun resolve(
        workoutType: WorkoutType,
        baseProfile: StepLengthProfile?,
        learnedProfiles: Iterable<LearnedStepLengthProfile> = emptyList(),
    ): StepLengthProfile? {
        if (workoutType == WorkoutType.CYCLING) return null

        val learned = when (workoutType) {
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            -> learnedProfiles.firstOrNull { it.workoutType == workoutType }

            WorkoutType.TREADMILL,
            WorkoutType.FREE_WORKOUT,
            WorkoutType.UNKNOWN,
            WorkoutType.CYCLING,
            -> null
        }
        return learned?.let {
            StepLengthProfile.create(
                stepLengthMeters = it.stepLengthMeters,
                source = StepLengthSource.CONTEXTUAL_LEARNED_PROFILE,
                updatedAt = it.updatedAt,
            ).valueOrNull()
        } ?: baseProfile
    }
}

/** Janela temporal semiaberta [start, end). */
@ConsistentCopyVisibility
data class WorkoutIntervalWindow private constructor(
    val start: Instant,
    val end: Instant,
) {
    val duration: Duration
        get() = Duration.between(start, end)

    fun contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)

    companion object {
        fun create(start: Instant, end: Instant): DomainResult<WorkoutIntervalWindow> =
            if (end.isAfter(start)) {
                DomainResult.Success(WorkoutIntervalWindow(start, end))
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            }
    }
}

/**
 * Fonte medida de intervalo, discriminada pelo contrato do parser.
 *
 * O ramo legacy mantém o packing u24 que já existia. O ramo v4 mantém cada
 * fato nativo em seu próprio campo; não existe conversão artificial para
 * [rawMotionValue].
 */
sealed interface MeasuredWorkoutIntervalSource {
    val parserVersion: String
    val measuredSteps: Long?

    data class LegacyPacked(
        override val parserVersion: String,
        val rawMotionValue: Long,
        val decodedSteps: Long?,
        val rawTemporalDistance: Long?,
        val rawTemporalCalories: Long?,
    ) : MeasuredWorkoutIntervalSource {
        override val measuredSteps: Long?
            get() = decodedSteps
    }

    data class D4V4Native(
        val sportValue: Long,
        override val measuredSteps: Long,
        val distanceMeters: Double,
        val caloriesRaw: Long,
        val pauseStateRaw: Long,
    ) : MeasuredWorkoutIntervalSource {
        override val parserVersion: String = WATCH_WORKOUT_D4_V4_PARSER_VERSION
    }
}

/** Fatos D4 crus, sem promover unidade ou semântica não comprovada. */
@ConsistentCopyVisibility
data class RawWorkoutInterval private constructor(
    val index: Int,
    val rawMotionValue: Long,
    val rawTemporalDistance: Long?,
    val rawTemporalCalories: Long?,
) {
    val unknownLower16: Int
        get() = (rawMotionValue and U16_MASK).toInt()

    val lower16BitsUnknown: Int
        get() = unknownLower16

    val rawTemporalDistanceValue: Long?
        get() = rawTemporalDistance

    val rawTemporalCaloriesValue: Long?
        get() = rawTemporalCalories

    companion object {
        fun create(
            index: Int,
            rawMotionValue: Long,
            rawTemporalDistance: Long? = null,
            rawTemporalCalories: Long? = null,
        ): DomainResult<RawWorkoutInterval> = if (
            isValidRawInterval(index, rawMotionValue, rawTemporalDistance, rawTemporalCalories)
        ) {
            DomainResult.Success(
                RawWorkoutInterval(index, rawMotionValue, rawTemporalDistance, rawTemporalCalories),
            )
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }

        internal fun fromValidated(
            index: Int,
            rawMotionValue: Long,
            rawTemporalDistance: Long?,
            rawTemporalCalories: Long?,
        ): RawWorkoutInterval = RawWorkoutInterval(
            index = index,
            rawMotionValue = rawMotionValue,
            rawTemporalDistance = rawTemporalDistance,
            rawTemporalCalories = rawTemporalCalories,
        )
    }
}

/**
 * Fato medido de um intervalo. [decodedSteps] só existe quando decodificação
 * foi confirmada; ausência é diferente de zero.
 */
@ConsistentCopyVisibility
data class MeasuredWorkoutInterval private constructor(
    val index: Int,
    val source: MeasuredWorkoutIntervalSource,
    val effectiveDuration: Duration?,
    val effectiveWindow: WorkoutIntervalWindow?,
) {
    /** Compatibilidade de leitura para consumidores do contrato legacy. */
    val rawMotionValue: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawMotionValue

    /** Passos medidos, independentemente de packing legacy ou layout nativo. */
    val measuredSteps: Long?
        get() = source.measuredSteps

    /** Alias histórico; v4 não possui decodedSteps legacy. */
    val decodedSteps: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.decodedSteps

    val rawTemporalDistance: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawTemporalDistance

    val rawTemporalCalories: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawTemporalCalories

    val nativeSportValue: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.D4V4Native)?.sportValue

    val nativeDistanceMeters: Double?
        get() = (source as? MeasuredWorkoutIntervalSource.D4V4Native)?.distanceMeters

    val nativeCaloriesRaw: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.D4V4Native)?.caloriesRaw

    val nativePauseStateRaw: Long?
        get() = (source as? MeasuredWorkoutIntervalSource.D4V4Native)?.pauseStateRaw

    val raw: RawWorkoutInterval?
        get() = rawMotionValue?.let { rawMotion -> RawWorkoutInterval.fromValidated(
            index = index,
            rawMotionValue = rawMotion,
            rawTemporalDistance = rawTemporalDistance,
            rawTemporalCalories = rawTemporalCalories,
        ) }

    val unknownLower16: Int?
        get() = rawMotionValue?.let { (it and U16_MASK).toInt() }

    val lower16BitsUnknown: Int?
        get() = unknownLower16

    val rawTemporalDistanceValue: Long?
        get() = rawTemporalDistance

    val rawTemporalCaloriesValue: Long?
        get() = rawTemporalCalories

    val decodedStepsConfirmed: Boolean
        get() = measuredSteps != null

    val isTemporallyResolved: Boolean
        get() = effectiveDuration != null && effectiveWindow != null

    /** Alias explícito para APIs que chamam janela efetiva apenas de window. */
    val window: WorkoutIntervalWindow?
        get() = effectiveWindow

    fun withEffectiveWindow(window: WorkoutIntervalWindow): DomainResult<MeasuredWorkoutInterval> =
        create(
            index = index,
            source = source,
            effectiveDuration = window.duration,
            effectiveWindow = window,
        )

    companion object {
        const val MAX_RAW_MOTION_VALUE: Long = 0xFF_FFFFL
        private const val MAX_NATIVE_U16: Long = 0xFFFFL
        private const val MAX_NATIVE_PAUSE: Long = 0xFFL

        fun create(
            index: Int,
            rawMotionValue: Long,
            decodedSteps: Long? = null,
            rawTemporalDistance: Long? = null,
            rawTemporalCalories: Long? = null,
            effectiveDuration: Duration? = null,
            effectiveWindow: WorkoutIntervalWindow? = null,
        ): DomainResult<MeasuredWorkoutInterval> {
            val source = MeasuredWorkoutIntervalSource.LegacyPacked(
                parserVersion = WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                rawMotionValue = rawMotionValue,
                decodedSteps = decodedSteps,
                rawTemporalDistance = rawTemporalDistance,
                rawTemporalCalories = rawTemporalCalories,
            )
            return create(
                index = index,
                source = source,
                effectiveDuration = effectiveDuration,
                effectiveWindow = effectiveWindow,
            )
        }

        fun fromD4V2(
            index: Int,
            rawMotionValue: Long,
            decodedSteps: Long? = null,
            rawTemporalDistance: Long? = null,
            rawTemporalCalories: Long? = null,
        ): DomainResult<MeasuredWorkoutInterval> = create(
            index = index,
            source = MeasuredWorkoutIntervalSource.LegacyPacked(
                parserVersion = WATCH_WORKOUT_D4_V2_PARSER_VERSION,
                rawMotionValue = rawMotionValue,
                decodedSteps = decodedSteps ?: (rawMotionValue ushr 16),
                rawTemporalDistance = rawTemporalDistance,
                rawTemporalCalories = rawTemporalCalories,
            ),
        )

        fun fromD4V3(
            index: Int,
            rawMotionValue: Long,
            decodedSteps: Long,
            rawTemporalDistance: Long? = null,
            rawTemporalCalories: Long? = null,
        ): DomainResult<MeasuredWorkoutInterval> = create(
            index = index,
            source = MeasuredWorkoutIntervalSource.LegacyPacked(
                parserVersion = WATCH_WORKOUT_D4_V3_PARSER_VERSION,
                rawMotionValue = rawMotionValue,
                decodedSteps = decodedSteps,
                rawTemporalDistance = rawTemporalDistance,
                rawTemporalCalories = rawTemporalCalories,
            ),
        )

        fun fromD4V4(
            index: Int,
            sportValue: Long,
            nativeSteps: Long,
            distanceMeters: Double,
            caloriesRaw: Long,
            pauseStateRaw: Long,
        ): DomainResult<MeasuredWorkoutInterval> = create(
            index = index,
            source = MeasuredWorkoutIntervalSource.D4V4Native(
                sportValue = sportValue,
                measuredSteps = nativeSteps,
                distanceMeters = distanceMeters,
                caloriesRaw = caloriesRaw,
                pauseStateRaw = pauseStateRaw,
            ),
        )

        private fun create(
            index: Int,
            source: MeasuredWorkoutIntervalSource,
            effectiveDuration: Duration? = null,
            effectiveWindow: WorkoutIntervalWindow? = null,
        ): DomainResult<MeasuredWorkoutInterval> {
            val rawMotionValue = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawMotionValue
            val decodedSteps = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.decodedSteps
            val rawTemporalDistance = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawTemporalDistance
            val rawTemporalCalories = (source as? MeasuredWorkoutIntervalSource.LegacyPacked)?.rawTemporalCalories
            val expectedDecodedSteps = rawMotionValue?.ushr(16)
            val validDecodedSteps = decodedSteps == null ||
                (decodedSteps >= 0L && decodedSteps == expectedDecodedSteps)
            val validTemporalValues = rawTemporalDistance.isValidRawDistance() &&
                rawTemporalCalories.isValidRawCalories()
            val native = source as? MeasuredWorkoutIntervalSource.D4V4Native
            val validNativeValues = native == null || (
                native.sportValue in 0L..MAX_NATIVE_U16 &&
                    native.measuredSteps in 0L..MAX_NATIVE_U16 &&
                    native.distanceMeters.isFinite() &&
                    native.distanceMeters in 0.0..MAX_NATIVE_U16.toDouble() &&
                    native.distanceMeters == native.distanceMeters.toLong().toDouble() &&
                    native.caloriesRaw in 0L..MAX_NATIVE_U16 &&
                    native.pauseStateRaw in 0L..MAX_NATIVE_PAUSE
            )
            val validEffectiveTime = when {
                effectiveDuration == null && effectiveWindow == null -> true
                effectiveDuration == null || effectiveWindow == null -> false
                else -> effectiveDuration.isPositive() && effectiveWindow.duration == effectiveDuration
            }
            return if (
                index >= 0 &&
                (rawMotionValue == null || rawMotionValue in 0L..MAX_RAW_MOTION_VALUE) &&
                validDecodedSteps &&
                validTemporalValues &&
                validNativeValues &&
                validEffectiveTime
            ) {
                DomainResult.Success(
                    MeasuredWorkoutInterval(
                        index = index,
                        source = source,
                        effectiveDuration = effectiveDuration,
                        effectiveWindow = effectiveWindow,
                    ),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
        }

        /** Ponte d4-v1: legado vira raw e permanece sem passos decodificados. */
        fun fromD4V1Legacy(
            index: Int,
            legacySteps: Long,
            decodedSteps: Long? = null,
        ): DomainResult<MeasuredWorkoutInterval> = create(
            index = index,
            source = MeasuredWorkoutIntervalSource.LegacyPacked(
                parserVersion = WATCH_WORKOUT_D4_V1_PARSER_VERSION,
                rawMotionValue = legacySteps,
                decodedSteps = decodedSteps,
                rawTemporalDistance = null,
                rawTemporalCalories = null,
            ),
        )
    }
}

/**
 * Compatibilidade controlada para consumidores ainda presos ao intervalo
 * legado. A ponte não decodifica d4-v1 nem promove distância/calorias antigas.
 */
object WatchWorkoutContractAdapters {
    fun fromInterval(
        interval: WorkoutInterval,
        parserVersion: ParserVersion,
    ): DomainResult<MeasuredWorkoutInterval> = when (parserVersion.value) {
        WATCH_WORKOUT_D4_V1_PARSER_VERSION -> fromD4V1(interval)
        WATCH_WORKOUT_D4_V2_PARSER_VERSION -> fromD4V2(interval)
        WATCH_WORKOUT_D4_V3_PARSER_VERSION -> fromD4V3(interval)
        WATCH_WORKOUT_D4_V4_PARSER_VERSION -> fromD4V4(interval)
        else -> DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
    }

    fun fromD4V1(interval: WorkoutInterval): DomainResult<MeasuredWorkoutInterval> =
        MeasuredWorkoutInterval.fromD4V1Legacy(
            index = interval.index,
            legacySteps = interval.rawMotionValue ?: interval.steps,
            decodedSteps = interval.decodedSteps,
        )

    fun fromD4V2(interval: WorkoutInterval): DomainResult<MeasuredWorkoutInterval> {
        val rawMotionValue = interval.rawMotionValue
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        if (interval.decodedSteps == null) return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        return MeasuredWorkoutInterval.fromD4V2(
            index = interval.index,
            rawMotionValue = rawMotionValue,
            decodedSteps = interval.decodedSteps,
            rawTemporalDistance = interval.rawDistanceValue,
            rawTemporalCalories = interval.rawCaloriesValue,
        )
    }

    fun fromD4V3(interval: WorkoutInterval): DomainResult<MeasuredWorkoutInterval> {
        val rawMotionValue = interval.rawMotionValue
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        val decodedSteps = interval.decodedSteps
            ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        return MeasuredWorkoutInterval.fromD4V3(
            index = interval.index,
            rawMotionValue = rawMotionValue,
            decodedSteps = decodedSteps,
            rawTemporalDistance = interval.rawDistanceValue,
            rawTemporalCalories = interval.rawCaloriesValue,
        )
    }

    fun fromD4V4(interval: WorkoutInterval): DomainResult<MeasuredWorkoutInterval> {
        val sportValue = interval.nativeSportValue
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        val nativeSteps = interval.nativeSteps
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        val distanceMeters = interval.nativeDistanceMeters
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        val caloriesRaw = interval.nativeCaloriesRaw
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        val pauseStateRaw = interval.nativePauseStateRaw
            ?: return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        if (interval.rawMotionValue != null || interval.rawDistanceValue != null ||
            interval.rawCaloriesValue != null || interval.decodedSteps != null ||
            interval.steps != nativeSteps
        ) return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        return MeasuredWorkoutInterval.fromD4V4(
            index = interval.index,
            sportValue = sportValue,
            nativeSteps = nativeSteps,
            distanceMeters = distanceMeters,
            caloriesRaw = caloriesRaw,
            pauseStateRaw = pauseStateRaw,
        )
    }

    fun fromLegacyInterval(
        interval: WorkoutInterval,
        activityOrigin: WorkoutActivityOrigin,
        parserVersion: ParserVersion,
    ): DomainResult<MeasuredWorkoutInterval> = if (
        activityOrigin == WorkoutActivityOrigin.WATCH_D3_D4 &&
        parserVersion.value == LEGACY_D4_PARSER_VERSION
    ) {
        MeasuredWorkoutInterval.fromD4V1Legacy(interval.index, interval.steps)
    } else {
        DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
    }

    fun toLegacyInterval(interval: MeasuredWorkoutInterval): DomainResult<WorkoutInterval> {
        val duration = interval.effectiveDuration ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        val legacy = interval.source as? MeasuredWorkoutIntervalSource.LegacyPacked
            ?: return DomainResult.Failure(DomainValidationError.INVALID_IDENTIFIER)
        val legacySteps = interval.decodedSteps ?: legacy.rawMotionValue
        return DomainResult.Success(
            WorkoutInterval(
                index = interval.index,
                duration = duration,
                steps = legacySteps,
                distanceMeters = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
                caloriesKilocalories = DomainValue.Absent(AbsenceReason.NOT_PROVIDED_BY_SOURCE),
            ),
        )
    }

    private const val LEGACY_D4_PARSER_VERSION = "d4-v1"
}

enum class TemporalSeriesEligibility {
    ELIGIBLE,
    INELIGIBLE,
}

/**
 * Motivos independentes de uma série não ser elegível. UNIQUE significa que a
 * restrição de unicidade foi violada, normalmente por índice duplicado.
 */
enum class TemporalSeriesValidationReason {
    UNIQUE,
    DUPLICATE_INDEX,
    NON_CONTIGUOUS,
    INDEX_GAP,
    FIRST_INDEX_NOT_ZERO,
    OUT_OF_ORDER,
    EMPTY,
    INVALID,
    INSUFFICIENT_TOTAL_DURATION,
    MISSING_DECODED_STEPS,
}

/** Marcador persistido de que origem temporal foi validada antes da ordenação Room. */
const val TEMPORAL_ORDER_VALIDATED_MARKER = "ORDER_VALIDATED"

typealias TemporalSeriesInvalidReason = TemporalSeriesValidationReason
typealias TemporalSeriesEligibilityReason = TemporalSeriesValidationReason

data class TemporalSeriesValidationResult(
    val eligibility: TemporalSeriesEligibility,
    val reasons: Set<TemporalSeriesValidationReason> = emptySet(),
) {
    val isEligible: Boolean
        get() = eligibility == TemporalSeriesEligibility.ELIGIBLE && reasons.isEmpty()
}

object TemporalSeriesValidator {
    fun validate(intervals: List<MeasuredWorkoutInterval>): TemporalSeriesValidationResult =
        validateIndices(intervals.map(MeasuredWorkoutInterval::index))

    fun validateIndices(indices: List<Int>): TemporalSeriesValidationResult {
        if (indices.isEmpty()) {
            return TemporalSeriesValidationResult(
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = setOf(TemporalSeriesValidationReason.EMPTY),
            )
        }

        val reasons = linkedSetOf<TemporalSeriesValidationReason>()
        if (indices.any { it < 0 }) reasons += TemporalSeriesValidationReason.INVALID
        if (indices.first() != 0) reasons += TemporalSeriesValidationReason.FIRST_INDEX_NOT_ZERO
        if (indices.zipWithNext().any { (first, second) -> second <= first }) {
            reasons += TemporalSeriesValidationReason.OUT_OF_ORDER
        }
        if (indices.toSet().size != indices.size) {
            reasons += TemporalSeriesValidationReason.UNIQUE
            reasons += TemporalSeriesValidationReason.DUPLICATE_INDEX
        }
        if (indices.sorted() != indices.indices.toList()) {
            reasons += TemporalSeriesValidationReason.NON_CONTIGUOUS
            reasons += TemporalSeriesValidationReason.INDEX_GAP
        }
        return TemporalSeriesValidationResult(
            eligibility = if (reasons.isEmpty()) {
                TemporalSeriesEligibility.ELIGIBLE
            } else {
                TemporalSeriesEligibility.INELIGIBLE
            },
            reasons = reasons.toSet(),
        )
    }
}

enum class WorkoutDurationCase {
    NORMALIZED,
    EXACT,
    LAST_INTERVAL_RESIDUAL,
    INSUFFICIENT_TOTAL_DURATION,
    EXCESS_TOTAL_DURATION,
}

data class WorkoutDurationResolution(
    val durationCase: WorkoutDurationCase,
    val totalDuration: Duration,
    val nominalIntervalDuration: Duration,
    val intervals: List<MeasuredWorkoutInterval>,
    val tailUncovered: Duration = Duration.ZERO,
    val eligibility: TemporalSeriesEligibility,
    val reasons: Set<TemporalSeriesValidationReason> = emptySet(),
) {
    val case: WorkoutDurationCase
        get() = durationCase

    val hasTailUncovered: Boolean
        get() = !tailUncovered.isZero

    val isEligible: Boolean
        get() = eligibility == TemporalSeriesEligibility.ELIGIBLE && reasons.isEmpty()
}

object WorkoutDurationResolver {
    val DEFAULT_INTERVAL_DURATION: Duration = Duration.ofSeconds(60)

    /**
     * Distribui duração oficial inteira entre todos os samples elegíveis. O
     * quantum nominal continua somente como proveniência no snapshot.
     */
    fun resolve(
        totalDuration: Duration,
        intervals: List<MeasuredWorkoutInterval>,
        sessionStartedAt: Instant,
        nominalIntervalDuration: Duration = DEFAULT_INTERVAL_DURATION,
    ): WorkoutDurationResolution {
        val indexValidation = TemporalSeriesValidator.validate(intervals)
        val reasons = linkedSetOf<TemporalSeriesValidationReason>().apply {
            addAll(indexValidation.reasons)
            if (intervals.any { it.measuredSteps == null }) {
                add(TemporalSeriesValidationReason.MISSING_DECODED_STEPS)
            }
        }
        val validNominalDuration = nominalIntervalDuration.isPositive()
        if (!validNominalDuration) reasons += TemporalSeriesValidationReason.INVALID

        // Índices inválidos não têm posição temporal confiável. Preserve fatos
        // measured sem fabricar janelas pela posição da lista.
        if (!indexValidation.isEligible || intervals.isEmpty()) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons.toSet(),
            )
        }

        if (!totalDuration.isPositive()) {
            reasons += TemporalSeriesValidationReason.INVALID
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons + TemporalSeriesValidationReason.EMPTY,
            )
        }
        if (!validNominalDuration) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons,
            )
        }

        val totalDurationNanos = runCatching { totalDuration.toNanos() }.getOrNull()
        if (totalDurationNanos == null || totalDurationNanos <= 0L) {
            reasons += TemporalSeriesValidationReason.INVALID
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons + TemporalSeriesValidationReason.INVALID,
            )
        }

        val intervalCount = intervals.size.toLong()
        val baseDurationNanos = totalDurationNanos / intervalCount
        val remainderNanos = totalDurationNanos % intervalCount
        if (baseDurationNanos <= 0L) {
            reasons += TemporalSeriesValidationReason.INSUFFICIENT_TOTAL_DURATION
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons,
            )
        }

        val durations = intervals.indices.map { index ->
            val nanos = runCatching {
                Math.addExact(baseDurationNanos, if (index.toLong() < remainderNanos) 1L else 0L)
            }.getOrNull() ?: return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons + TemporalSeriesValidationReason.INVALID,
            )
            Duration.ofNanos(nanos)
        }
        val resolvedIntervals = mutableListOf<MeasuredWorkoutInterval>()
        var currentStart = sessionStartedAt
        var windowsValid = true
        durations.forEachIndexed { index, duration ->
            val windowResult = runCatching {
                WorkoutIntervalWindow.create(currentStart, currentStart.plus(duration))
            }.getOrNull()?.valueOrNull()
            if (windowResult == null) {
                windowsValid = false
                return@forEachIndexed
            }
            val resolved = intervals[index].withEffectiveWindow(windowResult).valueOrNull()
            if (resolved == null) {
                windowsValid = false
            } else {
                resolvedIntervals += resolved
            }
            currentStart = windowResult.end
        }
        val expectedEnd = runCatching { sessionStartedAt.plus(totalDuration) }.getOrNull()
        if (!windowsValid || resolvedIntervals.size != intervals.size || expectedEnd == null || currentStart != expectedEnd) {
            reasons += TemporalSeriesValidationReason.INVALID
        }
        val eligible = reasons.isEmpty() && windowsValid
        return WorkoutDurationResolution(
            durationCase = WorkoutDurationCase.NORMALIZED,
            totalDuration = totalDuration,
            nominalIntervalDuration = nominalIntervalDuration,
            intervals = if (windowsValid) resolvedIntervals.toList() else intervals.toList(),
            tailUncovered = Duration.ZERO,
            eligibility = if (eligible) TemporalSeriesEligibility.ELIGIBLE else TemporalSeriesEligibility.INELIGIBLE,
            reasons = reasons.toSet(),
        )
    }

    /** Política temporal da Fase 014, usada somente para ler snapshots antigos. */
    internal fun resolveLegacy(
        totalDuration: Duration,
        intervals: List<MeasuredWorkoutInterval>,
        sessionStartedAt: Instant,
        nominalIntervalDuration: Duration = DEFAULT_INTERVAL_DURATION,
    ): WorkoutDurationResolution {
        val indexValidation = TemporalSeriesValidator.validate(intervals)
        val reasons = linkedSetOf<TemporalSeriesValidationReason>().apply {
            addAll(indexValidation.reasons)
            if (intervals.any { it.measuredSteps == null }) {
                add(TemporalSeriesValidationReason.MISSING_DECODED_STEPS)
            }
        }
        val validNominalDuration = nominalIntervalDuration.isPositive()
        if (!validNominalDuration) reasons += TemporalSeriesValidationReason.INVALID

        // Índices inválidos não têm posição temporal confiável. Preserve fatos
        // measured sem fabricar janelas pela posição da lista.
        if (!indexValidation.isEligible) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons.toSet(),
            )
        }

        if (intervals.isEmpty()) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons + TemporalSeriesValidationReason.EMPTY,
            )
        }
        if (totalDuration.isNegative || totalDuration.isZero) {
            reasons += TemporalSeriesValidationReason.INVALID
        }
        if (!validNominalDuration) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons,
            )
        }

        val intervalCount = intervals.size.toLong()
        val allDuration = nominalIntervalDuration.safeMultipliedBy(intervalCount)
        val beforeLastDuration = nominalIntervalDuration.safeMultipliedBy(intervalCount - 1L)
        if (allDuration == null || beforeLastDuration == null) {
            return WorkoutDurationResolution(
                durationCase = WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons + TemporalSeriesValidationReason.INVALID,
            )
        }

        val durationCase = when {
            totalDuration == allDuration -> WorkoutDurationCase.EXACT
            totalDuration > allDuration -> WorkoutDurationCase.EXCESS_TOTAL_DURATION
            totalDuration > beforeLastDuration -> WorkoutDurationCase.LAST_INTERVAL_RESIDUAL
            else -> WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION
        }
        if (durationCase == WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION) {
            reasons += TemporalSeriesValidationReason.INSUFFICIENT_TOTAL_DURATION
            return WorkoutDurationResolution(
                durationCase = durationCase,
                totalDuration = totalDuration,
                nominalIntervalDuration = nominalIntervalDuration,
                intervals = intervals.toList(),
                eligibility = TemporalSeriesEligibility.INELIGIBLE,
                reasons = reasons,
            )
        }

        val durations = when (durationCase) {
            WorkoutDurationCase.EXACT,
            WorkoutDurationCase.EXCESS_TOTAL_DURATION,
            -> List(intervals.size) { nominalIntervalDuration }

            WorkoutDurationCase.LAST_INTERVAL_RESIDUAL -> List(intervals.size) { index ->
                if (index < intervals.lastIndex) nominalIntervalDuration
                else totalDuration.minus(beforeLastDuration)
            }

            WorkoutDurationCase.NORMALIZED,
            WorkoutDurationCase.INSUFFICIENT_TOTAL_DURATION -> emptyList()
        }
        val resolvedIntervals = mutableListOf<MeasuredWorkoutInterval>()
        var currentStart = sessionStartedAt
        var windowsValid = true
        durations.forEachIndexed { index, duration ->
            val windowResult = runCatching {
                WorkoutIntervalWindow.create(currentStart, currentStart.plus(duration))
            }.getOrNull()?.valueOrNull()
            if (windowResult == null) {
                windowsValid = false
                return@forEachIndexed
            }
            val resolved = intervals[index].withEffectiveWindow(windowResult).valueOrNull()
            if (resolved == null) {
                windowsValid = false
            } else {
                resolvedIntervals += resolved
            }
            currentStart = windowResult.end
        }
        if (!windowsValid || resolvedIntervals.size != intervals.size) {
            reasons += TemporalSeriesValidationReason.INVALID
        }
        val tailUncovered = if (durationCase == WorkoutDurationCase.EXCESS_TOTAL_DURATION) {
            totalDuration.minus(allDuration)
        } else {
            Duration.ZERO
        }
        val eligible = reasons.isEmpty() && windowsValid
        return WorkoutDurationResolution(
            durationCase = durationCase,
            totalDuration = totalDuration,
            nominalIntervalDuration = nominalIntervalDuration,
            intervals = if (windowsValid) resolvedIntervals.toList() else intervals.toList(),
            tailUncovered = tailUncovered,
            eligibility = if (eligible) TemporalSeriesEligibility.ELIGIBLE else TemporalSeriesEligibility.INELIGIBLE,
            reasons = reasons.toSet(),
        )
    }
}

/**
 * Elegibilidade fechada da distância nativa D4 v4.
 *
 * A série temporal continua sendo a única fonte dos intervalos. O total do
 * header apenas comprova que essa série fecha; ele nunca preenche ou corrige
 * uma linha ausente.
 */
object WatchWorkoutNativeDistancePolicy {
    fun isEligible(
        workoutType: WorkoutType?,
        parserValidity: ParserValidity?,
        header: WatchWorkoutD4V4Header?,
        headerConsistency: WatchWorkoutD4V4Consistency?,
        intervals: List<MeasuredWorkoutInterval>,
        resolution: WorkoutDurationResolution,
        sessionStartedAt: Instant,
        totalDuration: Duration,
        expectedCrc: Int? = null,
    ): Boolean {
        if (workoutType != null && workoutType !in setOf(
                WorkoutType.UNKNOWN,
                WorkoutType.RUNNING,
                WorkoutType.WALKING,
            ) ||
            parserValidity != ParserValidity.VALID ||
            header == null ||
            headerConsistency?.isConsistent != true ||
            expectedCrc?.let { it != header.crc } == true ||
            !header.isStructurallyValid() ||
            !resolution.isEligible ||
            intervals.isEmpty() ||
            resolution.intervals.size != intervals.size ||
            header.recordCount != intervals.size ||
            header.startedAt != sessionStartedAt ||
            header.endedAt != runCatching { sessionStartedAt.plus(totalDuration) }.getOrNull()
        ) {
            return false
        }

        val indexValidation = TemporalSeriesValidator.validate(intervals)
        if (!indexValidation.isEligible) return false

        var totalSteps = 0L
        var totalDistance = 0.0
        var totalCaloriesRaw = 0L
        var totalSportValue = 0L
        for (interval in intervals) {
            val native = interval.source as? MeasuredWorkoutIntervalSource.D4V4Native
                ?: return false
            totalSteps = runCatching { Math.addExact(totalSteps, native.measuredSteps) }.getOrNull()
                ?: return false
            val distance = native.distanceMeters
            if (!distance.isFinite() || distance < 0.0 ||
                distance != distance.toLong().toDouble()
            ) return false
            totalDistance += distance
            if (!totalDistance.isFinite()) return false
            totalCaloriesRaw = runCatching { Math.addExact(totalCaloriesRaw, native.caloriesRaw) }.getOrNull()
                ?: return false
            totalSportValue = runCatching { Math.addExact(totalSportValue, native.sportValue) }.getOrNull()
                ?: return false
        }
        return totalSteps == header.totalSteps &&
            totalDistance == header.totalDistanceMeters.toDouble() &&
            totalCaloriesRaw == header.totalCaloriesRaw &&
            totalSportValue == header.totalSportValue
    }
}

internal fun WatchWorkoutD4V4Header.isStructurallyValid(): Boolean =
    markerRaw in 0..0xff &&
        !endedAt.isBefore(startedAt) &&
        totalSteps in 0L..0xffff_ffffL &&
        totalDistanceMeters in 0L..0xffff_ffffL &&
        totalCaloriesRaw in 0L..0xffff_ffffL &&
        totalSportValue in 0L..0xffff_ffffL &&
        recordCount in 1..0xffff &&
        pauseCount in 0..0xff &&
        pauseTime in 0..0xffff &&
        crc in 0..0xffff &&
        sportTypeRaw in 0..0xff &&
        reservedRaw.size == 3 &&
        reservedRaw.all { it in 0..0xff }

enum class WorkoutPlausibilityIndicator {
    IMPLAUSIBLE_CADENCE,
    IMPLAUSIBLE_SPEED,
}

/** Não há limiar global inventado nesta versão. Indicadores explícitos bloqueiam projeção. */
enum class WorkoutPlausibilityPolicy {
    /** Sem limiar auditado, projeção permanece bloqueada até evidência. */
    NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION,
    /** Política foi aceita explicitamente após evidência externa. */
    EXPLICITLY_ACCEPTED,
}

data class WorkoutSeriesPlausibility(
    val indicators: Set<WorkoutPlausibilityIndicator> = emptySet(),
    val policy: WorkoutPlausibilityPolicy = WorkoutPlausibilityPolicy.NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION,
) {
    val requiresExplicitAcceptance: Boolean
        get() = indicators.isEmpty() && policy == WorkoutPlausibilityPolicy.NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION

    val hasBlockingIndicators: Boolean
        get() = indicators.isNotEmpty()

    val projectionEligible: Boolean
        get() = policy == WorkoutPlausibilityPolicy.EXPLICITLY_ACCEPTED && indicators.isEmpty()

    val isProjectionEligible: Boolean
        get() = projectionEligible

    val blocksGranularProjection: Boolean
        get() = !projectionEligible

    /**
     * Registra aceite humano somente quando não há indicador tipado. A ausência
     * de limiar global não é convertida em limiar nem ignora R6.
     */
    fun acceptExplicitly(): DomainResult<WorkoutSeriesPlausibility> = when {
        indicators.isNotEmpty() -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
        policy == WorkoutPlausibilityPolicy.EXPLICITLY_ACCEPTED -> DomainResult.Success(this)
        policy == WorkoutPlausibilityPolicy.NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION ->
            DomainResult.Success(copy(policy = WorkoutPlausibilityPolicy.EXPLICITLY_ACCEPTED))
        else -> DomainResult.Failure(DomainValidationError.INVALID_STATE)
    }

    companion object {
        fun flagged(vararg indicators: WorkoutPlausibilityIndicator): WorkoutSeriesPlausibility =
            WorkoutSeriesPlausibility(indicators.toSet())
    }
}

data class WorkoutIntervalMetrics(
    val cadenceStepsPerMinute: Double,
    val speedMetersPerSecond: Double,
    val paceSecondsPerKilometer: Double?,
) {
    val cadence: Double
        get() = cadenceStepsPerMinute

    val speed: Double
        get() = speedMetersPerSecond

    val pace: Double?
        get() = paceSecondsPerKilometer
}

data class WorkoutCumulativeMetrics(
    val cumulativeSteps: Long,
    val coveredDuration: Duration,
    val cadenceStepsPerMinute: Double,
    val speedMetersPerSecond: Double,
    val paceSecondsPerKilometer: Double?,
) {
    val cadence: Double
        get() = cadenceStepsPerMinute

    val speed: Double
        get() = speedMetersPerSecond

    val pace: Double?
        get() = paceSecondsPerKilometer
}

@ConsistentCopyVisibility
data class DerivedWorkoutInterval private constructor(
    val index: Int,
    val effectiveDuration: Duration,
    val effectiveWindow: WorkoutIntervalWindow,
    val decodedSteps: Long,
    val canonicalIntervalDistanceMeters: Double,
    val metrics: WorkoutIntervalMetrics,
    val cumulativeSteps: Long,
    val cumulativeDistanceMeters: Double,
    val cumulativeDuration: Duration,
    val cumulativeMetrics: WorkoutCumulativeMetrics,
) {
    val cadenceStepsPerMinute: Double
        get() = metrics.cadenceStepsPerMinute

    val speedMetersPerSecond: Double
        get() = metrics.speedMetersPerSecond

    val paceSecondsPerKilometer: Double?
        get() = metrics.paceSecondsPerKilometer

    /** Política estável do SpeedRecord: ponto médio do intervalo efetivo. */
    val speedSampledAt: Instant
        get() = effectiveWindow.start.plus(effectiveDuration.dividedBy(2))

    companion object {
        internal fun create(
            measured: MeasuredWorkoutInterval,
            canonicalIntervalDistanceMeters: Double,
            cumulativeSteps: Long,
            cumulativeDistanceMeters: Double,
            cumulativeDuration: Duration,
        ): DomainResult<DerivedWorkoutInterval> {
            val duration = measured.effectiveDuration ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            val window = measured.effectiveWindow ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            val steps = measured.measuredSteps ?: return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            if (!canonicalIntervalDistanceMeters.isFinite() ||
                canonicalIntervalDistanceMeters < 0.0 ||
                steps < 0L ||
                !duration.isPositive()
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val durationSeconds = duration.secondsAsDouble()
            if (!durationSeconds.isFinite() || durationSeconds <= 0.0 ||
                cumulativeSteps < 0L || !cumulativeDistanceMeters.isFinite() || cumulativeDistanceMeters < 0.0
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val intervalMetrics = WorkoutIntervalMetrics(
                cadenceStepsPerMinute = if (steps == 0L) 0.0 else steps.toDouble() / (durationSeconds / 60.0),
                speedMetersPerSecond = if (canonicalIntervalDistanceMeters == 0.0) {
                    0.0
                } else {
                    canonicalIntervalDistanceMeters / durationSeconds
                },
                paceSecondsPerKilometer = canonicalIntervalDistanceMeters.takeIf { it > 0.0 }
                    ?.let { durationSeconds / (it / 1_000.0) },
            )
            val cumulativeMetrics = WorkoutCumulativeMetrics(
                cumulativeSteps = cumulativeSteps,
                coveredDuration = cumulativeDuration,
                cadenceStepsPerMinute = cumulativeCadence(cumulativeSteps, cumulativeDuration),
                speedMetersPerSecond = cumulativeSpeed(cumulativeDistanceMeters, cumulativeDuration),
                paceSecondsPerKilometer = cumulativePace(cumulativeDistanceMeters, cumulativeDuration),
            )
            return DomainResult.Success(
                DerivedWorkoutInterval(
                    index = measured.index,
                    effectiveDuration = duration,
                    effectiveWindow = window,
                    decodedSteps = steps,
                    canonicalIntervalDistanceMeters = canonicalIntervalDistanceMeters,
                    metrics = intervalMetrics,
                    cumulativeSteps = cumulativeSteps,
                    cumulativeDistanceMeters = cumulativeDistanceMeters,
                    cumulativeDuration = cumulativeDuration,
                    cumulativeMetrics = cumulativeMetrics,
                ),
            )
        }
    }
}

data class WorkoutSpeedSample(
    val sampledAt: Instant,
    val speedMetersPerSecond: Double,
    val intervalIndex: Int,
)

@ConsistentCopyVisibility
data class CanonicalWorkoutSeries private constructor(
    val intervals: List<DerivedWorkoutInterval>,
    val canonicalDistanceMeters: Double,
    val coveredDuration: Duration,
    val cumulativeMetrics: WorkoutCumulativeMetrics,
) {
    val speedSamples: List<WorkoutSpeedSample>
        get() = intervals.map { interval ->
            WorkoutSpeedSample(
                sampledAt = interval.speedSampledAt,
                speedMetersPerSecond = interval.speedMetersPerSecond,
                intervalIndex = interval.index,
            )
        }

    val isEligible: Boolean
        get() = true

    companion object {
        internal fun create(intervals: List<DerivedWorkoutInterval>): DomainResult<CanonicalWorkoutSeries> {
            if (intervals.isEmpty()) return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            val indexValidation = TemporalSeriesValidator.validateIndices(intervals.map { it.index })
            if (!indexValidation.isEligible) return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            if (intervals.zipWithNext().any { (first, second) -> first.effectiveWindow.end != second.effectiveWindow.start }) {
                return DomainResult.Failure(DomainValidationError.INVALID_TEMPORAL_ORDER)
            }
            if (intervals.any {
                    !it.effectiveDuration.isPositive() ||
                        !it.canonicalIntervalDistanceMeters.isFinite() ||
                        it.canonicalIntervalDistanceMeters < 0.0
                }
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val canonicalDistance = intervals.sumOf { it.canonicalIntervalDistanceMeters }
            val coveredDuration = Duration.between(
                intervals.first().effectiveWindow.start,
                intervals.last().effectiveWindow.end,
            )
            if (!canonicalDistance.isFinite() || canonicalDistance < 0.0 || !coveredDuration.isPositive()) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val cumulativeSteps = intervals.last().cumulativeSteps
            return DomainResult.Success(
                CanonicalWorkoutSeries(
                    intervals = intervals.toList(),
                    canonicalDistanceMeters = canonicalDistance,
                    coveredDuration = coveredDuration,
                    cumulativeMetrics = WorkoutCumulativeMetrics(
                        cumulativeSteps = cumulativeSteps,
                        coveredDuration = coveredDuration,
                        cadenceStepsPerMinute = cumulativeCadence(cumulativeSteps, coveredDuration),
                        speedMetersPerSecond = cumulativeSpeed(canonicalDistance, coveredDuration),
                        paceSecondsPerKilometer = cumulativePace(canonicalDistance, coveredDuration),
                    ),
                ),
            )
        }
    }
}

/** Estimativa agregada deliberadamente diagnóstica; nunca é distância canônica. */
@ConsistentCopyVisibility
data class DiagnosticAggregateStepEstimate private constructor(
    val totalSteps: Long,
    val stepLengthMetersUsed: Double,
    val estimatedDistanceMeters: Double,
) {
    val isDiagnosticOnly: Boolean
        get() = true

    companion object {
        fun create(totalSteps: Long, stepLengthMetersUsed: Double): DomainResult<DiagnosticAggregateStepEstimate> {
            val estimatedDistance = totalSteps.toDouble() * stepLengthMetersUsed
            return if (
                totalSteps >= 0L && stepLengthMetersUsed.isFinite() && stepLengthMetersUsed > 0.0 &&
                estimatedDistance.isFinite() && estimatedDistance >= 0.0
            ) {
                DomainResult.Success(
                    DiagnosticAggregateStepEstimate(totalSteps, stepLengthMetersUsed, estimatedDistance),
                )
            } else {
                DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
        }
    }
}

@ConsistentCopyVisibility
data class WorkoutDerivationSnapshot private constructor(
    val stepLengthMetersUsed: Double?,
    val stepLengthSource: StepLengthSource?,
    val derivationMethod: WorkoutDerivationMethod,
    /** Fator manual/aprendido aplicado à série nativa; nulo para demais fontes. */
    val distanceCalibrationFactor: Double?,
    val derivationVersion: String,
    val derivedAt: Instant,
    val temporalResolution: WorkoutDurationResolution,
    val plausibility: WorkoutSeriesPlausibility,
    val canonicalSeries: CanonicalWorkoutSeries?,
    val diagnosticAggregateStepEstimate: DiagnosticAggregateStepEstimate?,
    /** Linhas de split carregadas da projeção persistida, sem recálculo no read path. */
    val splits: List<WorkoutSplit> = emptyList(),
) {
    val canonicalDistanceMeters: Double?
        get() = canonicalSeries?.canonicalDistanceMeters

    val canonicalSeriesEligible: Boolean
        get() = canonicalSeries != null && temporalResolution.isEligible

    val isEligibleForFunctionalProjection: Boolean
        get() = canonicalSeriesEligible && plausibility.isProjectionEligible

    val isEligibleForHealthProjection: Boolean
        get() = isEligibleForFunctionalProjection

    /** Série válida, sem indicadores, mas ainda bloqueada até aceite explícito. */
    val requiresExplicitPlausibilityAcceptance: Boolean
        get() = canonicalSeriesEligible && plausibility.requiresExplicitAcceptance

    val hasPlausibilityIndicators: Boolean
        get() = plausibility.hasBlockingIndicators

    fun acceptExplicitPlausibility(): DomainResult<WorkoutDerivationSnapshot> {
        if (!canonicalSeriesEligible) return DomainResult.Failure(DomainValidationError.INVALID_STATE)
        return when (val accepted = plausibility.acceptExplicitly()) {
            is DomainResult.Success -> DomainResult.Success(copy(plausibility = accepted.value))
            is DomainResult.Failure -> accepted
        }
    }

    /** Elegibilidade local da série, independente da guarda remota de plausibilidade. */
    val isEligibleForCanonicalWorkflow: Boolean
        get() = canonicalSeriesEligible

    /**
     * Splits locais exigem série canônica e ausência de indicadores de
     * implausibilidade. A política de projeção remota continua independente:
     * ela pode bloquear Health Connect sem ocultar read models locais válidos.
     */
    val isEligibleForFunctionalSplits: Boolean
        get() = canonicalSeriesEligible && plausibility.indicators.isEmpty()

    val canonicalReadStatus: WorkoutCanonicalReadStatus
        get() = when {
            canonicalSeries == null -> WorkoutCanonicalReadStatus.SERIES_INELIGIBLE
            plausibility.indicators.isNotEmpty() -> WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED
            canonicalSeries.canonicalDistanceMeters >= 1_000.0 && splits.isEmpty() ->
                WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED
            else -> WorkoutCanonicalReadStatus.AVAILABLE
        }

    /** Anexa somente linhas de split já validadas e carregadas da persistência. */
    internal fun withPersistedSplits(value: List<WorkoutSplit>): WorkoutDerivationSnapshot = copy(splits = value)

    companion object {
        fun derive(
            intervals: List<MeasuredWorkoutInterval>,
            totalDuration: Duration,
            sessionStartedAt: Instant,
            stepLength: StepLengthProfile,
            derivationVersion: String,
            derivedAt: Instant,
            totalSteps: Long? = null,
            plausibility: WorkoutSeriesPlausibility = WorkoutSeriesPlausibility(),
            derivationMethod: WorkoutDerivationMethod = WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
        ): DomainResult<WorkoutDerivationSnapshot> = createInternal(
            intervals = intervals,
            totalDuration = totalDuration,
            sessionStartedAt = sessionStartedAt,
            stepLengthMetersUsed = stepLength.stepLengthMeters,
            stepLengthSource = stepLength.source,
            derivationMethod = derivationMethod,
            derivationVersion = derivationVersion,
            derivedAt = derivedAt,
            totalSteps = totalSteps,
            plausibility = plausibility,
        )

        fun derive(
            intervals: List<MeasuredWorkoutInterval>,
            totalDuration: Duration,
            sessionStartedAt: Instant,
            stepLengthMeters: Double,
            stepLengthSource: StepLengthSource,
            derivationVersion: String,
            derivedAt: Instant,
            totalSteps: Long? = null,
            plausibility: WorkoutSeriesPlausibility = WorkoutSeriesPlausibility(),
            derivationMethod: WorkoutDerivationMethod = WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
        ): DomainResult<WorkoutDerivationSnapshot> = createInternal(
            intervals = intervals,
            totalDuration = totalDuration,
            sessionStartedAt = sessionStartedAt,
            stepLengthMetersUsed = stepLengthMeters,
            stepLengthSource = stepLengthSource,
            derivationMethod = derivationMethod,
            derivationVersion = derivationVersion,
            derivedAt = derivedAt,
            totalSteps = totalSteps,
            plausibility = plausibility,
        )

        fun deriveNative(
            intervals: List<MeasuredWorkoutInterval>,
            totalDuration: Duration,
            sessionStartedAt: Instant,
            workoutType: WorkoutType?,
            header: WatchWorkoutD4V4Header,
            parserValidity: ParserValidity,
            headerConsistency: WatchWorkoutD4V4Consistency? = null,
            expectedCrc: Int? = null,
            distanceCalibrationFactor: Double? = null,
            derivationVersion: String = WATCH_WORKOUT_NATIVE_DERIVATION_VERSION,
            derivedAt: Instant,
            plausibility: WorkoutSeriesPlausibility = WorkoutSeriesPlausibility(),
        ): DomainResult<WorkoutDerivationSnapshot> = createInternal(
            intervals = intervals,
            totalDuration = totalDuration,
            sessionStartedAt = sessionStartedAt,
            stepLengthMetersUsed = null,
            stepLengthSource = null,
            derivationMethod = if (distanceCalibrationFactor == null) {
                WorkoutDerivationMethod.WATCH_NATIVE
            } else {
                WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED
            },
            derivationVersion = derivationVersion,
            derivedAt = derivedAt,
            totalSteps = null,
            plausibility = plausibility,
            nativeWorkoutType = workoutType,
            nativeHeader = header,
            nativeParserValidity = parserValidity,
            nativeHeaderConsistency = headerConsistency,
            nativeExpectedCrc = expectedCrc,
            nativeDistanceCalibrationFactor = distanceCalibrationFactor,
        )

        /**
         * Reconstrói snapshot v1 sem aplicar normalização nova ao read path.
         * Usado somente para preservar snapshots históricos até reprocessamento
         * explícito.
         */
        internal fun deriveLegacy(
            intervals: List<MeasuredWorkoutInterval>,
            totalDuration: Duration,
            sessionStartedAt: Instant,
            stepLengthMeters: Double,
            stepLengthSource: StepLengthSource,
            derivationVersion: String,
            derivedAt: Instant,
            totalSteps: Long? = null,
            plausibility: WorkoutSeriesPlausibility = WorkoutSeriesPlausibility(),
        ): DomainResult<WorkoutDerivationSnapshot> = createInternal(
            intervals = intervals,
            totalDuration = totalDuration,
            sessionStartedAt = sessionStartedAt,
            stepLengthMetersUsed = stepLengthMeters,
            stepLengthSource = stepLengthSource,
            derivationMethod = WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
            derivationVersion = derivationVersion,
            derivedAt = derivedAt,
            totalSteps = totalSteps,
            plausibility = plausibility,
            useLegacyTemporalPolicy = true,
        )

        private fun createInternal(
            intervals: List<MeasuredWorkoutInterval>,
            totalDuration: Duration,
            sessionStartedAt: Instant,
            stepLengthMetersUsed: Double?,
            stepLengthSource: StepLengthSource?,
            derivationMethod: WorkoutDerivationMethod,
            derivationVersion: String,
            derivedAt: Instant,
            totalSteps: Long?,
            plausibility: WorkoutSeriesPlausibility,
            useLegacyTemporalPolicy: Boolean = false,
            nativeWorkoutType: WorkoutType? = null,
            nativeHeader: WatchWorkoutD4V4Header? = null,
            nativeParserValidity: ParserValidity? = null,
            nativeHeaderConsistency: WatchWorkoutD4V4Consistency? = null,
            nativeExpectedCrc: Int? = null,
            nativeDistanceCalibrationFactor: Double? = null,
        ): DomainResult<WorkoutDerivationSnapshot> {
            val hasValidStepLength = stepLengthMetersUsed?.let {
                it.isFinite() && it > 0.0 && stepLengthSource != null
            } == true
            val hasNoStepLength = stepLengthMetersUsed == null && stepLengthSource == null
            val hasValidDistanceCalibrationFactor = nativeDistanceCalibrationFactor?.let {
                it.isFinite() && it > 0.0
            } == true
            val derivationInputsValid = when (derivationMethod) {
                WorkoutDerivationMethod.WATCH_NATIVE -> hasNoStepLength && nativeDistanceCalibrationFactor == null
                WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED ->
                    hasNoStepLength && hasValidDistanceCalibrationFactor
                WorkoutDerivationMethod.STEP_LENGTH_FALLBACK,
                WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
                -> hasValidStepLength && nativeDistanceCalibrationFactor == null
            }
            if (!derivationInputsValid || derivationVersion.isBlank()) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            if (totalSteps != null && totalSteps < 0L) {
                return DomainResult.Failure(DomainValidationError.INVALID_RANGE)
            }
            val temporalResolution = if (useLegacyTemporalPolicy) {
                WorkoutDurationResolver.resolveLegacy(
                    totalDuration = totalDuration,
                    intervals = intervals,
                    sessionStartedAt = sessionStartedAt,
                )
            } else {
                WorkoutDurationResolver.resolve(
                    totalDuration = totalDuration,
                    intervals = intervals,
                    sessionStartedAt = sessionStartedAt,
                )
            }
            if (derivationMethod in setOf(
                    WorkoutDerivationMethod.WATCH_NATIVE,
                    WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED,
                ) &&
                !WatchWorkoutNativeDistancePolicy.isEligible(
                    workoutType = nativeWorkoutType,
                    parserValidity = nativeParserValidity,
                    header = nativeHeader,
                    headerConsistency = nativeHeaderConsistency,
                    intervals = intervals,
                    resolution = temporalResolution,
                    sessionStartedAt = sessionStartedAt,
                    totalDuration = totalDuration,
                    expectedCrc = nativeExpectedCrc,
                )
            ) {
                return DomainResult.Failure(DomainValidationError.INVALID_STATE)
            }
            val diagnosticEstimate = totalSteps?.let {
                stepLengthMetersUsed?.let { stepLength ->
                    DiagnosticAggregateStepEstimate.create(it, stepLength).valueOrNull()
                }
            }
            val canonicalSeries = deriveCanonicalSeries(
                resolution = temporalResolution,
                derivationMethod = derivationMethod,
                stepLengthMetersUsed = stepLengthMetersUsed,
                distanceCalibrationFactor = nativeDistanceCalibrationFactor,
            )
            return DomainResult.Success(
                WorkoutDerivationSnapshot(
                    stepLengthMetersUsed = stepLengthMetersUsed,
                    stepLengthSource = stepLengthSource,
                    derivationMethod = derivationMethod,
                    distanceCalibrationFactor = nativeDistanceCalibrationFactor,
                    derivationVersion = derivationVersion,
                    derivedAt = derivedAt,
                    temporalResolution = temporalResolution,
                    plausibility = plausibility.copy(indicators = plausibility.indicators.toSet()),
                    canonicalSeries = canonicalSeries,
                    diagnosticAggregateStepEstimate = diagnosticEstimate,
                ),
            )
        }

        private fun deriveCanonicalSeries(
            resolution: WorkoutDurationResolution,
            derivationMethod: WorkoutDerivationMethod,
            stepLengthMetersUsed: Double?,
            distanceCalibrationFactor: Double?,
        ): CanonicalWorkoutSeries? {
            if (!resolution.isEligible) return null
            if (resolution.intervals.any { it.measuredSteps == null }) return null
            var cumulativeSteps = 0L
            var cumulativeDistance = 0.0
            var cumulativeDuration = Duration.ZERO
            val derived = mutableListOf<DerivedWorkoutInterval>()
            for (measured in resolution.intervals) {
                val steps = measured.measuredSteps ?: return null
                cumulativeSteps = try {
                    Math.addExact(cumulativeSteps, steps)
                } catch (_: ArithmeticException) {
                    return null
                }
                val distance = when (derivationMethod) {
                    WorkoutDerivationMethod.WATCH_NATIVE -> measured.nativeDistanceMeters ?: return null
                    WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED -> {
                        val nativeDistance = measured.nativeDistanceMeters ?: return null
                        nativeDistance * (distanceCalibrationFactor ?: return null)
                    }
                    WorkoutDerivationMethod.STEP_LENGTH_FALLBACK,
                    WorkoutDerivationMethod.FIXED_PERSONAL_STEP_LENGTH,
                    -> steps.toDouble() * (stepLengthMetersUsed ?: return null)
                }
                if (!distance.isFinite()) return null
                cumulativeDistance += distance
                cumulativeDuration = try {
                    cumulativeDuration.plus(measured.effectiveDuration ?: return null)
                } catch (_: ArithmeticException) {
                    return null
                }
                val derivedInterval = DerivedWorkoutInterval.create(
                    measured = measured,
                    canonicalIntervalDistanceMeters = distance,
                    cumulativeSteps = cumulativeSteps,
                    cumulativeDistanceMeters = cumulativeDistance,
                    cumulativeDuration = cumulativeDuration,
                ).valueOrNull() ?: return null
                derived += derivedInterval
            }
            return CanonicalWorkoutSeries.create(derived).valueOrNull()
        }
    }
}

enum class WorkoutDerivationMethod {
    FIXED_PERSONAL_STEP_LENGTH,
    STEP_LENGTH_FALLBACK,
    WATCH_NATIVE,
    WATCH_NATIVE_CONTEXT_CALIBRATED,
}

/**
 * Regra aditiva para revisão futura. O contrato legado de [WorkoutActivity]
 * continua exigindo calibração até consumidores canônicos serem migrados.
 */
object WatchWorkoutWorkflowPolicy {
    fun stateAfterReview(
        origin: WorkoutActivityOrigin,
        confirmedType: WorkoutType,
        derivationSnapshot: WorkoutDerivationSnapshot?,
    ): ActivityWorkflowState = if (
        confirmedType == WorkoutType.TREADMILL &&
        !(origin == WorkoutActivityOrigin.WATCH_D3_D4 &&
            derivationSnapshot?.isEligibleForCanonicalWorkflow == true &&
            derivationSnapshot.derivationMethod !in setOf(
                WorkoutDerivationMethod.WATCH_NATIVE,
                WorkoutDerivationMethod.WATCH_NATIVE_CONTEXT_CALIBRATED,
            ))
    ) {
        ActivityWorkflowState.PENDING_CALIBRATION
    } else {
        ActivityWorkflowState.READY_TO_SYNC
    }
}

data class WorkoutSplit(
    val number: Int,
    val targetDistanceMeters: Double,
    val window: WorkoutIntervalWindow,
    val distanceMeters: Double,
    val duration: Duration,
    val averageCadenceStepsPerMinute: Double,
    val averageSpeedMetersPerSecond: Double,
    val averagePaceSecondsPerKilometer: Double?,
    val averageBpm: Double?,
    val bpmCoveredDuration: Duration,
) {
    val isHalfOpen: Boolean
        get() = true
}

object WorkoutSplitCalculator {
    private const val SPLIT_DISTANCE_METERS = 1_000.0

    fun calculate(
        series: CanonicalWorkoutSeries,
        heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
    ): List<WorkoutSplit> = calculateOrNull(series, heartRateSamples).orEmpty()

    /**
     * Calcula projeção de splits ou sinaliza falha estrutural sem omitir apenas
     * fronteiras inválidas.
     *
     * O último split pode ser parcial. [null] representa incapacidade de
     * materializar todas as fronteiras esperadas.
     */
    fun calculateOrNull(
        series: CanonicalWorkoutSeries,
        heartRateSamples: List<WorkoutHeartRateSample> = emptyList(),
    ): List<WorkoutSplit>? {
        val splitBoundaries = splitBoundaries(series) ?: return null
        if (splitBoundaries.isEmpty()) return emptyList()
        val sessionWindow = WorkoutIntervalWindow.create(
            series.intervals.first().effectiveWindow.start,
            series.intervals.last().effectiveWindow.end,
        ).valueOrNull() ?: return null
        return splitBoundaries.mapIndexed { index, boundary ->
            val number = index + 1
            val startDistance = (number - 1) * SPLIT_DISTANCE_METERS
            val endDistance = boundary
            val start = boundaryAtDistance(series, startDistance) ?: return null
            val end = boundaryAtDistance(series, endDistance) ?: return null
            val window = WorkoutIntervalWindow.create(start, end).valueOrNull() ?: return null
            val portions = portionsForDistance(series, startDistance, endDistance)
            val duration = window.duration
            val durationSeconds = duration.secondsAsDouble()
            val splitSteps = portions.sumOf { it.steps }
            val splitDistance = endDistance - startDistance
            val bpm = weightedBpm(heartRateSamples, window, sessionWindow)
            WorkoutSplit(
                number = number,
                targetDistanceMeters = endDistance,
                window = window,
                distanceMeters = splitDistance,
                duration = duration,
                averageCadenceStepsPerMinute = if (splitSteps == 0.0) 0.0 else splitSteps / (durationSeconds / 60.0),
                averageSpeedMetersPerSecond = if (durationSeconds > 0.0) {
                    splitDistance / durationSeconds
                } else {
                    0.0
                },
                averagePaceSecondsPerKilometer = if (durationSeconds > 0.0 && splitDistance > 0.0) {
                    durationSeconds * 1_000.0 / splitDistance
                } else {
                    null
                },
                averageBpm = bpm.first,
                bpmCoveredDuration = bpm.second,
            )
        }
    }

    private fun splitBoundaries(series: CanonicalWorkoutSeries): List<Double>? {
        val distance = series.canonicalDistanceMeters
        if (!distance.isFinite() || distance <= 0.0) return emptyList()
        val completeCount = floor(distance / SPLIT_DISTANCE_METERS)
        if (!completeCount.isFinite() || completeCount < 0.0 || completeCount >= Int.MAX_VALUE.toDouble()) {
            return null
        }
        val boundaries = (1..completeCount.toInt()).map { it * SPLIT_DISTANCE_METERS }.toMutableList()
        val remainder = distance - completeCount * SPLIT_DISTANCE_METERS
        if (remainder > 1e-6) boundaries += distance
        return boundaries
    }

    private data class DistancePortion(
        val steps: Double,
    )

    private fun portionsForDistance(
        series: CanonicalWorkoutSeries,
        startDistance: Double,
        endDistance: Double,
    ): List<DistancePortion> {
        val portions = mutableListOf<DistancePortion>()
        var cumulativeBefore = 0.0
        for (interval in series.intervals) {
            val intervalDistance = interval.canonicalIntervalDistanceMeters
            val cumulativeAfter = cumulativeBefore + intervalDistance
            if (intervalDistance == 0.0) {
                if (cumulativeBefore >= startDistance && cumulativeBefore < endDistance) {
                    portions += DistancePortion(0.0)
                }
            } else {
                val overlapStart = max(startDistance, cumulativeBefore)
                val overlapEnd = min(endDistance, cumulativeAfter)
                if (overlapEnd > overlapStart) {
                    val fraction = (overlapEnd - overlapStart) / intervalDistance
                    portions += DistancePortion(interval.decodedSteps.toDouble() * fraction)
                }
            }
            cumulativeBefore = cumulativeAfter
        }
        return portions
    }

    private fun boundaryAtDistance(series: CanonicalWorkoutSeries, distance: Double): Instant? {
        if (distance <= 0.0) return series.intervals.first().effectiveWindow.start
        if (distance > series.canonicalDistanceMeters) return null
        var cumulativeBefore = 0.0
        for (interval in series.intervals) {
            val intervalDistance = interval.canonicalIntervalDistanceMeters
            val cumulativeAfter = cumulativeBefore + intervalDistance
            if (distance <= cumulativeAfter && intervalDistance > 0.0) {
                val fraction = (distance - cumulativeBefore) / intervalDistance
                return scaleInstant(interval.effectiveWindow.start, interval.effectiveDuration, fraction)
            }
            cumulativeBefore = cumulativeAfter
        }
        return series.intervals.last().effectiveWindow.end
    }

    private fun weightedBpm(
        samples: List<WorkoutHeartRateSample>,
        splitWindow: WorkoutIntervalWindow,
        sessionWindow: WorkoutIntervalWindow,
    ): Pair<Double?, Duration> {
        if (samples.isEmpty()) return null to Duration.ZERO
        val ordered = samples.sortedBy(WorkoutHeartRateSample::sampledAt)
        var weightedBpm = 0.0
        var covered = Duration.ZERO
        ordered.forEachIndexed { index, sample ->
            val sampleStart = maxInstant(sample.sampledAt, sessionWindow.start)
            val nextSampleAt = ordered.getOrNull(index + 1)?.sampledAt ?: sessionWindow.end
            val sampleEnd = minInstant(nextSampleAt, sessionWindow.end)
            val overlapStart = maxInstant(sampleStart, splitWindow.start)
            val overlapEnd = minInstant(sampleEnd, splitWindow.end)
            if (overlapEnd.isAfter(overlapStart)) {
                val overlap = Duration.between(overlapStart, overlapEnd)
                weightedBpm += sample.bpm.toDouble() * overlap.secondsAsDouble()
                covered = covered.plus(overlap)
            }
        }
        val coveredSeconds = covered.secondsAsDouble()
        return if (coveredSeconds > 0.0) weightedBpm / coveredSeconds to covered else null to covered
    }
}

private fun isValidRawInterval(
    index: Int,
    rawMotionValue: Long,
    rawTemporalDistance: Long?,
    rawTemporalCalories: Long?,
): Boolean = index >= 0 &&
    rawMotionValue in 0L..MeasuredWorkoutInterval.MAX_RAW_MOTION_VALUE &&
    rawTemporalDistance.isValidRawDistance() &&
    rawTemporalCalories.isValidRawCalories()

private fun Long?.isValidRawDistance(): Boolean = this == null || this in 0L..0xFF_FFFFL

private fun Long?.isValidRawCalories(): Boolean = this == null || this in 0L..0xFFFFL

private fun Duration.safeMultipliedBy(multiplier: Long): Duration? =
    runCatching { multipliedBy(multiplier) }.getOrNull()

private fun Duration.secondsAsDouble(): Double = seconds.toDouble() + nano.toDouble() / 1_000_000_000.0

private fun cumulativeCadence(steps: Long, duration: Duration): Double =
    if (steps == 0L || !duration.isPositive()) 0.0 else steps.toDouble() / (duration.secondsAsDouble() / 60.0)

private fun cumulativeSpeed(distanceMeters: Double, duration: Duration): Double =
    if (distanceMeters == 0.0 || !duration.isPositive()) 0.0 else distanceMeters / duration.secondsAsDouble()

private fun cumulativePace(distanceMeters: Double, duration: Duration): Double? =
    if (distanceMeters > 0.0 && duration.isPositive()) {
        duration.secondsAsDouble() / (distanceMeters / 1_000.0)
    } else {
        null
    }

private fun scaleInstant(start: Instant, duration: Duration, fraction: Double): Instant =
    start.plus(scaleDuration(duration, fraction))

private fun scaleDuration(duration: Duration, fraction: Double): Duration {
    val nanos = duration.secondsAsDouble() * 1_000_000_000.0 * fraction
    return Duration.ofNanos(nanos.toLong().coerceAtLeast(0L))
}

private fun maxInstant(first: Instant, second: Instant): Instant = if (first.isAfter(second)) first else second

private fun minInstant(first: Instant, second: Instant): Instant = if (first.isBefore(second)) first else second
