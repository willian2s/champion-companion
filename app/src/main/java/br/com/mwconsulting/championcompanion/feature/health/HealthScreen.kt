package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
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
            HealthHubHeader()
            HealthMetricSummaryCard(
                title = "Frequência cardíaca",
                value = state.latestMeasurement?.let { "${it.bpm} BPM" } ?: "Sem medições",
                supportingText = state.latestMeasurement?.measuredAt?.let(::formatMoment)
                    ?: "Abra para medir, importar D1 e ver histórico consolidado",
                extraText = state.latestD1ImportMetadata?.lastSuccessAt?.let { "Última importação D1: ${formatMoment(it)}" }
                    ?: state.associatedDeviceName?.let { "Origem: $it" }
                    ?: "Origem: relógio associado",
                onClick = onOpenHeartRate,
            )
            HealthMetricSummaryCard(
                title = "Peso",
                value = state.latestWeight?.let { "${decimalFormat().format(it.kilograms)} kg" } ?: "Sem dados",
                supportingText = state.latestWeight?.measuredAt?.let(::formatMoment) ?: "Abra para ver valor atual",
                extraText = state.latestWeight?.origin?.toHealthOriginLabel() ?: "Estado vazio quando nenhum peso existir",
                onClick = onOpenWeight,
            )
            HealthMetricSummaryCard(
                title = "Passos",
                value = state.todaySummary?.let { "${numberFormat().format(it.steps)} passos" } ?: "Sem dados",
                supportingText = state.todaySummary?.updatedAt?.let(::formatMoment) ?: "Abra para ver progresso diário",
                extraText = state.todaySummary?.sourceLabel ?: "Resumo diário persistido da Fase 07",
                onClick = onOpenSteps,
            )
            HealthMetricSummaryCard(
                title = "Distância",
                value = state.todaySummary?.estimatedDistanceMeters?.let { "${decimalFormat().format(it / 1000.0)} km" } ?: "Indisponível",
                supportingText = state.todaySummary?.updatedAt?.let(::formatMoment) ?: "Abra para ver origem e estado atual",
                extraText = if (state.todaySummary?.estimatedDistanceMeters != null) {
                    "Estimativa local"
                } else {
                    "Sem dados pessoais suficientes ou sem resumo diário"
                },
                onClick = onOpenDistance,
            )
        }
    }
}

@Composable
private fun HealthHubHeader() {
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
                text = "A área Saúde agora resume métricas persistidas e leva para telas específicas. Nenhum card inicia BLE diretamente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
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
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$title. $value. $supportingText. Toque para abrir detalhes."
            },
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
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
