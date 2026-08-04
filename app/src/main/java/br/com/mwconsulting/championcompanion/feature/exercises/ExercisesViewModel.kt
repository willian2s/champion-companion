package br.com.mwconsulting.championcompanion.feature.exercises

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncCoordinator
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectSyncOutcome
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationEvent
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSource
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationSourceFactory
import br.com.mwconsulting.championcompanion.core.location.WorkoutLocationStartResult
import br.com.mwconsulting.championcompanion.core.model.ActivityWorkflowState
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSession
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionId
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.CalibrateImportedActivityInput
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.DomainValue
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
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoute
import br.com.mwconsulting.championcompanion.core.model.WorkoutRouteRepository
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.model.WorkoutWorkflowSummary
import br.com.mwconsulting.championcompanion.core.model.nextAuditInstant
import br.com.mwconsulting.championcompanion.core.model.valueOrNull
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val COUNTDOWN_TICK_MILLIS = 1_000L
private const val FINISH_ACK_TIMEOUT_MILLIS = 10_000L
private const val FINISH_ACK_RELOAD_TIMEOUT_MILLIS = 2_000L
private val RECOVERY_VISIBLE_STATES = setOf(
    AppWorkoutSessionState.CREATED,
    AppWorkoutSessionState.ACTIVE,
    AppWorkoutSessionState.PAUSED,
    AppWorkoutSessionState.AWAITING_RECOVERY,
    AppWorkoutSessionState.FINISHING,
)

private data class ActiveWorkoutObservation(
    val session: AppWorkoutSession?,
    val route: WorkoutRoute?,
    val liveMetrics: WorkoutLiveMetricsSnapshot?,
)

private fun AppWorkoutSession.toLiveMetrics(route: WorkoutRoute?): WorkoutLiveMetricsSnapshot? {
    if (!recordsRoute) return null
    val distance = route?.metrics?.distanceMeters
        ?: (aggregates.distanceMeters as? DomainValue.Present)?.value
        ?: 0.0
    val activeSeconds = activeDuration.seconds.toDouble() + activeDuration.nano / 1_000_000_000.0
    val averageSpeed = distance.takeIf { it > 0.0 && activeSeconds > 0.0 }
        ?.div(activeSeconds)
    return WorkoutLiveMetricsSnapshot(
        sessionId = id,
        distanceMeters = distance,
        currentPaceSecondsPerKilometer = null,
        averagePaceSecondsPerKilometer = averageSpeed?.takeIf { it > 0.0 }?.let { 1_000.0 / it },
        currentSpeedMetersPerSecond = null,
        averageSpeedMetersPerSecond = averageSpeed,
        acceptedPointCount = route?.metrics?.acceptedPointCount ?: 0L,
        segmentCount = route?.metrics?.segmentCount ?: 0,
        gpsStatus = if (state == AppWorkoutSessionState.PAUSED) {
            WorkoutGpsCaptureStatus.PAUSED
        } else {
            WorkoutGpsCaptureStatus.STARTING
        },
    )
}

internal enum class WorkoutNotificationAvailability {
    AVAILABLE,
    RUNTIME_PERMISSION_REQUIRED,
    APP_NOTIFICATIONS_BLOCKED,
    CHANNEL_BLOCKED,
}

internal fun workoutNotificationAvailability(context: Context): WorkoutNotificationAvailability {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return WorkoutNotificationAvailability.RUNTIME_PERMISSION_REQUIRED
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        return WorkoutNotificationAvailability.APP_NOTIFICATIONS_BLOCKED
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.getNotificationChannel(WorkoutNotificationChannel.CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            return WorkoutNotificationAvailability.CHANNEL_BLOCKED
        }
    }
    return WorkoutNotificationAvailability.AVAILABLE
}

internal fun hasWorkoutNotificationPermission(context: Context): Boolean =
    workoutNotificationAvailability(context) == WorkoutNotificationAvailability.AVAILABLE

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
    private val appWorkoutSessionRepository: AppWorkoutSessionRepository,
    private val workoutRouteRepository: WorkoutRouteRepository,
    private val liveMetricsStore: WorkoutLiveMetricsStore,
    private val workoutSessionOwnershipCoordinator: WorkoutSessionOwnershipCoordinator,
    private val applicationContext: Context,
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

    // Active workout session state
    private val _activeSession = MutableStateFlow<AppWorkoutSession?>(null)
    val activeSession: StateFlow<AppWorkoutSession?> = _activeSession.asStateFlow()

    private val _activeDurationText = MutableStateFlow("00:00")
    val activeDurationText: StateFlow<String> = _activeDurationText.asStateFlow()

    private val _activeLiveMetrics = MutableStateFlow<WorkoutLiveMetricsSnapshot?>(null)
    val activeLiveMetrics: StateFlow<WorkoutLiveMetricsSnapshot?> = _activeLiveMetrics.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _activeCommandNotice = MutableStateFlow<String?>(null)
    val activeCommandNotice: StateFlow<String?> = _activeCommandNotice.asStateFlow()

    private val _finishCommandState = MutableStateFlow<WorkoutFinishCommandState>(WorkoutFinishCommandState.Idle)
    val finishCommandState: StateFlow<WorkoutFinishCommandState> = _finishCommandState.asStateFlow()

    private val _recoverableSession = MutableStateFlow<AppWorkoutSession?>(null)
    val recoverableSession: StateFlow<AppWorkoutSession?> = _recoverableSession.asStateFlow()

    private val _recoveryActionState = MutableStateFlow<RecoveryActionState>(RecoveryActionState.Idle)
    val recoveryActionState: StateFlow<RecoveryActionState> = _recoveryActionState.asStateFlow()

    private val _workoutStarted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val workoutStarted: SharedFlow<String> = _workoutStarted.asSharedFlow()

    private val _workoutRecoveryRequired = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val workoutRecoveryRequired: SharedFlow<String> = _workoutRecoveryRequired.asSharedFlow()

    private var durationTimerJob: Job? = null
    private var activeSessionObserverJob: Job? = null
    private var recoverySessionObserverJob: Job? = null
    private var finishAcknowledgementJob: Job? = null
    private var displaySessionRevision: Long? = null
    private var displaySessionState: AppWorkoutSessionState? = null
    private var displayRuntimeGeneration: Long? = null
    private var displayBaseActiveDuration: Duration = Duration.ZERO
    private var displayActiveSinceNanos: Long? = null

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
        appWorkoutSessionRepository: AppWorkoutSessionRepository,
        workoutRouteRepository: WorkoutRouteRepository,
        liveMetricsStore: WorkoutLiveMetricsStore,
        workoutSessionOwnershipCoordinator: WorkoutSessionOwnershipCoordinator,
        @ApplicationContext applicationContext: Context,
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
        appWorkoutSessionRepository = appWorkoutSessionRepository,
        workoutRouteRepository = workoutRouteRepository,
        liveMetricsStore = liveMetricsStore,
        workoutSessionOwnershipCoordinator = workoutSessionOwnershipCoordinator,
        applicationContext = applicationContext,
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
        activeSessionObserverJob?.cancel()
        durationTimerJob?.cancel()
        _activeLiveMetrics.value = null
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
        if (!hasWorkoutNotificationPermission(applicationContext)) {
            workoutPreparation.value = current.copy(
                notice = "Permita notificações para iniciar e manter o treino visível.",
            )
            return
        }

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

        // Create session and start foreground service
        scope.launch {
            val sessionId = AppWorkoutSessionId(ids.next())
            val createdAt = Instant.ofEpochMilli(Instant.now().toEpochMilli())
            val session = AppWorkoutSession.create(
                id = sessionId,
                type = workoutType,
                createdAt = createdAt,
            ).valueOrNull() ?: return@launch

            when (val result = appWorkoutSessionRepository.createStarted(session, createdAt)) {
                is RepositoryResult.Success -> {
                    if (startRecordingService(sessionId)) {
                        _workoutStarted.tryEmit(sessionId.value.toString())
                    } else {
                        compensateRejectedServiceStart(result.value)
                    }
                }
                else -> {
                    val openSession = appWorkoutSessionRepository.observeOpen().first()
                    failWorkoutStart(
                        if (openSession != null) {
                            val activeOwnedSession =
                                workoutSessionOwnershipCoordinator.isForegroundOwner(openSession.id) &&
                                    openSession.state in setOf(
                                        AppWorkoutSessionState.ACTIVE,
                                        AppWorkoutSessionState.PAUSED,
                                    )
                            if (activeOwnedSession) {
                                _workoutStarted.tryEmit(openSession.id.value.toString())
                            } else {
                                _workoutRecoveryRequired.tryEmit(openSession.id.value.toString())
                            }
                            if (activeOwnedSession) {
                                "Já existe um treino ativo."
                            } else {
                                "Já existe um treino pendente. Resolva a recuperação antes de iniciar outro."
                            }
                        } else {
                            "Não foi possível iniciar o treino. Tente novamente."
                        },
                    )
                }
            }
        }
    }

    // --- Session management for service commands ---

    private fun sessionIdString(): String? = _activeSession.value?.id?.value?.toString()

    fun pauseSession(): Boolean {
        val id = sessionIdString() ?: return false
        return sendServiceCommand(WorkoutRecordingService.ACTION_PAUSE, id)
    }

    fun resumeSession(): Boolean {
        val id = sessionIdString() ?: return false
        if (!hasWorkoutNotificationPermission(applicationContext)) {
            _activeCommandNotice.value =
                "Permita notificações antes de continuar o treino. A sessão permanece pausada."
            return false
        }
        return sendServiceCommand(WorkoutRecordingService.ACTION_RESUME, id)
    }

    fun retryRouteCapture(): Boolean {
        val session = _activeSession.value ?: return false
        if (!session.recordsRoute || session.state != AppWorkoutSessionState.ACTIVE) return false
        return sendServiceCommand(
            WorkoutRecordingService.ACTION_RETRY_ROUTE,
            session.id.value.toString(),
        )
    }

    fun finishSession(): Boolean {
        val session = _activeSession.value ?: return false
        if (session.state !in setOf(AppWorkoutSessionState.ACTIVE, AppWorkoutSessionState.PAUSED) ||
            _finishCommandState.value is WorkoutFinishCommandState.InProgress
        ) {
            return false
        }
        val id = session.id.value.toString()
        _finishCommandState.value = WorkoutFinishCommandState.InProgress(id)
        if (!sendServiceCommand(WorkoutRecordingService.ACTION_FINISH, id)) {
            failFinishCommand(id, "Não foi possível enviar a finalização. Tente novamente.")
            return false
        }
        waitForFinishAcknowledgement(session.id)
        return true
    }

    private fun sendServiceCommand(action: String, sessionId: String): Boolean {
        val intent = Intent(applicationContext, WorkoutRecordingService::class.java).apply {
            this.action = action
            putExtra(WorkoutRecordingService.EXTRA_SESSION_ID, sessionId)
        }
        return runCatching {
            ContextCompat.startForegroundService(applicationContext, intent)
        }.fold(
            onSuccess = {
                _activeCommandNotice.value = null
                true
            },
            onFailure = {
                _activeCommandNotice.value =
                    "Não foi possível enviar a ação ao serviço. Verifique as notificações e tente novamente."
                false
            },
        )
    }

    // --- Active workout screen ---

    fun onActiveWorkoutScreenShown(sessionId: String) {
        val id = try { AppWorkoutSessionId(UUID.fromString(sessionId)) } catch (_: Exception) { return }
        activeSessionObserverJob?.cancel()
        _activeLiveMetrics.value = null
        activeSessionObserverJob = scope.launch {
            combine(
                appWorkoutSessionRepository.observeSession(id),
                workoutRouteRepository.observeBySession(id),
                liveMetricsStore.observe(id),
            ) { session, route, liveMetrics ->
                ActiveWorkoutObservation(session, route, liveMetrics)
            }.collect { observation ->
                val session = observation.session
                _activeSession.value = session
                _isPaused.value = session?.state == AppWorkoutSessionState.PAUSED
                session?.let(::synchronizeDisplayTimer)
                _activeLiveMetrics.value = observation.liveMetrics
                    ?: session?.toLiveMetrics(observation.route)
                acknowledgePersistedFinish(id, session)
            }
        }
        startDurationTimer()
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (true) {
                delay(1_000L)
                val session = _activeSession.value ?: continue
                synchronizeDisplayTimer(session)
                val elapsedNanos = displayActiveSinceNanos?.let {
                    (SystemClock.elapsedRealtimeNanos() - it).coerceAtLeast(0L)
                } ?: 0L
                val displayDuration = displayBaseActiveDuration + Duration.ofNanos(elapsedNanos)
                _activeDurationText.value = formatDuration(displayDuration)
            }
        }
    }

    private fun synchronizeDisplayTimer(session: AppWorkoutSession) {
        if (displaySessionRevision == session.revision) return
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val currentDisplayedDuration = displayBaseActiveDuration + Duration.ofNanos(
            displayActiveSinceNanos?.let { (nowNanos - it).coerceAtLeast(0L) } ?: 0L,
        )
        val sameActiveRuntime = displaySessionState == AppWorkoutSessionState.ACTIVE &&
            session.state == AppWorkoutSessionState.ACTIVE &&
            displayRuntimeGeneration == session.runtimeGeneration
        displaySessionRevision = session.revision
        displaySessionState = session.state
        displayRuntimeGeneration = session.runtimeGeneration
        if (!sameActiveRuntime || session.activeDuration > currentDisplayedDuration) {
            displayBaseActiveDuration = session.activeDuration
            displayActiveSinceNanos = nowNanos.takeIf { session.state == AppWorkoutSessionState.ACTIVE }
        }
        _activeDurationText.value = formatDuration(
            if (sameActiveRuntime) maxOf(currentDisplayedDuration, session.activeDuration) else session.activeDuration,
        )
    }

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // --- Recovery screen ---

    fun onRecoveryScreenShown(sessionId: String?) {
        val previousAction = _recoveryActionState.value
        val previousSessionId = when (previousAction) {
            is RecoveryActionState.InProgress -> previousAction.sessionId
            is RecoveryActionState.Success -> previousAction.sessionId
            is RecoveryActionState.Failure -> previousAction.sessionId
            RecoveryActionState.Idle -> null
        }
        if (sessionId != null && previousSessionId != null && previousSessionId != sessionId) {
            _recoveryActionState.value = RecoveryActionState.Idle
        }
        recoverySessionObserverJob?.cancel()
        if (sessionId != null) {
            val id = try { AppWorkoutSessionId(UUID.fromString(sessionId)) } catch (_: Exception) { return }
            recoverySessionObserverJob = scope.launch {
                appWorkoutSessionRepository.observeSession(id).collect { session ->
                    _recoverableSession.value = session?.takeIf { it.state in RECOVERY_VISIBLE_STATES }
                }
            }
        } else {
            recoverySessionObserverJob = scope.launch {
                appWorkoutSessionRepository.observeRecoverable().collect { sessions ->
                    _recoverableSession.value = sessions.firstOrNull()
                }
            }
        }
    }

    fun recoverSession() {
        val session = _recoverableSession.value ?: return
        if (_recoveryActionState.value is RecoveryActionState.InProgress) return
        if (!hasWorkoutNotificationPermission(applicationContext)) {
            _recoveryActionState.value = RecoveryActionState.Failure(
                action = RecoveryAction.RESUME,
                sessionId = session.id.value.toString(),
                message = "Permita notificações antes de retomar o treino.",
            )
            return
        }
        _recoveryActionState.value = RecoveryActionState.InProgress(
            RecoveryAction.RESUME,
            session.id.value.toString(),
        )
        scope.launch {
            when (val result = appWorkoutSessionRepository.recover(
                sessionId = session.id,
                expectedRevision = session.revision,
                at = auditInstantForPersistedDurations(session),
            )) {
                is RepositoryResult.Success -> {
                    if (startRecordingService(session.id)) {
                        _recoveryActionState.value = RecoveryActionState.Success(
                            RecoveryAction.RESUME,
                            session.id.value.toString(),
                        )
                    } else {
                        compensateRejectedServiceStart(result.value, RecoveryAction.RESUME)
                    }
                }
                else -> {
                    reloadRecoveryAfterFailure(
                        session.id,
                        RecoveryAction.RESUME,
                        "Não foi possível retomar o treino. Revise o estado e tente novamente.",
                    )
                }
            }
        }
    }

    fun reconcileStaleRecoverySession() {
        val session = _recoverableSession.value ?: return
        if (session.state !in setOf(AppWorkoutSessionState.ACTIVE, AppWorkoutSessionState.PAUSED) ||
            _recoveryActionState.value is RecoveryActionState.InProgress
        ) return
        _recoveryActionState.value = RecoveryActionState.InProgress(
            RecoveryAction.RESUME,
            session.id.value.toString(),
        )
        scope.launch {
            when (val result = workoutSessionOwnershipCoordinator.reconcileSession(session)) {
                is StaleWorkoutReconciliation.RecoverySurface -> {
                    _recoverableSession.value = result.session
                    if (result.session.state == AppWorkoutSessionState.AWAITING_RECOVERY) {
                        _recoveryActionState.value = RecoveryActionState.Idle
                    } else {
                        _recoveryActionState.value = RecoveryActionState.Failure(
                            RecoveryAction.RESUME,
                            session.id.value.toString(),
                            "Não foi possível preparar a sessão para recuperação. Tente novamente.",
                        )
                    }
                }
                is StaleWorkoutReconciliation.ForegroundOwned -> {
                    _recoverableSession.value = result.session
                    _recoveryActionState.value = RecoveryActionState.Success(
                        RecoveryAction.RESUME,
                        session.id.value.toString(),
                    )
                }
                StaleWorkoutReconciliation.NoOpenSession -> {
                    _recoverableSession.value = null
                    _recoveryActionState.value = RecoveryActionState.Failure(
                        RecoveryAction.RESUME,
                        session.id.value.toString(),
                        "A sessão não está mais disponível para recuperação.",
                    )
                }
                StaleWorkoutReconciliation.Unavailable -> {
                    _recoverableSession.value = session
                    _recoveryActionState.value = RecoveryActionState.Failure(
                        RecoveryAction.RESUME,
                        session.id.value.toString(),
                        "Não foi possível confirmar o estado da sessão. Tente novamente.",
                    )
                }
            }
        }
    }

    fun discardRecoveredSession() {
        val session = _recoverableSession.value ?: return
        if (_recoveryActionState.value is RecoveryActionState.InProgress) return
        _recoveryActionState.value = RecoveryActionState.InProgress(
            RecoveryAction.DISCARD,
            session.id.value.toString(),
        )
        scope.launch {
            when (appWorkoutSessionRepository.discard(
                AppWorkoutSessionMutation(
                    sessionId = session.id,
                    expectedRevision = session.revision,
                    at = auditInstantForPersistedDurations(session),
                ),
            )) {
                is RepositoryResult.Success -> {
                    _recoverableSession.value = null
                    _recoveryActionState.value = RecoveryActionState.Success(
                        RecoveryAction.DISCARD,
                        session.id.value.toString(),
                    )
                }
                else -> reloadRecoveryAfterFailure(
                    session.id,
                    RecoveryAction.DISCARD,
                    "Não foi possível descartar o treino. Tente novamente.",
                )
            }
        }
    }

    fun finishRecoveredSession() {
        val session = _recoverableSession.value ?: return
        if (_recoveryActionState.value is RecoveryActionState.InProgress) return
        _recoveryActionState.value = RecoveryActionState.InProgress(
            RecoveryAction.FINISH,
            session.id.value.toString(),
        )
        scope.launch {
            when (val result = appWorkoutSessionRepository.beginFinishing(
                AppWorkoutSessionMutation(
                    sessionId = session.id,
                    expectedRevision = session.revision,
                    at = auditInstantForPersistedDurations(session),
                ),
            )) {
                is RepositoryResult.Success -> {
                    _recoverableSession.value = result.value
                    _recoveryActionState.value = RecoveryActionState.Success(
                        RecoveryAction.FINISH,
                        session.id.value.toString(),
                    )
                }
                else -> reloadRecoveryAfterFailure(
                    session.id,
                    RecoveryAction.FINISH,
                    "Não foi possível finalizar o treino. Revise o estado e tente novamente.",
                )
            }
        }
    }

    fun consumeRecoveryActionResult() {
        if (_recoveryActionState.value !is RecoveryActionState.InProgress) {
            _recoveryActionState.value = RecoveryActionState.Idle
        }
    }

    private fun startRecordingService(sessionId: AppWorkoutSessionId): Boolean = runCatching {
        ContextCompat.startForegroundService(
            applicationContext,
            WorkoutRecordingService.startIntent(applicationContext, sessionId.value.toString()),
        )
        true
    }.getOrDefault(false)

    private suspend fun compensateRejectedServiceStart(
        session: AppWorkoutSession,
        recoveryAction: RecoveryAction? = null,
    ) {
        val reconciliation = workoutSessionOwnershipCoordinator.reconcileSession(session)
        if (reconciliation is StaleWorkoutReconciliation.ForegroundOwned) {
            _recoverableSession.value = reconciliation.session
            if (recoveryAction == null) {
                _workoutStarted.tryEmit(session.id.value.toString())
            } else {
                _recoveryActionState.value = RecoveryActionState.Success(
                    recoveryAction,
                    session.id.value.toString(),
                )
            }
            return
        }
        val recoverable = when (reconciliation) {
            is StaleWorkoutReconciliation.RecoverySurface -> reconciliation.session
            StaleWorkoutReconciliation.Unavailable -> session
            StaleWorkoutReconciliation.NoOpenSession -> null
            is StaleWorkoutReconciliation.ForegroundOwned -> reconciliation.session
        }
        _recoverableSession.value = recoverable?.takeIf { it.state in RECOVERY_VISIBLE_STATES }
        _workoutRecoveryRequired.tryEmit(session.id.value.toString())
        if (recoveryAction == null) {
            failWorkoutStart("O serviço não pôde assumir o treino. A sessão ficou disponível para recuperação.")
        } else {
            _recoveryActionState.value = RecoveryActionState.Failure(
                recoveryAction,
                session.id.value.toString(),
                "O serviço não pôde assumir o treino. A sessão continua disponível para recuperação.",
            )
        }
    }

    private suspend fun reloadRecoveryAfterFailure(
        sessionId: AppWorkoutSessionId,
        action: RecoveryAction,
        message: String,
    ) {
        val reloaded = appWorkoutSessionRepository.observeSession(sessionId).first()
        val sessionIdText = sessionId.value.toString()
        when {
            action == RecoveryAction.RESUME && reloaded != null &&
                workoutSessionOwnershipCoordinator.isForegroundOwner(reloaded.id) &&
                reloaded.state in setOf(AppWorkoutSessionState.ACTIVE, AppWorkoutSessionState.PAUSED) -> {
                _recoverableSession.value = reloaded
                _recoveryActionState.value = RecoveryActionState.Success(action, sessionIdText)
            }
            action == RecoveryAction.FINISH && reloaded?.state == AppWorkoutSessionState.FINISHING -> {
                _recoverableSession.value = reloaded
                _recoveryActionState.value = RecoveryActionState.Success(action, sessionIdText)
            }
            action == RecoveryAction.DISCARD && reloaded?.state == AppWorkoutSessionState.DISCARDED -> {
                _recoverableSession.value = null
                _recoveryActionState.value = RecoveryActionState.Success(action, sessionIdText)
            }
            else -> {
                _recoverableSession.value = reloaded?.takeIf { it.state in RECOVERY_VISIBLE_STATES }
                _recoveryActionState.value = RecoveryActionState.Failure(
                    action = action,
                    sessionId = sessionIdText,
                    message = message,
                )
            }
        }
    }

    private fun failWorkoutStart(message: String) {
        workoutPreparation.value = workoutPreparation.value.copy(
            phase = WorkoutPreparationPhase.WAITING,
            remainingSeconds = null,
            notice = message,
            startRequest = null,
        )
    }

    private fun waitForFinishAcknowledgement(sessionId: AppWorkoutSessionId) {
        finishAcknowledgementJob?.cancel()
        finishAcknowledgementJob = scope.launch {
            delay(FINISH_ACK_TIMEOUT_MILLIS)
            val persisted = try {
                kotlinx.coroutines.withTimeoutOrNull(FINISH_ACK_RELOAD_TIMEOUT_MILLIS) {
                    appWorkoutSessionRepository.observeSession(sessionId).first()
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                null
            }
            if (persisted?.state in setOf(
                    AppWorkoutSessionState.FINISHING,
                    AppWorkoutSessionState.FINISHED,
                )
            ) {
                acknowledgePersistedFinish(sessionId, persisted)
            } else if ((_finishCommandState.value as? WorkoutFinishCommandState.InProgress)
                    ?.sessionId == sessionId.value.toString()
            ) {
                failFinishCommand(
                    sessionId.value.toString(),
                    "A finalização não foi confirmada. A sessão continua aberta; tente novamente.",
                )
            }
        }
    }

    private fun acknowledgePersistedFinish(
        sessionId: AppWorkoutSessionId,
        session: AppWorkoutSession?,
    ) {
        val expectedId = sessionId.value.toString()
        val inProgress = _finishCommandState.value as? WorkoutFinishCommandState.InProgress ?: return
        if (inProgress.sessionId != expectedId) return
        when (session?.state) {
            AppWorkoutSessionState.FINISHING,
            AppWorkoutSessionState.FINISHED,
            -> {
                finishAcknowledgementJob?.cancel()
                finishAcknowledgementJob = null
                _activeCommandNotice.value = null
                _finishCommandState.value = WorkoutFinishCommandState.Persisted(expectedId)
            }
            AppWorkoutSessionState.CREATED,
            AppWorkoutSessionState.AWAITING_RECOVERY,
            AppWorkoutSessionState.DISCARDED,
            null,
            -> failFinishCommand(
                expectedId,
                "A finalização não foi confirmada. Revise a recuperação e tente novamente.",
            )
            AppWorkoutSessionState.ACTIVE,
            AppWorkoutSessionState.PAUSED,
            -> Unit
        }
    }

    private fun failFinishCommand(sessionId: String, message: String) {
        finishAcknowledgementJob?.cancel()
        finishAcknowledgementJob = null
        _activeCommandNotice.value = message
        _finishCommandState.value = WorkoutFinishCommandState.Failure(sessionId, message)
    }

    private fun auditInstantForPersistedDurations(session: AppWorkoutSession): Instant {
        return session.nextAuditInstant(Instant.now())
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
