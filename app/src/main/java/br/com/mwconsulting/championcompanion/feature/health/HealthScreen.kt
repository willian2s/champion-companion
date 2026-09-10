package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HealthScreen(
    onOpenHeartRate: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenExercises: () -> Unit = {},
    onOpenDevice: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    healthViewModel: HealthViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[HealthViewModel::class.java]
        }
    },
) {
    val state by healthViewModel.state.collectAsState()

    LaunchedEffect(healthViewModel) {
        while (true) {
            healthViewModel.onIntent(HealthIntent.Load)
            val now = ZonedDateTime.now()
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay((nextDay.toEpochSecond() - now.toEpochSecond()).coerceAtLeast(1L) * 1_000L)
        }
    }

    val weightValue = state.latestWeight?.let {
        "${decimalFormat().format(it.kilograms)} kg"
    } ?: when (state.weightHistory) {
        HealthWeightHistoryState.Loading -> "Carregando"
        HealthWeightHistoryState.Unavailable -> "Indisponível"
        is HealthWeightHistoryState.Available -> "Sem dados"
    }
    val weightSupportingText = state.latestWeight?.measuredAt?.let(::formatMoment)
        ?: when (state.weightHistory) {
            HealthWeightHistoryState.Loading -> "Carregando dados"
            HealthWeightHistoryState.Unavailable -> "Não foi possível carregar"
            is HealthWeightHistoryState.Available -> "Nenhum registro ainda"
        }
    val weightExtraText = state.latestWeight?.origin?.toHealthOriginLabel()
        ?: when (state.weightHistory) {
            HealthWeightHistoryState.Loading -> "Origem: aguardando dados"
            HealthWeightHistoryState.Unavailable -> "Origem: indisponível"
            is HealthWeightHistoryState.Available -> "Origem: não disponível"
        }
    val weightStateLabel = when {
        state.latestWeight != null -> "Disponível"
        state.weightHistory is HealthWeightHistoryState.Loading -> "Carregando"
        state.weightHistory is HealthWeightHistoryState.Unavailable -> "Indisponível"
        else -> "Sem dados"
    }
    val todaySummary = state.todaySummary
    val distanceValue = when {
        todaySummary == null -> "Sem dados"
        todaySummary.estimatedDistanceMeters == null -> "Indisponível"
        else -> "${decimalFormat().format(todaySummary.estimatedDistanceMeters / 1000.0)} km"
    }
    val distanceSupportingText = when {
        todaySummary == null -> "Nenhum resumo hoje"
        todaySummary.estimatedDistanceMeters == null -> "Sem estimativa hoje"
        else -> formatMoment(todaySummary.updatedAt)
    }
    val distanceExtraText = when {
        todaySummary == null -> "Origem: não disponível"
        todaySummary.estimatedDistanceMeters == null -> "Origem: ${todaySummary.sourceLabel}"
        else -> "Origem: ${todaySummary.sourceLabel} • Estimativa local"
    }
    val distanceStateLabel = when {
        todaySummary == null -> "Sem dados"
        todaySummary.estimatedDistanceMeters == null -> "Indisponível"
        else -> "Disponível"
    }
    val today = LocalDate.now(ZoneId.systemDefault())
    val todayHeartRateEntries = state.todayHeartRateHistory.entries.filter {
        it.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val heartRateMinimum = todayHeartRateEntries.minOfOrNull { it.bpm }
    val heartRateMaximum = todayHeartRateEntries.maxOfOrNull { it.bpm }
    val heartRateValue = when {
        heartRateMinimum != null && heartRateMinimum == heartRateMaximum ->
            "$heartRateMinimum BPM"
        heartRateMinimum != null && heartRateMaximum != null ->
            "$heartRateMinimum a $heartRateMaximum BPM"
        else -> "Sem dados"
    }
    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screen, vertical = tokens.spacing.compact)
                    .heightIn(min = 1.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.item),
            ) {
                ChampionRedesignSurfaceHeader(
                    title = "Saúde",
                    supportingText = "Relógio: ${if (state.hasAssociatedDevice) "Conectado" else "Pendente"}",
                    actionsContent = {
                        ChampionRedesignHeaderAction(
                            contentDescription = "Abrir Dispositivo",
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
                Text(
                    text = "Principais métricas",
                    modifier = Modifier.semantics { heading() },
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HealthMetricsList(
                    heartRateValue = heartRateValue,
                    heartRateExtraText = state.associatedDeviceName?.let { "Origem: $it" }
                        ?: "Origem: não disponível",
                    weightValue = weightValue,
                    weightSupportingText = weightSupportingText,
                    weightExtraText = weightExtraText,
                    weightStateLabel = weightStateLabel,
                    stepsValue = todaySummary?.let { "${numberFormat().format(it.steps)} passos" } ?: "Sem dados",
                    stepsSupportingText = todaySummary?.updatedAt?.let(::formatMoment) ?: "Nenhum resumo hoje",
                    stepsExtraText = todaySummary?.sourceLabel?.let { "Origem: $it" }
                        ?: "Origem: não disponível",
                    distanceValue = distanceValue,
                    distanceSupportingText = distanceSupportingText,
                    distanceExtraText = distanceExtraText,
                    distanceStateLabel = distanceStateLabel,
                    onOpenHeartRate = onOpenHeartRate,
                    onOpenWeight = onOpenWeight,
                    onOpenSteps = onOpenSteps,
                    onOpenDistance = onOpenDistance,
                    onOpenExercises = onOpenExercises,
                )
            }
        }
    }
}

@Composable
private fun HealthMetricsList(
    heartRateValue: String,
    heartRateExtraText: String,
    weightValue: String,
    weightSupportingText: String,
    weightExtraText: String,
    weightStateLabel: String,
    stepsValue: String,
    stepsSupportingText: String,
    stepsExtraText: String,
    distanceValue: String,
    distanceSupportingText: String,
    distanceExtraText: String,
    distanceStateLabel: String,
    onOpenHeartRate: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenExercises: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        HealthListMetricCard(
            title = "Atividades",
            value = "Abrir atividades",
            supportingText = "Treinos importados",
            extraText = "Calendário dentro de Exercícios",
            isAvailable = true,
            onClick = onOpenExercises,
        )
        HealthListMetricCard(
            title = "Frequência cardíaca",
            value = heartRateValue,
            supportingText = "Hoje",
            extraText = heartRateExtraText,
            isAvailable = heartRateValue != "Sem dados",
            onClick = onOpenHeartRate,
        )
        HealthListMetricCard(
            title = "Peso",
            value = weightValue,
            supportingText = weightSupportingText,
            extraText = weightExtraText,
            isAvailable = weightStateLabel == "Disponível",
            onClick = onOpenWeight,
        )
        HealthListMetricCard(
            title = "Passos",
            value = stepsValue,
            supportingText = stepsSupportingText,
            extraText = stepsExtraText,
            isAvailable = stepsValue != "Sem dados",
            onClick = onOpenSteps,
        )
        HealthListMetricCard(
            title = "Distância",
            value = distanceValue,
            supportingText = distanceSupportingText,
            extraText = distanceExtraText,
            isAvailable = distanceStateLabel == "Disponível",
            onClick = onOpenDistance,
        )
    }
}

@Composable
private fun HealthListMetricCard(
    title: String,
    value: String,
    supportingText: String,
    extraText: String,
    isAvailable: Boolean,
    onClick: () -> Unit,
) {
    val cardColors = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        DarkColorScheme.primaryContainer to DarkColorScheme.onPrimaryContainer
    } else {
        LightColorScheme.primaryContainer to LightColorScheme.onPrimaryContainer
    }
    val valueColor = if (isAvailable) cardColors.second else cardColors.second.copy(alpha = 0.7f)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .semantics {
                contentDescription = "$title. $value. $supportingText. $extraText. Toque para abrir detalhes."
            },
        colors = CardDefaults.cardColors(
            containerColor = cardColors.first,
            contentColor = cardColors.second,
        ),
    ) {
        Row(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = value,
                    color = valueColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = extraText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = cardColors.second,
            )
        }
    }
}

private fun formatMoment(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun numberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

private fun decimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
