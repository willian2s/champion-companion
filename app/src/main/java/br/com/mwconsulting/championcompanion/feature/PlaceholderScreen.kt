package br.com.mwconsulting.championcompanion.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState

@Composable
internal fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(ChampionSpacing.extraLarge),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            title = title,
            message = "Fundação pronta. Esta área estará disponível em breve.",
        )
    }
}
