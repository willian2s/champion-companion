package br.com.mwconsulting.championcompanion.core.location

import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessInput
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessPolicy

/** Fronteira Android-free para política de prontidão baseada primeiro no fix. */
object GpsReadinessEvaluator {
    fun evaluate(
        input: GpsReadinessInput,
        policy: GpsReadinessPolicy = GpsReadinessPolicy(),
    ): GpsReadiness = GpsReadiness.evaluate(input, policy)
}
