package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionCartesianChartData
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartFormatters
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartInteractionPolicy
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartPoint
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartSeries
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartUnit
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartValueFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisFormatter
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionChartXAxisSpec
import br.com.mwconsulting.championcompanion.core.designsystem.chart.ChampionTimeSeriesChart
import br.com.mwconsulting.championcompanion.core.model.RunningPerformanceDistanceProvenance
import br.com.mwconsulting.championcompanion.core.model.RunningPerformanceHeartRateStatus
import br.com.mwconsulting.championcompanion.core.model.RunningPerformanceHistory
import br.com.mwconsulting.championcompanion.core.model.RunningPerformanceHistoryStatus
import br.com.mwconsulting.championcompanion.core.model.RunningPerformancePaceStatus
import br.com.mwconsulting.championcompanion.core.model.RunningPerformancePoint
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val RUNNING_HISTORY_PERIOD_LABEL = "Últimos 56 dias"
private const val RUNNING_HISTORY_PAGE_SIZE = 10

@Composable
fun RunningPerformanceHistoryScreen(
    state: RunningPerformanceHistoryUiState,
    onNavigateBack: () -> Unit,
    backLabel: String,
) {
    val locale = LocalConfiguration.current.locales[0]
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
            ChampionRedesignSurfaceHeader(
                title = "Tendência de corrida",
                supportingText = "Ritmo e frequência cardíaca",
                navigationContent = {
                    ChampionRedesignHeaderNavigationAction(
                        contentDescription = backLabel,
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
            Text(
                text = "Histórico factual das corridas confirmadas do relógio.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = RUNNING_HISTORY_PERIOD_LABEL,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            RunningPerformanceHistoryState(state = state, locale = locale)
        }
    }
}

@Composable
private fun RunningPerformanceHistoryState(
    state: RunningPerformanceHistoryUiState,
    locale: Locale,
) {
    when (state) {
        RunningPerformanceHistoryUiState.Loading -> LoadingState(
            label = "Carregando histórico de ritmo e frequência cardíaca",
            modifier = Modifier.fillMaxWidth(),
        )

        RunningPerformanceHistoryUiState.Unavailable -> unavailableHistoryState()

        is RunningPerformanceHistoryUiState.Ready -> when (state.result.status) {
            RunningPerformanceHistoryStatus.AVAILABLE -> RunningPerformanceHistoryContent(
                history = state.result,
                zoneId = state.zoneId,
                locale = locale,
                showComparableCharts = true,
            )

            RunningPerformanceHistoryStatus.NO_RUNS -> EmptyState(
                title = "Nenhuma corrida no período",
                message = "Corridas confirmadas aparecerão aqui quando estiverem disponíveis.",
                modifier = Modifier.fillMaxWidth(),
            )

            RunningPerformanceHistoryStatus.NO_COMPARABLE_RUNS -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
            ) {
                EmptyState(
                    title = "Nenhuma corrida comparável",
                    message = "As corridas confirmadas estão listadas abaixo, mas não há ritmo comparável para os gráficos.",
                    modifier = Modifier.fillMaxWidth(),
                )
                RunningPerformanceHistoryContent(
                    history = state.result,
                    zoneId = state.zoneId,
                    locale = locale,
                    showComparableCharts = false,
                )
            }

            RunningPerformanceHistoryStatus.UNAVAILABLE -> unavailableHistoryState()
        }
    }
}

@Composable
private fun unavailableHistoryState() {
    ErrorState(
        title = "Tendência de corrida indisponível",
        message = "A fonte de dados do histórico está indisponível. Nenhum histórico parcial é exibido.",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RunningPerformanceHistoryContent(
    history: RunningPerformanceHistory,
    zoneId: ZoneId,
    locale: Locale,
    showComparableCharts: Boolean,
) {
    val points = history.points
    val comparablePoints = points.filter { it.paceStatus == RunningPerformancePaceStatus.COMPARABLE_NATIVE }
    val heartRateGaps = comparablePoints.count { it.heartRateStatus != RunningPerformanceHeartRateStatus.AVAILABLE }
    val partialPoints = points.count(RunningPerformancePoint::partial)
    val provenanceLabels = points.mapNotNull { it.distanceProvenance.toUserLabel() }.distinct()
    val historyDescription = buildString {
        append("Período: $RUNNING_HISTORY_PERIOD_LABEL. ")
        append("${points.size} ${if (points.size == 1) "corrida" else "corridas"}. ")
        append("Ritmo em minutos e segundos por quilômetro, distância em km, frequência cardíaca em bpm e cobertura em percentual. ")
        if (showComparableCharts) {
            append("$heartRateGaps ${if (heartRateGaps == 1) "corrida tem" else "corridas têm"} lacuna ou ausência de frequência cardíaca nos gráficos.")
        } else {
            append("Não há gráficos comparáveis para exibir.")
        }
        if (partialPoints > 0) append(" $partialPoints ${if (partialPoints == 1) "corrida tem" else "corridas têm"} dados parciais.")
        if (provenanceLabels.isNotEmpty()) append(" Proveniências: ${provenanceLabels.joinToString(", ")}.")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = historyDescription },
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        if (showComparableCharts && comparablePoints.isNotEmpty()) {
            RunningPerformanceCharts(
                points = comparablePoints,
                zoneId = zoneId,
                locale = locale,
            )
        }
        RunningPerformancePointList(
            points = points,
            zoneId = zoneId,
            locale = locale,
        )
    }
}

@Composable
private fun RunningPerformancePointList(
    points: List<RunningPerformancePoint>,
    zoneId: ZoneId,
    locale: Locale,
) {
    val dateTimeFormatter = remember(zoneId, locale) {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", locale)
    }
    var selectedPage by rememberSaveable(points) { mutableIntStateOf(0) }
    val pageCount = (points.size + RUNNING_HISTORY_PAGE_SIZE - 1) / RUNNING_HISTORY_PAGE_SIZE
    val currentPage = selectedPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val visiblePoints = points
        .drop(currentPage * RUNNING_HISTORY_PAGE_SIZE)
        .take(RUNNING_HISTORY_PAGE_SIZE)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Corridas do período (${points.size})",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        visiblePoints.forEach { point ->
            RunningPerformancePointCard(
                point = point,
                dateTimeLabel = dateTimeFormatter.format(point.startedAt.atZone(zoneId)),
                locale = locale,
            )
        }
        if (pageCount > 1) {
            RunningPerformanceHistoryPagination(
                currentPage = currentPage,
                pageCount = pageCount,
                onPrevious = { selectedPage = (currentPage - 1).coerceAtLeast(0) },
                onNext = { selectedPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
            )
        }
    }
}

@Composable
private fun RunningPerformanceHistoryPagination(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = currentPage > 0) {
            Text("Anterior")
        }
        Text(
            text = "Página ${currentPage + 1} de $pageCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onNext, enabled = currentPage < pageCount - 1) {
            Text("Próxima")
        }
    }
}

@Composable
private fun RunningPerformancePointCard(
    point: RunningPerformancePoint,
    dateTimeLabel: String,
    locale: Locale,
) {
    val rowDescription = point.toAccessibilityDescription(dateTimeLabel, locale)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDescription },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = dateTimeLabel,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
            ) {
                PerformanceMetric(
                    label = "Ritmo",
                    value = point.paceSecondsPerKilometer?.let(::formatPace) ?: "indisponível",
                )
                PerformanceMetric(
                    label = "Distância",
                    value = point.canonicalDistanceMeters?.let { formatDistance(it, locale) } ?: "indisponível",
                )
            }
            RunningPerformanceHeartRateSummary(point)
            Text(
                text = "Proveniência: ${point.distanceProvenanceLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = point.paceStatusLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (point.partial) {
                Text(
                    text = "Dados parciais: a cobertura de frequência cardíaca não cobre todo o período da corrida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowScope.PerformanceMetric(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RunningPerformanceHeartRateSummary(point: RunningPerformancePoint) {
    val heartRateText = when (point.heartRateStatus) {
        RunningPerformanceHeartRateStatus.AVAILABLE ->
            "FC média: ${point.averageHeartRateBpm?.roundToInt() ?: "indisponível"} bpm"
        RunningPerformanceHeartRateStatus.NO_HEART_RATE_SAMPLES -> "FC média: sem amostras"
        RunningPerformanceHeartRateStatus.INVALID_SAMPLES -> "FC média: amostras inválidas"
        RunningPerformanceHeartRateStatus.SOURCE_UNAVAILABLE -> "FC média: fonte indisponível"
    }
    val coverageText = point.heartRateCoverage?.let { "Cobertura de FC: ${formatCoverage(it)}" }
        ?: "Cobertura de FC: indisponível"
    Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
        Text(text = heartRateText, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = coverageText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RunningPerformanceCharts(
    points: List<RunningPerformancePoint>,
    zoneId: ZoneId,
    locale: Locale,
) {
    val dateTimeFormatter = remember(zoneId, locale) {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", locale)
    }
    val labels = points.map { dateTimeFormatter.format(it.startedAt.atZone(zoneId)) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        Text(
            text = "Gráficos do histórico comparável",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Cada ponto representa uma corrida. Os dois gráficos usam a mesma ordem das corridas e escalas independentes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Ritmo",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        ChampionTimeSeriesChart(
            data = buildPaceChartData(points, labels),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Frequência cardíaca média",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        ChampionTimeSeriesChart(
            data = buildHeartRateChartData(points, labels),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun buildPaceChartData(
    points: List<RunningPerformancePoint>,
    labels: List<String>,
): ChampionCartesianChartData = ChampionCartesianChartData(
    series = listOf(
        ChampionChartSeries(
            key = "running-pace",
            label = "Ritmo",
            points = points.mapIndexed { index, point ->
                ChampionChartPoint(
                    x = index.toDouble(),
                    y = point.paceSecondsPerKilometer,
                    xLabel = labels[index],
                    contentDescription = "${labels[index]}: ${point.paceSecondsPerKilometer?.let(::formatPace) ?: "ritmo indisponível"}.",
                )
            },
        ),
    ),
    xAxis = runningHistoryXAxis(labels),
    yAxis = ChampionChartAxisSpec(
        title = "Ritmo (s/km)",
        formatter = ChampionChartValueFormatter { value -> formatPaceAxisValue(value) },
        unit = ChampionChartUnit("/km"),
        gridLinesVisible = true,
    ),
    interaction = ChampionChartInteractionPolicy(scrollEnabled = false, zoomEnabled = false),
    accessibilitySummary = "Gráfico de ritmo em segundos por quilômetro, com apresentação em minutos e segundos por quilômetro para ${points.size} corridas.",
)

private fun buildHeartRateChartData(
    points: List<RunningPerformancePoint>,
    labels: List<String>,
): ChampionCartesianChartData = ChampionCartesianChartData(
    series = listOf(
        ChampionChartSeries(
            key = "running-heart-rate",
            label = "Frequência cardíaca média",
            points = points.mapIndexed { index, point ->
                val heartRate = point.averageHeartRateBpm.takeIf {
                    point.heartRateStatus == RunningPerformanceHeartRateStatus.AVAILABLE
                }
                ChampionChartPoint(
                    x = index.toDouble(),
                    y = heartRate,
                    xLabel = labels[index],
                    contentDescription = "${labels[index]}: ${heartRate?.let { "${it.roundToInt()} bpm" } ?: "sem frequência cardíaca"}.",
                )
            },
        ),
    ),
    xAxis = runningHistoryXAxis(labels),
    yAxis = ChampionChartAxisSpec(
        title = "Frequência cardíaca",
        formatter = ChampionChartFormatters.integer(),
        unit = ChampionChartUnit("bpm"),
        gridLinesVisible = true,
    ),
    interaction = ChampionChartInteractionPolicy(scrollEnabled = false, zoomEnabled = false),
    accessibilitySummary = "Gráfico de frequência cardíaca média em bpm para ${points.size} corridas. Ausências de frequência cardíaca permanecem como lacunas.",
)

private fun runningHistoryXAxis(labels: List<String>): ChampionChartXAxisSpec = ChampionChartXAxisSpec(
    formatter = ChampionChartXAxisFormatter { value -> labels.getOrNull(value.roundToInt()) ?: "--" },
    title = "Corridas em ordem cronológica",
    gridLinesVisible = false,
    labelValues = labels.indices.map(Int::toDouble),
)

private fun RunningPerformancePoint.toAccessibilityDescription(
    dateTimeLabel: String,
    locale: Locale,
): String = buildString {
    append(dateTimeLabel)
    append(". Ritmo: ")
    append(paceSecondsPerKilometer?.let(::formatPace) ?: "indisponível")
    append(". Distância: ")
    append(canonicalDistanceMeters?.let { formatDistance(it, locale) } ?: "indisponível")
    append(". ")
    append(
        when (heartRateStatus) {
            RunningPerformanceHeartRateStatus.AVAILABLE ->
                "FC média ${averageHeartRateBpm?.roundToInt() ?: "indisponível"} bpm, ${heartRateCoverage?.let(::formatCoverage) ?: "cobertura indisponível"} de cobertura"
            RunningPerformanceHeartRateStatus.NO_HEART_RATE_SAMPLES -> "FC sem amostras"
            RunningPerformanceHeartRateStatus.INVALID_SAMPLES -> "FC com amostras inválidas"
            RunningPerformanceHeartRateStatus.SOURCE_UNAVAILABLE -> "FC com fonte indisponível"
        },
    )
    append(". Proveniência: ${distanceProvenanceLabel()}.")
    append(" ")
    append(paceStatusLabel())
    if (partial) append(" Dados parciais.")
}

private fun RunningPerformancePoint.distanceProvenanceLabel(): String = when (paceStatus) {
    RunningPerformancePaceStatus.AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE ->
        "Distância do relógio — versão não comparável"
    else -> distanceProvenance.toUserLabel() ?: "indisponível"
}

private fun RunningPerformancePoint.paceStatusLabel(): String = when (paceStatus) {
    RunningPerformancePaceStatus.COMPARABLE_NATIVE -> "Ritmo comparável no histórico."
    RunningPerformancePaceStatus.AVAILABLE_FALLBACK_NOT_COMPARABLE ->
        "Ritmo disponível; estimativa por comprimento de passo, fora dos gráficos comparáveis."
    RunningPerformancePaceStatus.AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE ->
        "Ritmo disponível; Distância do relógio — versão não comparável, fora dos gráficos comparáveis."
    RunningPerformancePaceStatus.NO_ACTIVE_SNAPSHOT ->
        "Ritmo indisponível: dados de derivação da corrida não estão disponíveis."
    RunningPerformancePaceStatus.TEMPORAL_INELIGIBLE ->
        "Ritmo indisponível: a temporalidade da corrida não é elegível."
    RunningPerformancePaceStatus.PLAUSIBILITY_BLOCKED ->
        "Ritmo não comparável: há indicador de plausibilidade registrado."
    RunningPerformancePaceStatus.INVALID_CANONICAL_FACTS ->
        "Ritmo indisponível: dados canônicos inválidos."
}

private fun RunningPerformanceDistanceProvenance?.toUserLabel(): String? = when (this) {
    RunningPerformanceDistanceProvenance.WATCH_NATIVE -> "Distância do relógio"
    RunningPerformanceDistanceProvenance.WATCH_NATIVE_CONTEXT_CALIBRATED -> "Distância do relógio calibrada"
    RunningPerformanceDistanceProvenance.STEP_LENGTH_FALLBACK,
    RunningPerformanceDistanceProvenance.FIXED_PERSONAL_STEP_LENGTH,
    -> "Distância estimada por comprimento de passo"
    null -> null
}

private fun formatPace(secondsPerKilometer: Double): String {
    if (!secondsPerKilometer.isFinite() || secondsPerKilometer <= 0.0) return "indisponível"
    val totalSeconds = secondsPerKilometer.roundToInt().coerceAtLeast(1)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')} /km"
}

private fun formatPaceAxisValue(secondsPerKilometer: Double): String =
    formatPace(secondsPerKilometer).removeSuffix(" /km")

private fun formatDistance(distanceMeters: Double, locale: Locale): String {
    if (!distanceMeters.isFinite() || distanceMeters <= 0.0) return "indisponível"
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = false
    }.format(distanceMeters / 1_000.0) + " km"
}

private fun formatCoverage(coverage: Double): String {
    if (!coverage.isFinite()) return "indisponível"
    return "${(coverage.coerceIn(0.0, 1.0) * 100.0).roundToInt()}%"
}
