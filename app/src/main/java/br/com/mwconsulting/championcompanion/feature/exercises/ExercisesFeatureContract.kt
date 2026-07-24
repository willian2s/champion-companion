package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
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
    val selectedFilter: WorkoutActivityFilter = WorkoutActivityFilter.ALL,
    val activityList: List<WorkoutActivity> = emptyList(),
    val workflowSummary: WorkoutWorkflowSummary = WorkoutWorkflowSummary(),
    val selectedActivity: WorkoutActivity? = null,
    val detailState: ExerciseDetailState = ExerciseDetailState.Loading,
    val reviewFormState: ExerciseReviewFormState = ExerciseReviewFormState(),
    val calibrationFormState: ExerciseCalibrationFormState = ExerciseCalibrationFormState(),
    val operationError: ExerciseOperationError? = null,
    val pendingActivities: List<WorkoutActivity> = emptyList(),
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val hasAssociatedDevice: Boolean = false,
    val associatedDeviceName: String? = null,
)

sealed interface ExerciseDetailState {
    data object Loading : ExerciseDetailState
    data class Ready(
        val activityId: WorkoutActivityId,
        val latestCalibration: TreadmillCalibrationSnapshot? = null,
    ) : ExerciseDetailState
    data object NotFound : ExerciseDetailState
    data class Failed(val error: ExerciseOperationError) : ExerciseDetailState
}

data class ExerciseReviewFormState(
    val loadedActivityId: WorkoutActivityId? = null,
    val selectedType: WorkoutType? = null,
    val titleInput: String = "",
    val notesInput: String = "",
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val savedAt: Instant? = null,
    val error: ExerciseOperationError? = null,
)

data class ExerciseCalibrationFormState(
    val loadedActivityId: WorkoutActivityId? = null,
    val reportedDistanceInput: String = "",
    val previewDistanceMeters: Double? = null,
    val previewAveragePaceSecondsPerKilometer: Double? = null,
    val previewAverageSpeedMetersPerSecond: Double? = null,
    val isSaving: Boolean = false,
    val completedAt: Instant? = null,
    val validationError: ExerciseOperationError? = null,
)

data class ExerciseOperationError(
    val category: ExerciseOperationErrorCategory,
    val message: String,
)

enum class ExerciseOperationErrorCategory {
    NOT_FOUND,
    INVALID_STATE,
    PARTIAL_BLOCKED,
    INVALID_TYPE,
    INVALID_TITLE,
    INVALID_NOTES,
    INVALID_DISTANCE,
    STORAGE,
    INTERNAL,
}

sealed interface ExercisesIntent {
    data object Load : ExercisesIntent
    data class SelectFilter(val filter: WorkoutActivityFilter) : ExercisesIntent
    data class OpenActivity(val activityId: WorkoutActivityId) : ExercisesIntent
    data object CloseActivity : ExercisesIntent
    data class EditReviewType(val type: WorkoutType) : ExercisesIntent
    data class EditReviewTitle(val title: String) : ExercisesIntent
    data class EditReviewNotes(val notes: String) : ExercisesIntent
    data object SubmitReview : ExercisesIntent
    data class EditCalibrationDistance(val distance: String) : ExercisesIntent
    data object SubmitCalibration : ExercisesIntent
    data object DiscardSelectedActivity : ExercisesIntent
    data object DismissOperationError : ExercisesIntent
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
