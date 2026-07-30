package br.com.mwconsulting.championcompanion.feature.exercises

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.GpsAccuracyQuality
import br.com.mwconsulting.championcompanion.core.model.GpsReadiness
import br.com.mwconsulting.championcompanion.core.model.GpsReadinessState
import br.com.mwconsulting.championcompanion.core.model.WorkoutCountdownPolicy
import br.com.mwconsulting.championcompanion.core.model.WorkoutType
import br.com.mwconsulting.championcompanion.core.navigation.AppDestination
import java.time.Duration

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
fun WorkoutTypeSelectionShell(
    onSelectType: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    WorkoutShellCard(
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
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onIntent(ExercisesIntent.RefreshWorkoutLocation)
    }

    if (selectedType == null) {
        WorkoutShellCard(
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
                Lifecycle.Event.ON_START -> onIntent(ExercisesIntent.EnterWorkoutPreparation(selectedType))
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
    WorkoutShellCard(
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
                enabled = preparation.workoutType == selectedType && preparation.canStartCountdown,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "GPS não usado em ${workoutType.toWorkoutTypeLabel()}. Nenhuma permissão de localização será solicitada."
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
    val isPaused by exercisesViewModel.isPaused.collectAsState()

    LaunchedEffect(sessionId) {
        exercisesViewModel.onActiveWorkoutScreenShown(sessionId)
    }

    val sessionState = session?.state

    LaunchedEffect(sessionState) {
        if (sessionState == AppWorkoutSessionState.FINISHING) {
            onNavigateBack()
        }
    }

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

            Spacer(modifier = Modifier.height(48.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pause/Resume button
                if (isPaused) {
                    FilledTonalButton(
                        onClick = { exercisesViewModel.resumeSession() },
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
                    onClick = { exercisesViewModel.finishSession() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Finalizar")
                    Spacer(Modifier.width(8.dp))
                    Text("Finalizar")
                }
            }
        }
    }
}

@Composable
fun WorkoutRecoveryShell(
    sessionId: String?,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateBack: () -> Unit,
    exercisesViewModel: ExercisesViewModel,
) {
    val recoverableSession by exercisesViewModel.recoverableSession.collectAsState()
    val session = recoverableSession

    LaunchedEffect(sessionId) {
        exercisesViewModel.onRecoveryScreenShown(sessionId)
    }

    WorkoutShellCard(
        title = "Treino interrompido",
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
                // Session info
                Text(
                    text = "Seu treino foi interrompido.",
                    style = MaterialTheme.typography.titleLarge,
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

                // Action buttons
                Button(
                    onClick = {
                        exercisesViewModel.recoverSession()
                        onNavigateTo(AppDestination.ActiveWorkout(session.id.value.toString()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Retomar treino")
                }

                OutlinedButton(
                    onClick = {
                        exercisesViewModel.finishRecoveredSession()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Finalizar treino")
                }

                TextButton(
                    onClick = {
                        exercisesViewModel.discardRecoveredSession()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Descartar treino",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
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
