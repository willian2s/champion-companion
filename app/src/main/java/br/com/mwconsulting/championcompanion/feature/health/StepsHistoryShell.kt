package br.com.mwconsulting.championcompanion.feature.health

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StepsDetailScreen(
    onNavigateBack: () -> Unit,
    backLabel: String = "Voltar para Saúde",
    healthViewModel: HealthViewModel = activityHealthViewModel(),
) {
    val state by healthViewModel.state.collectAsState()
    val summary = state.todaySummary
    MetricDetailScaffold(
        title = "Passos",
        description = "Esta tela reutiliza resumo diário persistido da Fase 07. Não cria nova sincronização nem inicia BLE ao abrir.",
        onNavigateBack = onNavigateBack,
        backLabel = backLabel,
    ) {
        HealthCard(title = "Resumo atual", modifier = Modifier.fillMaxWidth()) {
            if (summary == null) {
                HealthMetricEmptyState(message = "Nenhum resumo diário persistido foi encontrado para hoje.")
            } else {
                val progress = DailySummaryPolicy.progress(summary.steps, state.dailyStepsGoal)
                MetricValueBlock(
                    value = "${stepsNumberFormat().format(summary.steps)} passos",
                    supporting = "Meta ${stepsNumberFormat().format(progress.goal)} • ${(progress.progress * 100f).roundToInt()}%",
                    extra = "Atualizado em ${formatDateTime(summary.updatedAt)} • ${summary.sourceLabel}",
                )
                LinearProgressIndicator(
                    progress = { progress.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = ChampionSpacing.medium),
                )
            }
        }
        StepsHistoryShell()
    }
}

@Composable
private fun StepsHistoryShell() {
    HealthMetricChartSection(
        title = "Gráfico",
        message = "Gráficos de passos ficam para a fase de calendário e histórico consolidado.",
    )
    HealthMetricHistorySection(
        title = "Histórico",
        emptyMessage = "Histórico completo de passos ainda não faz parte desta fase.",
    )
}

private fun stepsNumberFormat(): NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
