package br.com.mwconsulting.championcompanion.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.feature.calendar.CalendarScreen
import br.com.mwconsulting.championcompanion.feature.device.DeviceScreen
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesIntent
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesScreen
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesUiState
import br.com.mwconsulting.championcompanion.feature.exercises.ExercisesViewModel
import br.com.mwconsulting.championcompanion.feature.exercises.WatchWorkoutImportSurfaceState
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
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import java.util.UUID

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

private enum class ExitConfirmationTarget {
    Health,
    Exercises,
}

@Composable
fun ChampionNavigation(modifier: Modifier = Modifier) {
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    val healthViewModel = remember(activity) {
        ViewModelProvider(activity)[HealthViewModel::class.java]
    }
    val exercisesViewModel = remember(activity) {
        ViewModelProvider(activity)[ExercisesViewModel::class.java]
    }
    val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Home) }
    val currentDestination = backStack.lastOrNull()
    var additionalMenuExpanded by remember { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<AppDestination?>(null) }
    var pendingBackNavigation by remember { mutableStateOf(false) }
    var pendingExitConfirmationTarget by remember { mutableStateOf<ExitConfirmationTarget?>(null) }
    var animationDirection by remember { mutableStateOf(NavigationAnimationDirection.Forward) }
    val healthState by healthViewModel.state.collectAsState()
    val exercisesState by exercisesViewModel.state.collectAsState()

    fun navigateTo(destination: AppDestination) {
        animationDirection = NavigationAnimationDirection.Forward
        if (destination.isTopLevelDestination()) {
            if (currentDestination.isHealthDestination()) {
                backStack.removeHealthDestinations()
            }
            if (currentDestination.isExercisesDestination()) {
                backStack.removeExercisesDestinations()
            }
            backStack.navigateToTopLevel(destination)
        } else {
            backStack.navigateForward(destination)
        }
    }

    fun navigateBack() {
        animationDirection = NavigationAnimationDirection.Backward
        backStack.removeLastOrNull()
    }

    fun requestExitConfirmation(destination: AppDestination? = null, isBackNavigation: Boolean = false) {
        pendingDestination = destination
        pendingBackNavigation = isBackNavigation
    }

    fun requestNavigation(destination: AppDestination) {
        if (destination == currentDestination) return
        when {
            currentDestination.isHealthMeasurementSurface() && healthState.hasBlockingHealthOperation() -> {
                pendingExitConfirmationTarget = ExitConfirmationTarget.Health
                requestExitConfirmation(destination = destination)
            }

            currentDestination == AppDestination.Exercises && exercisesState.hasBlockingExerciseOperation() -> {
                pendingExitConfirmationTarget = ExitConfirmationTarget.Exercises
                requestExitConfirmation(destination = destination)
            }

            else -> navigateTo(destination)
        }
    }

    fun requestBackNavigation() {
        when {
            currentDestination.isHealthMeasurementSurface() && healthState.hasBlockingHealthOperation() -> {
                pendingExitConfirmationTarget = ExitConfirmationTarget.Health
                requestExitConfirmation(isBackNavigation = true)
            }

            currentDestination == AppDestination.Exercises && exercisesState.hasBlockingExerciseOperation() -> {
                pendingExitConfirmationTarget = ExitConfirmationTarget.Exercises
                requestExitConfirmation(isBackNavigation = true)
            }

            else -> navigateBack()
        }
    }

    if (pendingDestination != null || pendingBackNavigation) {
        val isD1ImportInFlight = healthState.d1ImportState.isD1ImportInFlight()
        val confirmationTarget = pendingExitConfirmationTarget
        AlertDialog(
            onDismissRequest = {
                pendingDestination = null
                pendingBackNavigation = false
                pendingExitConfirmationTarget = null
            },
            title = {
                Text(
                    when (confirmationTarget) {
                        ExitConfirmationTarget.Health -> if (isD1ImportInFlight) "Cancelar importação?" else "Cancelar medição?"
                        ExitConfirmationTarget.Exercises -> "Cancelar importação?"
                        null -> "Confirmar saída?"
                    },
                )
            },
            text = {
                Text(
                    when (confirmationTarget) {
                        ExitConfirmationTarget.Health -> if (isD1ImportInFlight) {
                            "Sair da tela Saúde agora cancelará a importação D1 em andamento."
                        } else {
                            "Sair da tela Saúde agora cancelará a medição em andamento sem salvar resultado parcial."
                        }

                        ExitConfirmationTarget.Exercises -> {
                            "Sair da tela Exercícios agora cancelará a importação de treinos em andamento, preservando os blocos já salvos."
                        }

                        null -> "A operação atual será interrompida."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val destination = pendingDestination
                        val shouldNavigateBack = pendingBackNavigation
                        val target = pendingExitConfirmationTarget
                        pendingDestination = null
                        pendingBackNavigation = false
                        pendingExitConfirmationTarget = null
                        when (target) {
                            ExitConfirmationTarget.Health -> {
                                if (isD1ImportInFlight) {
                                    healthViewModel.onIntent(HealthIntent.ConfirmExitD1Import)
                                } else {
                                    healthViewModel.onIntent(HealthIntent.ConfirmExitMeasurement)
                                }
                            }

                            ExitConfirmationTarget.Exercises -> exercisesViewModel.onIntent(ExercisesIntent.ConfirmExitImport)
                            null -> Unit
                        }
                        if (shouldNavigateBack) {
                            if (backStack.size > 1) {
                                navigateBack()
                            } else {
                                activity.finish()
                            }
                        } else if (destination != null) {
                            navigateTo(destination)
                        }
                    },
                ) {
                    Text(
                        when (confirmationTarget) {
                            ExitConfirmationTarget.Health -> "Sair da Saúde"
                            ExitConfirmationTarget.Exercises -> "Sair de Exercícios"
                            null -> "Sair"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDestination = null
                    pendingBackNavigation = false
                    pendingExitConfirmationTarget = null
                }) {
                    Text(
                        when (confirmationTarget) {
                            ExitConfirmationTarget.Health -> if (isD1ImportInFlight) "Continuar importando" else "Continuar medindo"
                            ExitConfirmationTarget.Exercises -> "Continuar importando"
                            null -> "Continuar"
                        },
                    )
                }
            },
        )
    }

    val shouldInterceptBack =
        pendingDestination == null &&
            !pendingBackNavigation &&
            (backStack.size > 1 ||
                (currentDestination == AppDestination.Exercises && exercisesState.hasBlockingExerciseOperation()) ||
                (currentDestination.isHealthMeasurementSurface() && healthState.hasBlockingHealthOperation()))

    BackHandler(enabled = shouldInterceptBack) {
        requestBackNavigation()
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
                        } else if (item.destination == AppDestination.Exercises) {
                            currentDestination.isExercisesDestination()
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
            currentDestination = currentDestination ?: AppDestination.Home,
            contentPadding = innerPadding,
            onBackRequested = ::requestBackNavigation,
            onNavigateTo = ::requestNavigation,
            exercisesViewModel = exercisesViewModel,
            animationDirection = animationDirection,
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

private fun ExercisesUiState.hasBlockingExerciseOperation(): Boolean =
    importState.isImportInFlight()

private fun WatchWorkoutImportSurfaceState.isImportInFlight(): Boolean = when (this) {
    WatchWorkoutImportSurfaceState.Idle,
    is WatchWorkoutImportSurfaceState.Completed,
    is WatchWorkoutImportSurfaceState.Partial,
    is WatchWorkoutImportSurfaceState.Empty,
    is WatchWorkoutImportSurfaceState.Failed,
    is WatchWorkoutImportSurfaceState.Cancelled,
    -> false

    is WatchWorkoutImportSurfaceState.Preparing,
    is WatchWorkoutImportSurfaceState.Connecting,
    is WatchWorkoutImportSurfaceState.ReadingIndex,
    is WatchWorkoutImportSurfaceState.ReadingBlocks,
    is WatchWorkoutImportSurfaceState.Parsing,
    is WatchWorkoutImportSurfaceState.Persisting,
    -> true
}

@Composable
private fun ChampionNavDisplay(
    currentDestination: AppDestination,
    contentPadding: PaddingValues,
    onBackRequested: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    exercisesViewModel: ExercisesViewModel,
    animationDirection: NavigationAnimationDirection,
) {
    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = { animationDirection.contentTransform() },
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        label = "champion_navigation_slide",
    ) { destination ->
        when (destination) {
            AppDestination.Home -> HomeScreen(
                onOpenDevice = { onNavigateTo(AppDestination.Device) },
                onOpenProfile = { onNavigateTo(AppDestination.Profile) },
                onOpenHeartRate = { onNavigateTo(AppDestination.HealthHeartRate) },
            )
            AppDestination.Health -> HealthScreen(
                onOpenHeartRate = { onNavigateTo(AppDestination.HealthHeartRate) },
                onOpenWeight = { onNavigateTo(AppDestination.HealthWeight) },
                onOpenSteps = { onNavigateTo(AppDestination.HealthSteps) },
                onOpenDistance = { onNavigateTo(AppDestination.HealthDistance) },
            )
            AppDestination.HealthHeartRate -> HeartRateDetailScreen(
                onNavigateBack = { onNavigateTo(AppDestination.Health) },
            )
            AppDestination.HealthWeight -> WeightDetailScreen(
                onNavigateBack = { onNavigateTo(AppDestination.Health) },
            )
            AppDestination.HealthSteps -> StepsDetailScreen(
                onNavigateBack = { onNavigateTo(AppDestination.Health) },
            )
            AppDestination.HealthDistance -> DistanceDetailScreen(
                onNavigateBack = { onNavigateTo(AppDestination.Health) },
            )
            AppDestination.Calendar -> CalendarScreen()
            AppDestination.Exercises -> ExercisesScreen(
                viewModel = exercisesViewModel,
                onOpenActivity = { activityId ->
                    onNavigateTo(AppDestination.ExerciseDetail(activityId.value.toString()))
                },
            )
            is AppDestination.ExerciseDetail -> ExercisesScreen(
                viewModel = exercisesViewModel,
                openedActivityId = destination.activityId.toWorkoutActivityId(),
                onNavigateBack = onBackRequested,
                onOpenActivity = { activityId ->
                    onNavigateTo(AppDestination.ExerciseDetail(activityId.value.toString()))
                },
            )
            AppDestination.Device -> DeviceScreen()
            AppDestination.Profile -> ProfileScreen()
            AppDestination.Settings -> SettingsScreen()
        }
    }
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

private fun MutableList<AppDestination>.removeExercisesDestinations() {
    removeAll { it.isExercisesDestination() }
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

private fun AppDestination?.isExercisesDestination(): Boolean = when (this) {
    AppDestination.Exercises,
    is AppDestination.ExerciseDetail,
    -> true

    else -> false
}

private fun AppDestination.isTopLevelDestination(): Boolean = when (this) {
    AppDestination.Home,
    AppDestination.Health,
    AppDestination.Calendar,
    AppDestination.Exercises,
    AppDestination.Device,
    AppDestination.Profile,
    AppDestination.Settings,
    -> true

    is AppDestination.ExerciseDetail,
    AppDestination.HealthHeartRate,
    AppDestination.HealthWeight,
    AppDestination.HealthSteps,
    AppDestination.HealthDistance,
    -> false
}

private enum class NavigationAnimationDirection {
    Forward,
    Backward,
}

private fun NavigationAnimationDirection.contentTransform(): ContentTransform {
    val durationMillis = 260
    return when (this) {
        NavigationAnimationDirection.Forward -> {
            slideInHorizontally(
                animationSpec = tween(durationMillis = durationMillis),
                initialOffsetX = { it },
            ) + fadeIn(animationSpec = tween(durationMillis = durationMillis)) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = durationMillis),
                    targetOffsetX = { -it / 4 },
                ) + fadeOut(animationSpec = tween(durationMillis = durationMillis))
        }

        NavigationAnimationDirection.Backward -> {
            slideInHorizontally(
                animationSpec = tween(durationMillis = durationMillis),
                initialOffsetX = { -it / 4 },
            ) + fadeIn(animationSpec = tween(durationMillis = durationMillis)) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = durationMillis),
                    targetOffsetX = { it },
                ) + fadeOut(animationSpec = tween(durationMillis = durationMillis))
        }
    }
}

private fun String.toWorkoutActivityId(): WorkoutActivityId = WorkoutActivityId(UUID.fromString(this))
