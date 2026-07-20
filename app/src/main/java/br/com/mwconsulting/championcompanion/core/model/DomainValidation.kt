package br.com.mwconsulting.championcompanion.core.model

object DomainValidation {
    fun requiredText(value: String): DomainResult<String> =
        if (value.isBlank()) {
            DomainResult.Failure(DomainValidationError.EMPTY_VALUE)
        } else {
            DomainResult.Success(value)
        }

    fun positive(value: Long): DomainResult<Long> =
        if (value > 0) {
            DomainResult.Success(value)
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }

    fun withinRange(value: Int, range: IntRange): DomainResult<Int> =
        if (value in range) {
            DomainResult.Success(value)
        } else {
            DomainResult.Failure(DomainValidationError.INVALID_RANGE)
        }
}
