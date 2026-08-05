package br.com.mwconsulting.championcompanion.feature.exercises

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionAggregates
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.DomainValue
import br.com.mwconsulting.championcompanion.core.model.ForegroundBleOperationOwner
import br.com.mwconsulting.championcompanion.core.model.GpsAccuracyQuality
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessState
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.model.WorkoutCountdownPolicy
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.navigation.AppDestination
import br.com.mwconsulting.championcompanion.core.notification.WorkoutNotificationChannel
import java.time.Duration
import java.util.Locale

private data class WorkoutTypeOption(
    val type: WorkoutType,
    val label: String,
    val description: String,
)

private val workoutTypes = listOf(
    WorkoutTypeOption(WorkoutType.WALKING, "Caminhada", "Ao ar livre, com localização precisa e rota."),
    WorkoutTypeOption(WorkoutType.RUNNING, "Corrida", "Ao ar livre, com localização precisa e rota."),
    WorkoutTypeOption(WorkoutType.FREE_WORKOUT, "Treino livre", "Sem uso de localização ou GPS."),
    WorkoutTypeOption(WorkoutType.TREADMILL, "Esteira", "Ambiente interno, sem uso de localização ou GPS."),
)

@Composable
fun PhoneWorkoutDisabledShell(onGoToExercises: () -> Unit) {
    LaunchedEffect(Unit) { onGoToExercises() }
    WorkoutShellCard(
        title = "Treino pelo celular indisponível",
        onNavigateBack = onGoToExercises,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            Text(
                text = PhoneWorkoutPolicy.DISCONTINUED_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
            )
            PrimaryActionButton(
                label = "Ir para Exercícios",
                onClick = onGoToExercises,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun WorkoutTypeSelectionShell(
    onSelectType: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    WorkoutSelectionPreparationLayout(
        title = "Selecionar treino",
        description = "Escolha um tipo. Você revisará todas as condições antes de iniciar.",
        onNavigateBack = onNavigateBack,
    ) {
        workoutTypes.forEach { option ->
            Card(
                onClick = { onSelectType(option.type.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${option.label}. ${option.description}"
                    },
                shape = ChampionShapeMedium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(ChampionSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text(option.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutPreparationShell(
    workoutType: String,
    state: ExercisesUiState,
    onIntent: (ExercisesIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val selectedType = workoutType.toWorkoutTypeOrNull()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val requiresNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationAvailability by remember(selectedType) {
        mutableStateOf(workoutNotificationAvailability(context))
    }
    var notificationPermissionRequested by rememberSaveable(selectedType) { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        notificationPermissionRequested = true
        notificationAvailability = workoutNotificationAvailability(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onIntent(ExercisesIntent.RefreshWorkoutLocation)
    }

    if (selectedType == null) {
        WorkoutSelectionPreparationLayout(
            title = "Tipo de treino inválido",
            description = "Volte e selecione caminhada, corrida, treino livre ou esteira.",
            onNavigateBack = onNavigateBack,
        )
        return
    }

    LaunchedEffect(selectedType) {
        onIntent(ExercisesIntent.EnterWorkoutPreparation(selectedType))
    }
    DisposableEffect(lifecycleOwner, selectedType) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    notificationAvailability = workoutNotificationAvailability(context)
                    onIntent(ExercisesIntent.EnterWorkoutPreparation(selectedType))
                }
                Lifecycle.Event.ON_STOP -> onIntent(ExercisesIntent.SuspendWorkoutPreparation)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val preparation = state.workoutPreparation
    WorkoutSelectionPreparationLayout(
        title = "Preparar ${selectedType.toWorkoutTypeLabel().lowercase()}",
        description = "Confira countdown, GPS quando aplicável e relógio opcional.",
        onNavigateBack = {
            onIntent(ExercisesIntent.ExitWorkoutPreparation)
            onNavigateBack()
        },
    ) {
        CountdownPreferenceCard(
            preparation = preparation,
            onSelect = { onIntent(ExercisesIntent.SelectWorkoutCountdown(it)) },
        )

        if (selectedType.requiresGps()) {
            GpsPreparationCard(
                readiness = preparation.gpsReadiness,
                onRequestPrecisePermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                onOpenLocationSettings = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                },
                onRetry = { onIntent(ExercisesIntent.RefreshWorkoutLocation) },
            )
        } else {
            NoGpsCard(selectedType)
        }

        if (requiresNotificationPermission ||
            notificationAvailability != WorkoutNotificationAvailability.AVAILABLE
        ) {
            val showNotificationRationale = (context as? Activity)?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }.orFalse()
            NotificationPermissionCard(
                availability = notificationAvailability,
                deniedAfterRequest = notificationPermissionRequested,
                canRequestRuntimePermission = notificationAvailability ==
                    WorkoutNotificationAvailability.RUNTIME_PERMISSION_REQUIRED &&
                    (!notificationPermissionRequested || showNotificationRationale),
                onRequest = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onOpenSettings = {
                    context.openWorkoutNotificationSettings(notificationAvailability)
                },
            )
        }

        OptionalWatchCard(
            hasAssociatedDevice = state.hasAssociatedDevice,
            associatedDeviceName = state.associatedDeviceName,
        )

        preparation.preferenceError?.let { PreparationNotice(it, isError = true) }
        preparation.notice?.let { PreparationNotice(it, isError = false) }

        when (preparation.phase) {
            WorkoutPreparationPhase.COUNTING_DOWN -> CountdownCard(
                remainingSeconds = preparation.remainingSeconds ?: preparation.countdownSeconds,
                onCancel = { onIntent(ExercisesIntent.CancelWorkoutCountdown) },
            )

            WorkoutPreparationPhase.START_REQUESTED -> StartRequestedCard(preparation.startRequest)

            WorkoutPreparationPhase.INACTIVE,
            WorkoutPreparationPhase.WAITING,
            -> PrimaryActionButton(
                label = if (preparation.countdownSeconds == 0) "Iniciar agora" else "Iniciar countdown",
                onClick = { onIntent(ExercisesIntent.StartWorkoutCountdown) },
                enabled = preparation.workoutType == selectedType && preparation.canStartCountdown &&
                    notificationAvailability == WorkoutNotificationAvailability.AVAILABLE,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = "Voltar ou cancelar antes de zero não cria sessão, atividade, rota nem serviço.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkoutSelectionPreparationLayout(
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(ChampionSpacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ChampionShapeLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        ) {
            Column(
                modifier = Modifier.padding(ChampionSpacing.large),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Voltar")
                }
            }
        }
        content()
    }
}

@Composable
private fun NotificationPermissionCard(
    availability: WorkoutNotificationAvailability,
    deniedAfterRequest: Boolean,
    canRequestRuntimePermission: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val granted = availability == WorkoutNotificationAvailability.AVAILABLE
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Notificação da sessão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    granted -> "Permitida. O treino permanecerá visível enquanto estiver ativo."
                    availability == WorkoutNotificationAvailability.CHANNEL_BLOCKED ->
                        "O canal da sessão está desativado. Reative-o nas configurações para iniciar ou continuar o treino."
                    availability == WorkoutNotificationAvailability.APP_NOTIFICATIONS_BLOCKED ->
                        "As notificações do app estão desativadas. Reative-as nas configurações para iniciar ou continuar o treino."
                    deniedAfterRequest ->
                        "Permissão recusada. Ela é necessária para manter a sessão visível; conceda-a antes de iniciar ou continuar."
                    else ->
                        "Permita notificações para manter controles e estado do treino visíveis. Sem essa permissão, o início fica bloqueado."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!granted) {
                PrimaryActionButton(
                    label = if (canRequestRuntimePermission) {
                        "Permitir notificações"
                    } else {
                        "Abrir configurações de notificações"
                    },
                    onClick = if (canRequestRuntimePermission) onRequest else onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun Context.openWorkoutNotificationSettings(
    availability: WorkoutNotificationAvailability,
) {
    val intent = if (availability == WorkoutNotificationAvailability.CHANNEL_BLOCKED) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, WorkoutNotificationChannel.CHANNEL_ID)
        }
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    }
    startActivity(intent)
}

private fun Boolean?.orFalse(): Boolean = this ?: false

@Composable
private fun CountdownPreferenceCard(
    preparation: WorkoutPreparationUiState,
    onSelect: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = ChampionShapeMedium) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Countdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "A última escolha fica salva neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                WorkoutCountdownPolicy.allowedSeconds.sorted().forEach { seconds ->
                    FilterChip(
                        selected = preparation.countdownSeconds == seconds,
                        onClick = { onSelect(seconds) },
                        enabled = preparation.phase == WorkoutPreparationPhase.WAITING,
                        label = { Text(if (seconds == 0) "Sem countdown" else "$seconds s") },
                    )
                }
            }
        }
    }
}

@Composable
private fun GpsPreparationCard(
    readiness: GpsReadiness?,
    onRequestPrecisePermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val presentation = readiness.toGpsPresentation()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Status do GPS. ${presentation.title}. ${presentation.message}"
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (readiness?.canStart == true) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("GPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(presentation.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(presentation.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                readiness.toGpsQualityDetail(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (readiness?.state) {
                GpsReadinessState.PERMISSION_REQUIRED,
                GpsReadinessState.PRECISE_PERMISSION_REQUIRED,
                -> PrimaryActionButton(
                    label = "Permitir localização precisa",
                    onClick = onRequestPrecisePermission,
                    modifier = Modifier.fillMaxWidth(),
                )

                GpsReadinessState.LOCATION_DISABLED -> PrimaryActionButton(
                    label = "Ativar localização",
                    onClick = onOpenLocationSettings,
                    modifier = Modifier.fillMaxWidth(),
                )

                GpsReadinessState.SEARCHING,
                GpsReadinessState.LOST,
                GpsReadinessState.UNAVAILABLE,
                null,
                -> TextButton(onClick = onRetry) { Text("Verificar novamente") }

                GpsReadinessState.READY -> Unit
            }
        }
    }
}

@Composable
private fun NoGpsCard(workoutType: WorkoutType) {
    val guidance = if (workoutType == WorkoutType.TREADMILL) {
        "A distância real fica indisponível durante a sessão. Após finalizar, informe a distância mostrada pela esteira no fluxo de calibração; o app não estima esse valor."
    } else {
        "A sessão não mede distância."
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "GPS não usado em ${workoutType.toWorkoutTypeLabel()}. Nenhuma permissão de localização será solicitada. $guidance"
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Sem GPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${workoutType.toWorkoutTypeLabel()} não usa localização e não solicitará essa permissão.")
            Text(guidance, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OptionalWatchCard(hasAssociatedDevice: Boolean, associatedDeviceName: String?) {
    val status = if (hasAssociatedDevice) {
        "Relógio associado: ${associatedDeviceName ?: "Champion C001"}. A conexão ao vivo será verificada somente ao iniciar a sessão."
    } else {
        "Nenhum relógio associado. O treino ainda pode ser iniciado normalmente pelo celular."
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Relógio opcional. $status"
            },
        shape = ChampionShapeMedium,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Relógio opcional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Para BPM ou passos do Champion C001, inicie também um treino no relógio. O app não aciona modo esporte automaticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CountdownCard(remainingSeconds: Int, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (remainingSeconds > 0) {
                    "Treino iniciará em $remainingSeconds segundos"
                } else {
                    "Solicitando início do treino"
                }
                liveRegion = LiveRegionMode.Polite
            },
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Início em", style = MaterialTheme.typography.titleMedium)
            Text(
                remainingSeconds.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            PrimaryActionButton(
                label = "Cancelar countdown",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StartRequestedCard(request: WorkoutStartRequest?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Countdown concluído. Solicitação de início emitida."
                liveRegion = LiveRegionMode.Polite
            },
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Countdown concluído", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (request == null) {
                    "Preparando solicitação de início."
                } else {
                    "Solicitação de início emitida uma vez. A sessão ativa será assumida pelo próximo estágio do fluxo."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PreparationNotice(message: String, isError: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Text(message, modifier = Modifier.padding(ChampionSpacing.medium), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ActiveWorkoutShell(
    sessionId: String,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateBack: () -> Unit,
    exercisesViewModel: ExercisesViewModel,
) {
    val session by exercisesViewModel.activeSession.collectAsState()
    val durationText by exercisesViewModel.activeDurationText.collectAsState()
    val liveMetrics by exercisesViewModel.activeLiveMetrics.collectAsState()
    val watchParticipation by exercisesViewModel.watchParticipation.collectAsState()
    val isPaused by exercisesViewModel.isPaused.collectAsState()
    val activeCommandNotice by exercisesViewModel.activeCommandNotice.collectAsState()
    val finishCommandState by exercisesViewModel.finishCommandState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requiresNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationAvailability by remember(sessionId) {
        mutableStateOf(workoutNotificationAvailability(context))
    }
    var notificationPermissionRequested by rememberSaveable(sessionId) { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        notificationPermissionRequested = true
        notificationAvailability = workoutNotificationAvailability(context)
    }
    var showFinishConfirmation by rememberSaveable(sessionId) { mutableStateOf(false) }
    val finishInProgress = (finishCommandState as? WorkoutFinishCommandState.InProgress)
        ?.sessionId == sessionId

    LaunchedEffect(sessionId) {
        exercisesViewModel.onActiveWorkoutScreenShown(sessionId)
    }
    DisposableEffect(lifecycleOwner, sessionId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                notificationAvailability = workoutNotificationAvailability(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sessionState = session?.state

    LaunchedEffect(sessionState) {
        when (sessionState) {
            AppWorkoutSessionState.CREATED,
            AppWorkoutSessionState.AWAITING_RECOVERY,
            AppWorkoutSessionState.FINISHING,
            -> onNavigateTo(AppDestination.WorkoutRecovery(sessionId))
            AppWorkoutSessionState.FINISHED,
            AppWorkoutSessionState.DISCARDED,
            -> onNavigateBack()
            else -> Unit
        }
    }

    val nonGpsSession = session?.takeIf {
        it.type == WorkoutType.FREE_WORKOUT || it.type == WorkoutType.TREADMILL
    }
    val displayedWatchParticipation = watchParticipation.takeIf { it.sessionId == session?.id }
        ?: WatchWorkoutParticipationState(sessionId = session?.id)
    WorkoutShellCard(
        title = "Treino ativo",
        onNavigateBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Workout type label
            Text(
                text = session?.type?.toWorkoutTypeLabel() ?: "Carregando...",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Duration display
            Text(
                text = durationText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pause indicator
            if (isPaused) {
                Text(
                    text = "Pausado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (session?.recordsRoute == true) {
                WorkoutGpsMetricsCard(
                    metrics = liveMetrics,
                    onRetryCapture = { exercisesViewModel.retryRouteCapture() },
                )
            } else if (nonGpsSession != null) {
                WorkoutNoGpsMetricsCard(
                    workoutType = nonGpsSession.type,
                    aggregates = nonGpsSession.aggregates,
                )
            }

            if (session != null) {
                WatchWorkoutParticipationCard(
                    state = displayedWatchParticipation,
                    onEnable = { exercisesViewModel.enableWatchParticipation() },
                    onRetry = { exercisesViewModel.retryWatchParticipation() },
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            activeCommandNotice?.let { PreparationNotice(it, isError = true) }

            if (isPaused && (requiresNotificationPermission ||
                    notificationAvailability != WorkoutNotificationAvailability.AVAILABLE)
            ) {
                val showNotificationRationale = (context as? Activity)?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                }.orFalse()
                NotificationPermissionCard(
                    availability = notificationAvailability,
                    deniedAfterRequest = notificationPermissionRequested,
                    canRequestRuntimePermission = notificationAvailability ==
                        WorkoutNotificationAvailability.RUNTIME_PERMISSION_REQUIRED &&
                        (!notificationPermissionRequested || showNotificationRationale),
                    onRequest = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onOpenSettings = {
                        context.openWorkoutNotificationSettings(notificationAvailability)
                    },
                )
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pause/Resume button
                if (isPaused) {
                    FilledTonalButton(
                        onClick = { exercisesViewModel.resumeSession() },
                        enabled = notificationAvailability == WorkoutNotificationAvailability.AVAILABLE,
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Continuar")
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar")
                    }
                } else {
                    FilledTonalButton(
                        onClick = { exercisesViewModel.pauseSession() },
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pausar")
                        Spacer(Modifier.width(8.dp))
                        Text("Pausar")
                    }
                }

                // Finish button
                Button(
                    onClick = { showFinishConfirmation = true },
                    enabled = !finishInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Finalizar treino")
                }
            }
        }
    }

    if (showFinishConfirmation && session != null) {
        WorkoutMutationConfirmationDialog(
            workoutType = session!!.type,
            durationText = durationText,
            confirmLabel = "Confirmar",
            onConfirm = {
                if (exercisesViewModel.finishSession()) {
                    showFinishConfirmation = false
                }
            },
            onCancel = { showFinishConfirmation = false },
        )
    }
}

@Composable
private fun WatchWorkoutParticipationCard(
    state: WatchWorkoutParticipationState,
    onEnable: () -> Boolean,
    onRetry: () -> Boolean,
) {
    val action = when (state.status) {
        WatchWorkoutParticipationStatus.NOT_ENABLED,
        WatchWorkoutParticipationStatus.ASSOCIATED,
        -> "Associar relógio à sessão" to onEnable

        WatchWorkoutParticipationStatus.CONNECTED,
        -> if (!state.participationEnabled) {
            "Associar relógio à sessão" to onEnable
        } else {
            null
        }

        WatchWorkoutParticipationStatus.DISCONNECTED,
        WatchWorkoutParticipationStatus.CONFLICT,
        -> "Tentar novamente" to onRetry

        WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE,
        WatchWorkoutParticipationStatus.CONNECTING,
        -> null
    }
    val statusLabel = when (state.status) {
        WatchWorkoutParticipationStatus.NOT_ENABLED -> "Não ativado"
        WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE -> "Sem relógio associado"
        WatchWorkoutParticipationStatus.ASSOCIATED -> "Associado"
        WatchWorkoutParticipationStatus.CONNECTING -> "Conectando"
        WatchWorkoutParticipationStatus.CONNECTED -> "Conectado"
        WatchWorkoutParticipationStatus.DISCONNECTED -> "Desconectado"
        WatchWorkoutParticipationStatus.CONFLICT -> "Conflito de operação"
    }
    val body = when (state.status) {
        WatchWorkoutParticipationStatus.NOT_ENABLED ->
            "A sessão continua funcionando sem relógio. A participação só começa após ação explícita."
        WatchWorkoutParticipationStatus.NO_ASSOCIATED_DEVICE ->
            "Nenhum relógio está associado. O treino continua funcionando sem relógio."
        WatchWorkoutParticipationStatus.ASSOCIATED ->
            "Relógio associado, mas a participação ainda não está ativa. O modo esportivo continua não comprovado."
        WatchWorkoutParticipationStatus.CONNECTING ->
            "Conectando ao relógio. O app não inicia modo esportivo e ainda não atribui BPM ou passos."
        WatchWorkoutParticipationStatus.CONNECTED -> if (state.participationEnabled) {
            "Relógio conectado. Modo esportivo não comprovado; BPM e passos permanecem ausentes. Inicie também o treino no relógio se quiser dados esportivos."
        } else {
            "Relógio associado e conectado, mas ainda não participa desta sessão. A participação só começa após ação explícita."
        }
        WatchWorkoutParticipationStatus.DISCONNECTED ->
            "Relógio desconectado. O treino do celular continua normalmente. Você pode tentar uma reconexão controlada."
        WatchWorkoutParticipationStatus.CONFLICT ->
            "${state.conflictOwner.toWorkoutWatchConflictCopy()} Aguarde essa operação terminar antes de associar o relógio ao treino."
    }
    val description = "Relógio opcional. Status: $statusLabel. $body"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (state.status == WatchWorkoutParticipationStatus.CONFLICT) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Relógio opcional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Status: $statusLabel", style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action?.let { (label, callback) ->
                OutlinedButton(onClick = { callback() }) {
                    Text(label)
                }
            }
        }
    }
}

private fun ForegroundBleOperationOwner?.toWorkoutWatchConflictCopy(): String = when (this) {
    ForegroundBleOperationOwner.DAILY_STEPS_REFRESH -> "Uma sincronização diária do relógio está em andamento."
    ForegroundBleOperationOwner.MANUAL_HEART_RATE -> "Uma medição manual está em andamento."
    ForegroundBleOperationOwner.WATCH_D1_HISTORY -> "Uma importação de histórico está em andamento."
    ForegroundBleOperationOwner.WATCH_WORKOUT_IMPORT -> "Uma importação de treino do relógio está em andamento."
    ForegroundBleOperationOwner.APP_WORKOUT_SESSION -> "A participação do relógio nesta sessão já está em andamento."
    ForegroundBleOperationOwner.DEVICE_CONNECTION -> "Uma conexão do dispositivo está em andamento."
    null -> "Outra operação do relógio está em andamento."
}

@Composable
private fun WorkoutNoGpsMetricsCard(
    workoutType: WorkoutType,
    aggregates: AppWorkoutSessionAggregates,
) {
    val availableMetrics = buildList {
        if (aggregates.stepsAvailable) {
            add("Passos ${formatWorkoutSteps(aggregates.steps)}")
        }
        when (val calories = aggregates.caloriesKilocalories) {
            is DomainValue.Present -> add("Calorias ${formatWorkoutCalories(calories.value)}")
            is DomainValue.Absent -> Unit
        }
        when (val averageBpm = aggregates.averageBpm) {
            is DomainValue.Present -> add("BPM médio ${averageBpm.value}")
            is DomainValue.Absent -> Unit
        }
        when (val minimumBpm = aggregates.minimumBpm) {
            is DomainValue.Present -> add("BPM mínimo ${minimumBpm.value}")
            is DomainValue.Absent -> Unit
        }
        when (val maximumBpm = aggregates.maximumBpm) {
            is DomainValue.Present -> add("BPM máximo ${maximumBpm.value}")
            is DomainValue.Absent -> Unit
        }
    }
    val distanceDescription = if (workoutType == WorkoutType.TREADMILL) {
        "Distância indisponível antes da calibração."
    } else {
        "Distância não medida nesta sessão."
    }
    val metricsDescription = if (availableMetrics.isEmpty()) {
        "Nenhuma métrica opcional disponível no momento."
    } else {
        "Métricas disponíveis: ${availableMetrics.joinToString(separator = ", ")}."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${workoutType.toWorkoutTypeLabel()} sem GPS. $distanceDescription $metricsDescription"
            },
        shape = ChampionShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Sessão sem GPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${workoutType.toWorkoutTypeLabel()} não usa GPS. A ausência de GPS não impede o treino.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(distanceDescription, style = MaterialTheme.typography.bodyMedium)
            if (availableMetrics.isEmpty()) {
                Text(
                    metricsDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                availableMetrics.forEach { metric ->
                    Text(metric, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun formatWorkoutSteps(value: Long): String =
    java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value)

private fun formatWorkoutCalories(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.0f kcal", value)

@Composable
private fun WorkoutGpsMetricsCard(
    metrics: WorkoutLiveMetricsSnapshot?,
    onRetryCapture: () -> Unit,
) {
    val current = metrics ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Métricas GPS. Distância ${formatRouteDistance(current.distanceMeters)}. " +
                    "GPS ${current.gpsStatus.toDisplayLabel()}."
            },
        colors = CardDefaults.cardColors(
            containerColor = if (current.captureError == null) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Métricas GPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "GPS: ${current.gpsStatus.toDisplayLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Distância: ${formatRouteDistance(current.distanceMeters)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.large)) {
                MetricValue("Ritmo atual", formatPace(current.currentPaceSecondsPerKilometer))
                MetricValue("Ritmo médio", formatPace(current.averagePaceSecondsPerKilometer))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.large)) {
                MetricValue("Velocidade", formatSpeed(current.currentSpeedMetersPerSecond))
                MetricValue("Média", formatSpeed(current.averageSpeedMetersPerSecond))
            }
            Text(
                "${current.acceptedPointCount} pontos aceitos · ${current.segmentCount} segmentos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            current.captureError?.let { message ->
                PreparationNotice(message, isError = true)
                OutlinedButton(onClick = onRetryCapture, modifier = Modifier.fillMaxWidth()) {
                    Text("Tentar salvar rota novamente")
                }
            }
            if (current.captureError == null && current.gpsStatus in setOf(
                    WorkoutGpsCaptureStatus.UNAVAILABLE,
                    WorkoutGpsCaptureStatus.LOCATION_DISABLED,
                    WorkoutGpsCaptureStatus.PERMISSION_REQUIRED,
                )
            ) {
                OutlinedButton(onClick = onRetryCapture, modifier = Modifier.fillMaxWidth()) {
                    Text("Verificar GPS novamente")
                }
            }
        }
    }
}

@Composable
private fun RowScope.MetricValue(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatRouteDistance(distanceMeters: Double): String = if (distanceMeters >= 1_000.0) {
    String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1_000.0)
} else {
    String.format(Locale.getDefault(), "%.0f m", distanceMeters)
}

private fun formatPace(secondsPerKilometer: Double?): String {
    if (secondsPerKilometer == null || !secondsPerKilometer.isFinite() || secondsPerKilometer <= 0.0) {
        return "—"
    }
    val totalSeconds = secondsPerKilometer.toLong().coerceAtLeast(1L)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')} /km"
}

private fun formatSpeed(metersPerSecond: Double?): String = if (
    metersPerSecond == null || !metersPerSecond.isFinite() || metersPerSecond < 0.0
) {
    "—"
} else {
    String.format(Locale.getDefault(), "%.1f km/h", metersPerSecond * 3.6)
}

private fun WorkoutGpsCaptureStatus.toDisplayLabel(): String = when (this) {
    WorkoutGpsCaptureStatus.STARTING -> "iniciando"
    WorkoutGpsCaptureStatus.READY -> "pronto"
    WorkoutGpsCaptureStatus.SEARCHING -> "procurando sinal"
    WorkoutGpsCaptureStatus.LOST -> "sinal perdido"
    WorkoutGpsCaptureStatus.UNAVAILABLE -> "indisponível"
    WorkoutGpsCaptureStatus.PERMISSION_REQUIRED -> "permissão necessária"
    WorkoutGpsCaptureStatus.LOCATION_DISABLED -> "localização desativada"
    WorkoutGpsCaptureStatus.PAUSED -> "pausado"
    WorkoutGpsCaptureStatus.ERROR -> "captura pausada"
}

@Composable
fun WorkoutRecoveryShell(
    sessionId: String?,
    onNavigateBack: () -> Unit,
    exercisesViewModel: ExercisesViewModel,
) {
    val recoverableSession by exercisesViewModel.recoverableSession.collectAsState()
    val actionState by exercisesViewModel.recoveryActionState.collectAsState()
    val session = recoverableSession
    var pendingConfirmation by rememberSaveable(sessionId) {
        mutableStateOf<RecoveryAction?>(null)
    }
    val isBusy = actionState is RecoveryActionState.InProgress
    val discardSucceeded = (actionState as? RecoveryActionState.Success)?.action == RecoveryAction.DISCARD

    LaunchedEffect(sessionId) {
        exercisesViewModel.onRecoveryScreenShown(sessionId)
    }

    LaunchedEffect(discardSucceeded) {
        if (discardSucceeded) {
            exercisesViewModel.consumeRecoveryActionResult()
            onNavigateBack()
        }
    }

    WorkoutShellCard(
        title = "Treino pelo celular descontinuado",
        onNavigateBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (session == null) {
                Text(
                    text = "Nenhum treino pendente de recuperação.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onNavigateBack) {
                    Text("Voltar")
                }
            } else {
                Text(
                    text = PhoneWorkoutPolicy.DISCONTINUED_MESSAGE,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = if (session.state == AppWorkoutSessionState.FINISHING) {
                        PhoneWorkoutPolicy.FINISHING_NOT_DISCARDABLE_MESSAGE
                    } else {
                        PhoneWorkoutPolicy.NON_RESUMABLE_MESSAGE
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = "Tipo: ${session.type.toWorkoutTypeLabel()}",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = "Duração: ${formatDuration(session.activeDuration)}",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (val result = actionState) {
                    is RecoveryActionState.InProgress -> Row(
                        horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("Processando ação…")
                    }
                    is RecoveryActionState.Failure -> PreparationNotice(result.message, isError = true)
                    else -> Unit
                }

                if (session.state in setOf(
                        AppWorkoutSessionState.CREATED,
                        AppWorkoutSessionState.ACTIVE,
                        AppWorkoutSessionState.PAUSED,
                        AppWorkoutSessionState.AWAITING_RECOVERY,
                    ) && session.workoutActivityId == null
                ) {
                    TextButton(
                        onClick = { pendingConfirmation = RecoveryAction.DISCARD },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Descartar",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }


    val confirmedSession = session
    if (pendingConfirmation != null && confirmedSession != null) {
        WorkoutMutationConfirmationDialog(
            workoutType = confirmedSession.type,
            durationText = formatDuration(confirmedSession.activeDuration),
            confirmLabel = "Confirmar",
            onConfirm = {
                pendingConfirmation = null
                exercisesViewModel.discardRecoveredSession()
            },
            onCancel = { pendingConfirmation = null },
        )
    }
}

@Composable
fun WorkoutFinishConfirmationShell(
    sessionId: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRecoveryRequired: () -> Unit,
    exercisesViewModel: ExercisesViewModel,
) {
    val session by exercisesViewModel.activeSession.collectAsState()
    val durationText by exercisesViewModel.activeDurationText.collectAsState()
    val activeCommandNotice by exercisesViewModel.activeCommandNotice.collectAsState()
    val finishCommandState by exercisesViewModel.finishCommandState.collectAsState()
    val finishInProgress = (finishCommandState as? WorkoutFinishCommandState.InProgress)
        ?.sessionId == sessionId
    val finishPersisted = (finishCommandState as? WorkoutFinishCommandState.Persisted)
        ?.sessionId == sessionId

    LaunchedEffect(sessionId) {
        exercisesViewModel.onActiveWorkoutScreenShown(sessionId)
    }
    LaunchedEffect(session?.state) {
        when (session?.state) {
            AppWorkoutSessionState.CREATED,
            AppWorkoutSessionState.AWAITING_RECOVERY,
            AppWorkoutSessionState.FINISHING,
            -> onRecoveryRequired()
            AppWorkoutSessionState.FINISHED,
            AppWorkoutSessionState.DISCARDED,
            -> onCancel()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ChampionSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (session == null) {
            CircularProgressIndicator()
        } else {
            Text("Tipo: ${session!!.type.toWorkoutTypeLabel()}")
            Text("Duração: $durationText")
            activeCommandNotice?.let { PreparationNotice(it, isError = true) }
            if (finishInProgress) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Confirmando finalização…")
                }
            }
            Spacer(Modifier.height(ChampionSpacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                Button(
                    onClick = onConfirm,
                    enabled = !finishInProgress && !finishPersisted,
                ) {
                    Text("Confirmar")
                }
                TextButton(onClick = onCancel, enabled = !finishInProgress && !finishPersisted) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun WorkoutMutationConfirmationDialog(
    workoutType: WorkoutType,
    durationText: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                Text("Tipo: ${workoutType.toWorkoutTypeLabel()}")
                Text("Duração: $durationText")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
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

@Composable
private fun WorkoutShellCard(
    title: String,
    description: String = "",
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(ChampionSpacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
    ) {
        TextButton(onClick = onNavigateBack) {
            Text("Voltar")
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ChampionShapeLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        ) {
            Column(
                modifier = Modifier.padding(ChampionSpacing.large),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                content()
            }
        }
    }
}

private data class GpsPresentation(val title: String, val message: String)

private fun GpsReadiness?.toGpsPresentation(): GpsPresentation = when (this?.state) {
    null -> GpsPresentation("Verificando GPS", "Aguarde a leitura de permissão, serviços e fix recente.")
    GpsReadinessState.PERMISSION_REQUIRED -> GpsPresentation(
        "Permissão necessária",
        "Permita localização precisa somente para preparar caminhada ou corrida.",
    )
    GpsReadinessState.PRECISE_PERMISSION_REQUIRED -> GpsPresentation(
        "Localização aproximada detectada",
        "Precisão aproximada não é suficiente para registrar esta rota. Autorize localização precisa.",
    )
    GpsReadinessState.LOCATION_DISABLED -> GpsPresentation(
        "Localização desativada",
        "Ative os serviços de localização e volte para continuar.",
    )
    GpsReadinessState.SEARCHING -> when (accuracyQuality) {
        GpsAccuracyQuality.WEAK -> GpsPresentation("GPS fraco", "Fix recente encontrado, mas a precisão ainda não permite iniciar.")
        else -> GpsPresentation("Procurando GPS", "Aguarde um fix recente com precisão boa ou forte.")
    }
    GpsReadinessState.READY -> when (accuracyQuality) {
        GpsAccuracyQuality.STRONG -> GpsPresentation("GPS pronto · forte", "Condições válidas para iniciar.")
        else -> GpsPresentation("GPS pronto · bom", "Condições válidas para iniciar.")
    }
    GpsReadinessState.LOST -> GpsPresentation("GPS perdido", "A prontidão foi perdida. Aguarde novo fix antes de iniciar.")
    GpsReadinessState.UNAVAILABLE -> GpsPresentation("GPS indisponível", "Não foi possível obter um fix utilizável neste momento.")
}

private fun GpsReadiness?.toGpsQualityDetail(): String = when {
    this == null -> "Nenhum ponto de aquecimento é salvo."
    gnss?.averageCn0DbHz != null -> {
        "${gnss.usedInFixSatelliteCount} de ${gnss.visibleSatelliteCount} satélites usados; C/N0 disponível como complemento."
    }
    else -> "Qualidade baseada na precisão aproximada informada pelo GPS; detalhes C/N0 indisponíveis."
}

private fun String.toWorkoutTypeOrNull(): WorkoutType? =
    runCatching { WorkoutType.valueOf(this) }.getOrNull()?.takeIf { it != WorkoutType.UNKNOWN }

private fun WorkoutType.toWorkoutTypeLabel(): String = when (this) {
    WorkoutType.WALKING -> "Caminhada"
    WorkoutType.RUNNING -> "Corrida"
    WorkoutType.FREE_WORKOUT -> "Treino livre"
    WorkoutType.TREADMILL -> "Esteira"
    WorkoutType.UNKNOWN -> "Tipo desconhecido"
}

private fun WorkoutType.requiresGps(): Boolean = this == WorkoutType.WALKING || this == WorkoutType.RUNNING
