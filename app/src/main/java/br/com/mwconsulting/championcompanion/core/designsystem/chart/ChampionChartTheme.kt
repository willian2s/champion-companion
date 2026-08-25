package br.com.mwconsulting.championcompanion.core.designsystem.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens visuais compartilhados, independentes do renderer de gráficos. */
@Immutable
data class ChampionChartTheme(
    val primarySeries: Color,
    val secondarySeries: Color,
    val tertiarySeries: Color,
    val axis: Color,
    val grid: Color,
    val reference: Color,
    val marker: Color,
    val markerContainer: Color,
    val onMarkerContainer: Color,
    val gap: Color,
)

/**
 * Resolve os tokens a partir do Material 3 vigente, incluindo dynamic color
 * quando habilitado pelo tema raiz do aplicativo.
 */
@Composable
fun championChartTheme(): ChampionChartTheme {
    val colors = MaterialTheme.colorScheme
    return ChampionChartTheme(
        primarySeries = colors.primary,
        secondarySeries = colors.secondary,
        tertiarySeries = colors.tertiary,
        axis = colors.onSurfaceVariant,
        grid = colors.outlineVariant.copy(alpha = 0.55f),
        reference = colors.primary.copy(alpha = 0.72f),
        marker = colors.primary,
        markerContainer = colors.surfaceContainerHighest,
        onMarkerContainer = colors.onSurface,
        gap = colors.outline,
    )
}
