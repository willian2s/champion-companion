package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    val isInteractive = enabled && !isLoading
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = if (isLoading) "$label, carregando" else label
            },
        enabled = isInteractive,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = ChampionSpacing.small),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            leadingContent?.invoke()
        }
        Text(text = label)
    }
}

@Composable
fun ConnectionStatus(
    state: ConnectionVisualState,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalChampionStatusColors.current.colorsFor(state)
    StatusBadge(
        metadata = state.visualMetadata(),
        colors = colors,
        modifier = modifier,
        trailingContent = trailingContent,
    )
}

@Composable
fun SyncStatusBadge(
    state: SyncVisualState,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalChampionStatusColors.current.colorsFor(state)
    StatusBadge(
        metadata = state.visualMetadata(),
        colors = colors,
        modifier = modifier,
        trailingContent = trailingContent,
    )
}

@Composable
fun ActivityStatusBadge(
    state: ActivityVisualState,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalChampionStatusColors.current.colorsFor(state)
    StatusBadge(
        metadata = state.visualMetadata(),
        colors = colors,
        modifier = modifier,
        trailingContent = trailingContent,
    )
}

@Composable
private fun StatusBadge(
    metadata: ChampionStatusVisualMetadata,
    colors: ChampionStatusColorPair,
    modifier: Modifier,
    trailingContent: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(colors.container, ChampionShapeSmall)
            .semantics(mergeDescendants = true) {
                contentDescription = metadata.contentDescription
            }
            .padding(horizontal = ChampionSpacing.medium, vertical = ChampionSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Text(
            text = metadata.icon.glyph(),
            color = colors.content,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = metadata.label,
            color = colors.content,
            style = MaterialTheme.typography.labelLarge,
        )
        trailingContent()
    }
}

private fun ChampionStatusIcon.glyph(): String = when (this) {
    ChampionStatusIcon.Connected -> "✓"
    ChampionStatusIcon.Disconnected -> "×"
    ChampionStatusIcon.SyncPending -> "↻"
    ChampionStatusIcon.SyncSuccess -> "✓"
    ChampionStatusIcon.SyncFailure -> "!"
    ChampionStatusIcon.HeartRate -> "♥"
    ChampionStatusIcon.ActivityRunning -> "▶"
    ChampionStatusIcon.ActivityPaused -> "Ⅱ"
}
