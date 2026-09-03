package br.com.mwconsulting.championcompanion.feature.calendar

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.CalendarDataSource
import br.com.mwconsulting.championcompanion.core.model.CalendarDataIssueReason
import br.com.mwconsulting.championcompanion.core.model.CalendarDaySummary
import br.com.mwconsulting.championcompanion.core.model.CalendarHeartRateSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarSyncState
import br.com.mwconsulting.championcompanion.core.model.CalendarSyncSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarWeightMeasurementSummary
import br.com.mwconsulting.championcompanion.core.model.CalendarWorkoutSummary
import br.com.mwconsulting.championcompanion.core.model.DataOrigin
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.StepLengthSource
import br.com.mwconsulting.championcompanion.core.model.WorkoutCanonicalReadStatus
import br.com.mwconsulting.championcompanion.core.model.WeightUnit
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.navigation.AppDestination
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CalendarDayScreen(
    localDateIso: String,
    onNavigateBack: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    backLabel: String = "Voltar para Calendário",
    calendarViewModel: CalendarViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) { ViewModelProvider(activity)[CalendarViewModel::class.java] }
    },
) {
    val state by calendarViewModel.state.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val localDate = remember(localDateIso) {
        AppDestination.CalendarDay(localDateIso).parseLocalDateOrNull()
    }
    val dayError = state.error?.takeIf { it.scope == CalendarUiErrorScope.DAY }
    val dayDetail = state.dayDetail?.takeIf { it.localDate == localDate }

    LaunchedEffect(calendarViewModel, localDate) {
        localDate?.let { calendarViewModel.onIntent(CalendarIntent.OpenDate(it)) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            if (localDate == null) {
                DayHeader(
                    supportingText = "Data inválida",
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                    showContextualBack = false,
                )
                ErrorState(
                    title = "Data inválida",
                    message = "Não foi possível abrir o detalhe diário. Volte ao calendário e selecione uma data válida.",
                )
                BackButton(onNavigateBack, backLabel)
            } else {
                DayHeader(
                    supportingText = localDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
                    ),
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
                when {
                    state.dayLoading -> LoadingState(
                        label = "Carregando detalhe diário",
                        message = "Consultando somente dados locais",
                        modifier = Modifier.fillMaxWidth(),
                    )

                    dayError != null && dayDetail == null -> {
                        ErrorState(
                            title = "Detalhe diário indisponível",
                            message = dayError.toCalendarDayMessage(),
                            actionLabel = if (dayError.isRetryable) "Tentar novamente" else null,
                            onAction = if (dayError.isRetryable) {
                                { calendarViewModel.onIntent(CalendarIntent.Retry) }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                     }

                     dayDetail != null -> {
                         CalendarDayContent(
                             summary = dayDetail,
                             locale = locale,
                             onNavigateTo = onNavigateTo,
                             onRetry = if (dayError?.isRetryable != false) {
                                 { calendarViewModel.onIntent(CalendarIntent.Retry) }
                             } else {
                                 null
                             },
                         )
                     }

                    else -> EmptyState(
                        title = "Detalhe diário sem dados",
                        message = "Nenhum dado local foi encontrado para esta data.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                BackButton(onNavigateBack, backLabel)
            }
        }
    }
}

@Composable
private fun DayHeader(
    supportingText: String,
    onNavigateBack: () -> Unit,
    backLabel: String,
    showContextualBack: Boolean = true,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = LightColorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                "Detalhe diário",
                modifier = Modifier.semantics { heading() },
                color = White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                supportingText,
                color = White,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (showContextualBack) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = White,
                    ),
                    border = BorderStroke(1.dp, White),
                ) {
                    Text(backLabel)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayContent(
    summary: CalendarDaySummary?,
    locale: Locale,
    onNavigateTo: (AppDestination) -> Unit,
    onRetry: (() -> Unit)?,
) {
    val day = summary ?: return
    DayOverviewCard(day)
    DaySectionTitle("Métricas do dia")
    StepsDayCard(day, onOpen = { onNavigateTo(AppDestination.HealthSteps) })
    DistanceDayCard(day, onOpen = { onNavigateTo(AppDestination.HealthDistance) })
    HeartRateDayCard(day, onOpen = { onNavigateTo(AppDestination.HealthHeartRate) })
    WorkoutsDayCard(
        day = day,
        locale = locale,
        onOpen = { onNavigateTo(AppDestination.ExerciseDetail(it)) },
    )
    WeightDayCard(day, onOpen = { onNavigateTo(AppDestination.HealthWeight) })
    if (day.issues.isNotEmpty()) {
        DayIssuesCard(day, onRetry)
    }
    SyncDayCard(day)
}

@Composable
private fun DayOverviewCard(day: CalendarDaySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, healthPrimaryAccent()),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Panorama do dia",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            DayOverviewRow(
                DayOverviewMetric(
                    title = "Passos",
                    value = day.dailySummary?.let { "${it.steps} passos" } ?: "Sem dados",
                    supportingText = "Meta ${day.stepsGoal}",
                    accentColor = healthTertiaryAccent(),
                ),
                DayOverviewMetric(
                    title = "Distância",
                    value = day.dailySummary?.estimatedDistanceMeters?.let {
                        "${NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(it / 1_000.0)} km"
                    } ?: "Indisponível",
                    supportingText = "Estimativa local",
                    accentColor = healthPrimaryAccent(),
                ),
            )
            DayOverviewRow(
                DayOverviewMetric(
                    title = "Frequência cardíaca",
                    value = day.heartRateSummary?.let { "${it.averageBpm.roundToInt()} BPM" } ?: "Sem dados",
                    supportingText = day.heartRateSummary?.let { "${it.measurementCount} leituras" } ?: "Nenhuma leitura",
                    accentColor = healthPrimaryAccent(),
                ),
                DayOverviewMetric(
                    title = "Atividades",
                    value = "${day.workouts.size}",
                    supportingText = if (day.workouts.isEmpty()) "Nenhuma registrada" else "Registradas no dia",
                    accentColor = healthSecondaryAccent(),
                ),
            )
            DayOverviewRow(
                DayOverviewMetric(
                    title = "Peso",
                    value = day.weightMeasurements.firstOrNull()?.toWeightLabel() ?: "Sem dados",
                    supportingText = if (day.weightMeasurements.isEmpty()) "Nenhuma medida" else "Mais recente",
                    accentColor = healthSecondaryAccent(),
                ),
                DayOverviewMetric(
                    title = "Estado",
                    value = if (day.issues.isEmpty()) "Completo" else "Parcial",
                    supportingText = if (day.issues.isEmpty()) "Fontes locais disponíveis" else "Revise problemas abaixo",
                    accentColor = if (day.issues.isEmpty()) healthPrimaryAccent() else MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

private data class DayOverviewMetric(
    val title: String,
    val value: String,
    val supportingText: String,
    val accentColor: Color,
)

@Composable
private fun DayOverviewRow(first: DayOverviewMetric, second: DayOverviewMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        DayOverviewMetricContent(first, Modifier.weight(1f))
        DayOverviewMetricContent(second, Modifier.weight(1f))
    }
}

@Composable
private fun DayOverviewMetricContent(metric: DayOverviewMetric, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 48.dp),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
    ) {
        Text(metric.title, style = MaterialTheme.typography.labelLarge)
        Text(
            metric.value,
            color = metric.accentColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            metric.supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DaySectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun StepsDayCard(day: CalendarDaySummary, onOpen: () -> Unit) {
    DayCard(title = "Passos", accentColor = healthTertiaryAccent()) {
        val daily = day.dailySummary
        if (daily == null) {
            Text(
                if (day.hasIssue(CalendarDataSource.DAILY_SUMMARY)) {
                    "Passos indisponíveis: resumo diário local não pôde ser consultado."
                } else {
                    "Sem resumo diário local para esta data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("${daily.steps} passos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Meta atual: ${day.stepsGoal} passos")
            Text("Origem: ${daily.origin.toCalendarLabel()} • Atualizado em ${daily.updatedAt.formatCalendarTime(day.zoneId)}")
        }
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Abrir detalhes de passos")
        }
    }
}

@Composable
private fun DistanceDayCard(day: CalendarDaySummary, onOpen: () -> Unit) {
    DayCard(title = "Distância", accentColor = healthPrimaryAccent()) {
        val distance = day.dailySummary?.estimatedDistanceMeters
        if (distance == null) {
            Text(
                if (day.hasIssue(CalendarDataSource.DAILY_SUMMARY)) {
                    "Distância indisponível: resumo diário local não pôde ser consultado."
                } else {
                    "Distância estimada indisponível para esta data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "${NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(distance / 1_000.0)} km",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text("Valor derivado exclusivamente do resumo diário; não soma atividades.")
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Abrir detalhes de distância")
        }
    }
}

@Composable
private fun HeartRateDayCard(day: CalendarDaySummary, onOpen: () -> Unit) {
    DayCard(title = "Frequência cardíaca", accentColor = healthPrimaryAccent()) {
        val heartRate = day.heartRateSummary
        if (heartRate == null) {
            Text(
                if (day.hasIssue(CalendarDataSource.HEART_RATE)) {
                    "BPM indisponível: fonte local não pôde ser consultada."
                } else {
                    "Sem BPM manual ou D1 válido nesta data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HeartRateSummaryContent(heartRate)
        }
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Abrir detalhes de frequência cardíaca")
        }
    }
}

@Composable
private fun HeartRateSummaryContent(summary: CalendarHeartRateSummary) {
    Text("${summary.averageBpm.roundToInt()} BPM em ${summary.measurementCount} leituras", style = MaterialTheme.typography.titleLarge)
    Text("Mínimo ${summary.minimumBpm} • Máximo ${summary.maximumBpm} • Último ${summary.latestMeasurement.bpm} BPM")
}

@Composable
private fun WorkoutsDayCard(
    day: CalendarDaySummary,
    locale: Locale,
    onOpen: (String) -> Unit,
) {
    DayCard(title = "Atividades (${day.workouts.size})", accentColor = healthSecondaryAccent()) {
        if (day.workouts.isEmpty()) {
            Text(
                if (day.hasIssue(CalendarDataSource.WORKOUT_ACTIVITY)) {
                    "Atividades indisponíveis: fonte local não pôde ser consultada."
                } else {
                    "Nenhuma atividade local não descartada nesta data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            day.workouts.forEach { workout ->
                WorkoutDayCard(workout, day.zoneId, locale, onOpen)
            }
        }
    }
}

@Composable
private fun WorkoutDayCard(
    workout: CalendarWorkoutSummary,
    zoneId: ZoneId,
    locale: Locale,
    onOpen: (String) -> Unit,
) {
    Card(
        onClick = { onOpen(workout.activityId.value.toString()) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = workout.accessibleDescription()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(workout.title ?: workout.displayType(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Tipo: ${workout.displayType()} • Início: ${workout.startedAt.formatCalendarTime(zoneId, locale)}")
            Text("Origem: ${workout.origin.toCalendarLabel()}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (workout.isWatchWorkout && !workout.isCycling) {
                    "Duração: ${workout.duration.toCalendarLabel()} • Distância estimada: ${workout.distanceMeters.toDistanceLabel()}"
                } else {
                    "Duração: ${workout.duration.toCalendarLabel()} • Distância: ${workout.distanceMeters.toDistanceLabel()}"
                },
            )
            if (workout.isWatchWorkout && !workout.isCycling) {
                Text(
                    text = workout.canonicalStatusMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (workout.canonicalReadStatus == WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (workout.derivationVersion != null || workout.stepLengthSource != null) {
                    Text(
                        text = "Comprimento do passo: ${workout.stepLengthSource?.toCalendarLabel() ?: "indisponível"} • Derivação ${workout.derivationVersion ?: "indisponível"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("Calorias: ${workout.caloriesKilocalories.toNumberLabel()} • BPM médio: ${workout.averageBpm.toBpmLabel()}")
            Text("Workflow: ${workout.workflowState.toCalendarLabel()}")
            Text("Abrir detalhe da atividade", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WeightDayCard(day: CalendarDaySummary, onOpen: () -> Unit) {
    DayCard(title = "Peso", accentColor = healthSecondaryAccent()) {
        val latest = day.weightMeasurements.firstOrNull()
        if (latest == null) {
            Text(
                if (day.hasIssue(CalendarDataSource.WEIGHT_MEASUREMENT)) {
                    "Peso indisponível: fonte local não pôde ser consultada."
                } else {
                    "Nenhuma medida de peso local nesta data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(latest.toWeightLabel(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Medida mais recente: ${latest.measuredAt.formatCalendarTime(day.zoneId)}")
            if (day.weightMeasurements.size > 1) {
                Text("Outras medidas")
                day.weightMeasurements.drop(1).forEach { measurement ->
                    Text("• ${measurement.toWeightLabel()} — ${measurement.measuredAt.formatCalendarTime(day.zoneId)}")
                }
            }
        }
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Abrir detalhes de peso")
        }
    }
}

@Composable
private fun SyncDayCard(day: CalendarDaySummary) {
    val summary = day.syncSummary
    val syncUnavailable = day.hasIssue(CalendarDataSource.HEALTH_SYNC_QUEUE)
    DayCard(title = "Fila local informativa", accentColor = healthTertiaryAccent()) {
        CalendarLocalSyncStatus(
            state = summary.state.takeUnless { syncUnavailable },
        )
        if (syncUnavailable) {
            Text(
                "A fila local não pôde ser consultada integralmente ou contém operação sem dado local relacionado. Nenhum sucesso foi presumido.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (summary.operationCount == 0) {
            Text("Sem operações locais para esta data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Pendentes ${summary.pendingCount} • Sincronizando ${summary.syncingCount} • Falhas ${summary.failedCount} • Sincronizados ${summary.syncedCount}")
        }
        if (day.workouts.isNotEmpty()) {
            Text("Status por atividade", style = MaterialTheme.typography.labelLarge)
            day.workouts.forEach { workout ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = workout.title ?: workout.displayType(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = workout.syncSummary.state
                            .takeUnless { syncUnavailable }
                            ?.toCalendarLabel()
                            ?: "Indisponível",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(
            "Status derivado somente da fila local. Falhas não removem dados locais e nenhuma sincronização é iniciada nesta tela.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayIssuesCard(day: CalendarDaySummary, onRetry: (() -> Unit)?) {
    DayCard(title = "Problemas de fontes", accentColor = MaterialTheme.colorScheme.error) {
        Text(
            "Parte das fontes locais não pôde ser consultada. Ausência, zero e falha permanecem estados distintos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        day.issues
            .sortedBy { it.source.name }
            .forEach { issue ->
                Text(
                    "• ${issue.source.toCalendarLabel()}: ${issue.reason.toCalendarLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        if (onRetry != null) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Tentar novamente")
            }
        }
    }
}

@Composable
private fun CalendarLocalSyncStatus(
    state: CalendarSyncState?,
    modifier: Modifier = Modifier,
) {
    val label = state?.toCalendarLabel() ?: "Status indisponível"
    val containerColor = when (state) {
        CalendarSyncState.NO_OPERATIONS -> MaterialTheme.colorScheme.surfaceContainerHighest
        CalendarSyncState.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        CalendarSyncState.SYNCING -> MaterialTheme.colorScheme.secondaryContainer
        CalendarSyncState.SYNCED -> MaterialTheme.colorScheme.primaryContainer
        CalendarSyncState.FAILED, null -> MaterialTheme.colorScheme.errorContainer
        CalendarSyncState.PARTIAL -> MaterialTheme.colorScheme.inverseSurface
    }
    val contentColor = when (state) {
        CalendarSyncState.NO_OPERATIONS -> MaterialTheme.colorScheme.onSurfaceVariant
        CalendarSyncState.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
        CalendarSyncState.SYNCING -> MaterialTheme.colorScheme.onSecondaryContainer
        CalendarSyncState.SYNCED -> MaterialTheme.colorScheme.onPrimaryContainer
        CalendarSyncState.FAILED, null -> MaterialTheme.colorScheme.onErrorContainer
        CalendarSyncState.PARTIAL -> MaterialTheme.colorScheme.inverseOnSurface
    }
    val indicator = when (state) {
        CalendarSyncState.NO_OPERATIONS -> "—"
        CalendarSyncState.PENDING -> "…"
        CalendarSyncState.SYNCING -> "↻"
        CalendarSyncState.SYNCED -> "✓"
        CalendarSyncState.FAILED, null -> "!"
        CalendarSyncState.PARTIAL -> "◐"
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Status da fila local"
            stateDescription = label
            liveRegion = LiveRegionMode.Polite
        },
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ChampionSpacing.medium, vertical = ChampionSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(indicator, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DayCard(
    title: String,
    accentColor: Color = healthPrimaryAccent(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                title,
                color = accentColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun BackButton(
    onNavigateBack: () -> Unit,
    backLabel: String,
) {
    Button(
        onClick = onNavigateBack,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(backLabel)
    }
}

private fun CalendarWorkoutSummary.displayType(): String = (confirmedType ?: suggestedType).toCalendarLabel()

private val CalendarWorkoutSummary.isWatchWorkout: Boolean
    get() = origin == DataOrigin.WATCH_D3_D4

private val CalendarWorkoutSummary.isCycling: Boolean
    get() = confirmedType == WorkoutType.CYCLING

private fun CalendarWorkoutSummary.accessibleDescription(): String =
    if (isCycling) {
        "${title ?: displayType()}. Distância manual ${distanceMeters.toDistanceLabel()}."
    } else {
        "${title ?: displayType()}. ${canonicalStatusMessage()} Distância ${distanceMeters.toDistanceLabel()}."
    }

private fun CalendarWorkoutSummary.canonicalStatusMessage(): String = if (isCycling) {
    "Distância manual opcional; série derivada preservada somente para diagnóstico."
} else {
    when (canonicalReadStatus) {
        WorkoutCanonicalReadStatus.NOT_APPLICABLE -> "Distância segue contrato legado da atividade phone."
        WorkoutCanonicalReadStatus.MISSING_STEP_LENGTH ->
            "Sem comprimento do passo. Configure Perfil e reprocese a atividade por ação explícita."
        WorkoutCanonicalReadStatus.NOT_MATERIALIZED ->
            "Derivação canônica não materializada. Configure comprimento do passo e reprocese por ação explícita."
        WorkoutCanonicalReadStatus.SERIES_INELIGIBLE ->
            "Série canônica inelegível. Distância original, calibrada e agregada não são usadas como substitutas."
        WorkoutCanonicalReadStatus.PLAUSIBILITY_BLOCKED ->
            "Série estimada contém valores implausíveis e não deve ser tratada como confiável."
        WorkoutCanonicalReadStatus.SPLITS_NOT_MATERIALIZED ->
            "Série canônica disponível; splits ainda não materializados."
        WorkoutCanonicalReadStatus.AVAILABLE ->
            "Distância e métricas funcionais vêm da série canônica persistida."
    }
}

private fun StepLengthSource.toCalendarLabel(): String = when (this) {
    StepLengthSource.MANUAL -> "entrada manual"
    StepLengthSource.HEIGHT_SUGGESTION_ACCEPTED -> "sugestão aceita pela altura"
    StepLengthSource.REFERENCE_DISTANCE_CALIBRATION -> "calibração de distância"
    StepLengthSource.CONTEXTUAL_LEARNED_PROFILE -> "perfil contextual aprendido"
}

private fun WorkoutType.toCalendarLabel(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
    WorkoutType.CYCLING -> "Bicicleta"
    WorkoutType.UNKNOWN -> "Tipo indefinido"
}

private fun ActivityWorkflowState.toCalendarLabel(): String = when (this) {
    ActivityWorkflowState.PENDING_REVIEW -> "Aguardando revisão"
    ActivityWorkflowState.PENDING_CALIBRATION -> "Aguardando calibração"
    ActivityWorkflowState.READY_TO_SYNC -> "Pronta para sincronizar"
    ActivityWorkflowState.SYNCING -> "Sincronizando"
    ActivityWorkflowState.SYNCED -> "Sincronizada"
    ActivityWorkflowState.SYNC_FAILED -> "Com erro"
    ActivityWorkflowState.DISCARDED -> "Descartada"
}

private fun CalendarSyncState.toCalendarLabel(): String = when (this) {
    CalendarSyncState.NO_OPERATIONS -> "Sem operações"
    CalendarSyncState.PENDING -> "Pendente"
    CalendarSyncState.SYNCING -> "Sincronizando"
    CalendarSyncState.SYNCED -> "Sincronizado"
    CalendarSyncState.FAILED -> "Falha"
    CalendarSyncState.PARTIAL -> "Parcial"
}

private fun CalendarDataSource.toCalendarLabel(): String = when (this) {
    CalendarDataSource.ASSOCIATED_DEVICE -> "dispositivo associado"
    CalendarDataSource.DAILY_SUMMARY -> "resumo diário"
    CalendarDataSource.HEART_RATE -> "frequência cardíaca"
    CalendarDataSource.WORKOUT_ACTIVITY -> "atividades"
    CalendarDataSource.WEIGHT_MEASUREMENT -> "peso"
    CalendarDataSource.HEALTH_SYNC_QUEUE -> "fila local"
    CalendarDataSource.PREFERENCES -> "preferências"
}

private fun CalendarDataIssueReason.toCalendarLabel(): String = when (this) {
    CalendarDataIssueReason.SOURCE_UNAVAILABLE -> "fonte indisponível"
    CalendarDataIssueReason.INVALID_LOCAL_DATA -> "dados locais inválidos"
    CalendarDataIssueReason.UNRESOLVED_AGGREGATE -> "agregado não resolvido"
}

private fun CalendarDaySummary.hasIssue(source: CalendarDataSource): Boolean =
    issues.any { it.source == source }

@Composable
private fun healthPrimaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

@Composable
private fun healthSecondaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.secondary
} else {
    LightColorScheme.secondary
}

@Composable
private fun healthTertiaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.tertiary
} else {
    LightColorScheme.tertiary
}

private fun br.com.mwconsulting.championcompanion.core.model.DataOrigin.toCalendarLabel(): String = when (this) {
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_DAILY_SUMMARY -> "Relógio"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D1_HISTORY -> "Histórico D1"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.WATCH_D3_D4 -> "Relógio D3/D4"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.MANUAL_ENTRY -> "Entrada manual"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.APP_WORKOUT_SESSION -> "Aplicativo"
    br.com.mwconsulting.championcompanion.core.model.DataOrigin.DERIVED -> "Derivada localmente"
}

private fun DomainValue<Double>.toNumberLabel(): String = when (this) {
    is DomainValue.Present -> NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(value)
    is DomainValue.Absent -> "indisponível"
}

private fun DomainValue<Int>.toBpmLabel(): String = when (this) {
    is DomainValue.Present -> "$value BPM"
    is DomainValue.Absent -> "indisponível"
}

private fun DomainValue<Double>.toDistanceLabel(): String = when (this) {
    is DomainValue.Present -> "${NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(value / 1_000.0)} km"
    is DomainValue.Absent -> "indisponível"
}

private fun CalendarWeightMeasurementSummary.toWeightLabel(): String =
    "${NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(value)} ${if (unit == WeightUnit.KILOGRAMS) "kg" else "lb"}"

private fun Duration.toCalendarLabel(): String {
    val totalMinutes = toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 -> "$hours h"
        else -> "$minutes min"
    }
}

private fun Instant.formatCalendarTime(zoneId: ZoneId, locale: Locale = Locale.forLanguageTag("pt-BR")): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(locale)
    .withZone(zoneId)
    .format(this)

private fun CalendarUiError.toCalendarDayMessage(): String = when (category) {
    CalendarUiErrorCategory.INVALID_DATE -> "A data não pode ser aberta neste detalhe."
    CalendarUiErrorCategory.INVALID_LOCAL_DATA -> "Há dados locais inválidos para esta data."
    CalendarUiErrorCategory.SOURCE_UNAVAILABLE,
    CalendarUiErrorCategory.INTERNAL,
    -> "Não foi possível consultar dados locais. Tente novamente."
}
