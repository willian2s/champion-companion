package br.com.mwconsulting.championcompanion.core.preferences

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesCodecTest {
    @Test
    fun `missing preferences use safe defaults`() {
        assertEquals(AppPreferences(), AppPreferencesCodec.decode(mutablePreferencesOf()))
    }

    @Test
    fun `unknown stored theme falls back to system`() {
        val stored = mutablePreferencesOf(stringPreferencesKey("theme") to "UNSUPPORTED")

        assertEquals(ThemePreference.FOLLOW_SYSTEM, AppPreferencesCodec.decode(stored).theme)
    }

    @Test
    fun `preferences round trip without domain data`() {
        val expected = AppPreferences(
            theme = ThemePreference.DARK,
            useDynamicColors = false,
            healthSyncEnabled = true,
            diagnosticCaptureEnabled = true,
        )
        val stored = mutablePreferencesOf()

        AppPreferencesCodec.encode(stored, expected)

        assertEquals(expected, AppPreferencesCodec.decode(stored))
    }
}
