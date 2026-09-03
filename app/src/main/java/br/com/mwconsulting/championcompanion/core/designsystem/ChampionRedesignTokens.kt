package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Espaçamentos da composição visual opt-in do redesign. */
@Immutable
data class ChampionRedesignSpacingValues(
    val screen: Dp = 20.dp,
    val section: Dp = 24.dp,
    val card: Dp = 20.dp,
    val item: Dp = 16.dp,
    val compact: Dp = 8.dp,
)

/**
 * Tokens do redesign, separados de [ChampionSpacing] para impedir alteração
 * visual implícita em consumidores que ainda usam fundação legada.
 */
val ChampionRedesignSpacing = ChampionRedesignSpacingValues()

/** Formas usadas por superfícies migradas para a composição do redesign. */
val ChampionRedesignShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Tipografia sem fonte externa. [FontFamily.Default] mantém recurso nativo do
 * sistema e permite que escala de fonte do usuário continue sendo respeitada.
 */
val ChampionRedesignTypography = Typography().copy(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.Default,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.Default,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.Default),
    titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily.Default),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.Default),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.Default),
    bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.Default),
    labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.Default),
    labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.Default),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.Default),
)

/** Elevações discretas para cards e superfícies do redesign. */
@Immutable
data class ChampionRedesignElevationValues(
    val card: Dp = 0.dp,
    val raisedCard: Dp = 2.dp,
    val floating: Dp = 4.dp,
)

val ChampionRedesignElevation = ChampionRedesignElevationValues()

/** Conjunto local de tokens consumido somente por componentes opt-in. */
@Immutable
data class ChampionRedesignTokenSet(
    val spacing: ChampionRedesignSpacingValues = ChampionRedesignSpacing,
    val shapes: Shapes = ChampionRedesignShapes,
    val typography: Typography = ChampionRedesignTypography,
    val elevation: ChampionRedesignElevationValues = ChampionRedesignElevation,
)

val LocalChampionRedesignTokens = staticCompositionLocalOf { ChampionRedesignTokenSet() }
