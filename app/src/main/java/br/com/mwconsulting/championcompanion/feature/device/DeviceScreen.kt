package br.com.mwconsulting.championcompanion.feature.device

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.ConnectionStatus
import br.com.mwconsulting.championcompanion.core.designsystem.ConnectionVisualState
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.ErrorState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
import br.com.mwconsulting.championcompanion.core.designsystem.MetricCard
import br.com.mwconsulting.championcompanion.core.designsystem.PermissionBanner
import br.com.mwconsulting.championcompanion.core.designsystem.PrimaryActionButton
import br.com.mwconsulting.championcompanion.core.designsystem.SyncStatusBadge
import br.com.mwconsulting.championcompanion.core.designsystem.SyncVisualState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DeviceScreen(
    deviceManagementViewModel: DeviceManagementViewModel = run {
        val activity = LocalContext.current as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[DeviceManagementViewModel::class.java]
        }
    },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by deviceManagementViewModel.state.collectAsState()
    var showForgetConfirmation by remember { mutableStateOf(false) }
    var showReplacementConfirmation by remember { mutableStateOf(false) }
    var debugPocVisible by remember { mutableStateOf(false) }
    var diagnosticsNotice by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        deviceManagementViewModel.onIntent(DeviceManagementIntent.RefreshPrerequisites)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> deviceManagementViewModel.onIntent(DeviceManagementIntent.Load)
                Lifecycle.Event.ON_RESUME -> deviceManagementViewModel.onIntent(DeviceManagementIntent.RefreshPrerequisites)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(deviceManagementViewModel) {
        deviceManagementViewModel.events.collectLatest { event ->
            when (event) {
                is DeviceManagementEvent.RequestPermissions -> {
                    val permissions = event.permissions.toRuntimePermissionNames()
                    if (permissions.isNotEmpty()) {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }

                DeviceManagementEvent.OpenBluetoothSettings -> {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }

                DeviceManagementEvent.ShowForgetConfirmation -> {
                    showForgetConfirmation = true
                }

                DeviceManagementEvent.OpenDiagnostics -> {
                    diagnosticsNotice = "Diagnostico seguro entra na task 006-10."
                }

                DeviceManagementEvent.ShareDiagnostics -> {
                    diagnosticsNotice = "Exportacao sanitizada entra na task 006-10."
                }
            }
        }
    }

    if (debugPocVisible) {
        DeviceManagementDebugPocScreen(
            onBack = { debugPocVisible = false },
        )
        return
    }

    DeviceManagementScreen(
        state = state,
        isDebugBuild = context.isDebugBuild(),
        onAddDevice = { deviceManagementViewModel.onIntent(DeviceManagementIntent.AddDevice) },
        onRetry = { deviceManagementViewModel.onIntent(DeviceManagementIntent.Retry) },
        onCancelScan = { deviceManagementViewModel.onIntent(DeviceManagementIntent.CancelScan) },
        onSelectCandidate = { deviceManagementViewModel.onIntent(DeviceManagementIntent.SelectCandidate(it)) },
        onConnectSelectedCandidate = {
            if (state.associatedDevice != null) {
                showReplacementConfirmation = true
            } else {
                deviceManagementViewModel.onIntent(DeviceManagementIntent.ConnectSelectedCandidate)
            }
        },
        onConnectAssociatedDevice = { deviceManagementViewModel.onIntent(DeviceManagementIntent.ConnectAssociatedDevice) },
        onDisconnectDevice = { deviceManagementViewModel.onIntent(DeviceManagementIntent.DisconnectDevice) },
        onForgetDevice = { deviceManagementViewModel.onIntent(DeviceManagementIntent.ForgetDevice) },
        onDismissError = { deviceManagementViewModel.onIntent(DeviceManagementIntent.DismissError) },
        onRequestPermissions = { deviceManagementViewModel.onIntent(DeviceManagementIntent.Retry) },
        onOpenBluetoothSettings = { deviceManagementViewModel.onIntent(DeviceManagementIntent.Retry) },
        onOpenDiagnostics = { deviceManagementViewModel.onIntent(DeviceManagementIntent.OpenDiagnostics) },
        onExportDiagnostics = { deviceManagementViewModel.onIntent(DeviceManagementIntent.ExportDiagnostics) },
        onOpenDebugPoc = { debugPocVisible = true },
    )

    if (showForgetConfirmation) {
        AlertDialog(
            onDismissRequest = { showForgetConfirmation = false },
            title = { Text("Esquecer dispositivo?") },
            text = { Text("Associacao atual sera removida. Historico e dados ja salvos permanecem intactos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showForgetConfirmation = false
                        deviceManagementViewModel.onIntent(DeviceManagementIntent.ConfirmForgetDevice)
                    },
                ) {
                    Text("Esquecer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showReplacementConfirmation) {
        AlertDialog(
            onDismissRequest = { showReplacementConfirmation = false },
            title = { Text("Substituir dispositivo associado?") },
            text = { Text("Conexao bem-sucedida vai trocar associacao primaria pelo dispositivo selecionado.") },
            confirmButton = {
                Button(
                    onClick = {
                        showReplacementConfirmation = false
                        deviceManagementViewModel.onIntent(DeviceManagementIntent.ConnectSelectedCandidate)
                    },
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplacementConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    diagnosticsNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { diagnosticsNotice = null },
            title = { Text("Acao reservada") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { diagnosticsNotice = null }) {
                    Text("Fechar")
                }
            },
        )
    }
}

@Composable
private fun DeviceManagementScreen(
    state: DeviceManagementUiState,
    isDebugBuild: Boolean,
    onAddDevice: () -> Unit,
    onRetry: () -> Unit,
    onCancelScan: () -> Unit,
    onSelectCandidate: (DeviceManagementCandidateId) -> Unit,
    onConnectSelectedCandidate: () -> Unit,
    onConnectAssociatedDevice: () -> Unit,
    onDisconnectDevice: () -> Unit,
    onForgetDevice: () -> Unit,
    onDismissError: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onOpenDebugPoc: () -> Unit,
) {
    val associatedDevice = state.associatedDevice
    val selectedCandidate = state.candidates.firstOrNull { it.isSelected }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dispositivo", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Gerencie scan, conexao, associacao e informacoes seguras do Champion C001.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HealthCard(
                title = "Resumo da conexao",
                status = {
                    ConnectionStatus(
                        state = state.connectionState.toVisualState(),
                    )
                },
            ) {
                Text(
                    text = state.screenState.label(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                state.activeOperation?.let {
                    Text(
                        text = "Operacao atual: ${it.label()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            when (state.screenState) {
                DeviceManagementScreenState.Loading -> LoadingState(
                    label = "Carregando dispositivo",
                    message = "Preparando estado seguro da feature.",
                    modifier = Modifier.fillMaxWidth(),
                )

                DeviceManagementScreenState.PermissionsRequired -> PermissionBanner(
                    title = "Permissoes BLE necessarias",
                    message = "Conceda permissoes para iniciar scan ou conectar dispositivo.",
                    actionLabel = "Conceder permissoes",
                    onAction = onRequestPermissions,
                )

                DeviceManagementScreenState.BluetoothDisabled -> PermissionBanner(
                    title = "Bluetooth desligado",
                    message = "Ative Bluetooth para procurar ou conectar Champion C001.",
                    actionLabel = "Abrir Bluetooth",
                    onAction = onOpenBluetoothSettings,
                )

                else -> Unit
            }

            state.error?.let { error ->
                ErrorState(
                    title = error.category.title(),
                    message = error.message,
                    actionLabel = if (error.isRecoverable) "Tentar novamente" else "Fechar",
                    onAction = if (error.isRecoverable) onRetry else onDismissError,
                )
            }

            if (associatedDevice != null) {
                AssociatedDeviceSection(
                    device = associatedDevice,
                    connectionState = state.connectionState,
                )
            } else if (state.candidates.isEmpty() && state.screenState != DeviceManagementScreenState.Scanning) {
                EmptyState(
                    title = "Nenhum dispositivo associado",
                    message = "Adicione seu Champion C001 para conectar manualmente e manter associacao segura.",
                )
            }

            if (state.candidates.isNotEmpty() || state.screenState == DeviceManagementScreenState.Scanning) {
                ScanResultsSection(
                    candidates = state.candidates,
                    isScanning = state.screenState == DeviceManagementScreenState.Scanning,
                    selectedCandidate = selectedCandidate,
                    onSelectCandidate = onSelectCandidate,
                    onCancelScan = onCancelScan,
                    onConnectSelectedCandidate = onConnectSelectedCandidate,
                )
            }

            ActionSection(
                state = state,
                hasAssociatedDevice = associatedDevice != null,
                hasSelectedCandidate = selectedCandidate != null,
                onAddDevice = onAddDevice,
                onRetry = onRetry,
                onConnectAssociatedDevice = onConnectAssociatedDevice,
                onDisconnectDevice = onDisconnectDevice,
                onForgetDevice = onForgetDevice,
                onOpenDiagnostics = onOpenDiagnostics,
                onExportDiagnostics = onExportDiagnostics,
            )

            if (isDebugBuild) {
                HealthCard(title = "PoC tecnica debug") {
                    Text(
                        text = "Fluxo tecnico continua separado do gerenciamento principal e permanece disponivel apenas em debug.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ChampionSpacing.medium),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onOpenDebugPoc) {
                            Text("Abrir PoC tecnica")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociatedDeviceSection(
    device: DeviceManagementAssociatedDevice,
    connectionState: DeviceManagementConnectionState,
) {
    HealthCard(
        title = "Dispositivo associado",
        status = {
            ConnectionStatus(state = connectionState.toVisualState())
        },
    ) {
        Text(device.displayName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = device.modelName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = ChampionSpacing.medium))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = {}, enabled = false, label = { Text(device.safeIdentifier) })
            device.firmware?.let { firmware ->
                AssistChip(onClick = {}, enabled = false, label = { Text("FW $firmware") })
            }
            SyncStatusBadge(state = device.lastImportedAt.toSyncVisualState())
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChampionSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                label = "Ultima conexao",
                value = device.lastSuccessfulConnectionAt.formatTimestamp("Nunca conectado"),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Ultima sincronizacao",
                value = device.lastImportedAt.formatTimestamp("Ainda nao disponivel"),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScanResultsSection(
    candidates: List<DeviceManagementScanCandidate>,
    isScanning: Boolean,
    selectedCandidate: DeviceManagementScanCandidate?,
    onSelectCandidate: (DeviceManagementCandidateId) -> Unit,
    onCancelScan: () -> Unit,
    onConnectSelectedCandidate: () -> Unit,
) {
    HealthCard(title = if (isScanning) "Procurando dispositivos" else "Candidatos encontrados") {
        if (isScanning) {
            LoadingState(
                label = "Scan em andamento",
                message = "Resultados aparecem durante janela unica de busca.",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancelScan) {
                    Text("Cancelar scan")
                }
            }
        }

        if (candidates.isEmpty() && !isScanning) {
            EmptyState(
                title = "Nenhum Champion encontrado",
                message = "Inicie novo scan para procurar dispositivo compativel por perto.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                candidates.forEach { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        onClick = { onSelectCandidate(candidate.candidateId) },
                    )
                }
                selectedCandidate?.let {
                    PrimaryActionButton(
                        label = "Conectar dispositivo selecionado",
                        onClick = onConnectSelectedCandidate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning,
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: DeviceManagementScanCandidate,
    onClick: () -> Unit,
) {
    HealthCard(
        title = candidate.displayName,
        status = {
            if (candidate.isSelected) {
                AssistChip(onClick = {}, enabled = false, label = { Text("Selecionado") })
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(candidate.displayName)
                    append(", ")
                    append(candidate.safeIdentifier)
                    if (candidate.isSelected) append(", selecionado")
                }
            },
    ) {
        Text(candidate.safeIdentifier, style = MaterialTheme.typography.bodyLarge)
        candidate.signalLabel?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ActionSection(
    state: DeviceManagementUiState,
    hasAssociatedDevice: Boolean,
    hasSelectedCandidate: Boolean,
    onAddDevice: () -> Unit,
    onRetry: () -> Unit,
    onConnectAssociatedDevice: () -> Unit,
    onDisconnectDevice: () -> Unit,
    onForgetDevice: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onExportDiagnostics: () -> Unit,
) {
    val canStartScan = state.activeOperation == null &&
        state.screenState !in setOf(
            DeviceManagementScreenState.Scanning,
            DeviceManagementScreenState.Connecting,
            DeviceManagementScreenState.PreparingConnection,
            DeviceManagementScreenState.Disconnecting,
            DeviceManagementScreenState.Forgetting,
        )
    val canConnectAssociated = hasAssociatedDevice &&
        state.activeOperation == null &&
        state.connectionState !in setOf(
            DeviceManagementConnectionState.CONNECTED,
            DeviceManagementConnectionState.CONNECTING,
            DeviceManagementConnectionState.PREPARING_CONNECTION,
            DeviceManagementConnectionState.DISCONNECTING,
        )
    val canDisconnect = hasAssociatedDevice &&
        state.activeOperation == null &&
        state.connectionState in setOf(
            DeviceManagementConnectionState.CONNECTED,
            DeviceManagementConnectionState.CONNECTING,
            DeviceManagementConnectionState.PREPARING_CONNECTION,
            DeviceManagementConnectionState.SEARCHING_REMEMBERED_DEVICE,
        )

    HealthCard(title = "Acoes") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryActionButton(
                label = if (hasAssociatedDevice) "Procurar outro dispositivo" else "Adicionar dispositivo",
                onClick = onAddDevice,
                enabled = canStartScan,
            )
            OutlinedButton(onClick = onRetry, enabled = state.retryAction != null && state.activeOperation == null) {
                Text("Tentar novamente")
            }
            OutlinedButton(onClick = onConnectAssociatedDevice, enabled = canConnectAssociated) {
                Text("Conectar associado")
            }
            OutlinedButton(onClick = onDisconnectDevice, enabled = canDisconnect) {
                Text("Desconectar")
            }
            OutlinedButton(onClick = onForgetDevice, enabled = hasAssociatedDevice && state.activeOperation == null) {
                Text("Esquecer")
            }
            OutlinedButton(onClick = onOpenDiagnostics, enabled = hasAssociatedDevice && state.activeOperation == null) {
                Text("Diagnostico")
            }
            OutlinedButton(onClick = onExportDiagnostics, enabled = hasAssociatedDevice && state.activeOperation == null) {
                Text("Exportar")
            }
        }

        if (hasSelectedCandidate && !hasAssociatedDevice) {
            Text(
                text = "Selecione candidato e use botao de conexao na secao de resultados.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun Set<DeviceManagementPermission>.toRuntimePermissionNames(): List<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> buildList<String> {
        if (this@toRuntimePermissionNames.contains(DeviceManagementPermission.BLUETOOTH_SCAN)) add(Manifest.permission.BLUETOOTH_SCAN)
        if (this@toRuntimePermissionNames.contains(DeviceManagementPermission.BLUETOOTH_CONNECT)) add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    else -> buildList<String> {
        if (this@toRuntimePermissionNames.contains(DeviceManagementPermission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}.distinct()

private fun android.content.Context.isDebugBuild(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

private fun DeviceManagementScreenState.label(): String = when (this) {
    DeviceManagementScreenState.Loading -> "Carregando"
    DeviceManagementScreenState.NoAssociatedDevice -> "Nenhum dispositivo associado"
    DeviceManagementScreenState.PermissionsRequired -> "Permissoes necessarias"
    DeviceManagementScreenState.BluetoothDisabled -> "Bluetooth desligado"
    DeviceManagementScreenState.ReadyToScan -> "Pronto para scan"
    DeviceManagementScreenState.Scanning -> "Procurando dispositivos"
    DeviceManagementScreenState.ScanResults -> "Resultados de scan"
    DeviceManagementScreenState.Connecting -> "Conectando"
    DeviceManagementScreenState.PreparingConnection -> "Preparando conexao"
    DeviceManagementScreenState.Connected -> "Conectado"
    DeviceManagementScreenState.Disconnecting -> "Desconectando"
    DeviceManagementScreenState.Disconnected -> "Desconectado"
    DeviceManagementScreenState.DeviceNotFound -> "Dispositivo nao encontrado"
    DeviceManagementScreenState.ConnectionFailed -> "Falha de conexao"
    DeviceManagementScreenState.Forgetting -> "Esquecendo dispositivo"
    DeviceManagementScreenState.Error -> "Erro"
}

private fun DeviceManagementActiveOperation.label(): String = when (this) {
    DeviceManagementActiveOperation.REQUESTING_PERMISSIONS -> "Solicitando permissoes"
    DeviceManagementActiveOperation.SCANNING -> "Executando scan"
    DeviceManagementActiveOperation.CONNECTING -> "Conectando dispositivo"
    DeviceManagementActiveOperation.DISCONNECTING -> "Desconectando dispositivo"
    DeviceManagementActiveOperation.FORGETTING -> "Removendo associacao"
    DeviceManagementActiveOperation.OPENING_DIAGNOSTICS -> "Abrindo diagnostico"
    DeviceManagementActiveOperation.EXPORTING_DIAGNOSTICS -> "Exportando diagnostico"
}

private fun DeviceManagementErrorCategory.title(): String = when (this) {
    DeviceManagementErrorCategory.PREREQUISITES -> "Ajuste necessario"
    DeviceManagementErrorCategory.SCAN -> "Falha no scan"
    DeviceManagementErrorCategory.CONNECTION -> "Falha na conexao"
    DeviceManagementErrorCategory.HANDSHAKE -> "Falha na preparacao"
    DeviceManagementErrorCategory.DISCONNECT -> "Falha ao desconectar"
    DeviceManagementErrorCategory.FORGET -> "Falha ao esquecer dispositivo"
    DeviceManagementErrorCategory.DIAGNOSTICS -> "Falha em diagnostico"
    DeviceManagementErrorCategory.INTERNAL -> "Falha interna"
}

private fun DeviceManagementConnectionState.toVisualState(): ConnectionVisualState = when (this) {
    DeviceManagementConnectionState.CONNECTED -> ConnectionVisualState.Connected
    else -> ConnectionVisualState.Disconnected
}

private fun Instant?.toSyncVisualState(): SyncVisualState = if (this == null) {
    SyncVisualState.Pending
} else {
    SyncVisualState.Success
}

private fun Instant?.formatTimestamp(fallback: String): String {
    if (this == null) return fallback
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}
