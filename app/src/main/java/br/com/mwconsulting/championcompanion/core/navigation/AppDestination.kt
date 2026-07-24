package br.com.mwconsulting.championcompanion.core.navigation

import java.util.UUID
import kotlinx.serialization.Serializable

/** Parameter-free destinations safe to place in navigation state. */
@Serializable
sealed class AppDestination {
    @Serializable
    data object Home : AppDestination() {
        override val route = "home"
    }

    @Serializable
    data object Health : AppDestination() {
        override val route = "health"
    }

    @Serializable
    data object HealthHeartRate : AppDestination() {
        override val route = "health/heart-rate"
    }

    @Serializable
    data object HealthWeight : AppDestination() {
        override val route = "health/weight"
    }

    @Serializable
    data object HealthSteps : AppDestination() {
        override val route = "health/steps"
    }

    @Serializable
    data object HealthDistance : AppDestination() {
        override val route = "health/distance"
    }

    @Serializable
    data object Calendar : AppDestination() {
        override val route = "calendar"
    }

    @Serializable
    data object Exercises : AppDestination() {
        override val route = "exercises"
    }

    @Serializable
    data class ExerciseDetail(val activityId: String) : AppDestination() {
        override val route = "exercises/$activityId"

        init {
            UUID.fromString(activityId)
        }
    }

    @Serializable
    data object Device : AppDestination() {
        override val route = "device"
    }

    @Serializable
    data object Profile : AppDestination() {
        override val route = "profile"
    }

    @Serializable
    data object Settings : AppDestination() {
        override val route = "settings"
    }

    /** Stable, parameter-free route identifier. */
    abstract val route: String
}
