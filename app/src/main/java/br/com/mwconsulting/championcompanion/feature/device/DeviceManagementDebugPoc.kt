package br.com.mwconsulting.championcompanion.feature.device

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleReadiness
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleState
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionCapabilitySnapshot
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionConnectionManager
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionDisconnectReason
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionRuntimePermission
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionStoredDiscoveryCapabilityResolver
import br.com.mwconsulting.championcompanion.core.bluetooth.toSafeMessage
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidChampionBleClient
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidChampionBleScanner
import br.com.mwconsulting.championcompanion.core.bluetooth.android.AndroidGattDiscoveryMapper
import br.com.mwconsulting.championcompanion.core.bluetooth.android.DefaultAndroidBleEnvironment
import br.com.mwconsulting.championcompanion.core.bluetooth.android.DefaultAndroidGattFacade
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceManagementDebugPocScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val environment = remember(appContext) { DefaultAndroidBleEnvironment(appContext) }
    val coordinator = remember(appContext, environment) {
        val discoveryMapper = AndroidGattDiscoveryMapper()
        val gattFacade = DefaultAndroidGattFacade(appContext, discoveryMapper)
        val client = AndroidChampionBleClient(gattFacade)
        val scanner = AndroidChampionBleScanner(appContext, environment)
        val resolver = ChampionStoredDiscoveryCapabilityResolver(client::discoverySnapshot)
        val manager = ChampionConnectionManager(
            scanner = scanner,
            client = client,
            capabilityResolver = resolver,
            parentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
        DevicePocCoordinator(manager, Dispatchers.IO)
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    val state by coordinator.state.collectAsState()
    val scanReadiness = remember(refreshKey) { ChampionBlePrerequisites.forScan(environment.snapshot()) }
    val connectReadiness = remember(refreshKey) { ChampionBlePrerequisites.forConnect(environment.snapshot()) }
    val missingRuntimePermissions = remember(scanReadiness, connectReadiness) {
        (scanReadiness.missing + connectReadiness.missing)
            .flatMap { it.permissions }
            .toSet()
            .toRuntimePermissionNames()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshKey++
    }
    val now by produceState(initialValue = Instant.now(), state.heartRateStartedAt) {
        while (true) {
            value = Instant.now()
            delay(1_000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(scanReadiness.isReady, connectReadiness.isReady, state.pocState) {
        if (state.pocState in setOf(
                DevicePocState.Idle,
                DevicePocState.PermissionsRequired,
                DevicePocState.ReadyToScan,
            )
        ) {
            if (scanReadiness.isReady && connectReadiness.isReady) {
                coordinator.markReadyToScan()
            } else {
                coordinator.markPermissionsRequired()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Voltar ao gerenciamento")
        }
        Text(
            text = "PoC BLE tecnica",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Validacao manual de permissoes, scan, conexao, A1, FEA1, D0/F002 e selecao final. Sem automacao de ciclo e sem dados sensiveis.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (missingRuntimePermissions.isNotEmpty()) {
            InfoCard(
                title = "Permissoes necessarias",
                message = "Conceda permissoes BLE somente apos acao explicita para liberar scan e conexao tecnica.",
                actionLabel = "Conceder permissoes",
                onAction = { permissionLauncher.launch(missingRuntimePermissions.toTypedArray()) },
            )
        }

        SectionCard(title = "Acoes") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(missingRuntimePermissions.toTypedArray()) },
                    enabled = missingRuntimePermissions.isNotEmpty(),
                ) {
                    Text("Permissoes")
                }
                OutlinedButton(
                    onClick = { refreshKey++ },
                ) {
                    Text("Atualizar")
                }
                Button(
                    onClick = {
                        scope.launch {
                            if (scanReadiness.isReady) {
                                coordinator.markReadyToScan()
                                coordinator.scan(
                                    br.com.mwconsulting.championcompanion.core.bluetooth.ChampionScanRequest(5.seconds),
                                )
                            } else {
                                coordinator.markPermissionsRequired()
                            }
                        }
                    },
                    enabled = scanReadiness.isReady && state.canStartScan(),
                ) {
                    Text("Iniciar scan")
                }
                Button(
                    onClick = {
                        val candidate = state.candidate ?: return@Button
                        scope.launch { coordinator.connectAndBootstrap(candidate) }
                    },
                    enabled = connectReadiness.isReady && state.candidate != null && state.canConnect(),
                ) {
                    Text("Conectar")
                }
                Button(
                    onClick = { scope.launch { coordinator.executeHandshake() } },
                    enabled = state.pocState == DevicePocState.TransportReady,
                ) {
                    Text("Executar handshake")
                }
                Button(
                    onClick = { scope.launch { coordinator.awaitSteps() } },
                    enabled = state.pocState in setOf(
                        DevicePocState.HandshakeConfirmed,
                        DevicePocState.WaitingForSteps,
                        DevicePocState.StepsObserved,
                    ),
                ) {
                    Text("Aguardar passos")
                }
                Button(
                    onClick = { scope.launch { coordinator.startHeartRateSession() } },
                    enabled = state.pocState in setOf(
                        DevicePocState.HandshakeConfirmed,
                        DevicePocState.WaitingForSteps,
                        DevicePocState.StepsObserved,
                        DevicePocState.CycleCompleted,
                    ),
                ) {
                    Text("Iniciar BPM")
                }
                Button(
                    onClick = { scope.launch { coordinator.stopHeartRateSession() } },
                    enabled = state.pocState in setOf(
                        DevicePocState.StartingHeartRate,
                        DevicePocState.MeasuringHeartRate,
                        DevicePocState.StoppingHeartRate,
                    ),
                ) {
                    Text("Parar BPM")
                }
                OutlinedButton(
                    onClick = { scope.launch { coordinator.disconnect(ChampionDisconnectReason.USER_REQUESTED) } },
                    enabled = state.transportState != ChampionBleState.Disconnected || state.activeSessionId != null,
                ) {
                    Text("Desconectar")
                }
                OutlinedButton(
                    onClick = { scope.launch { coordinator.resetCycle() } },
                ) {
                    Text("Reiniciar ciclo")
                }
                OutlinedButton(
                    onClick = {
                        val report = DevicePocCycleReport.fromState(state, Instant.now())
                        val exportedReport = DevicePocReportExporter.export(context, report)
                        val shareIntent = DevicePocReportExporter.createShareIntent(exportedReport).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Relatorio sanitizado da PoC BLE")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatorio sanitizado"))
                    },
                    enabled = state.pocState == DevicePocState.CycleCompleted || state.pocState is DevicePocState.Failed,
                ) {
                    Text("Exportar relatorio")
                }
            }
        }

        SectionCard(title = "Estado do ambiente") {
            PermissionStatusRow("Bluetooth LE suportado", if (scanReadiness.prerequisiteSnapshot.isBleSupported) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Bluetooth habilitado", if (scanReadiness.prerequisiteSnapshot.isBluetoothEnabled) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Scan pronto", if (scanReadiness.isReady) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Conexao pronta", if (connectReadiness.isReady) "Sim" else "Nao")
        }

        SectionCard(title = "Permissoes observadas") {
            val snapshot = environment.snapshot().permissions
            PermissionStatusRow("BLUETOOTH_SCAN", snapshot.bluetoothScanGranted.formatGranted())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("BLUETOOTH_CONNECT", snapshot.bluetoothConnectGranted.formatGranted())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("ACCESS_FINE_LOCATION", snapshot.fineLocationGranted.formatLegacyPermission(snapshot.sdkInt))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("BLUETOOTH (legado)", snapshot.bluetoothGranted.formatLegacyPermission(snapshot.sdkInt))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("BLUETOOTH_ADMIN (legado)", snapshot.bluetoothAdminGranted.formatLegacyPermission(snapshot.sdkInt))
        }

        SectionCard(title = "Bloqueios atuais") {
            BlockingReasons(title = "Scan", readiness = scanReadiness)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            BlockingReasons(title = "Conexao", readiness = connectReadiness)
        }

        SectionCard(title = "Ciclo tecnico") {
            PermissionStatusRow("Estado PoC", state.pocState.label())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Estado transporte", state.transportState.label())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Sessao ativa", if (state.activeSessionId != null) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Handshake confirmado", if (state.pocState !is DevicePocState.Failed && state.pocState.isHandshakeConfirmedOrLater()) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Modo do handshake", state.handshakeOutcome().label())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Passos observados", if (state.hasObservedSteps) "Sim" else "Nao")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Capability FEA1", capabilityState(state.isStepsCapabilityAvailable, state.isStepsCapabilityMissing))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Capability F002", capabilityState(state.isHeartRateCapabilityAvailable, state.isHeartRateCapabilityMissing))
        }

        SectionCard(title = "Dispositivo candidato") {
            PermissionStatusRow("Nome", state.candidate?.name ?: "Nenhum")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Nome local", state.candidate?.localName ?: "Nenhum")
        }

        SectionCard(title = "Capabilities disponiveis") {
            val capabilities = state.transportState.capabilitiesOrNull()
            if (capabilities == null) {
                Text("Ainda indisponiveis.", style = MaterialTheme.typography.bodyMedium)
            } else {
                CapabilityChips(capabilities)
            }
        }

        SectionCard(title = "Passos FEA1") {
            PermissionStatusRow("Pacotes", state.stepsPacketCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Ultimo valor tecnico", state.lastStepCount?.toString() ?: "Indisponivel")
        }

        SectionCard(title = "Sessao D0/F002") {
            PermissionStatusRow("Duracao atual", state.currentDurationLabel(now))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Pacotes D0", state.d0PacketCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Ultimo BPM D0", state.lastD0Bpm?.toString() ?: "Indisponivel")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Pacotes F002", state.f002PacketCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Amostras F002 em buffer", state.f002BufferedSampleCount.toString())
        }

        SectionCard(title = "Estimativa e valor final") {
            PermissionStatusRow("BPM estimado F002", state.estimatedF002Bpm?.toString() ?: "Indisponivel")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Qualidade F002", state.estimatedF002Quality?.label() ?: "Indisponivel")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Score F002", state.estimatedF002QualityScore.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Sample rate", state.estimatedF002SampleRateHz?.let { String.format("%.2f Hz", it) } ?: "Indisponivel")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Picos F002", state.estimatedF002PeakCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Valor final", state.finalHeartRateBpm?.toString() ?: "Indisponivel")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow("Fonte final", state.finalHeartRateSource.label())
        }

        SectionCard(title = "Erro sanitizado") {
            val error = state.lastError
            Text(
                text = error?.toSafeMessage() ?: "Sem erro tecnico atual.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (error != null) {
                Text(
                    text = if (error.isRecoverable) "Recuperavel" else "Nao recuperavel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun BlockingReasons(
    title: String,
    readiness: ChampionBleReadiness,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )
    if (readiness.missing.isEmpty()) {
        Text(
            text = "Sem bloqueios.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        readiness.missing.forEach { missing ->
            Text(
                text = "* ${missing.error.toSafeMessage()}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CapabilityChips(capabilities: ChampionCapabilitySnapshot) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        capabilities.availableChannels.toList().sortedBy { it.name }.forEach { channel ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(channel.name) },
            )
        }
    }
}

private fun Set<ChampionRuntimePermission>.toRuntimePermissionNames(): List<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> buildList<String> {
        if (this@toRuntimePermissionNames.contains(ChampionRuntimePermission.BLUETOOTH_SCAN)) add(Manifest.permission.BLUETOOTH_SCAN)
        if (this@toRuntimePermissionNames.contains(ChampionRuntimePermission.BLUETOOTH_CONNECT)) add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    else -> buildList<String> {
        if (this@toRuntimePermissionNames.contains(ChampionRuntimePermission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}.distinct()

private fun Boolean.formatGranted(): String = if (this) "Concedida" else "Pendente"

private fun Boolean.formatLegacyPermission(sdkInt: Int): String =
    if (sdkInt >= Build.VERSION_CODES.S) {
        "Nao aplicavel"
    } else {
        formatGranted()
    }

private fun DeviceUiState.canStartScan(): Boolean = pocState in setOf(
    DevicePocState.ReadyToScan,
    DevicePocState.DeviceFound,
    DevicePocState.Idle,
    DevicePocState.PermissionsRequired,
)

private fun DeviceUiState.canConnect(): Boolean = pocState in setOf(
    DevicePocState.DeviceFound,
    DevicePocState.ReadyToScan,
)

private fun DeviceUiState.currentDurationLabel(now: Instant): String {
    val startedAt = heartRateStartedAt ?: return "Indisponivel"
    val duration = Duration.between(startedAt, now).coerceAtLeast(Duration.ZERO)
    val totalSeconds = duration.seconds
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun DevicePocState.label(): String = when (this) {
    DevicePocState.Idle -> "Idle"
    DevicePocState.PermissionsRequired -> "Permissoes necessarias"
    DevicePocState.ReadyToScan -> "Pronto para scan"
    DevicePocState.Scanning -> "Scanning"
    DevicePocState.DeviceFound -> "Dispositivo encontrado"
    DevicePocState.Connecting -> "Conectando"
    DevicePocState.TransportReady -> "TransportReady"
    DevicePocState.Handshaking -> "Handshake em andamento"
    DevicePocState.HandshakeConfirmed -> "Handshake confirmado"
    DevicePocState.WaitingForSteps -> "Aguardando passos"
    DevicePocState.StepsObserved -> "Passos observados"
    DevicePocState.StartingHeartRate -> "Iniciando BPM"
    DevicePocState.MeasuringHeartRate -> "Medindo BPM"
    DevicePocState.StoppingHeartRate -> "Parando BPM"
    DevicePocState.CycleCompleted -> "Ciclo concluido"
    DevicePocState.Disconnecting -> "Desconectando"
    is DevicePocState.Failed -> "Falha"
}

private fun ChampionBleState.label(): String = when (this) {
    ChampionBleState.Disconnected -> "Disconnected"
    is ChampionBleState.Scanning -> "Scanning"
    is ChampionBleState.Connecting -> "Connecting"
    is ChampionBleState.DiscoveringServices -> "DiscoveringServices"
    is ChampionBleState.ResolvingChannels -> "ResolvingChannels"
    is ChampionBleState.ConfiguringSubscriptions -> "ConfiguringSubscriptions"
    is ChampionBleState.TransportReady -> "TransportReady"
    is ChampionBleState.Disconnecting -> "Disconnecting"
    is ChampionBleState.Failed -> "Failed"
}

private fun DevicePocHeartRateSignalQuality.label(): String = when (this) {
    DevicePocHeartRateSignalQuality.GOOD -> "Boa"
    DevicePocHeartRateSignalQuality.MEDIUM -> "Media"
    DevicePocHeartRateSignalQuality.LOW -> "Baixa"
}

private fun DevicePocHeartRateFinalSource.label(): String = when (this) {
    DevicePocHeartRateFinalSource.D0_CURRENT -> "D0 atual"
    DevicePocHeartRateFinalSource.F002_ESTIMATE -> "Estimativa F002"
    DevicePocHeartRateFinalSource.UNAVAILABLE -> "Indisponivel"
}

private fun DevicePocHandshakeOutcome.label(): String = when (this) {
    DevicePocHandshakeOutcome.UNCONFIRMED -> "Nao confirmado"
    DevicePocHandshakeOutcome.CONFIRMED -> "Confirmado por ACK"
    DevicePocHandshakeOutcome.ASSUMED -> "Assumido sem ACK"
}

private fun capabilityState(isAvailable: Boolean, isMissing: Boolean): String = when {
    isAvailable -> "Disponivel"
    isMissing -> "Ausente para esta etapa"
    else -> "Ainda nao observado"
}

private fun ChampionBleState.capabilitiesOrNull(): ChampionCapabilitySnapshot? = when (this) {
    is ChampionBleState.ConfiguringSubscriptions -> capabilities
    is ChampionBleState.TransportReady -> capabilities
    else -> null
}

private fun DeviceUiState.handshakeOutcome(): DevicePocHandshakeOutcome = when {
    pocState.isHandshakeConfirmedOrLater() && isHandshakeAssumed -> DevicePocHandshakeOutcome.ASSUMED
    pocState.isHandshakeConfirmedOrLater() -> DevicePocHandshakeOutcome.CONFIRMED
    else -> DevicePocHandshakeOutcome.UNCONFIRMED
}
