package br.com.mwconsulting.championcompanion.core.bluetooth

data class ChampionRetryDecision(
    val shouldRetry: Boolean,
    val nextAttempt: Int,
)

fun ChampionRetryPolicy.retryDecisionFor(
    currentAttempt: Int,
    error: ChampionBleError,
): ChampionRetryDecision = when {
    error is ChampionBleError.OperationCancelled -> ChampionRetryDecision(false, currentAttempt)
    error is ChampionBleError.QueueClosed -> ChampionRetryDecision(false, currentAttempt)
    error is ChampionBleError.StaleSession -> ChampionRetryDecision(false, currentAttempt)
    error is ChampionBleError.CleanupInProgress -> ChampionRetryDecision(false, currentAttempt)
    this is ChampionRetryPolicy.None -> ChampionRetryDecision(false, currentAttempt)
    this is ChampionRetryPolicy.Limited && currentAttempt < maxAttempts ->
        ChampionRetryDecision(true, currentAttempt + 1)
    else -> ChampionRetryDecision(false, currentAttempt)
}
