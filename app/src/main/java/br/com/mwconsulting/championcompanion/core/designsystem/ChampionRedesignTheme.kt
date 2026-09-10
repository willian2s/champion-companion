package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema local e opt-in para superfícies migradas ao redesign.
 *
 * Preserva [MaterialTheme.colorScheme] vigente e não é aplicado por
 * [ChampionCompanionTheme]. Assim, consumidores legados não mudam por efeito
 * global.
 */
@Composable
fun ChampionRedesignTheme(
    tokens: ChampionRedesignTokenSet = ChampionRedesignTokenSet(),
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    CompositionLocalProvider(
        LocalChampionRedesignTokens provides tokens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = tokens.typography,
            shapes = tokens.shapes,
            content = content,
        )
    }
}
