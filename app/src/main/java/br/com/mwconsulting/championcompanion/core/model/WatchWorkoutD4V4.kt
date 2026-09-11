package br.com.mwconsulting.championcompanion.core.model

import java.time.Instant

/** Header completo do Sport Model fixo observado no protocolo D4 v4. */
data class WatchWorkoutD4V4Header(
    val markerRaw: Int,
    val startedAt: Instant,
    val endedAt: Instant,
    val totalSteps: Long,
    val totalDistanceMeters: Long,
    val totalCaloriesRaw: Long,
    val totalSportValue: Long,
    val recordCount: Int,
    val pauseCount: Int,
    val pauseTime: Int,
    val crc: Int,
    val sportTypeRaw: Int,
    val reservedRaw: List<Int>,
)

/** Fato de um pacote temporal D4 v4; BPM inválido fica ausente apenas em [bpm]. */
data class WatchWorkoutD4V4Sample(
    val sourceSampleIndex: Int,
    val bpmRaw: Int,
    val bpm: Int?,
    val sportValue: Int,
    val steps: Int,
    val caloriesRaw: Int,
    val distanceMeters: Int,
    val pauseStateRaw: Int,
)

data class WatchWorkoutD4V4Consistency(
    val recordCountMatches: Boolean,
    val stepsMatch: Boolean,
    val distanceMetersMatch: Boolean,
    val caloriesRawMatch: Boolean,
    val sportValueMatch: Boolean,
) {
    val isConsistent: Boolean
        get() = recordCountMatches && stepsMatch && distanceMetersMatch &&
            caloriesRawMatch && sportValueMatch
}

/** Contrato interno do parser v4, sem reinterpretar colunas legacy. */
data class WatchWorkoutD4V4Block(
    val header: WatchWorkoutD4V4Header,
    val samples: List<WatchWorkoutD4V4Sample>,
    val consistency: WatchWorkoutD4V4Consistency,
)
