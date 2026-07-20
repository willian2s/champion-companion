package br.com.mwconsulting.championcompanion.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChampionCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dynamicColorScheme = if (useDynamicColors && Build.VERSION.SDK_INT >= DYNAMIC_COLOR_MIN_API) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val colorScheme = selectChampionColorScheme(
        darkTheme = darkTheme,
        useDynamicColors = useDynamicColors,
        sdkInt = Build.VERSION.SDK_INT,
        dynamicColorScheme = dynamicColorScheme,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChampionTypography,
        shapes = ChampionShapes,
        content = content,
    )
}
