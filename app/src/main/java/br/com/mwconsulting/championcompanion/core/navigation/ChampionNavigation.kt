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
import br.com.mwconsulting.championcompanion.feature.health.DistanceDetailScreen
import br.com.mwconsulting.championcompanion.feature.health.D1HeartRateImportSurfaceState
import br.com.mwconsulting.championcompanion.feature.health.HealthScreen
import br.com.mwconsulting.championcompanion.feature.health.HealthUiState
import br.com.mwconsulting.championcompanion.feature.health.HeartRateDetailScreen
import br.com.mwconsulting.championcompanion.feature.health.HealthSurfaceState
import br.com.mwconsulting.championcompanion.feature.health.HealthViewModel
import br.com.mwconsulting.championcompanion.feature.health.StepsDetailScreen
import br.com.mwconsulting.championcompanion.feature.health.WeightDetailScreen
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
    var pendingBackNavigation by remember { mutableStateOf(false) }
    val healthState by healthViewModel.state.collectAsState()

    fun navigateTo(destination: AppDestination) {
        if (destination.isTopLevelDestination()) {
            if (currentDestination.isHealthDestination()) {
                backStack.removeHealthDestinations()
            }
            backStack.navigateToTopLevel(destination)
        } else {
            backStack.navigateForward(destination)
        }
    }

    fun navigateBack() {
        backStack.removeLastOrNull()
    }

    fun requestExitConfirmation(destination: AppDestination? = null, isBackNavigation: Boolean = false) {
        pendingDestination = destination
        pendingBackNavigation = isBackNavigation
    }

    fun requestNavigation(destination: AppDestination) {
        if (
            currentDestination.isHealthMeasurementSurface() &&
            destination != currentDestination &&
            healthState.hasBlockingHealthOperation()
        ) {
            requestExitConfirmation(destination = destination)
        } else {
            navigateTo(destination)
        }
    }

    fun requestBackNavigation() {
        if (currentDestination.isHealthMeasurementSurface() && healthState.hasBlockingHealthOperation()) {
            requestExitConfirmation(isBackNavigation = true)
        } else {
            navigateBack()
        }
    }

    if (pendingDestination != null || pendingBackNavigation) {
        val isD1ImportInFlight = healthState.d1ImportState.isD1ImportInFlight()
        AlertDialog(
            onDismissRequest = {
                pendingDestination = null
                pendingBackNavigation = false
            },
            title = { Text(if (isD1ImportInFlight) "Cancelar importação?" else "Cancelar medição?") },
            text = {
                Text(
                    if (isD1ImportInFlight) {
                        "Sair da tela Saúde agora cancelará a importação D1 em andamento."
                    } else {
                        "Sair da tela Saúde agora cancelará a medição em andamento sem salvar resultado parcial."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val destination = pendingDestination
                        val shouldNavigateBack = pendingBackNavigation
                        pendingDestination = null
                        pendingBackNavigation = false
                        if (isD1ImportInFlight) {
                            healthViewModel.onIntent(HealthIntent.ConfirmExitD1Import)
                        } else {
                            healthViewModel.onIntent(HealthIntent.ConfirmExitMeasurement)
                        }
                        if (shouldNavigateBack) {
                            navigateBack()
                        } else if (destination != null) {
                            navigateTo(destination)
                        }
                    },
                ) {
                    Text("Sair da Saúde")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDestination = null
                    pendingBackNavigation = false
                }) {
                    Text(if (isD1ImportInFlight) "Continuar importando" else "Continuar medindo")
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
                        selected = if (item.destination == AppDestination.Health) {
                            currentDestination.isHealthDestination()
                        } else {
                            currentDestination == item.destination
                        },
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
        ChampionNavDisplay(
            backStack = backStack,
            contentPadding = innerPadding,
            onBackRequested = ::requestBackNavigation,
        )
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

private fun D1HeartRateImportSurfaceState.isD1ImportInFlight(): Boolean = when (this) {
    is D1HeartRateImportSurfaceState.Preparing,
    is D1HeartRateImportSurfaceState.Connecting,
    is D1HeartRateImportSurfaceState.Reading,
    is D1HeartRateImportSurfaceState.Persisting,
    -> true

    D1HeartRateImportSurfaceState.Idle,
    is D1HeartRateImportSurfaceState.Completed,
    is D1HeartRateImportSurfaceState.CompletedWithGaps,
    is D1HeartRateImportSurfaceState.Partial,
    is D1HeartRateImportSurfaceState.Failed,
    is D1HeartRateImportSurfaceState.Cancelled,
    -> false
}

private fun HealthUiState.hasBlockingHealthOperation(): Boolean =
    surfaceState.isMeasurementInFlight() || d1ImportState.isD1ImportInFlight()

@Composable
private fun ChampionNavDisplay(
    backStack: MutableList<AppDestination>,
    contentPadding: PaddingValues,
    onBackRequested: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBackRequested,
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
                    AppDestination.Health -> HealthScreen(
                        onOpenHeartRate = { backStack.navigateForward(AppDestination.HealthHeartRate) },
                        onOpenWeight = { backStack.navigateForward(AppDestination.HealthWeight) },
                        onOpenSteps = { backStack.navigateForward(AppDestination.HealthSteps) },
                        onOpenDistance = { backStack.navigateForward(AppDestination.HealthDistance) },
                    )
                    AppDestination.HealthHeartRate -> HeartRateDetailScreen(
                        onNavigateBack = onBackRequested,
                    )
                    AppDestination.HealthWeight -> WeightDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                    AppDestination.HealthSteps -> StepsDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                    AppDestination.HealthDistance -> DistanceDetailScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
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

private fun MutableList<AppDestination>.navigateForward(destination: AppDestination) {
    if (lastOrNull() == destination) return
    add(destination)
}

private fun MutableList<AppDestination>.removeHealthDestinations() {
    removeAll { it.isHealthDestination() }
}

private fun AppDestination?.isHealthDestination(): Boolean = when (this) {
    AppDestination.Health,
    AppDestination.HealthHeartRate,
    AppDestination.HealthWeight,
    AppDestination.HealthSteps,
    AppDestination.HealthDistance,
    -> true

    else -> false
}

private fun AppDestination?.isHealthMeasurementSurface(): Boolean = this == AppDestination.HealthHeartRate

private fun AppDestination.isTopLevelDestination(): Boolean = when (this) {
    AppDestination.Home,
    AppDestination.Health,
    AppDestination.Calendar,
    AppDestination.Exercises,
    AppDestination.Device,
    AppDestination.Profile,
    AppDestination.Settings,
    -> true

    AppDestination.HealthHeartRate,
    AppDestination.HealthWeight,
    AppDestination.HealthSteps,
    AppDestination.HealthDistance,
    -> false
}
