package br.com.mwconsulting.championcompanion.core.model

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>

    data class Failure(val error: DomainValidationError) : DomainResult<Nothing>
}

enum class DomainValidationError {
    EMPTY_VALUE,
    INVALID_IDENTIFIER,
    INVALID_RANGE,
    INVALID_TEMPORAL_ORDER,
}

fun <T> DomainResult<T>.valueOrNull(): T? = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> null
}
