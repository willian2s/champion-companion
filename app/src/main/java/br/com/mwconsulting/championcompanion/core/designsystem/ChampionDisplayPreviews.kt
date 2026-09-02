package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Exibição clara", showBackground = true)
@Composable
private fun LightDisplayPreview() {
    DisplayPreview(darkTheme = false)
}

@Preview(name = "Exibição escura", showBackground = true)
@Composable
private fun DarkDisplayPreview() {
    DisplayPreview(darkTheme = true)
}

@Preview(name = "Exibição fonte ampliada", fontScale = 1.3f, showBackground = true)
@Composable
private fun LargeFontDisplayPreview() {
    DisplayPreview(darkTheme = false)
}

@Composable
private fun DisplayPreview(darkTheme: Boolean) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            HealthCard(title = "Resumo de hoje", status = { ActivityStatusBadge(ActivityVisualState.Running) }) {
                Text(
                    text = "Dados apresentados pelo chamador",
                    modifier = Modifier.padding(top = ChampionSpacing.small),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            MetricCard(
                label = "Passos",
                value = "8.420",
                supportingText = "Atualizado agora",
            )
        }
    }
}
