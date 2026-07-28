package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DistanceDetailScreen(
    onNavigateBack: () -> Unit,
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val summary = state.todaySummary
    MetricDetailScaffold(
        title = "Distância",
        description = "Esta tela reutiliza distância estimada já persistida no resumo diário. Quando faltarem dados pessoais, o valor permanece indisponível.",
        onNavigateBack = onNavigateBack,
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null || summary.estimatedDistanceMeters == null) {
                HealthMetricEmptyState(
                    message = if (summary == null) {
                        "Nenhum resumo diário persistido foi encontrado para hoje."
                    } else {
                        "A distância de hoje está indisponível porque ainda faltam dados pessoais suficientes para a estimativa."
                    },
                )
            } else {
                MetricValueBlock(
                    value = "${distanceDecimalFormat().format(summary.estimatedDistanceMeters / 1000.0)} km",
                    supporting = "Estimativa de hoje",
                    extra = "Atualizado em ${formatDateTime(summary.updatedAt)} • ${summary.sourceLabel}",
                )
            }
        }
        DistanceHistoryShell()
    }
}

@Composable
private fun DistanceHistoryShell() {
    HealthMetricChartSection(
        title = "Gráfico",
        message = "Gráficos de distância ficam para a fase de calendário e histórico consolidado.",
    )
    HealthMetricHistorySection(
        title = "Histórico",
        emptyMessage = "Histórico completo de distância ainda não faz parte desta fase.",
    )
}

private fun distanceDecimalFormat(): NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 0
}
