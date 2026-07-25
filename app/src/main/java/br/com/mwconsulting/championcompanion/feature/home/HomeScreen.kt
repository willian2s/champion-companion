package br.com.mwconsulting.championcompanion.feature.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeExtraLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeSmall
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
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
                            CustomAccessibilityAction("Atualizar dados do relogio") {
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
                        .padding(ChampionSpacing.extraLarge)
                        .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
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
                                    contentDescription = "Sincronizando dados do relogio"
                                    liveRegion = LiveRegionMode.Polite
                                },
                        )
                    }

                    HomeRefreshStatus(
                        refresh = state.refresh,
                        healthConnectSync = state.healthConnectSync,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HomeSurface(
                        state = state,
                        onOpenDevice = onOpenDevice,
                        onOpenHeartRate = onOpenHeartRate,
                        onOpenExercises = onOpenExercises,
                        onRefresh = { homeViewModel.onIntent(HomeIntent.Refresh) },
                        onRetry = { homeViewModel.onIntent(HomeIntent.Retry) },
                    )
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
    onOpenExercises: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    state.summary?.let { summary ->
        HomeSummaryContent(
            summary = summary,
            latestHeartRate = state.latestHeartRate,
            watch = state.watch,
            healthConnectSync = state.healthConnectSync,
            workoutPendingBanner = state.workoutPendingBanner,
            pendingActivities = state.pendingActivities,
            onOpenHeartRate = onOpenHeartRate,
            onOpenExercises = onOpenExercises,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    when (state.surfaceState) {
        HomeSurfaceState.Loading -> LoadingState(
            label = "Carregando Home",
            message = "Preparando resumo diário e status do relógio.",
            modifier = Modifier.fillMaxWidth(),
        )

        HomeSurfaceState.Empty -> EmptyState(
            title = "Sem resumo de hoje",
            message = "Puxe para baixo para sincronizar os dados do relógio. Leitores de tela podem usar a ação Atualizar dados do relógio.",
            modifier = Modifier.fillMaxWidth(),
        )

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
            modifier = Modifier.fillMaxWidth(),
        )

        HomeSurfaceState.BluetoothDisabled -> ErrorState(
            title = "Bluetooth desligado",
            message = "Ative o Bluetooth e depois puxe para baixo para tentar novamente.",
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

    if (state.surfaceState == HomeSurfaceState.Empty && state.summary == null) {
        HomeRefreshHint(onRefresh = onRefresh)
    }

    if (state.summary == null) {
        HomeLatestHeartRateCard(
            latestHeartRate = state.latestHeartRate,
            onClick = onOpenHeartRate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeSummaryContent(
    summary: HomeDailySummaryModel,
    latestHeartRate: HomeLatestHeartRateModel?,
    watch: HomeWatchSnapshot,
    healthConnectSync: HomeHealthConnectSyncModel,
    workoutPendingBanner: HomeWorkoutPendingBanner?,
    pendingActivities: List<HomePendingActivityModel>,
    onOpenHeartRate: () -> Unit,
    onOpenExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        HealthCard(
            title = "Resumo diário",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = formatSteps(summary.steps),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Meta ${formatGoal(summary.dailyGoal)} • ${formatPercent(summary.progress)} da meta",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { summary.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ChampionSpacing.medium),
            )
            Text(
                text = "Puxe para baixo para atualizar os dados do relógio.",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (healthConnectSync.hasVisibleState) {
            HomeHealthConnectStatusCard(healthConnectSync)
        }

        workoutPendingBanner?.let { banner ->
            HomeWorkoutPendingBannerCard(
                banner = banner,
                onOpenExercises = onOpenExercises,
            )
        }

        HomeLatestHeartRateCard(
            latestHeartRate = latestHeartRate,
            onClick = onOpenHeartRate,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Distancia estimada",
                value = formatDistance(summary.estimatedDistanceMeters),
                supportingText = estimateSupport(summary.estimatedDistanceMeters),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Calorias estimadas",
                value = formatCalories(summary.estimatedCaloriesKilocalories),
                supportingText = estimateSupport(summary.estimatedCaloriesKilocalories),
                modifier = Modifier.weight(1f),
            )
        }

        if (pendingActivities.isNotEmpty()) {
            HealthCard(
                title = "Atividades pendentes",
                modifier = Modifier.fillMaxWidth(),
            ) {
                pendingActivities.forEachIndexed { index, activity ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = ChampionSpacing.medium))
                    }
                    Text(text = activity.title, style = MaterialTheme.typography.titleSmall)
                    activity.subtitle?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        HomeMetadataFooter(
            lastImportedAt = watch.lastImportedAt,
        )
    }
}

@Composable
private fun HomeWorkoutPendingBannerCard(
    banner: HomeWorkoutPendingBanner,
    onOpenExercises: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenExercises),
        colors = CardDefaults.cardColors(
            containerColor = if (banner.isCalibrationPriority) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = banner.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = banner.message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (banner.isCalibrationPriority) {
                    "Toque para abrir Exercícios e concluir a calibração prioritária."
                } else {
                    "Toque para abrir Exercícios e revisar os treinos pendentes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeLatestHeartRateCard(
    latestHeartRate: HomeLatestHeartRateModel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        label = "Última frequência cardíaca",
        value = latestHeartRate?.let { "${it.bpm} BPM" } ?: "Sem medições",
        supportingText = latestHeartRate?.let { formatMeasurementTimestamp(it.measuredAt) }
            ?: "Disponível após a primeira medição manual",
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("Última frequência cardíaca. ")
                    append(latestHeartRate?.let { "${it.bpm} BPM. ${formatMeasurementTimestamp(it.measuredAt)}." } ?: "Sem medições.")
                    append(" Toque para abrir detalhes.")
                }
            },
    )
}

@Composable
private fun HomeRefreshStatus(
    refresh: HomeRefreshState,
    healthConnectSync: HomeHealthConnectSyncModel,
    modifier: Modifier = Modifier,
) {
    val message = when {
        refresh.isRefreshing -> refresh.refreshStage.toStatusMessage()
        refresh.error != null -> "Não foi possível atualizar os dados."
        healthConnectSync.syncingCount > 0 -> "Health Connect sincronizando em segundo plano."
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
private fun HomeHealthConnectStatusCard(sync: HomeHealthConnectSyncModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    sync.syncingCount > 0 -> "Sincronização secundária em andamento. O resumo local já foi salvo."
                    sync.failedCount > 0 -> "Há falhas pendentes no Health Connect, mas o resumo local continua válido."
                    sync.pendingCount > 0 -> "Existem dados aguardando sincronização no Health Connect."
                    sync.syncedCount > 0 -> "Dados locais recentes já possuem sincronização registrada no Health Connect."
                    else -> "Sem atividade recente de sincronização no Health Connect."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Pendentes ${sync.pendingCount} • Falhas ${sync.failedCount} • Sincronizando ${sync.syncingCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sync.lastSyncedAt?.let {
                Text(
                    "Último envio ao Health Connect: ${formatDateTime(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeRefreshHint(onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Atualização manual",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Puxe para baixo para sincronizar o resumo diário. Se usar leitor de tela, acione Atualizar dados do relógio nas ações disponíveis.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryActionButton(
                label = "Atualizar agora",
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeExtraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = ChampionSpacing.small),
        ) {
            WatchStatusButton(
                status = status,
                onClick = onOpenDevice,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ProfileShortcutButton(
                onClick = onOpenProfile,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun WatchStatusButton(
    status: HomeWatchStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(ChampionShapeExtraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Abrir Dispositivo. Status do relógio: ${status.label}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Watch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(10.dp)
                .clip(ChampionShapeSmall)
                .background(status.color()),
        )
    }
}

@Composable
private fun ProfileShortcutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(ChampionShapeExtraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Abrir Perfil"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
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

@Composable
private fun HomeWatchStatus.color() = when (this) {
    HomeWatchStatus.Connected -> Color(0xFF22C55E)
    HomeWatchStatus.Connecting -> Color(0xFF3B82F6)
    HomeWatchStatus.Pending -> Color(0xFFFACC15)
    HomeWatchStatus.BluetoothDisabled,
    HomeWatchStatus.PermissionsRequired,
    -> Color(0xFFEF4444)
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

private fun formatDistance(distanceMeters: Double?): String {
    if (distanceMeters == null) return "Indisponível"
    val kilometers = distanceMeters / 1000.0
    return "${decimalFormat().format(kilometers)} km"
}

private fun formatCalories(calories: Double?): String {
    if (calories == null) return "Indisponível"
    return "${decimalFormat().format(calories)} kcal"
}

private fun estimateSupport(value: Double?): String = if (value == null) {
    "Sem dados pessoais suficientes"
} else {
    "Estimativa local"
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

private fun formatMeasurementTimestamp(instant: Instant): String =
    "Registrada em ${formatDateTime(instant)}"

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
