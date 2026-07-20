package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Ações e status claros", showBackground = true)
@Composable
private fun LightActionAndStatusPreview() {
    ActionAndStatusPreview(darkTheme = false)
}

@Preview(name = "Ações e status escuros", showBackground = true)
@Composable
private fun DarkActionAndStatusPreview() {
    ActionAndStatusPreview(darkTheme = true)
}

@Preview(name = "Ações e status fonte ampliada", fontScale = 1.3f, showBackground = true)
@Composable
private fun LargeFontActionAndStatusPreview() {
    ActionAndStatusPreview(darkTheme = false)
}

@Composable
private fun ActionAndStatusPreview(darkTheme: Boolean) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            PrimaryActionButton(label = "Sincronizar", onClick = {})
            PrimaryActionButton(label = "Sincronizar", onClick = {}, isLoading = true)
            ConnectionVisualState.entries.forEach { ConnectionStatus(it) }
            SyncVisualState.entries.forEach { SyncStatusBadge(it) }
            ActivityVisualState.entries.forEach { ActivityStatusBadge(it) }
        }
    }
}
