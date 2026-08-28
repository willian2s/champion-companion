package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.EvolutionActivitySnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalRecord
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordMetric
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadSnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutType

internal data class PersonalRecordScope(
    val metric: PersonalRecordMetric,
    val workoutTypes: List<WorkoutType>,
)

internal val PERSONAL_RECORD_SCOPES = listOf(
    PersonalRecordScope(
        metric = PersonalRecordMetric.LONGEST_DISTANCE,
        workoutTypes = listOf(
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            WorkoutType.TREADMILL,
            WorkoutType.CYCLING,
        ),
    ),
    PersonalRecordScope(
        metric = PersonalRecordMetric.LONGEST_DURATION,
        workoutTypes = listOf(
            WorkoutType.WALKING,
            WorkoutType.RUNNING,
            WorkoutType.FREE_WORKOUT,
            WorkoutType.TREADMILL,
            WorkoutType.CYCLING,
        ),
    ),
    PersonalRecordScope(
        metric = PersonalRecordMetric.MOST_STEPS,
        workoutTypes = listOf(
            WorkoutType.RUNNING,
            WorkoutType.WALKING,
            WorkoutType.TREADMILL,
            WorkoutType.FREE_WORKOUT,
        ),
    ),
)

/** Agrega recordistas atuais a partir de fatos normalizados all-history. */
fun PersonalRecordsReadSnapshot.toPersonalRecordsSnapshot(): PersonalRecordsSnapshot =
    activities.toPersonalRecordsSnapshot()

/** Variante pura para consumidores que já possuem atividades normalizadas. */
fun List<EvolutionActivitySnapshot>.toPersonalRecordsSnapshot(): PersonalRecordsSnapshot {
    requireValidPersonalRecordActivities()
    return PersonalRecordsSnapshot(
        records = PERSONAL_RECORD_SCOPES.flatMap { scope ->
            scope.workoutTypes.mapNotNull { workoutType ->
                bestRecordFor(scope.metric, workoutType)
            }
        },
    )
}

private fun List<EvolutionActivitySnapshot>.bestRecordFor(
    metric: PersonalRecordMetric,
    workoutType: WorkoutType,
): PersonalRecord? = asSequence()
    .filter { it.workoutType == workoutType }
    .mapNotNull { activity ->
        activity.personalRecordValue(metric)?.let { value ->
            PersonalRecord(
                metric = metric,
                workoutType = workoutType,
                activityId = activity.activityId,
                value = value,
                achievedAt = activity.startedAt,
            )
        }
    }
    .maxWithOrNull(PERSONAL_RECORD_ORDER)

private val PERSONAL_RECORD_ORDER: Comparator<PersonalRecord> =
    compareBy<PersonalRecord> { it.value }
        .thenBy { it.achievedAt }
        .thenBy { it.activityId.value.toString() }

internal fun EvolutionActivitySnapshot.personalRecordValue(metric: PersonalRecordMetric): Double? = when (metric) {
    PersonalRecordMetric.LONGEST_DISTANCE -> distanceMeters?.takeIf { it.isFinite() && it > 0.0 }
    PersonalRecordMetric.LONGEST_DURATION -> duration?.let {
        (it.seconds.toDouble() + it.nano.toDouble() / 1_000_000_000.0)
            .takeIf { value -> value.isFinite() && value > 0.0 }
    }
    PersonalRecordMetric.MOST_STEPS -> steps?.toDouble()?.takeIf { it.isFinite() && it > 0.0 }
}

internal fun List<EvolutionActivitySnapshot>.requireValidPersonalRecordActivities() {
    require(distinctBy(EvolutionActivitySnapshot::activityId).size == size) {
        "Atividades duplicadas na leitura de recordes"
    }
    require(all { activity ->
        activity.workoutType != WorkoutType.UNKNOWN &&
            (activity.duration == null || !activity.duration.isNegative && !activity.duration.isZero) &&
            (activity.distanceMeters == null || activity.distanceMeters.isFinite() && activity.distanceMeters > 0.0) &&
            (activity.steps == null || activity.steps > 0L)
    }) {
        "Atividade inválida na leitura de recordes"
    }
}
