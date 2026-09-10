package br.com.mwconsulting.championcompanion.feature.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSummaryCard
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens
import br.com.mwconsulting.championcompanion.feature.device.DeviceManagementIntent
import br.com.mwconsulting.championcompanion.feature.device.DeviceManagementViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDevice: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHeartRate: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenExercises: () -> Unit,
    homeViewModel: HomeViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[HomeViewModel::class.java]
        }
    },
    deviceManagementViewModel: DeviceManagementViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[DeviceManagementViewModel::class.java]
        }
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by homeViewModel.state.collectAsState()

    LaunchedEffect(homeViewModel, deviceManagementViewModel) {
        homeViewModel.onIntent(HomeIntent.Load)
        deviceManagementViewModel.onIntent(DeviceManagementIntent.Load)
        deviceManagementViewModel.onIntent(DeviceManagementIntent.RefreshPrerequisites)
    }

    DisposableEffect(lifecycleOwner, homeViewModel, deviceManagementViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    homeViewModel.onIntent(HomeIntent.Load)
                    deviceManagementViewModel.onIntent(DeviceManagementIntent.Load)
                }

                Lifecycle.Event.ON_RESUME -> {
                    homeViewModel.onIntent(HomeIntent.Load)
                    deviceManagementViewModel.onIntent(DeviceManagementIntent.RefreshPrerequisites)
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            deviceManagementViewModel.onIntent(DeviceManagementIntent.ExitVisibleScreen)
        }
    }

    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            PullToRefreshBox(
                isRefreshing = state.refresh.isRefreshing,
                onRefresh = { homeViewModel.onIntent(HomeIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Atualizar dados do relógio") {
                                    homeViewModel.onIntent(HomeIntent.Refresh)
                                    true
                                },
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compact),
                    ) {
                        HomeTopBar(
                            title = "Hoje",
                            status = state.watch.toHomeWatchStatus(),
                            onOpenDevice = onOpenDevice,
                            onOpenProfile = onOpenProfile,
                        )

                        if (state.refresh.isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Sincronizando dados do relógio"
                                        liveRegion = LiveRegionMode.Polite
                                    },
                            )
                        }

                        HomeRefreshStatus(
                            refresh = state.refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HomeSurface(
                            state = state,
                            onOpenDevice = onOpenDevice,
                            onOpenHeartRate = onOpenHeartRate,
                            onOpenSteps = onOpenSteps,
                            onOpenDistance = onOpenDistance,
                            onOpenExercises = onOpenExercises,
                            onRetry = { homeViewModel.onIntent(HomeIntent.Retry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSurface(
    state: HomeUiState,
    onOpenDevice: () -> Unit,
    onOpenHeartRate: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenExercises: () -> Unit,
    onRetry: () -> Unit,
) {
    val hasSummary = state.summary != null
    val summary = state.summary ?: HomeDailySummaryModel(
        steps = 0,
        dailyGoal = 0,
        progress = 0f,
        estimatedDistanceMeters = 0.0,
        estimatedCaloriesKilocalories = null,
        sourceLabel = "",
        updatedAt = Instant.EPOCH,
    )
    HomeSummaryContent(
        summary = summary,
        latestHeartRate = state.latestHeartRate.takeIf { hasSummary },
        watch = state.watch,
        onOpenHeartRate = onOpenHeartRate,
        onOpenSteps = onOpenSteps,
        onOpenDistance = onOpenDistance,
        onOpenExercises = onOpenExercises,
        modifier = Modifier.fillMaxWidth(),
    )

    HomePendingContent(
        workoutPendingBanner = state.workoutPendingBanner,
        onOpenExercises = onOpenExercises,
        modifier = Modifier.fillMaxWidth(),
    )

    when (state.surfaceState) {
        HomeSurfaceState.Loading,
        HomeSurfaceState.Empty,
        -> Unit

        HomeSurfaceState.DeviceRequired -> ErrorState(
            title = "Associe um relógio",
            message = "Nenhum relógio principal foi associado. Abra Dispositivo para concluir a configuração.",
            actionLabel = "Abrir Dispositivo",
            onAction = onOpenDevice,
            modifier = Modifier.fillMaxWidth(),
        )

        HomeSurfaceState.PermissionsRequired -> ErrorState(
            title = "Permissões necessárias",
            message = "Permissões de Bluetooth são necessárias para atualizar o resumo diário.",
            actionLabel = "Tentar novamente",
            onAction = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )

        HomeSurfaceState.BluetoothDisabled -> ErrorState(
            title = "Bluetooth desligado",
            message = "Ative o Bluetooth e depois puxe para baixo para tentar novamente.",
            actionLabel = "Tentar novamente",
            onAction = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )

        HomeSurfaceState.RefreshFailed -> state.refresh.error?.let { error ->
            ErrorState(
                title = "Não foi possível atualizar os dados",
                message = error.message,
                actionLabel = if (error.isRetryable) "Tentar novamente" else null,
                onAction = if (error.isRetryable) onRetry else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HomeSurfaceState.Ready -> Unit
    }

}

@Composable
private fun homeCardPalette(): HomeCardPalette {
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    val blue = HomeCardColors(colors.primaryContainer, colors.onPrimaryContainer)
    return HomeCardPalette(
        steps = blue,
        exercises = blue,
        distance = blue,
        heartRate = blue,
        pending = blue,
        incompleteStepsOverlay = if (darkTheme) colors.onPrimary else colors.onPrimaryContainer,
    )
}

private data class HomeCardColors(
    val container: Color,
    val content: Color,
)

private data class HomeCardPalette(
    val steps: HomeCardColors,
    val exercises: HomeCardColors,
    val distance: HomeCardColors,
    val heartRate: HomeCardColors,
    val pending: HomeCardColors,
    val incompleteStepsOverlay: Color,
)

@Composable
private fun HomeSummaryContent(
    summary: HomeDailySummaryModel,
    latestHeartRate: HomeLatestHeartRateModel?,
    watch: HomeWatchSnapshot,
    onOpenHeartRate: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalChampionRedesignTokens.current
    val palette = homeCardPalette()
    val distance = summary.estimatedDistanceMeters ?: 0.0
    val lineHeight = 56.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cardHeight = lineHeight * 3 + tokens.spacing.compact * 2

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compact),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compact),
        ) {
            HomeDailySummaryCard(
                summary = summary,
                onOpenSteps = onOpenSteps,
                modifier = Modifier
                    .weight(1f),
                cardHeight = cardHeight,
                containerColor = palette.steps.container,
                contentColor = palette.steps.content,
                incompleteOverlay = palette.incompleteStepsOverlay,
            )
            HomeMetricsColumn(
                latestHeartRate = latestHeartRate,
                distance = distance,
                onOpenExercises = onOpenExercises,
                onOpenHeartRate = onOpenHeartRate,
                onOpenDistance = onOpenDistance,
                cardHeight = cardHeight,
                lineHeight = lineHeight,
                exerciseColors = palette.exercises,
                distanceColors = palette.distance,
                heartRateColors = palette.heartRate,
                modifier = Modifier.weight(1f),
            )
        }

        HomeMetadataFooter(
            lastImportedAt = watch.lastImportedAt,
        )
    }
}

@Composable
private fun HomeDailySummaryCard(
    summary: HomeDailySummaryModel,
    onOpenSteps: () -> Unit,
    cardHeight: Dp,
    containerColor: Color,
    contentColor: Color,
    incompleteOverlay: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalChampionRedesignTokens.current
    Card(
        onClick = onOpenSteps,
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "Passos. ${formatSteps(summary.steps)}. Meta ${formatGoal(summary.dailyGoal)}. ${formatPercent(summary.progress)} da meta de passos"
            },
        shape = tokens.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(tokens.shapes.medium),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((1f - summary.progress).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(incompleteOverlay.copy(alpha = 0.12f)),
            )
            Column(modifier = Modifier.padding(tokens.spacing.card)) {
                Text(
                    text = "Passos",
                    modifier = Modifier.semantics { heading() },
                    color = contentColor,
                    style = tokens.typography.labelLarge,
                )
                Text(
                    text = formatSteps(summary.steps),
                    color = contentColor,
                    style = tokens.typography.headlineSmall,
                )
                Text(
                    text = "Meta ${formatGoal(summary.dailyGoal)}",
                    color = contentColor,
                    style = tokens.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeMetricsColumn(
    latestHeartRate: HomeLatestHeartRateModel?,
    distance: Double?,
    onOpenExercises: () -> Unit,
    onOpenHeartRate: () -> Unit,
    onOpenDistance: () -> Unit,
    cardHeight: Dp,
    lineHeight: Dp,
    exerciseColors: HomeCardColors,
    distanceColors: HomeCardColors,
    heartRateColors: HomeCardColors,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalChampionRedesignTokens.current
    val cardSpacing = tokens.spacing.compact
    Column(
        modifier = modifier.height(cardHeight),
        verticalArrangement = Arrangement.spacedBy(cardSpacing),
    ) {
        HomeCompactSummaryLine(
            title = "Exercícios",
            value = "Abrir atividades",
            onClick = onOpenExercises,
            containerColor = exerciseColors.container,
            contentColor = exerciseColors.content,
            modifier = Modifier.height(lineHeight),
        )
        distance?.let {
            HomeCompactSummaryLine(
                title = "Distância",
                value = formatDistance(it),
                onClick = onOpenDistance,
                containerColor = distanceColors.container,
                contentColor = distanceColors.content,
                modifier = Modifier.height(lineHeight),
            )
        }
        latestHeartRate?.let {
            HomeCompactSummaryLine(
                title = "Freq. cardíaca",
                value = "${it.bpm} BPM • ${formatTime(it.measuredAt)}",
                onClick = onOpenHeartRate,
                containerColor = heartRateColors.container,
                contentColor = heartRateColors.content,
                modifier = Modifier.height(lineHeight),
            )
        }
        if (latestHeartRate == null) {
            HomeCompactSummaryLine(
                title = "Freq. cardíaca",
                value = "0 BPM",
                onClick = onOpenHeartRate,
                containerColor = heartRateColors.container,
                contentColor = heartRateColors.content,
                modifier = Modifier.height(lineHeight),
            )
        }
    }
}

@Composable
private fun HomePendingContent(
    workoutPendingBanner: HomeWorkoutPendingBanner?,
    onOpenExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reviewPending = workoutPendingBanner?.takeIf { it.pendingReviewCount > 0 } ?: return
    val pendingColors = homeCardPalette().pending
    val title = "Revisão pendente"
    val value = "${reviewPending.pendingReviewCount} pendência(s)"
    val supportingText = "${reviewPending.pendingReviewCount} treino(s) aguardando revisão local antes da sincronização futura."
    ChampionRedesignSummaryCard(
        title = title,
        value = value,
        supportingText = supportingText,
        containerColor = pendingColors.container,
        contentColor = pendingColors.content,
        modifier = modifier.semantics {
            contentDescription = "$title. $supportingText. Toque para abrir Exercícios."
        },
        onClick = onOpenExercises,
    )
}

@Composable
private fun HomeCompactSummaryLine(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalChampionRedesignTokens.current
    val cardModifier = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {
            contentDescription = "$title. $value"
        }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = contentColor,
                    style = tokens.typography.labelMedium,
                )
                Text(
                    text = value,
                    color = contentColor,
                    style = tokens.typography.bodyLarge,
                )
            }
        }
    }

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            shape = tokens.shapes.small,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            content()
        }
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = tokens.shapes.small,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeRefreshStatus(
    refresh: HomeRefreshState,
    modifier: Modifier = Modifier,
) {
    val message = when {
        refresh.isRefreshing -> refresh.refreshStage.toStatusMessage()
        refresh.error != null -> "Não foi possível atualizar os dados."
        else -> null
    } ?: return

    Text(
        text = message,
        modifier = modifier.semantics {
            contentDescription = message
            liveRegion = LiveRegionMode.Polite
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun HomeMetadataFooter(
    lastImportedAt: Instant?,
) {
    Text(
        text = "Última sincronização: ${lastImportedAt?.let(::formatDateTime) ?: "Sem sincronização"}",
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun HomeTopBar(
    title: String,
    status: HomeWatchStatus,
    onOpenDevice: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    ChampionRedesignSurfaceHeader(
        title = title,
        supportingText = "Relógio: ${status.label}",
        actionsContent = {
            ChampionRedesignHeaderAction(
                contentDescription = "Abrir Dispositivo. Status do relógio: ${status.label}",
                onClick = onOpenDevice,
            ) {
                Icon(imageVector = Icons.Filled.Watch, contentDescription = null)
            }
            ChampionRedesignHeaderAction(
                contentDescription = "Abrir Perfil",
                onClick = onOpenProfile,
            ) {
                Icon(imageVector = Icons.Filled.PersonOutline, contentDescription = null)
            }
        },
    )
}

private enum class HomeWatchStatus(
    val label: String,
) {
    Connected("Conectado"),
    Connecting("Conectando"),
    Pending("Pendente"),
    BluetoothDisabled("Bluetooth desligado"),
    PermissionsRequired("Permissões pendentes"),
}

private fun HomeWatchSnapshot.toHomeWatchStatus(): HomeWatchStatus = when (connectionState) {
    HomeWatchConnectionState.Connected -> HomeWatchStatus.Connected
    HomeWatchConnectionState.Connecting -> HomeWatchStatus.Connecting
    HomeWatchConnectionState.BluetoothDisabled -> HomeWatchStatus.BluetoothDisabled
    HomeWatchConnectionState.PermissionsRequired -> HomeWatchStatus.PermissionsRequired
    HomeWatchConnectionState.Disconnected,
    HomeWatchConnectionState.Unknown,
    -> HomeWatchStatus.Pending
}

private fun HomeRefreshStage?.toStatusMessage(): String = when (this) {
    HomeRefreshStage.Preparing -> "Preparando sincronização..."
    HomeRefreshStage.Connecting -> "Conectando ao relógio..."
    HomeRefreshStage.WaitingForSteps -> "Aguardando os dados do relógio..."
    HomeRefreshStage.Persisting -> "Atualizando resumo..."
    null -> "Sincronizando os dados do relógio"
}

private fun formatSteps(steps: Long): String = numberFormat().format(steps)

private fun formatGoal(goal: Int): String = "${numberFormat().format(goal)} passos"

private fun formatPercent(progress: Float): String {
    val percent = (progress * 100f).toInt()
    return "$percent%"
}

private fun formatDistance(distanceMeters: Double): String {
    val kilometers = distanceMeters / 1000.0
    return "${decimalFormat().format(kilometers)} km"
}

private fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
