package br.com.mwconsulting.championcompanion.feature.health

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.SyncStatusBadge
import br.com.mwconsulting.championcompanion.core.designsystem.SyncVisualState
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
            Text(
                text = "Métricas locais",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HealthMetricSummaryCard(
                title = "Frequência cardíaca",
                value = state.latestMeasurement?.let { "${it.bpm} BPM" } ?: "Sem medições",
                supportingText = state.latestMeasurement?.measuredAt?.let(::formatMoment)
                    ?: "Nenhuma medição persistida",
                extraText = state.latestD1ImportMetadata?.lastSuccessAt?.let { "Última importação D1: ${formatMoment(it)}" }
                    ?: state.associatedDeviceName?.let { "Origem: $it" }
                    ?: "Origem: não disponível",
                isAvailable = state.latestMeasurement != null,
                onClick = onOpenHeartRate,
            )
            HealthMetricSummaryCard(
                title = "Peso",
                value = state.latestWeight?.let { "${decimalFormat().format(it.kilograms)} kg" } ?: "Sem dados",
                supportingText = state.latestWeight?.measuredAt?.let(::formatMoment) ?: "Nenhum peso persistido",
                extraText = state.latestWeight?.origin?.toHealthOriginLabel() ?: "Origem: não disponível",
                isAvailable = state.latestWeight != null,
                onClick = onOpenWeight,
            )
            HealthMetricSummaryCard(
                title = "Passos",
                value = state.todaySummary?.let { "${numberFormat().format(it.steps)} passos" } ?: "Sem dados",
                supportingText = state.todaySummary?.updatedAt?.let(::formatMoment) ?: "Resumo diário indisponível",
                extraText = state.todaySummary?.sourceLabel?.let { "Origem: $it" } ?: "Origem: não disponível",
                isAvailable = state.todaySummary != null,
                onClick = onOpenSteps,
            )
            HealthMetricSummaryCard(
                title = "Distância",
                value = state.todaySummary?.estimatedDistanceMeters?.let { "${decimalFormat().format(it / 1000.0)} km" } ?: "Indisponível",
                supportingText = state.todaySummary?.updatedAt?.let(::formatMoment)
                    ?: "Sem resumo diário ou dados pessoais suficientes",
                extraText = state.todaySummary?.let { summary ->
                    if (summary.estimatedDistanceMeters != null) {
                        "Origem: ${summary.sourceLabel} • Estimativa local"
                    } else {
                        "Origem: não disponível"
                    }
                } ?: "Origem: não disponível",
                isAvailable = state.todaySummary?.estimatedDistanceMeters != null,
                onClick = onOpenDistance,
            )
            if (state.healthConnectSync.hasVisibleState) {
                HealthConnectHubStatusCard(state.healthConnectSync)
            }
        }
    }
}

@Composable
private fun HealthHubHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChampionSpacing.small),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = "Saúde",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Resumo local de hoje. Abra uma métrica para ver detalhes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HealthMetricSummaryCard(
    title: String,
    value: String,
    supportingText: String,
    extraText: String,
    isAvailable: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $value. ${if (isAvailable) "Disponível" else "Sem dados"}. $supportingText. $extraText. Toque para abrir detalhes."
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
            Text(
                text = "›",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun HealthConnectHubStatusCard(sync: HealthConnectSyncUiModel) {
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
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(ChampionSpacing.small))
                SyncStatusBadge(state = sync.toVisualState())
            }
            Text(
                when {
                    sync.syncingCount > 0 -> "Sincronização secundária em andamento. Os dados locais continuam como mensagem principal."
                    sync.failedCount > 0 -> "Há falhas pendentes no Health Connect, sem rebaixar os dados locais já persistidos."
                    sync.pendingCount > 0 -> "Existem registros aguardando envio ao Health Connect."
                    else -> "Sincronização secundária sem pendências visíveis no momento."
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
                    "Último envio ao Health Connect: ${formatMoment(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun HealthConnectSyncUiModel.toVisualState(): SyncVisualState = when {
    failedCount > 0 -> SyncVisualState.Failure
    pendingCount > 0 || syncingCount > 0 -> SyncVisualState.Pending
    else -> SyncVisualState.Success
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
