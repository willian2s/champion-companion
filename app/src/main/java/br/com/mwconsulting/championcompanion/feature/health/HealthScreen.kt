package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.HeartRateMeasurement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
fun HealthScreen(
    healthViewModel: HealthViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[HealthViewModel::class.java]
        }
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val state by healthViewModel.state.collectAsState()
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(healthViewModel) {
        healthViewModel.onIntent(HealthIntent.Load)
    }

    DisposableEffect(lifecycleOwner, healthViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
                -> healthViewModel.onIntent(HealthIntent.Load)

                Lifecycle.Event.ON_STOP -> healthViewModel.onIntent(HealthIntent.AppMovedToBackground)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = state.surfaceState.isMeasurementInFlight()) {
        showExitConfirmation = true
    }

    DisposableEffect(view, state.surfaceState.isMeasurementInFlight()) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = state.surfaceState.isMeasurementInFlight()
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Cancelar medição?") },
            text = { Text("A medição em andamento será cancelada e nenhum resultado parcial será salvo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        healthViewModel.onIntent(HealthIntent.ConfirmExitMeasurement)
                    },
                ) {
                    Text("Cancelar medição")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Continuar medindo")
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge)
                .heightIn(min = 1.dp),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            HealthHeader()
            HealthSessionSurface(
                state = state,
                onStart = { healthViewModel.onIntent(HealthIntent.StartMeasurement) },
                onCancel = { healthViewModel.onIntent(HealthIntent.CancelMeasurement) },
                onRetry = { healthViewModel.onIntent(HealthIntent.Retry) },
                onDismiss = { healthViewModel.onIntent(HealthIntent.DismissError) },
            )
            LatestMeasurementCard(
                measurement = state.latestMeasurement,
                hasAssociatedDevice = state.hasAssociatedDevice,
                associatedDeviceName = state.associatedDeviceName,
            )
            RecentMeasurementsCard(measurements = state.recentMeasurements)
        }
    }
}

@Composable
private fun HealthHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Saúde",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Inicie a medição manual por toque. O resultado final válido será salvo ao concluir a sessão.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HealthSessionSurface(
    state: HealthUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (val surface = state.surfaceState) {
        HealthSurfaceState.Idle -> IdleMeasurementCard(
            state = state,
            onStart = onStart,
        )

        is HealthSurfaceState.Preparing -> MeasurementProgressCard(
            title = "Preparando medição",
            message = "Validando dispositivo associado e preparando a sessão manual.",
            session = surface.session,
            showProgress = false,
            onCancel = onCancel,
        )

        is HealthSurfaceState.Connecting -> MeasurementProgressCard(
            title = "Conectando ao relógio",
            message = "Aguarde enquanto a conexão é preparada para a medição manual.",
            session = surface.session,
            showProgress = false,
            onCancel = onCancel,
        )

        is HealthSurfaceState.Starting -> MeasurementProgressCard(
            title = "Iniciando medição",
            message = "A medição será iniciada em instantes.",
            session = surface.session,
            showProgress = false,
            onCancel = onCancel,
        )

        is HealthSurfaceState.Measuring -> MeasurementProgressCard(
            title = "Medindo frequência cardíaca",
            message = "Permaneça com o relógio ajustado ao pulso até a conclusão.",
            session = surface.session,
            showProgress = true,
            onCancel = onCancel,
        )

        is HealthSurfaceState.Stopping -> MeasurementProgressCard(
            title = "Finalizando medição",
            message = "Estamos consolidando o resultado final da sessão.",
            session = surface.session,
            showProgress = true,
            onCancel = null,
        )

        is HealthSurfaceState.Completed -> CompletedMeasurementCard(
            session = surface.session,
            onStart = onStart,
            onDismiss = onDismiss,
        )

        is HealthSurfaceState.Cancelled -> CancelledMeasurementCard(
            session = surface.session,
            onStart = onStart,
            onDismiss = onDismiss,
        )

        is HealthSurfaceState.Failed -> FailedMeasurementCard(
            state = state,
            session = surface.session,
            onRetry = onRetry,
            onDismiss = onDismiss,
            onStart = onStart,
        )
    }
}

@Composable
private fun IdleMeasurementCard(
    state: HealthUiState,
    onStart: () -> Unit,
) {
    HealthCard(
        title = "Medição manual",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (state.hasAssociatedDevice) {
                "Toque para iniciar uma medição de 60 segundos no relógio associado."
            } else {
                "Associe um relógio antes de iniciar a medição manual."
            },
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        state.associatedDeviceName?.let { name ->
            Text(
                text = "Relógio associado: $name",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PrimaryActionButton(
            label = "Medir frequência cardíaca",
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.large),
            enabled = state.hasAssociatedDevice,
        )
    }
}

@Composable
private fun MeasurementProgressCard(
    title: String,
    message: String,
    session: ManualHeartRateSessionState,
    showProgress: Boolean,
    onCancel: (() -> Unit)?,
) {
    HealthCard(
        title = title,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier
                .padding(top = ChampionSpacing.medium)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CurrentHeartRateHero(
            session = session,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.large),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            MetricCard(
                label = "Tempo restante",
                value = formatRemaining(session.remaining),
                supportingText = "Sessão de 60 segundos",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Qualidade do sinal",
                value = session.signalQuality.toUiLabel(),
                supportingText = session.signalQuality.toGuidance(),
                modifier = Modifier.weight(1f),
            )
        }
        if (showProgress) {
            val progress = session.progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ChampionSpacing.large)
                    .semantics {
                        contentDescription = "Progresso da medição"
                        stateDescription = "${(progress * 100).roundToInt()} por cento concluído"
                        progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
                    },
            )
        }
        onCancel?.let {
            PrimaryActionButton(
                label = "Cancelar medição",
                onClick = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ChampionSpacing.large),
            )
        }
    }
}

@Composable
private fun CurrentHeartRateHero(
    session: ManualHeartRateSessionState,
    modifier: Modifier = Modifier,
) {
    val reading = session.currentReading
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ChampionSpacing.large)
                .semantics(mergeDescendants = true) {
                    contentDescription = when (reading) {
                        null -> "Aguardando a primeira leitura da frequência cardíaca"
                        else -> "Frequência cardíaca atual: ${reading.bpm} batimentos por minuto"
                    }
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = reading?.bpm?.let { "$it BPM" } ?: "-- BPM",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (reading?.source) {
                    ManualHeartRateReadingSource.D0_LIVE -> "Leitura confirmada pelo relógio"
                    ManualHeartRateReadingSource.F002_ESTIMATE -> "Valor estimado a partir do sinal"
                    null -> "Aguardando a primeira leitura"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CompletedMeasurementCard(
    session: ManualHeartRateSessionState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val result = session.finalResult
    HealthCard(
        title = "Medição concluída",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "A medição foi concluída e o resultado final foi salvo com sucesso.",
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CurrentHeartRateHero(
            session = session.copy(currentReading = result?.toCurrentReading()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.large),
        )
        result?.let {
            Text(
                text = "Horário: ${formatDateTime(it.measuredAt)}",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = when (it.source) {
                    ManualHeartRateFinalSource.D0_FINAL -> "Resultado final confirmado pelo relógio"
                    ManualHeartRateFinalSource.F002_FINAL -> "Resultado final estimado a partir do sinal"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            PrimaryActionButton(
                label = "Nova medição",
                onClick = {
                    onDismiss()
                    onStart()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Concluir")
            }
        }
    }
}

@Composable
private fun CancelledMeasurementCard(
    session: ManualHeartRateSessionState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Medição cancelada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = session.error?.message ?: "A medição foi cancelada antes da conclusão e nenhum dado parcial foi salvo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                PrimaryActionButton(
                    label = "Nova medição",
                    onClick = {
                        onDismiss()
                        onStart()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun FailedMeasurementCard(
    state: HealthUiState,
    session: ManualHeartRateSessionState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    val error = session.error
    if (error == null) {
        EmptyState(
            title = "Falha ao concluir a medição",
            message = "Não foi possível determinar um estado seguro para a sessão manual.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    ErrorState(
        title = "Não foi possível concluir a medição",
        message = error.message,
        actionLabel = when {
            error.isRetryable -> "Tentar novamente"
            state.hasAssociatedDevice -> "Nova medição"
            else -> null
        },
        onAction = when {
            error.isRetryable -> onRetry
            state.hasAssociatedDevice -> onStart
            else -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )

    TextButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Fechar aviso")
    }
}

@Composable
private fun LatestMeasurementCard(
    measurement: HeartRateMeasurement?,
    hasAssociatedDevice: Boolean,
    associatedDeviceName: String?,
) {
    if (measurement == null) {
        EmptyState(
            title = "Sem medições salvas",
            message = if (hasAssociatedDevice) {
                "Quando você concluir a primeira medição manual, o último resultado aparecerá aqui."
            } else {
                "Associe um relógio e conclua uma medição manual para começar o histórico de frequência cardíaca."
            },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    HealthCard(
        title = "Última medição",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "${measurement.bpm} BPM",
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = measurement.measuredAt?.let(::formatMeasurementMoment) ?: "Horário indisponível",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = associatedDeviceName?.let { "Origem: $it" } ?: "Origem: relógio associado",
            modifier = Modifier.padding(top = ChampionSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RecentMeasurementsCard(
    measurements: List<HeartRateMeasurement>,
) {
    HealthCard(
        title = "Medições recentes",
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (measurements.isEmpty()) {
            Text(
                text = "Ainda não há medições manuais recentes para exibir.",
                modifier = Modifier.padding(top = ChampionSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@HealthCard
        }

        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            measurements.forEach { measurement ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                    ) {
                        Text(
                            text = measurement.measuredAt?.let(::formatMeasurementMoment) ?: "Horário indisponível",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = measurement.source.toMeasurementSourceLabel(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "${measurement.bpm} BPM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
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

private fun ManualHeartRateSignalQuality?.toUiLabel(): String = when (this) {
    ManualHeartRateSignalQuality.GOOD -> "Boa"
    ManualHeartRateSignalQuality.REGULAR -> "Regular"
    ManualHeartRateSignalQuality.LOW -> "Baixa"
    null -> "Aguardando"
}

private fun ManualHeartRateSignalQuality?.toGuidance(): String = when (this) {
    ManualHeartRateSignalQuality.GOOD -> "Sinal estável para a sessão"
    ManualHeartRateSignalQuality.REGULAR -> "Mantenha o pulso parado para melhorar a leitura"
    ManualHeartRateSignalQuality.LOW -> "Ajuste o relógio ao pulso para melhorar o sinal"
    null -> "A qualidade aparecerá quando houver sinal suficiente"
}

private fun ManualHeartRateResult.toCurrentReading(): ManualHeartRateCurrentReading =
    ManualHeartRateCurrentReading(
        bpm = bpm,
        source = when (source) {
            ManualHeartRateFinalSource.D0_FINAL -> ManualHeartRateReadingSource.D0_LIVE
            ManualHeartRateFinalSource.F002_FINAL -> ManualHeartRateReadingSource.F002_ESTIMATE
        },
        observedAt = measuredAt,
    )

private fun formatRemaining(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatMeasurementMoment(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())
    val prefix = when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
    return "$prefix, $time"
}

private fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun br.com.mwconsulting.championcompanion.core.model.HeartRateSource.toMeasurementSourceLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.MANUAL_MEASUREMENT -> "Medição manual"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.MEASUREMENT_STREAM -> "Fluxo de medição"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.WATCH_D1_HISTORY -> "Histórico do relógio"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.APP_WORKOUT_SESSION -> "Sessão iniciada no app"
    br.com.mwconsulting.championcompanion.core.model.HeartRateSource.WATCH_WORKOUT_ACTIVITY -> "Atividade do relógio"
}
