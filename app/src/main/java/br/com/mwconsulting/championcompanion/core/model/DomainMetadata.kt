package br.com.mwconsulting.championcompanion.core.model

enum class DataOrigin {
    WATCH_DAILY_SUMMARY,
    WATCH_D1_HISTORY,
    WATCH_D3_D4,
    MANUAL_ENTRY,
    APP_WORKOUT_SESSION,
    DERIVED,
}

enum class DataValidity {
    VALID,
    INVALID,
}

enum class MetricProvenance {
    WATCH_REPORTED,
    ESTIMATED,
    USER_REPORTED,
    CALIBRATED,
    DERIVED,
}

sealed interface DomainValue<out T> {
    data class Present<T>(val value: T) : DomainValue<T>

    data class Absent(val reason: AbsenceReason) : DomainValue<Nothing>
}

enum class AbsenceReason {
    NOT_PROVIDED_BY_SOURCE,
    NOT_APPLICABLE,
    NOT_YET_RESOLVED,
    UNKNOWN,
}
