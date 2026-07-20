package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChampionStatusTokensTest {
    @Test
    fun `every visual state has accessible label description and icon`() {
        val metadata = ConnectionVisualState.entries.map { it.visualMetadata() } +
            SyncVisualState.entries.map { it.visualMetadata() } +
            ActivityVisualState.entries.map { it.visualMetadata() }

        assertEquals(8, metadata.size)
        assertEquals(8, metadata.map { it.icon }.distinct().size)
        metadata.forEach {
            assertTrue(it.label.isNotBlank())
            assertTrue(it.contentDescription.isNotBlank())
        }
    }

    @Test
    fun `visual states map to their semantic color token`() {
        assertEquals(LightChampionStatusColors.connected, LightChampionStatusColors.colorsFor(ConnectionVisualState.Connected))
        assertEquals(LightChampionStatusColors.disconnected, LightChampionStatusColors.colorsFor(ConnectionVisualState.Disconnected))
        assertEquals(LightChampionStatusColors.syncPending, LightChampionStatusColors.colorsFor(SyncVisualState.Pending))
        assertEquals(LightChampionStatusColors.syncSuccess, LightChampionStatusColors.colorsFor(SyncVisualState.Success))
        assertEquals(LightChampionStatusColors.syncFailure, LightChampionStatusColors.colorsFor(SyncVisualState.Failure))
        assertEquals(LightChampionStatusColors.heartRate, LightChampionStatusColors.colorsFor(ActivityVisualState.HeartRate))
        assertEquals(LightChampionStatusColors.activityRunning, LightChampionStatusColors.colorsFor(ActivityVisualState.Running))
        assertEquals(LightChampionStatusColors.activityPaused, LightChampionStatusColors.colorsFor(ActivityVisualState.Paused))
    }

    @Test
    fun `status color pairs meet minimum text contrast in both themes`() {
        listOf(LightChampionStatusColors, DarkChampionStatusColors)
            .flatMap { it.colorPairs() }
            .forEach { (container, content) ->
                assertTrue(
                    "Expected contrast >= 4.5, was ${contrastRatio(content, container)}",
                    contrastRatio(content, container) >= 4.5f,
                )
            }
    }
}

private fun ChampionStatusColors.colorPairs(): List<ChampionStatusColorPair> = listOf(
    connected,
    disconnected,
    syncPending,
    syncSuccess,
    syncFailure,
    heartRate,
    activityRunning,
    activityPaused,
)

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
