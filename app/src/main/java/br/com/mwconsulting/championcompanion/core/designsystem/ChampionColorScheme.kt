package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paletas próprias do Champion Companion, derivadas da seed azul-petróleo #006782.
 * Não reutilizam cores, tokens ou recursos do MotionLog.
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006782),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAEEBFF),
    onPrimaryContainer = Color(0xFF001F29),
    inversePrimary = Color(0xFF73D2F2),
    secondary = Color(0xFF4B626C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE7F2),
    onSecondaryContainer = Color(0xFF061F28),
    tertiary = Color(0xFF5A5C7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF171937),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF181C1E),
    surface = Color(0xFFF7FAFC),
    onSurface = Color(0xFF181C1E),
    surfaceVariant = Color(0xFFDCE4E8),
    onSurfaceVariant = Color(0xFF40484C),
    surfaceTint = Color(0xFF006782),
    inverseSurface = Color(0xFF2D3133),
    inverseOnSurface = Color(0xFFEFF1F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF70797D),
    outlineVariant = Color(0xFFC0C8CC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7FAFC),
    surfaceDim = Color(0xFFD7DBDD),
    surfaceContainer = Color(0xFFEBEFF1),
    surfaceContainerHigh = Color(0xFFE5E9EB),
    surfaceContainerHighest = Color(0xFFE0E3E5),
    surfaceContainerLow = Color(0xFFF1F4F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF73D2F2),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D62),
    onPrimaryContainer = Color(0xFFAEEBFF),
    inversePrimary = Color(0xFF006782),
    secondary = Color(0xFFB2CAD5),
    onSecondary = Color(0xFF1D333C),
    secondaryContainer = Color(0xFF344A53),
    onSecondaryContainer = Color(0xFFCEE7F2),
    tertiary = Color(0xFFC2C3EB),
    onTertiary = Color(0xFF2B2E4F),
    tertiaryContainer = Color(0xFF424466),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF101416),
    onBackground = Color(0xFFE0E3E5),
    surface = Color(0xFF101416),
    onSurface = Color(0xFFE0E3E5),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CC),
    surfaceTint = Color(0xFF73D2F2),
    inverseSurface = Color(0xFFE0E3E5),
    inverseOnSurface = Color(0xFF2D3133),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A9296),
    outlineVariant = Color(0xFF40484C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF373A3C),
    surfaceDim = Color(0xFF101416),
    surfaceContainer = Color(0xFF1C2022),
    surfaceContainerHigh = Color(0xFF272A2D),
    surfaceContainerHighest = Color(0xFF313538),
    surfaceContainerLow = Color(0xFF181C1E),
    surfaceContainerLowest = Color(0xFF0B0F11),
)

internal fun selectChampionColorScheme(
    darkTheme: Boolean,
    useDynamicColors: Boolean,
    sdkInt: Int,
    dynamicColorScheme: ColorScheme?,
): ColorScheme = when {
    useDynamicColors && sdkInt >= DYNAMIC_COLOR_MIN_API && dynamicColorScheme != null -> dynamicColorScheme
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}

internal const val DYNAMIC_COLOR_MIN_API = 31
