package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Estados claros", showBackground = true)
@Composable
private fun LightStatusTokensPreview() {
    StatusTokensPreview(darkTheme = false)
}

@Preview(name = "Estados escuros", showBackground = true)
@Composable
private fun DarkStatusTokensPreview() {
    StatusTokensPreview(darkTheme = true)
}

@Composable
private fun StatusTokensPreview(darkTheme: Boolean) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        val colors = LocalChampionStatusColors.current
        Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
            ConnectionVisualState.entries.forEach { state -> StatusTokenPreviewItem(state.visualMetadata(), colors.colorsFor(state)) }
            SyncVisualState.entries.forEach { state -> StatusTokenPreviewItem(state.visualMetadata(), colors.colorsFor(state)) }
            ActivityVisualState.entries.forEach { state -> StatusTokenPreviewItem(state.visualMetadata(), colors.colorsFor(state)) }
        }
    }
}

@Composable
private fun StatusTokenPreviewItem(
    metadata: ChampionStatusVisualMetadata,
    colors: ChampionStatusColorPair,
) {
    Text(
        text = "${metadata.icon}: ${metadata.label}",
        color = colors.content,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.container, ChampionShapeSmall)
            .padding(ChampionSpacing.medium),
        style = MaterialTheme.typography.labelLarge,
    )
}
