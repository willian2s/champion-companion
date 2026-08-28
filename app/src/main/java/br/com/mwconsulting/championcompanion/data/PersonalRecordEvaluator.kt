package br.com.mwconsulting.championcompanion.data

import br.com.mwconsulting.championcompanion.core.model.EvolutionActivitySnapshot
import br.com.mwconsulting.championcompanion.core.model.PersonalRecord
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordAchievement
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordMetric
import br.com.mwconsulting.championcompanion.core.model.PersonalRecordsReadSnapshot

/** Avaliação pura de conquistas contra o histórico local conhecido. */
object PersonalRecordEvaluator {
    /**
     * Avalia histórico já encapsulado como leitura all-history. A atividade
     * atual pode estar ou não incluída em [allHistorySnapshot]; seu ID sempre é
     * excluído da baseline das demais atividades.
     */
    fun evaluate(
        previousSnapshot: EvolutionActivitySnapshot?,
        newSnapshot: EvolutionActivitySnapshot?,
        allHistorySnapshot: PersonalRecordsReadSnapshot,
    ): List<PersonalRecordAchievement> = evaluate(
        previousSnapshot = previousSnapshot,
        newSnapshot = newSnapshot,
        otherEligibleActivities = allHistorySnapshot.activities,
    )

    /**
     * Avalia uma atividade persistida contra atividades elegíveis all-history.
     * [previousSnapshot] nulo representa atividade anteriormente inelegível.
     */
    fun evaluate(
        previousSnapshot: EvolutionActivitySnapshot?,
        newSnapshot: EvolutionActivitySnapshot?,
        otherEligibleActivities: List<EvolutionActivitySnapshot>,
    ): List<PersonalRecordAchievement> {
        otherEligibleActivities.requireValidPersonalRecordActivities()
        val current = newSnapshot ?: return emptyList()
        listOf(current).requireValidPersonalRecordActivities()
        previousSnapshot?.let { previous ->
            require(previous.activityId == current.activityId) {
                "Snapshots de recorde devem pertencer à mesma atividade"
            }
            listOf(previous).requireValidPersonalRecordActivities()
        }

        return PERSONAL_RECORD_SCOPES
            .filter { scope -> current.workoutType in scope.workoutTypes }
            .mapNotNull { scope ->
                evaluateScope(
                    metric = scope.metric,
                    previousSnapshot = previousSnapshot,
                    newSnapshot = current,
                    otherEligibleActivities = otherEligibleActivities,
                )
            }
    }

    private fun evaluateScope(
        metric: PersonalRecordMetric,
        previousSnapshot: EvolutionActivitySnapshot?,
        newSnapshot: EvolutionActivitySnapshot,
        otherEligibleActivities: List<EvolutionActivitySnapshot>,
    ): PersonalRecordAchievement? {
        val newValue = newSnapshot.personalRecordValue(metric) ?: return null
        val baselineOther = otherEligibleActivities.asSequence()
            .filter { activity ->
                activity.activityId != newSnapshot.activityId &&
                    activity.workoutType == newSnapshot.workoutType
            }
            .mapNotNull { activity -> activity.personalRecordValue(metric) }
            .maxOrNull()
        val oldEligibleValue = previousSnapshot
            ?.takeIf { previous ->
                previous.activityId == newSnapshot.activityId &&
                    previous.workoutType == newSnapshot.workoutType
            }
            ?.personalRecordValue(metric)
        val previousBest = listOfNotNull(baselineOther, oldEligibleValue).maxOrNull()

        if (previousBest != null && newValue <= previousBest) return null

        val difference = previousBest?.let { best ->
            (newValue - best).takeIf(Double::isFinite)
                ?: throw IllegalArgumentException("Diferença de recorde excede limite numérico")
        }
        return PersonalRecordAchievement(
            record = PersonalRecord(
                metric = metric,
                workoutType = newSnapshot.workoutType,
                activityId = newSnapshot.activityId,
                value = newValue,
                achievedAt = newSnapshot.startedAt,
            ),
            previousBest = previousBest,
            difference = difference,
        )
    }
}
