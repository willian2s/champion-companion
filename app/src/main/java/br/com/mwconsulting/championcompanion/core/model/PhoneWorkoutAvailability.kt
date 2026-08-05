package br.com.mwconsulting.championcompanion.core.model

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

enum class PhoneWorkoutAvailability {
    DISCONTINUED,
}

/**
 * Guard único para impedir novas operações de treino iniciadas pelo celular
 * sem remover leitura histórica ou dados persistidos.
 */
object PhoneWorkoutPolicy {
    val availability: PhoneWorkoutAvailability = PhoneWorkoutAvailability.DISCONTINUED

    const val DISCONTINUED_MESSAGE =
        "Treinos iniciados pelo celular foram descontinuados. Importe treinos gravados no relógio pela tela Exercícios."
    const val NON_RESUMABLE_MESSAGE =
        "Esta sessão foi preservada, mas treinos iniciados pelo celular não podem mais ser retomados."
    const val FINISHING_NOT_DISCARDABLE_MESSAGE =
        "Esta sessão em finalização foi preservada. O contrato atual não permite descartá-la."
    const val SHUTDOWN_UNCONFIRMED_MESSAGE =
        "Não foi possível confirmar o encerramento seguro do runtime antigo. Tente novamente."

    private val shutdownGeneration = AtomicLong(0L)
    private val shutdownConfirmedGeneration = MutableStateFlow(0L)

    fun isDiscontinued(): Boolean = availability == PhoneWorkoutAvailability.DISCONTINUED

    fun blocksNewSession(): Boolean = isDiscontinued()

    /** Todas [AppWorkoutSession] são gravações operacionais iniciadas pelo celular. */
    fun blocks(session: AppWorkoutSession): Boolean = isDiscontinued()

    fun requestShutdown(): Long = shutdownGeneration.incrementAndGet()

    fun currentShutdownGeneration(): Long = shutdownGeneration.get()

    fun confirmShutdown(generation: Long) {
        if (generation > 0L && shutdownGeneration.get() == generation) {
            shutdownConfirmedGeneration.value = generation
        }
    }

    suspend fun awaitShutdown(
        generation: Long = currentShutdownGeneration(),
        timeoutMillis: Long = 2_000L,
    ): Boolean =
        if (generation <= 0L) {
            false
        } else {
        withTimeoutOrNull(timeoutMillis) {
            shutdownConfirmedGeneration.first { it == generation }
            true
        } ?: false
        }
}
