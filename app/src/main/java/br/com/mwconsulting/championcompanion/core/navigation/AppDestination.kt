package br.com.mwconsulting.championcompanion.core.navigation

import kotlinx.serialization.Serializable

/** Parameter-free destinations safe to place in navigation state. */
@Serializable
sealed class AppDestination {
    @Serializable
    data object Home : AppDestination()

    @Serializable
    data object Health : AppDestination()

    @Serializable
    data object Calendar : AppDestination()

    @Serializable
    data object Exercises : AppDestination()

    @Serializable
    data object Device : AppDestination()

    @Serializable
    data object Profile : AppDestination()

    @Serializable
    data object Settings : AppDestination()
}
