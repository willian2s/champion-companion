package br.com.mwconsulting.championcompanion.core.error

/** Infrastructure failures allowed to cross into the presentation layer. */
sealed interface InfrastructureError {
    data object NetworkUnavailable : InfrastructureError
    data object ServiceUnavailable : InfrastructureError
    data object Unexpected : InfrastructureError
}

/** Message safe to expose in UI. It never includes a cause or internal detail. */
data class UiError(val message: String)

fun InfrastructureError.toUiError(): UiError = when (this) {
    InfrastructureError.NetworkUnavailable -> UiError("Não foi possível conectar. Tente novamente.")
    InfrastructureError.ServiceUnavailable -> UiError("Serviço indisponível no momento. Tente novamente.")
    InfrastructureError.Unexpected -> UiError("Não foi possível concluir esta ação. Tente novamente.")
}
