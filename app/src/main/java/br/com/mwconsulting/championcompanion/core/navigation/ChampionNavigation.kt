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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.mwconsulting.championcompanion.feature.calendar.CalendarScreen
import br.com.mwconsulting.championcompanion.feature.device.DeviceScreen
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesScreen
import br.com.mwconsulting.championcompanion.feature.health.HealthIntent
import br.com.mwconsulting.championcompanion.feature.health.HealthScreen
import br.com.mwconsulting.championcompanion.feature.health.HealthSurfaceState
import br.com.mwconsulting.championcompanion.feature.health.HealthViewModel
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
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    val healthViewModel = remember(activity) {
        ViewModelProvider(activity)[HealthViewModel::class.java]
    }
    val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Home) }
    val currentDestination = backStack.lastOrNull()
    var additionalMenuExpanded by remember { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<AppDestination?>(null) }
    val healthState by healthViewModel.state.collectAsState()

    fun requestNavigation(destination: AppDestination) {
        if (
            currentDestination == AppDestination.Health &&
            destination != AppDestination.Health &&
            healthState.surfaceState.isMeasurementInFlight()
        ) {
            pendingDestination = destination
        } else {
            backStack.navigateToTopLevel(destination)
        }
    }

    pendingDestination?.let { destination ->
        AlertDialog(
            onDismissRequest = { pendingDestination = null },
            title = { Text("Cancelar medição?") },
            text = { Text("Sair da tela Saúde agora cancelará a medição em andamento sem salvar resultado parcial.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDestination = null
                        healthViewModel.onIntent(HealthIntent.ConfirmExitMeasurement)
                        backStack.navigateToTopLevel(destination)
                    },
                ) {
                    Text("Sair da Saúde")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDestination = null }) {
                    Text("Continuar medindo")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val navigationBarScope = this
                primaryItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination == item.destination,
                        onClick = { requestNavigation(item.destination) },
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
                                    requestNavigation(item.destination)
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

private fun HealthSurfaceState.isMeasurementInFlight(): Boolean = when (this) {
    is HealthSurfaceState.Preparing,
    is HealthSurfaceState.Connecting,
    is HealthSurfaceState.Starting,
    is HealthSurfaceState.Measuring,
    is HealthSurfaceState.Stopping,
    -> true

    HealthSurfaceState.Idle,
    is HealthSurfaceState.Completed,
    is HealthSurfaceState.Cancelled,
    is HealthSurfaceState.Failed,
    -> false
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
                        onOpenProfile = { backStack.navigateToTopLevel(AppDestination.Profile) },
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
