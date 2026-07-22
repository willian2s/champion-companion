package br.com.mwconsulting.championcompanion.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.mwconsulting.championcompanion.core.model.AppPreferences
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.DailySummaryPolicy
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.ThemePreference
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AppPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {
    override fun observe(): Flow<AppPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(AppPreferencesCodec::decode)

    override fun observeDailyStepsGoal(): Flow<Int> = observe().map { it.dailyStepsGoal }

    override suspend fun update(preferences: AppPreferences): RepositoryResult<Unit> =
        if (preferences.dailyStepsGoal <= 0) {
            RepositoryResult.UnexpectedState
        } else try {
            dataStore.edit { stored -> AppPreferencesCodec.encode(stored, preferences) }
            RepositoryResult.Success(Unit)
        } catch (_: IOException) {
            RepositoryResult.StorageUnavailable
        }

    override suspend fun updateDailyStepsGoal(goal: Int): RepositoryResult<Unit> {
        if (goal <= 0) return RepositoryResult.UnexpectedState
        return update(observe().first().copy(dailyStepsGoal = goal))
    }
}

internal object AppPreferencesCodec {
    private val theme = stringPreferencesKey("theme")
    private val dynamicColors = booleanPreferencesKey("dynamic_colors")
    private val healthSyncEnabled = booleanPreferencesKey("health_sync_enabled")
    private val diagnosticCaptureEnabled = booleanPreferencesKey("diagnostic_capture_enabled")
    private val dailyStepsGoal = intPreferencesKey("daily_steps_goal")

    fun decode(stored: Preferences): AppPreferences = AppPreferences(
        theme = stored[theme]?.let { value -> ThemePreference.entries.find { it.name == value } }
            ?: ThemePreference.FOLLOW_SYSTEM,
        useDynamicColors = stored[dynamicColors] ?: true,
        healthSyncEnabled = stored[healthSyncEnabled] ?: false,
        diagnosticCaptureEnabled = stored[diagnosticCaptureEnabled] ?: false,
        dailyStepsGoal = DailySummaryPolicy.sanitizeDailyStepsGoal(
            stored[dailyStepsGoal] ?: DEFAULT_DAILY_STEPS_GOAL,
        ),
    )

    fun encode(stored: androidx.datastore.preferences.core.MutablePreferences, preferences: AppPreferences) {
        stored[theme] = preferences.theme.name
        stored[dynamicColors] = preferences.useDynamicColors
        stored[healthSyncEnabled] = preferences.healthSyncEnabled
        stored[diagnosticCaptureEnabled] = preferences.diagnosticCaptureEnabled
        stored[dailyStepsGoal] = DailySummaryPolicy.sanitizeDailyStepsGoal(preferences.dailyStepsGoal)
    }
}
