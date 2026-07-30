package br.com.mwconsulting.championcompanion.feature.exercises
import android.util.Log
import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncOutcome
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationEvent
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSourceFactory
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
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
import br.com.mwconsulting.championcompanion.core.model.WorkoutCountdownPolicy
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
    private val watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
    private val workoutLocationSourceFactory: WorkoutLocationSourceFactory,
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
    private val workoutCountdownPreference = MutableStateFlow(
        br.com.mwconsulting.championcompanion.core.model.DEFAULT_WORKOUT_COUNTDOWN_SECONDS,
    )
    private val workoutPreparation = MutableStateFlow(WorkoutPreparationUiState())

    private var preparationVisible = false
    private var locationSource: WorkoutLocationSource? = null
    private var locationEventsJob: Job? = null
    private var locationRuntimeGeneration = 0L
    private var activeLocationRuntimeGeneration: Long? = null
    private var preparationVisibilitySequence = 0L
    private var activePreparationVisibilityToken: Long? = null
    private var countdownJob: Job? = null
    private var countdownRunId = 0L

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
                logWorkflowDbSnapshot(
                    source = "activity_list",
                    activities = activities,
                    summary = workflowSummary.value,
                )
            }
        }
        scope.launch {
            workoutActivityRepository.observeWorkflowSummary().collect { summary ->
                workflowSummary.value = summary
                logWorkflowDbSnapshot(
                    source = "workflow_summary",
                    activities = activityList.value,
                    summary = summary,
                )
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
            appPreferencesRepository.observeWorkoutCountdownSeconds().collect { storedSeconds ->
                val seconds = WorkoutCountdownPolicy.sanitize(storedSeconds)
                workoutCountdownPreference.value = seconds
                val current = workoutPreparation.value
                if (current.phase != WorkoutPreparationPhase.COUNTING_DOWN &&
                    current.phase != WorkoutPreparationPhase.START_REQUESTED
                ) {
                    workoutPreparation.value = current.copy(countdownSeconds = seconds)
                }
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
                workoutPreparation,
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
                val preparation = values[12] as WorkoutPreparationUiState
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
                        (
                            calibrationForm.loadedActivityId == activity?.id &&
                                calibrationForm.reportedDistanceInput.isNotBlank() &&
                                calibrationForm.completedAt == null
                            ),
                    canDiscardSelectedActivity = activity?.workflowState in setOf(
                        ActivityWorkflowState.PENDING_REVIEW,
                        ActivityWorkflowState.PENDING_CALIBRATION,
                        ActivityWorkflowState.READY_TO_SYNC,
                    ),
                    dailyStepsGoal = goal,
                    hasAssociatedDevice = device != null,
                    associatedDeviceName = device?.name,
                    workoutPreparation = preparation,
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
        healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
        watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
        workoutLocationSourceFactory: WorkoutLocationSourceFactory,
    ) : this(
        deviceRepository = deviceRepository,
        workoutActivityRepository = workoutActivityRepository,
        treadmillCalibrationRepository = treadmillCalibrationRepository,
        appPreferencesRepository = appPreferencesRepository,
        healthConnectSyncCoordinator = healthConnectSyncCoordinator,
        watchWorkoutImportCoordinator = watchWorkoutImportCoordinator,
        workoutLocationSourceFactory = workoutLocationSourceFactory,
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
            ExercisesIntent.SyncSelectedActivity -> syncSelectedActivity()
            ExercisesIntent.DiscardSelectedActivity -> discardSelectedActivity()
            ExercisesIntent.ConfirmExitDetail -> closeSelectedActivity()
            ExercisesIntent.DismissOperationError -> operationError.value = null
            ExercisesIntent.StartImport -> startImport()
            ExercisesIntent.CancelImport -> cancelImport(WatchWorkoutImportCancellationReason.USER_REQUESTED)
            ExercisesIntent.RetryImport -> retryImport()
            ExercisesIntent.DismissImport -> dismissImport()
            ExercisesIntent.ConfirmExitImport -> cancelImport(WatchWorkoutImportCancellationReason.EXIT_CONFIRMED)
            ExercisesIntent.AppMovedToBackground -> {
                cancelImport(WatchWorkoutImportCancellationReason.BACKGROUND)
                suspendWorkoutPreparation()
            }
            ExercisesIntent.OpenDevice -> Unit
            is ExercisesIntent.EnterWorkoutPreparation -> enterWorkoutPreparation(intent.workoutType)
            ExercisesIntent.SuspendWorkoutPreparation -> suspendWorkoutPreparation()
            ExercisesIntent.ExitWorkoutPreparation -> exitWorkoutPreparation()
            ExercisesIntent.RefreshWorkoutLocation -> refreshWorkoutLocation()
            is ExercisesIntent.SelectWorkoutCountdown -> selectWorkoutCountdown(intent.seconds)
            ExercisesIntent.StartWorkoutCountdown -> startWorkoutCountdown()
            ExercisesIntent.CancelWorkoutCountdown -> cancelWorkoutCountdown("Countdown cancelado.")
        }
    }

    override fun onCleared() {
        stopWorkoutLocation()
        cancelCountdownInternal()
        if (ownsScope) {
            scope.cancel()
        }
        super.onCleared()
    }

    private fun enterWorkoutPreparation(workoutType: WorkoutType) {
        if (workoutType == WorkoutType.UNKNOWN) return
        val current = workoutPreparation.value
        val typeChanged = current.workoutType != workoutType
        if (!preparationVisible || typeChanged) {
            activePreparationVisibilityToken = ++preparationVisibilitySequence
        }
        preparationVisible = true
        if (typeChanged) {
            cancelCountdownInternal()
            stopWorkoutLocation()
            workoutPreparation.value = WorkoutPreparationUiState(
                workoutType = workoutType,
                phase = WorkoutPreparationPhase.WAITING,
                countdownSeconds = workoutCountdownPreference.value,
            )
        } else if (current.phase == WorkoutPreparationPhase.INACTIVE) {
            workoutPreparation.value = current.copy(
                phase = WorkoutPreparationPhase.WAITING,
                remainingSeconds = null,
                notice = null,
            )
        }

        if (workoutType.requiresGps() && locationSource == null &&
            workoutPreparation.value.phase != WorkoutPreparationPhase.START_REQUESTED
        ) {
            startWorkoutLocation()
        } else if (!workoutType.requiresGps()) {
            stopWorkoutLocation()
            workoutPreparation.value = workoutPreparation.value.copy(gpsReadiness = null)
        }
    }

    private fun suspendWorkoutPreparation() {
        if (!preparationVisible && workoutPreparation.value.phase == WorkoutPreparationPhase.INACTIVE) return
        preparationVisible = false
        activePreparationVisibilityToken = null
        if (workoutPreparation.value.phase == WorkoutPreparationPhase.COUNTING_DOWN) {
            cancelWorkoutCountdown("Countdown interrompido porque a preparação saiu da tela.")
        } else {
            cancelCountdownInternal()
        }
        stopWorkoutLocation()
    }

    private fun exitWorkoutPreparation() {
        preparationVisible = false
        activePreparationVisibilityToken = null
        cancelCountdownInternal()
        stopWorkoutLocation()
        workoutPreparation.value = WorkoutPreparationUiState(
            countdownSeconds = workoutCountdownPreference.value,
        )
    }

    private fun refreshWorkoutLocation() {
        val current = workoutPreparation.value
        if (!preparationVisible || !current.requiresGps ||
            current.phase == WorkoutPreparationPhase.START_REQUESTED
        ) return
        stopWorkoutLocation()
        workoutPreparation.value = workoutPreparation.value.copy(notice = null)
        startWorkoutLocation()
    }

    private fun startWorkoutLocation() {
        val current = workoutPreparation.value
        if (!preparationVisible || !current.requiresGps || locationSource != null) return

        invalidateGpsReadiness()
        val source = workoutLocationSourceFactory.create()
        val generation = ++locationRuntimeGeneration
        activeLocationRuntimeGeneration = generation
        locationSource = source
        locationEventsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            source.events.collect { event ->
                if (event.runtimeGeneration == generation && source === locationSource) {
                    handleWorkoutLocationEvent(event)
                }
            }
        }
        if (source.start(generation) != WorkoutLocationStartResult.STARTED) {
            stopWorkoutLocation()
            workoutPreparation.value = workoutPreparation.value.copy(
                notice = "GPS indisponível para esta preparação. Tente novamente.",
            )
        }
    }

    private fun handleWorkoutLocationEvent(event: WorkoutLocationEvent) {
        if (event !is WorkoutLocationEvent.Readiness) return
        val current = workoutPreparation.value
        if (!preparationVisible || !current.requiresGps ||
            event.runtimeGeneration != activeLocationRuntimeGeneration
        ) return
        val stateAfterEvent = current.copy(
            gpsReadiness = event.value,
            gpsReadinessGeneration = event.runtimeGeneration,
        )
        workoutPreparation.value = stateAfterEvent
        if (current.phase == WorkoutPreparationPhase.COUNTING_DOWN && !event.value.canStart) {
            cancelWorkoutCountdown(
                notice = "A prontidão do GPS foi perdida. Aguarde um novo sinal pronto.",
                state = stateAfterEvent,
            )
        }
    }

    private fun stopWorkoutLocation() {
        val source = locationSource
        val sourceGeneration = activeLocationRuntimeGeneration
        locationSource = null
        activeLocationRuntimeGeneration = null
        locationEventsJob?.cancel()
        locationEventsJob = null
        if (source != null) {
            sourceGeneration?.let(source::stop)
            source.close()
        }
        invalidateGpsReadiness()
    }

    private fun selectWorkoutCountdown(seconds: Int) {
        val sanitized = WorkoutCountdownPolicy.sanitize(seconds)
        val current = workoutPreparation.value
        if (current.phase == WorkoutPreparationPhase.COUNTING_DOWN ||
            current.phase == WorkoutPreparationPhase.START_REQUESTED
        ) return
        workoutPreparation.value = current.copy(
            countdownSeconds = sanitized,
            preferenceError = null,
            notice = null,
        )
        scope.launch {
            when (appPreferencesRepository.updateWorkoutCountdownSeconds(sanitized)) {
                is RepositoryResult.Success -> Unit
                RepositoryResult.Conflict,
                RepositoryResult.NotFound,
                RepositoryResult.StorageUnavailable,
                RepositoryResult.UnexpectedState,
                -> workoutPreparation.value = workoutPreparation.value.copy(
                    preferenceError = "Não foi possível salvar a duração do countdown.",
                )
            }
        }
    }

    private fun startWorkoutCountdown() {
        val current = workoutPreparation.value
        val workoutType = current.workoutType ?: return
        val visibilityToken = activePreparationVisibilityToken ?: return
        val gpsGeneration = if (current.requiresGps) activeGpsReadinessGeneration(current) ?: return else null
        if (!preparationVisible || !current.canStartCountdown || current.startRequest != null) return

        cancelCountdownInternal()
        val runId = ++countdownRunId
        val seconds = WorkoutCountdownPolicy.sanitize(current.countdownSeconds)
        if (seconds == 0) {
            emitWorkoutStartRequest(runId, visibilityToken, gpsGeneration, workoutType, seconds)
            return
        }

        workoutPreparation.value = current.copy(
            phase = WorkoutPreparationPhase.COUNTING_DOWN,
            countdownSeconds = seconds,
            remainingSeconds = seconds,
            notice = null,
        )
        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(COUNTDOWN_TICK_MILLIS)
                if (runId != countdownRunId ||
                    workoutPreparation.value.phase != WorkoutPreparationPhase.COUNTING_DOWN
                ) return@launch
                remaining -= 1
                if (remaining > 0) {
                    workoutPreparation.value = workoutPreparation.value.copy(remainingSeconds = remaining)
                }
            }
            emitWorkoutStartRequest(runId, visibilityToken, gpsGeneration, workoutType, seconds)
        }
    }

    private fun emitWorkoutStartRequest(
        runId: Long,
        visibilityToken: Long,
        gpsGeneration: Long?,
        workoutType: WorkoutType,
        countdownSeconds: Int,
    ) {
        val current = workoutPreparation.value
        if (runId != countdownRunId || !preparationVisible ||
            activePreparationVisibilityToken != visibilityToken || current.workoutType != workoutType ||
            current.startRequest != null ||
            current.phase !in setOf(WorkoutPreparationPhase.WAITING, WorkoutPreparationPhase.COUNTING_DOWN) ||
            (current.requiresGps && activeGpsReadinessGeneration(current) != gpsGeneration)
        ) {
            if (current.phase == WorkoutPreparationPhase.COUNTING_DOWN) {
                cancelWorkoutCountdown("As pré-condições mudaram. Revise a preparação antes de iniciar.")
            }
            return
        }

        val request = WorkoutStartRequest(
            requestId = ids.next().toString(),
            workoutType = workoutType,
            requestedAt = Instant.now(),
            countdownSeconds = countdownSeconds,
            preparationVisibilityToken = visibilityToken,
            gpsReadinessGeneration = gpsGeneration,
        )
        countdownJob = null
        workoutPreparation.value = current.copy(
            phase = WorkoutPreparationPhase.START_REQUESTED,
            remainingSeconds = 0,
            notice = null,
            startRequest = request,
        )
        stopWorkoutLocation()
    }

    private fun cancelWorkoutCountdown(
        notice: String,
        state: WorkoutPreparationUiState = workoutPreparation.value,
    ) {
        val current = state
        if (current.phase != WorkoutPreparationPhase.COUNTING_DOWN) return
        cancelCountdownInternal()
        workoutPreparation.value = current.copy(
            phase = WorkoutPreparationPhase.WAITING,
            remainingSeconds = null,
            notice = notice,
        )
    }

    private fun cancelCountdownInternal() {
        countdownRunId += 1
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun invalidateGpsReadiness() {
        val current = workoutPreparation.value
        if (current.gpsReadiness == null && current.gpsReadinessGeneration == null) return
        val invalidated = current.copy(
            gpsReadiness = null,
            gpsReadinessGeneration = null,
        )
        workoutPreparation.value = invalidated
        if (invalidated.phase == WorkoutPreparationPhase.COUNTING_DOWN) {
            cancelWorkoutCountdown(
                notice = "A prontidão do GPS foi perdida. Aguarde uma nova verificação.",
                state = invalidated,
            )
        }
    }

    private fun activeGpsReadinessGeneration(state: WorkoutPreparationUiState): Long? {
        val readinessGeneration = state.gpsReadinessGeneration
        return readinessGeneration.takeIf {
            state.gpsReadiness?.canStart == true && it == activeLocationRuntimeGeneration
        }
    }

    private fun WorkoutType.requiresGps(): Boolean =
        this == WorkoutType.WALKING || this == WorkoutType.RUNNING

    private companion object {
        const val COUNTDOWN_TICK_MILLIS = 1_000L
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

    private fun syncSelectedActivity() {
        val activity = selectedActivity.value ?: return
        if (activity.workflowState !in setOf(ActivityWorkflowState.READY_TO_SYNC, ActivityWorkflowState.SYNC_FAILED)) {
            operationError.value = ExerciseOperationError(
                ExerciseOperationErrorCategory.INVALID_STATE,
                "A atividade precisa estar pronta ou com erro para sincronizar com o Health Connect.",
            )
            return
        }
        scope.launch {
            when (val outcome = healthConnectSyncCoordinator.syncActivity(activity.id)) {
                is HealthConnectSyncOutcome.Completed -> operationError.value = null
                is HealthConnectSyncOutcome.NothingToSync -> operationError.value = null
                is HealthConnectSyncOutcome.CompletedWithFailures -> {
                    operationError.value = ExerciseOperationError(
                        ExerciseOperationErrorCategory.PARTIAL_BLOCKED,
                        "Alguns records do treino foram sincronizados, mas ${outcome.failures.size} ainda precisam de retry.",
                    )
                }

                is HealthConnectSyncOutcome.PermissionRequired -> {
                    operationError.value = ExerciseOperationError(
                        ExerciseOperationErrorCategory.PERMISSION_REQUIRED,
                        "Permissões do Health Connect ainda faltam para concluir a sincronização deste treino.",
                    )
                }

                is HealthConnectSyncOutcome.ProviderUnavailable -> {
                    operationError.value = ExerciseOperationError(
                        ExerciseOperationErrorCategory.PROVIDER_UNAVAILABLE,
                        if (outcome.updateRequired) {
                            "Atualize o Health Connect para sincronizar este treino."
                        } else {
                            "Health Connect indisponível neste dispositivo no momento."
                        },
                    )
                }

                is HealthConnectSyncOutcome.Disabled -> {
                    operationError.value = ExerciseOperationError(
                        ExerciseOperationErrorCategory.SYNC_DISABLED,
                        "Ative a integração com Health Connect em Configurações para sincronizar este treino.",
                    )
                }

                is HealthConnectSyncOutcome.Failed -> {
                    operationError.value = ExerciseOperationError(
                        ExerciseOperationErrorCategory.SYNC_FAILED,
                        outcome.reason,
                    )
                }
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
            val previous = calibrationFormState.value
            val preserveCompleted = previous.loadedActivityId == activity.id && previous.completedAt != null
            val input = when {
                preserveCompleted -> ""
                previous.loadedActivityId == activity.id -> previous.reportedDistanceInput
                else -> ""
            }
            calibrationFormState.value = ExerciseCalibrationFormState(
                loadedActivityId = activity.id,
                reportedDistanceInput = input,
                previewDistanceMeters = previewDistanceMeters(activity, input),
                previewAveragePaceSecondsPerKilometer = previewAveragePace(activity, input),
                previewAverageSpeedMetersPerSecond = previewAverageSpeed(activity, input),
                completedAt = previous.completedAt.takeIf { preserveCompleted },
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
            completedAt = null,
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

    private fun logWorkflowDbSnapshot(
        source: String,
        activities: List<WorkoutActivity>,
        summary: WorkoutWorkflowSummary,
    ) {
        val syncingCount = activities.count { it.workflowState == ActivityWorkflowState.SYNCING }
        val readyCount = activities.count { it.workflowState == ActivityWorkflowState.READY_TO_SYNC }
        val syncedCount = activities.count { it.workflowState == ActivityWorkflowState.SYNCED }
        val failedCount = activities.count { it.workflowState == ActivityWorkflowState.SYNC_FAILED }
        val discardedCount = activities.count { it.workflowState == ActivityWorkflowState.DISCARDED }
        val pendingReviewCount = activities.count { it.workflowState == ActivityWorkflowState.PENDING_REVIEW }
        val pendingCalibrationCount = activities.count { it.workflowState == ActivityWorkflowState.PENDING_CALIBRATION }
        Log.d(
            "ChampionCompanion",
            "ExercisesDbStatus source=$source filter=${selectedFilter.value.name} " +
                "summary[pendingReview=${summary.pendingReviewCount},pendingCalibration=${summary.pendingCalibrationCount},ready=${summary.readyToSyncCount},synced=${summary.syncedCount},failed=${summary.syncFailedCount},discarded=${summary.discardedCount}] " +
                "list[count=${activities.size},pendingReview=$pendingReviewCount,pendingCalibration=$pendingCalibrationCount,ready=$readyCount,syncing=$syncingCount,synced=$syncedCount,failed=$failedCount,discarded=$discardedCount]",
        )
    }
}
