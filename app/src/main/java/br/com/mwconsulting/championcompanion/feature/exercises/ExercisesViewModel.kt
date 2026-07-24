package br.com.mwconsulting.championcompanion.feature.exercises
import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.CalibrateImportedActivityInput
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.model.SubmitWorkoutReviewInput
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationRepository
import br.com.mwconsulting.championcompanion.core.model.TreadmillCalibrationSnapshot
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivity
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityFilter
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityId
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel internal constructor(
    private val deviceRepository: DeviceRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val treadmillCalibrationRepository: TreadmillCalibrationRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
    private val scope: CoroutineScope,
    private val ids: UuidV7Generator,
) : ViewModel(), ExercisesFeatureContract {
    private val mutableState = MutableStateFlow(ExercisesUiState())
    private val associatedDevice = MutableStateFlow<Device?>(null)
    private val importState = MutableStateFlow<WatchWorkoutImportSurfaceState>(WatchWorkoutImportSurfaceState.Idle)
    private val latestImportMetadata = MutableStateFlow<WatchWorkoutImportMetadata?>(null)
    private val dailyStepsGoal = MutableStateFlow(br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL)
    private val selectedFilter = MutableStateFlow(WorkoutActivityFilter.ALL)
    private val activityList = MutableStateFlow<List<WorkoutActivity>>(emptyList())
    private val workflowSummary = MutableStateFlow(WorkoutWorkflowSummary())
    private val selectedActivityId = MutableStateFlow<WorkoutActivityId?>(null)
    private val selectedActivity = MutableStateFlow<WorkoutActivity?>(null)
    private val latestCalibration = MutableStateFlow<TreadmillCalibrationSnapshot?>(null)
    private val detailState = MutableStateFlow<ExerciseDetailState>(ExerciseDetailState.Loading)
    private val reviewFormState = MutableStateFlow(ExerciseReviewFormState())
    private val calibrationFormState = MutableStateFlow(ExerciseCalibrationFormState())
    private val operationError = MutableStateFlow<ExerciseOperationError?>(null)

    internal var ownsScope: Boolean = false

    override val state: StateFlow<ExercisesUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                associatedDevice.value = device
            }
        }
        scope.launch {
            selectedFilter.flatMapLatest { filter ->
                workoutActivityRepository.observeActivities(filter)
            }.collect { activities ->
                activityList.value = activities
            }
        }
        scope.launch {
            workoutActivityRepository.observeWorkflowSummary().collect { summary ->
                workflowSummary.value = summary
            }
        }
        scope.launch {
            selectedActivityId.flatMapLatest { activityId ->
                if (activityId == null) flowOf(null) else workoutActivityRepository.observeActivity(activityId)
            }.distinctUntilChanged().collect { activity ->
                selectedActivity.value = activity
                detailState.value = when {
                    selectedActivityId.value == null -> ExerciseDetailState.Loading
                    activity == null -> ExerciseDetailState.NotFound
                    else -> ExerciseDetailState.Ready(activity.id, latestCalibration.value)
                }
                syncFormsWithSelectedActivity(activity)
            }
        }
        scope.launch {
            selectedActivityId.flatMapLatest { activityId ->
                if (activityId == null) flowOf(null) else treadmillCalibrationRepository.observeLatestCalibration(activityId)
            }.collect { calibration ->
                latestCalibration.value = calibration
                val activityId = selectedActivityId.value
                if (activityId != null && selectedActivity.value != null) {
                    detailState.value = ExerciseDetailState.Ready(activityId, calibration)
                }
            }
        }
        scope.launch {
            associatedDevice.flatMapLatest { device ->
                if (device == null) flowOf(null) else workoutActivityRepository.observeWatchWorkoutImportMetadata(device.id)
            }.collect { metadata ->
                latestImportMetadata.value = metadata
            }
        }
        scope.launch {
            appPreferencesRepository.observeDailyStepsGoal().collect { goal ->
                dailyStepsGoal.value = goal
            }
        }
        scope.launch {
            watchWorkoutImportCoordinator.state.collect { surface ->
                importState.value = surface
            }
        }
        scope.launch {
            combine(
                importState,
                latestImportMetadata,
                selectedFilter,
                activityList,
                workflowSummary,
                selectedActivity,
                detailState,
                reviewFormState,
                calibrationFormState,
                operationError,
                dailyStepsGoal,
                associatedDevice,
            ) { values ->
                val currentImportState = values[0] as WatchWorkoutImportSurfaceState
                val metadata = values[1] as WatchWorkoutImportMetadata?
                val currentFilter = values[2] as WorkoutActivityFilter
                val activities = values[3] as List<WorkoutActivity>
                val summary = values[4] as WorkoutWorkflowSummary
                val activity = values[5] as WorkoutActivity?
                val currentDetailState = values[6] as ExerciseDetailState
                val reviewForm = values[7] as ExerciseReviewFormState
                val calibrationForm = values[8] as ExerciseCalibrationFormState
                val error = values[9] as ExerciseOperationError?
                val goal = values[10] as Int
                val device = values[11] as Device?
                ExercisesUiState(
                    importState = currentImportState,
                    latestImportMetadata = metadata,
                    selectedFilter = currentFilter,
                    activityList = activities,
                    workflowSummary = summary,
                    selectedActivity = activity,
                    detailState = currentDetailState,
                    reviewFormState = reviewForm,
                    calibrationFormState = calibrationForm,
                    operationError = error,
                    pendingActivities = activities.filter { it.workflowState == ActivityWorkflowState.PENDING_REVIEW },
                    hasUnsavedDetailChanges = reviewForm.hasUnsavedChanges ||
                        (calibrationForm.loadedActivityId == activity?.id && calibrationForm.reportedDistanceInput.isNotBlank()),
                    canDiscardSelectedActivity = activity?.workflowState in setOf(
                        ActivityWorkflowState.PENDING_REVIEW,
                        ActivityWorkflowState.PENDING_CALIBRATION,
                        ActivityWorkflowState.READY_TO_SYNC,
                    ),
                    dailyStepsGoal = goal,
                    hasAssociatedDevice = device != null,
                    associatedDeviceName = device?.name,
                )
            }.collect { mutableState.value = it }
        }
    }

    @Inject
    constructor(
        deviceRepository: DeviceRepository,
        workoutActivityRepository: WorkoutActivityRepository,
        treadmillCalibrationRepository: TreadmillCalibrationRepository,
        appPreferencesRepository: AppPreferencesRepository,
        watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
    ) : this(
        deviceRepository = deviceRepository,
        workoutActivityRepository = workoutActivityRepository,
        treadmillCalibrationRepository = treadmillCalibrationRepository,
        appPreferencesRepository = appPreferencesRepository,
        watchWorkoutImportCoordinator = watchWorkoutImportCoordinator,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ids = UuidV7Generator(),
    ) {
        ownsScope = true
    }

    override fun onIntent(intent: ExercisesIntent) {
        when (intent) {
            ExercisesIntent.Load -> Unit
            is ExercisesIntent.SelectFilter -> selectedFilter.value = intent.filter
            is ExercisesIntent.OpenActivity -> selectedActivityId.value = intent.activityId
            ExercisesIntent.CloseActivity -> closeSelectedActivity()
            is ExercisesIntent.EditReviewType -> updateReviewForm { copy(selectedType = intent.type, hasUnsavedChanges = true, error = null) }
            is ExercisesIntent.EditReviewTitle -> updateReviewForm { copy(titleInput = intent.title, hasUnsavedChanges = true, error = null) }
            is ExercisesIntent.EditReviewNotes -> updateReviewForm { copy(notesInput = intent.notes, hasUnsavedChanges = true, error = null) }
            ExercisesIntent.SubmitReview -> submitReview()
            is ExercisesIntent.EditCalibrationDistance -> updateCalibrationDistance(intent.distance)
            ExercisesIntent.SubmitCalibration -> submitCalibration()
            ExercisesIntent.DiscardSelectedActivity -> discardSelectedActivity()
            ExercisesIntent.ConfirmExitDetail -> closeSelectedActivity()
            ExercisesIntent.DismissOperationError -> operationError.value = null
            ExercisesIntent.StartImport -> startImport()
            ExercisesIntent.CancelImport -> cancelImport(WatchWorkoutImportCancellationReason.USER_REQUESTED)
            ExercisesIntent.RetryImport -> retryImport()
            ExercisesIntent.DismissImport -> dismissImport()
            ExercisesIntent.ConfirmExitImport -> cancelImport(WatchWorkoutImportCancellationReason.EXIT_CONFIRMED)
            ExercisesIntent.AppMovedToBackground -> cancelImport(WatchWorkoutImportCancellationReason.BACKGROUND)
            ExercisesIntent.OpenDevice -> Unit
        }
    }

    override fun onCleared() {
        if (ownsScope) {
            scope.cancel()
        }
        super.onCleared()
    }

    private fun startImport() {
        val device = associatedDevice.value
        if (device == null) {
            importState.value = WatchWorkoutImportSurfaceState.Failed(
                WatchWorkoutImportSessionState(
                    importAttemptId = ids.next().toString(),
                    error = WatchWorkoutImportError(
                        category = WatchWorkoutImportErrorCategory.NO_ASSOCIATED_DEVICE,
                        message = "Nenhum relogio associado foi encontrado para importar treinos.",
                        isRetryable = false,
                    ),
                ),
            )
            return
        }

        scope.launch {
            val attemptId = ids.next().toString()
            val error = watchWorkoutImportCoordinator.startImport(
                WatchWorkoutImportRequest(
                    importAttemptId = attemptId,
                    associatedDevice = device,
                ),
            )
            if (error != null) {
                importState.value = WatchWorkoutImportSurfaceState.Failed(
                    WatchWorkoutImportSessionState(
                        importAttemptId = attemptId,
                        error = error,
                    ),
                )
            }
        }
    }

    private fun retryImport() {
        dismissImport()
        startImport()
    }

    private fun dismissImport() {
        watchWorkoutImportCoordinator.reset()
        importState.value = WatchWorkoutImportSurfaceState.Idle
    }

    private fun cancelImport(reason: WatchWorkoutImportCancellationReason) {
        scope.launch {
            watchWorkoutImportCoordinator.cancelImport(reason)
        }
    }

    private fun closeSelectedActivity() {
        selectedActivityId.value = null
        selectedActivity.value = null
        latestCalibration.value = null
        detailState.value = ExerciseDetailState.Loading
        reviewFormState.value = ExerciseReviewFormState()
        calibrationFormState.value = ExerciseCalibrationFormState()
        operationError.value = null
    }

    private fun submitReview() {
        val activity = selectedActivity.value ?: return
        val form = reviewFormState.value
        val selectedType = form.selectedType ?: run {
            operationError.value = ExerciseOperationError(ExerciseOperationErrorCategory.INVALID_TYPE, "Selecione um tipo válido para concluir a revisão.")
            return
        }
        if (form.isSaving) return

        reviewFormState.value = form.copy(isSaving = true, error = null)
        scope.launch {
            when (
                val result = workoutActivityRepository.submitReview(
                    SubmitWorkoutReviewInput(
                        activityId = activity.id,
                        confirmedType = selectedType,
                        title = form.titleInput,
                        notes = form.notesInput,
                        reviewedAt = Instant.now(),
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    reviewFormState.value = reviewFormState.value.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        savedAt = Instant.now(),
                        error = null,
                    )
                    operationError.value = null
                }

                RepositoryResult.NotFound -> reviewError(ExerciseOperationErrorCategory.NOT_FOUND, "Atividade não encontrada para concluir a revisão.")
                RepositoryResult.StorageUnavailable -> reviewError(ExerciseOperationErrorCategory.STORAGE, "Não foi possível salvar a revisão da atividade.")
                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                -> reviewError(ExerciseOperationErrorCategory.INVALID_STATE, "A atividade não está em um estado válido para revisão.")
            }
        }
    }

    private fun submitCalibration() {
        val activity = selectedActivity.value ?: return
        val form = calibrationFormState.value
        if (form.isSaving) return
        val reportedMeters = parseDistanceMeters(form.reportedDistanceInput) ?: run {
            val error = ExerciseOperationError(ExerciseOperationErrorCategory.INVALID_DISTANCE, "Informe uma distância válida para calibrar a atividade.")
            calibrationFormState.value = form.copy(validationError = error)
            operationError.value = error
            return
        }

        calibrationFormState.value = form.copy(isSaving = true, validationError = null)
        scope.launch {
            when (
                val result = treadmillCalibrationRepository.calibrateImportedActivity(
                    CalibrateImportedActivityInput(
                        activityId = activity.id,
                        reportedMeters = reportedMeters,
                        appliedAt = Instant.now(),
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    calibrationFormState.value = calibrationFormState.value.copy(
                        isSaving = false,
                        completedAt = Instant.now(),
                        validationError = null,
                    )
                    operationError.value = null
                }

                RepositoryResult.NotFound -> calibrationError(ExerciseOperationErrorCategory.NOT_FOUND, "Atividade não encontrada para calibrar.")
                RepositoryResult.StorageUnavailable -> calibrationError(ExerciseOperationErrorCategory.STORAGE, "Não foi possível salvar a calibração da atividade.")
                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                -> calibrationError(ExerciseOperationErrorCategory.INVALID_STATE, "A atividade não está em um estado válido para calibração.")
            }
        }
    }

    private fun discardSelectedActivity() {
        val activity = selectedActivity.value ?: return
        scope.launch {
            when (val result = workoutActivityRepository.discard(activity.id)) {
                is RepositoryResult.Success -> {
                    operationError.value = null
                    closeSelectedActivity()
                }

                RepositoryResult.NotFound -> operationError.value = ExerciseOperationError(ExerciseOperationErrorCategory.NOT_FOUND, "Atividade não encontrada para descarte.")
                RepositoryResult.StorageUnavailable -> operationError.value = ExerciseOperationError(ExerciseOperationErrorCategory.STORAGE, "Não foi possível descartar a atividade.")
                RepositoryResult.Conflict,
                RepositoryResult.UnexpectedState,
                -> operationError.value = ExerciseOperationError(ExerciseOperationErrorCategory.INVALID_STATE, "A atividade não está em um estado válido para descarte.")
            }
        }
    }

    private fun syncFormsWithSelectedActivity(activity: WorkoutActivity?) {
        if (activity == null) {
            reviewFormState.value = ExerciseReviewFormState()
            calibrationFormState.value = ExerciseCalibrationFormState()
            return
        }
        if (reviewFormState.value.loadedActivityId != activity.id || !reviewFormState.value.hasUnsavedChanges) {
            reviewFormState.value = ExerciseReviewFormState(
                loadedActivityId = activity.id,
                selectedType = activity.confirmedType ?: activity.suggestedType,
                titleInput = activity.title.orEmpty(),
                notesInput = activity.notes.orEmpty(),
            )
        }
        if (calibrationFormState.value.loadedActivityId != activity.id || !calibrationFormState.value.isSaving) {
            val input = if (calibrationFormState.value.loadedActivityId == activity.id) calibrationFormState.value.reportedDistanceInput else ""
            calibrationFormState.value = ExerciseCalibrationFormState(
                loadedActivityId = activity.id,
                reportedDistanceInput = input,
                previewDistanceMeters = previewDistanceMeters(activity, input),
                previewAveragePaceSecondsPerKilometer = previewAveragePace(activity, input),
                previewAverageSpeedMetersPerSecond = previewAverageSpeed(activity, input),
            )
        }
    }

    private fun updateCalibrationDistance(distance: String) {
        val activity = selectedActivity.value
        calibrationFormState.value = calibrationFormState.value.copy(
            reportedDistanceInput = distance,
            previewDistanceMeters = previewDistanceMeters(activity, distance),
            previewAveragePaceSecondsPerKilometer = previewAveragePace(activity, distance),
            previewAverageSpeedMetersPerSecond = previewAverageSpeed(activity, distance),
            validationError = null,
        )
    }

    private fun updateReviewForm(transform: ExerciseReviewFormState.() -> ExerciseReviewFormState) {
        reviewFormState.value = reviewFormState.value.transform()
    }

    private fun reviewError(category: ExerciseOperationErrorCategory, message: String) {
        val error = ExerciseOperationError(category, message)
        reviewFormState.value = reviewFormState.value.copy(isSaving = false, error = error)
        operationError.value = error
    }

    private fun calibrationError(category: ExerciseOperationErrorCategory, message: String) {
        val error = ExerciseOperationError(category, message)
        calibrationFormState.value = calibrationFormState.value.copy(isSaving = false, validationError = error)
        operationError.value = error
    }

    private fun parseDistanceMeters(input: String): Double? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val kilometers = normalized.toDoubleOrNull() ?: return null
        if (!kilometers.isFinite() || kilometers <= 0.0) return null
        return kilometers * 1_000.0
    }

    private fun previewDistanceMeters(activity: WorkoutActivity?, input: String): Double? =
        if (activity == null) null else parseDistanceMeters(input)

    private fun previewAveragePace(activity: WorkoutActivity?, input: String): Double? {
        val reportedMeters = parseDistanceMeters(input) ?: return null
        val durationSeconds = activity?.metrics?.duration?.seconds ?: return null
        if (durationSeconds <= 0L) return null
        return durationSeconds.toDouble() * 1_000.0 / reportedMeters
    }

    private fun previewAverageSpeed(activity: WorkoutActivity?, input: String): Double? {
        val reportedMeters = parseDistanceMeters(input) ?: return null
        val durationSeconds = activity?.metrics?.duration?.seconds ?: return null
        if (durationSeconds <= 0L) return null
        return reportedMeters / durationSeconds.toDouble()
    }
}
