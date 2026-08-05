package br.com.mwconsulting.championcompanion.core.model

/** Small UI and future integration choices only; domain records remain in Room. */
data class AppPreferences(
    val theme: ThemePreference = ThemePreference.FOLLOW_SYSTEM,
    val useDynamicColors: Boolean = true,
    val healthSyncEnabled: Boolean = false,
    val diagnosticCaptureEnabled: Boolean = false,
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val workoutCountdownSeconds: Int = DEFAULT_WORKOUT_COUNTDOWN_SECONDS,
)

enum class ThemePreference {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
}
