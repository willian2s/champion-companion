package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared application shell. Keeps destination content independent from navigation chrome. */
@Composable
fun ChampionAppShell(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = bottomBar,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            content(innerPadding)
        }
    }
}

/** Standard vertical rhythm for screen-level content. Opt in per screen during rollout. */
@Composable
fun ChampionScreenContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(
                horizontal = ChampionSpacing.screenHorizontal,
                vertical = ChampionSpacing.screenVertical,
            ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            ChampionSpacing.sectionGap,
        ),
        content = content,
    )
}

/** Shared surface for editorial cards and metric groups. Stateless and opt in. */
@Composable
fun ChampionCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = ChampionShapeMedium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (elevated) ChampionElevation.raisedCard else ChampionElevation.card,
        shadowElevation = if (elevated) ChampionElevation.card else 0.dp,
        content = content,
    )
}
