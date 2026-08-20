package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
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
