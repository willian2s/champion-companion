package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ChampionSpacingValues(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val extraHuge: Dp = 40.dp,
    val massive: Dp = 48.dp,
)

val ChampionSpacing = ChampionSpacingValues()

/**
 * Tokens neutros para superfícies que aderem à fundação visual nova.
 *
 * Consumidores optam explicitamente por esses tokens; valores existentes e
 * superfícies ainda não migradas permanecem inalterados.
 */
@Immutable
data class ChampionVisualTokens(
    val pageHorizontalPadding: Dp = ChampionSpacing.extraLarge,
    val pageVerticalPadding: Dp = ChampionSpacing.large,
    val sectionSpacing: Dp = ChampionSpacing.extraLarge,
    val contentSpacing: Dp = ChampionSpacing.medium,
    val controlSpacing: Dp = ChampionSpacing.small,
    val componentPadding: Dp = ChampionSpacing.large,
    val minimumTouchTarget: Dp = ChampionSpacing.massive,
    val chartMinimumHeight: Dp = 192.dp,
    val navigationTransitionDurationMillis: Int = 260,
) {
    init {
        require(pageHorizontalPadding >= 0.dp)
        require(pageVerticalPadding >= 0.dp)
        require(sectionSpacing >= 0.dp)
        require(contentSpacing >= 0.dp)
        require(controlSpacing >= 0.dp)
        require(componentPadding >= 0.dp)
        require(minimumTouchTarget >= 48.dp)
        require(chartMinimumHeight > 0.dp)
        require(navigationTransitionDurationMillis >= 0)
    }
}

val DefaultChampionVisualTokens = ChampionVisualTokens()

/** Local opt-in provider; does not mutate [ChampionCompanionTheme]. */
val LocalChampionVisualTokens = staticCompositionLocalOf { DefaultChampionVisualTokens }

val ChampionShapeSmall = RoundedCornerShape(8.dp)
val ChampionShapeMedium = RoundedCornerShape(12.dp)
val ChampionShapeLarge = RoundedCornerShape(16.dp)
val ChampionShapeExtraLarge = RoundedCornerShape(24.dp)

val ChampionShapes = Shapes(
    ChampionShapeSmall,
    ChampionShapeSmall,
    ChampionShapeMedium,
    ChampionShapeLarge,
    ChampionShapeExtraLarge,
)

val ChampionTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.Default),
    displayMedium = Typography().displayMedium.copy(fontFamily = FontFamily.Default),
    displaySmall = Typography().displaySmall.copy(fontFamily = FontFamily.Default),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.Default),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.Default),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.Default),
    titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.Default),
    titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.Default),
    titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily.Default),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.Default),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.Default),
    bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.Default),
    labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.Default),
    labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.Default),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.Default),
)

@Immutable
data class ChampionElevationTokens(
    val card: Dp = 1.dp,
    val raisedCard: Dp = 3.dp,
    val floating: Dp = 6.dp,
)

val ChampionElevation = ChampionElevationTokens()
