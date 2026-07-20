package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionThemeTest {
    @Test
    fun `light theme selects own light scheme when dynamic colors disabled`() {
        assertSame(
            LightColorScheme,
            selectChampionColorScheme(
                darkTheme = false,
                useDynamicColors = false,
                sdkInt = DYNAMIC_COLOR_MIN_API,
                dynamicColorScheme = lightColorScheme(primary = Color.Magenta),
            ),
        )
    }

    @Test
    fun `dark theme selects own dark scheme below dynamic color API`() {
        assertSame(
            DarkColorScheme,
            selectChampionColorScheme(
                darkTheme = true,
                useDynamicColors = true,
                sdkInt = DYNAMIC_COLOR_MIN_API - 1,
                dynamicColorScheme = lightColorScheme(primary = Color.Magenta),
            ),
        )
    }

    @Test
    fun `dynamic scheme selected when enabled on supported API`() {
        val dynamicColorScheme = lightColorScheme(primary = Color.Magenta)

        assertSame(
            dynamicColorScheme,
            selectChampionColorScheme(
                darkTheme = false,
                useDynamicColors = true,
                sdkInt = DYNAMIC_COLOR_MIN_API,
                dynamicColorScheme = dynamicColorScheme,
            ),
        )
    }

    @Test
    fun `own dark scheme is fallback when dynamic scheme unavailable`() {
        assertSame(
            DarkColorScheme,
            selectChampionColorScheme(
                darkTheme = true,
                useDynamicColors = true,
                sdkInt = DYNAMIC_COLOR_MIN_API,
                dynamicColorScheme = null,
            ),
        )
    }

    @Test
    fun `color pairs meet minimum text contrast`() {
        val pairs = listOf(
            LightColorScheme.primary to LightColorScheme.onPrimary,
            LightColorScheme.primaryContainer to LightColorScheme.onPrimaryContainer,
            LightColorScheme.secondary to LightColorScheme.onSecondary,
            LightColorScheme.secondaryContainer to LightColorScheme.onSecondaryContainer,
            LightColorScheme.tertiary to LightColorScheme.onTertiary,
            LightColorScheme.tertiaryContainer to LightColorScheme.onTertiaryContainer,
            LightColorScheme.background to LightColorScheme.onBackground,
            LightColorScheme.surface to LightColorScheme.onSurface,
            LightColorScheme.error to LightColorScheme.onError,
            LightColorScheme.errorContainer to LightColorScheme.onErrorContainer,
            DarkColorScheme.primary to DarkColorScheme.onPrimary,
            DarkColorScheme.primaryContainer to DarkColorScheme.onPrimaryContainer,
            DarkColorScheme.secondary to DarkColorScheme.onSecondary,
            DarkColorScheme.secondaryContainer to DarkColorScheme.onSecondaryContainer,
            DarkColorScheme.tertiary to DarkColorScheme.onTertiary,
            DarkColorScheme.tertiaryContainer to DarkColorScheme.onTertiaryContainer,
            DarkColorScheme.background to DarkColorScheme.onBackground,
            DarkColorScheme.surface to DarkColorScheme.onSurface,
            DarkColorScheme.error to DarkColorScheme.onError,
            DarkColorScheme.errorContainer to DarkColorScheme.onErrorContainer,
        )

        pairs.forEach { (background, foreground) ->
            assertTrue(
                "Expected contrast >= 4.5, was ${contrastRatio(foreground, background)}",
                contrastRatio(foreground, background) >= 4.5f,
            )
        }
    }

    @Test
    fun `spacing scale has specified values`() {
        assertEquals(4f, ChampionSpacing.extraSmall.value)
        assertEquals(8f, ChampionSpacing.small.value)
        assertEquals(12f, ChampionSpacing.medium.value)
        assertEquals(16f, ChampionSpacing.large.value)
        assertEquals(24f, ChampionSpacing.extraLarge.value)
        assertEquals(32f, ChampionSpacing.huge.value)
        assertEquals(40f, ChampionSpacing.extraHuge.value)
        assertEquals(48f, ChampionSpacing.massive.value)
    }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = relativeLuminance(foreground).coerceAtLeast(relativeLuminance(background))
    val darker = relativeLuminance(foreground).coerceAtMost(relativeLuminance(background))
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun relativeLuminance(color: Color): Float =
    0.2126f * color.red.linearized() +
        0.7152f * color.green.linearized() +
        0.0722f * color.blue.linearized()

private fun Float.linearized(): Float =
    if (this <= 0.04045f) this / 12.92f else ((this + 0.055f) / 1.055f).let { it * it * it }
