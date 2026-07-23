package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportProgress
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportResult
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import java.time.Instant

interface ExercisesFeatureContract : UiStateStore<ExercisesUiState> {
    fun onIntent(intent: ExercisesIntent)
}

data class ExercisesUiState(
    val importState: WatchWorkoutImportSurfaceState = WatchWorkoutImportSurfaceState.Idle,
    val latestImportMetadata: WatchWorkoutImportMetadata? = null,
    val pendingActivities: List<WorkoutActivity> = emptyList(),
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val hasAssociatedDevice: Boolean = false,
    val associatedDeviceName: String? = null,
)

sealed interface ExercisesIntent {
    data object Load : ExercisesIntent
    data object StartImport : ExercisesIntent
    data object CancelImport : ExercisesIntent
    data object RetryImport : ExercisesIntent
    data object DismissImport : ExercisesIntent
    data object ConfirmExitImport : ExercisesIntent
    data object AppMovedToBackground : ExercisesIntent
    data object OpenDevice : ExercisesIntent
}

sealed interface WatchWorkoutImportSurfaceState {
    data object Idle : WatchWorkoutImportSurfaceState

    data class Preparing(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Connecting(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class ReadingIndex(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class ReadingBlocks(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Parsing(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Persisting(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Completed(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Partial(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Empty(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Failed(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
    data class Cancelled(val session: WatchWorkoutImportSessionState) : WatchWorkoutImportSurfaceState
}

data class WatchWorkoutImportSessionState(
    val importAttemptId: String,
    val bleSessionId: String? = null,
    val startedAt: Instant? = null,
    val progress: WatchWorkoutImportProgress = WatchWorkoutImportProgress(),
    val result: WatchWorkoutImportResult? = null,
    val metadata: WatchWorkoutImportMetadata? = null,
    val error: WatchWorkoutImportError? = null,
)
