package br.com.mwconsulting.championcompanion.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.mwconsulting.championcompanion.feature.calendar.CalendarScreen
import br.com.mwconsulting.championcompanion.feature.device.DeviceScreen
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesScreen
import br.com.mwconsulting.championcompanion.feature.health.HealthScreen
import br.com.mwconsulting.championcompanion.feature.home.HomeScreen
import br.com.mwconsulting.championcompanion.feature.profile.ProfileScreen
import br.com.mwconsulting.championcompanion.feature.settings.SettingsScreen

private data class NavigationItem(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
)

private data class AdditionalNavigationItem(
    val destination: AppDestination,
    val label: String,
)

private val primaryItems = listOf(
    NavigationItem(AppDestination.Home, "Início", Icons.Filled.Home),
    NavigationItem(AppDestination.Health, "Saúde", Icons.Filled.Favorite),
    NavigationItem(AppDestination.Calendar, "Calendário", Icons.Filled.CalendarMonth),
    NavigationItem(AppDestination.Exercises, "Exercícios", Icons.Filled.FitnessCenter),
)

private val additionalItems = listOf(
    AdditionalNavigationItem(AppDestination.Device, "Dispositivo"),
    AdditionalNavigationItem(AppDestination.Profile, "Perfil"),
    AdditionalNavigationItem(AppDestination.Settings, "Configurações"),
)

@Composable
fun ChampionNavigation(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Home) }
    val currentDestination = backStack.lastOrNull()
    var additionalMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val navigationBarScope = this
                primaryItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination == item.destination,
                        onClick = { backStack.navigateToTopLevel(item.destination) },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    navigationBarScope.NavigationBarItem(
                        modifier = Modifier.fillMaxWidth(),
                        selected = additionalItems.any { currentDestination == it.destination },
                        onClick = { additionalMenuExpanded = true },
                        icon = { Icon(imageVector = Icons.Filled.Menu, contentDescription = "Mais") },
                        label = { Text("Mais") },
                    )
                    DropdownMenu(
                        expanded = additionalMenuExpanded,
                        onDismissRequest = { additionalMenuExpanded = false },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        additionalItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    additionalMenuExpanded = false
                                    backStack.navigateToTopLevel(item.destination)
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        ChampionNavDisplay(backStack = backStack, contentPadding = innerPadding)
    }
}

@Composable
private fun ChampionNavDisplay(
    backStack: MutableList<AppDestination>,
    contentPadding: PaddingValues,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        entryProvider = { destination ->
            NavEntry(destination) {
                when (destination) {
                    AppDestination.Home -> HomeScreen(
                        onOpenDevice = { backStack.navigateToTopLevel(AppDestination.Device) },
                    )
                    AppDestination.Health -> HealthScreen()
                    AppDestination.Calendar -> CalendarScreen()
                    AppDestination.Exercises -> ExercisesScreen()
                    AppDestination.Device -> DeviceScreen()
                    AppDestination.Profile -> ProfileScreen()
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        },
    )
}

private fun MutableList<AppDestination>.navigateToTopLevel(destination: AppDestination) {
    if (lastOrNull() == destination) return

    removeAll { it == destination }
    add(destination)
}
