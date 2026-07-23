package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.lifecycle.ViewModel
import br.com.mwconsulting.championcompanion.core.model.AppPreferencesRepository
import br.com.mwconsulting.championcompanion.core.model.Device
import br.com.mwconsulting.championcompanion.core.model.DeviceRepository
import br.com.mwconsulting.championcompanion.core.model.UuidV7Generator
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportError
import br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportErrorCategory
import br.com.mwconsulting.championcompanion.core.model.WorkoutActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel internal constructor(
    private val deviceRepository: DeviceRepository,
    private val workoutActivityRepository: WorkoutActivityRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
    private val scope: CoroutineScope,
    private val ids: UuidV7Generator,
) : ViewModel(), ExercisesFeatureContract {
    private val mutableState = MutableStateFlow(ExercisesUiState())
    private val associatedDevice = MutableStateFlow<Device?>(null)
    private val importState = MutableStateFlow<WatchWorkoutImportSurfaceState>(WatchWorkoutImportSurfaceState.Idle)
    private val pendingActivities = MutableStateFlow<List<br.com.mwconsulting.championcompanion.core.model.WorkoutActivity>>(emptyList())
    private val latestImportMetadata = MutableStateFlow<br.com.mwconsulting.championcompanion.core.model.WatchWorkoutImportMetadata?>(null)
    private val dailyStepsGoal = MutableStateFlow(br.com.mwconsulting.championcompanion.core.model.DEFAULT_DAILY_STEPS_GOAL)

    internal var ownsScope: Boolean = false

    override val state: StateFlow<ExercisesUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            deviceRepository.observeAssociatedDevice().collect { device ->
                associatedDevice.value = device
            }
        }
        scope.launch {
            workoutActivityRepository.observePendingReview().collect { activities ->
                pendingActivities.value = activities
            }
        }
        scope.launch {
            associatedDevice.flatMapLatest { device ->
                if (device == null) {
                    flowOf(null)
                } else {
                    workoutActivityRepository.observeWatchWorkoutImportMetadata(device.id)
                }
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
                pendingActivities,
                dailyStepsGoal,
                associatedDevice,
            ) { currentImportState, metadata, activities, goal, device ->
                ExercisesUiState(
                    importState = currentImportState,
                    latestImportMetadata = metadata,
                    pendingActivities = activities,
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
        appPreferencesRepository: AppPreferencesRepository,
        watchWorkoutImportCoordinator: WatchWorkoutImportCoordinator,
    ) : this(
        deviceRepository = deviceRepository,
        workoutActivityRepository = workoutActivityRepository,
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
}
