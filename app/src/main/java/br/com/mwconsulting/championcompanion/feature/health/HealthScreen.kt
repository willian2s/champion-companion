package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionScreenContent
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HealthScreen(
    onOpenHeartRate: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenDistance: () -> Unit,
    healthViewModel: HealthViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[HealthViewModel::class.java]
        }
    },
) {
    val state by healthViewModel.state.collectAsState()

    LaunchedEffect(healthViewModel) {
        healthViewModel.onIntent(HealthIntent.Load)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ChampionScreenContent(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = 1.dp),
        ) {
            HealthHubHeader()
            Text(
                text = "Métricas locais",
                modifier = Modifier.semantics { heading() },
                color = if (isSystemInDarkTheme()) White else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HealthMetricSummaryCard(
                title = "Frequência cardíaca",
                value = state.latestMeasurement?.let { "${it.bpm} BPM" } ?: "Sem medições",
                supportingText = state.latestMeasurement?.measuredAt?.let(::formatMoment)
                    ?: "Nenhuma medição ainda",
                extraText = state.associatedDeviceName?.let { "Origem: $it" }
                    ?: "Origem: não disponível",
                accentColor = healthPrimaryAccent(),
                isAvailable = state.latestMeasurement != null,
                stateLabel = if (state.latestMeasurement != null) "Disponível" else "Sem dados",
                onClick = onOpenHeartRate,
            )
            HealthMetricSummaryCard(
                title = "Peso",
                value = weightValue,
                supportingText = weightSupportingText,
                extraText = weightExtraText,
                accentColor = healthSecondaryAccent(),
                isAvailable = state.latestWeight != null,
                stateLabel = weightStateLabel,
                onClick = onOpenWeight,
            )
            HealthMetricSummaryCard(
                title = "Passos",
                value = state.todaySummary?.let { "${numberFormat().format(it.steps)} passos" } ?: "Sem dados",
                supportingText = state.todaySummary?.updatedAt?.let(::formatMoment) ?: "Nenhum resumo hoje",
                extraText = state.todaySummary?.sourceLabel?.let { "Origem: $it" } ?: "Origem: não disponível",
                accentColor = healthTertiaryAccent(),
                isAvailable = state.todaySummary != null,
                stateLabel = if (state.todaySummary != null) "Disponível" else "Sem dados",
                onClick = onOpenSteps,
            )
            HealthMetricSummaryCard(
                title = "Distância",
                value = distanceValue,
                supportingText = distanceSupportingText,
                extraText = distanceExtraText,
                accentColor = healthPrimaryAccent(),
                isAvailable = state.todaySummary?.estimatedDistanceMeters != null,
                stateLabel = distanceStateLabel,
                onClick = onOpenDistance,
            )
        }
    }
}

@Composable
private fun HealthHubHeader() {
    val headerContainerColor = LightColorScheme.primary
    val headerContentColor = White

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = headerContainerColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Saúde",
                modifier = Modifier.semantics { heading() },
                color = headerContentColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Veja seus principais indicadores.",
                color = headerContentColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun HealthMetricSummaryCard(
    title: String,
    value: String,
    supportingText: String,
    extraText: String,
    accentColor: Color,
    isAvailable: Boolean,
    stateLabel: String,
    onClick: () -> Unit,
) {
    val valueColor = if (isAvailable) {
        accentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $value. $stateLabel. $supportingText. $extraText. Toque para abrir detalhes."
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
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
                tint = accentColor,
            )
        }
    }
}

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
