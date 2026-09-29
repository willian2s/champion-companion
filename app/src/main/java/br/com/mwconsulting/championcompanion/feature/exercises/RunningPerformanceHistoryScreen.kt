package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.model.RunningPerformanceHistoryStatus

@Composable
fun RunningPerformanceHistoryScreen(
    state: RunningPerformanceHistoryUiState,
    onNavigateBack: () -> Unit,
    backLabel: String,
) {
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
                supportingText = "Ritmo e frequência cardíaca nos últimos 56 dias.",
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
            RunningPerformanceHistoryState(state)
        }
    }
}

@Composable
private fun RunningPerformanceHistoryState(state: RunningPerformanceHistoryUiState) {
    when (state) {
        RunningPerformanceHistoryUiState.Loading -> LoadingState(
            label = "Carregando tendência de corrida",
            modifier = Modifier.fillMaxSize(),
        )

        RunningPerformanceHistoryUiState.Unavailable -> ErrorState(
            title = "Tendência de corrida indisponível",
            message = "Não foi possível carregar o histórico de corrida.",
            modifier = Modifier.fillMaxSize(),
        )

        is RunningPerformanceHistoryUiState.Ready -> when (state.result.status) {
            RunningPerformanceHistoryStatus.AVAILABLE -> Text(
                text = "${state.result.points.size} ${if (state.result.points.size == 1) "corrida" else "corridas"} encontrada(s) nos últimos 56 dias.",
                style = MaterialTheme.typography.bodyLarge,
            )

            RunningPerformanceHistoryStatus.NO_RUNS -> EmptyState(
                title = "Nenhuma corrida no período",
                message = "Corridas confirmadas aparecerão aqui quando estiverem disponíveis.",
                modifier = Modifier.fillMaxWidth(),
            )

            RunningPerformanceHistoryStatus.NO_COMPARABLE_RUNS -> EmptyState(
                title = "Nenhuma corrida comparável",
                message = "Não há ritmo comparável disponível para as corridas confirmadas no período.",
                modifier = Modifier.fillMaxWidth(),
            )

            RunningPerformanceHistoryStatus.UNAVAILABLE -> ErrorState(
                title = "Tendência de corrida indisponível",
                message = "Não foi possível carregar o histórico de corrida.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
