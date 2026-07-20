package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Claro", showBackground = true)
@Composable
private fun ChampionThemeLightPreview() {
    ChampionThemePreview(darkTheme = false)
}

@Preview(name = "Escuro", showBackground = true)
@Composable
private fun ChampionThemeDarkPreview() {
    ChampionThemePreview(darkTheme = true)
}

@Preview(name = "Fonte ampliada", fontScale = 1.3f, showBackground = true)
@Composable
private fun ChampionThemeLargeFontPreview() {
    ChampionThemePreview(darkTheme = false)
}

@Composable
private fun ChampionThemePreview(darkTheme: Boolean) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        Surface {
            Column(modifier = Modifier.padding(ChampionSpacing.extraLarge)) {
                Text("Champion Companion", style = MaterialTheme.typography.headlineMedium)
                Text("Tema Material 3", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
