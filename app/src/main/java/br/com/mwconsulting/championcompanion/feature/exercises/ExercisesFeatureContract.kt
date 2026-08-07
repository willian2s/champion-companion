package br.com.mwconsulting.championcompanion.feature.exercises

import br.com.mwconsulting.championcompanion.core.common.UiStateStore
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.DEFAULT_WORKOUT_COUNTDOWN_SECONDS
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
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
    val isReprocessing: Boolean = false,
    val pendingActivities: List<WorkoutActivity> = emptyList(),
    val hasUnsavedDetailChanges: Boolean = false,
    val canDiscardSelectedActivity: Boolean = false,
    val dailyStepsGoal: Int = DEFAULT_DAILY_STEPS_GOAL,
    val hasAssociatedDevice: Boolean = false,
    val associatedDeviceName: String? = null,
    val workoutPreparation: WorkoutPreparationUiState = WorkoutPreparationUiState(),
)

enum class WorkoutPreparationPhase {
    INACTIVE,
    WAITING,
    COUNTING_DOWN,
    START_REQUESTED,
}

data class WorkoutPreparationUiState(
    val workoutType: WorkoutType? = null,
    val phase: WorkoutPreparationPhase = WorkoutPreparationPhase.INACTIVE,
    val countdownSeconds: Int = DEFAULT_WORKOUT_COUNTDOWN_SECONDS,
    val remainingSeconds: Int? = null,
    val gpsReadiness: GpsReadiness? = null,
    val gpsReadinessGeneration: Long? = null,
    val notice: String? = null,
    val preferenceError: String? = null,
    val startRequest: WorkoutStartRequest? = null,
) {
    val requiresGps: Boolean
        get() = workoutType == WorkoutType.WALKING || workoutType == WorkoutType.RUNNING

    val canStartCountdown: Boolean
        get() = phase == WorkoutPreparationPhase.WAITING &&
            (!requiresGps || gpsReadiness?.canStart == true && gpsReadinessGeneration != null)
}

/** Handoff idempotente para 013-08; não cria sessão, atividade, rota ou serviço. */
data class WorkoutStartRequest(
    val requestId: String,
    val workoutType: WorkoutType,
    val requestedAt: Instant,
    val countdownSeconds: Int,
    val preparationVisibilityToken: Long,
    val gpsReadinessGeneration: Long?,
)

enum class RecoveryAction {
    RESUME,
    FINISH,
    DISCARD,
}

sealed interface RecoveryActionState {
    data object Idle : RecoveryActionState
    data class InProgress(val action: RecoveryAction, val sessionId: String) : RecoveryActionState
    data class Success(val action: RecoveryAction, val sessionId: String) : RecoveryActionState
    data class Failure(
        val action: RecoveryAction,
        val sessionId: String,
        val message: String,
    ) : RecoveryActionState
}

sealed interface WorkoutFinishCommandState {
    data object Idle : WorkoutFinishCommandState
    data class InProgress(val sessionId: String) : WorkoutFinishCommandState
    data class Persisted(val sessionId: String) : WorkoutFinishCommandState
    data class Failure(val sessionId: String, val message: String) : WorkoutFinishCommandState
}

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
    PERMISSION_REQUIRED,
    PROVIDER_UNAVAILABLE,
    SYNC_DISABLED,
    SYNC_FAILED,
    INVALID_TYPE,
    INVALID_TITLE,
    INVALID_NOTES,
    INVALID_DISTANCE,
    REPROCESS_RAW_MISSING,
    REPROCESS_PARSER_UNKNOWN,
    REPROCESS_INDEX_INELIGIBLE,
    REPROCESS_PROFILE_MISSING,
    REPROCESS_TEMPORAL_INVALID,
    REPROCESS_SYNCED_BLOCKED,
    REPROCESS_SYNCING,
    REPROCESS_UNAVAILABLE,
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
    data object ReprocessSelectedActivity : ExercisesIntent
    data object SyncSelectedActivity : ExercisesIntent
    data object DiscardSelectedActivity : ExercisesIntent
    data object ConfirmExitDetail : ExercisesIntent
    data object DismissOperationError : ExercisesIntent
    data object StartImport : ExercisesIntent
    data object CancelImport : ExercisesIntent
    data object RetryImport : ExercisesIntent
    data object DismissImport : ExercisesIntent
    data object ConfirmExitImport : ExercisesIntent
    data object AppMovedToBackground : ExercisesIntent
    data object OpenDevice : ExercisesIntent
    data class EnterWorkoutPreparation(val workoutType: WorkoutType) : ExercisesIntent
    data object SuspendWorkoutPreparation : ExercisesIntent
    data object ExitWorkoutPreparation : ExercisesIntent
    data object RefreshWorkoutLocation : ExercisesIntent
    data class SelectWorkoutCountdown(val seconds: Int) : ExercisesIntent
    data object StartWorkoutCountdown : ExercisesIntent
    data object CancelWorkoutCountdown : ExercisesIntent
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
