package br.com.mwconsulting.championcompanion.feature.device

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.ConnectionStatus
import br.com.mwconsulting.championcompanion.core.designsystem.ConnectionVisualState
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.core.designsystem.HealthCard
import br.com.mwconsulting.championcompanion.core.designsystem.LoadingState
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
    var diagnosticsVisible by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        deviceManagementViewModel.onIntent(DeviceManagementIntent.RefreshPrerequisites)
        deviceManagementViewModel.onIntent(DeviceManagementIntent.Load)
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            deviceManagementViewModel.onIntent(DeviceManagementIntent.ExitVisibleScreen)
        }
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
                    diagnosticsVisible = true
                }

                DeviceManagementEvent.ShareDiagnostics -> {
                    runCatching {
                        val report = buildDeviceDiagnosticsReport(context, state)
                        val exportedReport = DeviceDiagnosticsFeatureReportExporter.export(context, report)
                        val shareIntent = DeviceDiagnosticsFeatureReportExporter
                            .createShareIntent(exportedReport)
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório"))
                    }.onFailure {
                        diagnosticsNotice = "Nao foi possivel exportar relatorio sanitizado."
                    }
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
        onOpenDiagnostics = { diagnosticsVisible = true },
        onExportDiagnostics = {
            runCatching {
                val report = buildDeviceDiagnosticsReport(context, state)
                val exportedReport = DeviceDiagnosticsFeatureReportExporter.export(context, report)
                val shareIntent = DeviceDiagnosticsFeatureReportExporter.createShareIntent(exportedReport)
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório"))
            }.onFailure {
                diagnosticsNotice = "Nao foi possivel exportar relatorio sanitizado."
            }
        },
        onOpenDebugPoc = { debugPocVisible = true },
    )

    if (showForgetConfirmation) {
        AlertDialog(
            onDismissRequest = { showForgetConfirmation = false },
            title = { Text("Esquecer dispositivo?") },
            text = { Text("A associação atual será removida. Histórico e dados já salvos permanecem intactos.") },
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
            text = { Text("A conexão bem-sucedida vai trocar a associação principal pelo dispositivo selecionado.") },
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

    if (diagnosticsVisible) {
        DeviceDiagnosticsDialog(
            report = buildDeviceDiagnosticsReport(context, state),
            onDismiss = { diagnosticsVisible = false },
        )
    }

    diagnosticsNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { diagnosticsNotice = null },
            title = { Text("Ação reservada") },
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
private fun DeviceDiagnosticsDialog(
    report: DeviceDiagnosticsReport,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diagnóstico seguro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
            ) {
                DeviceInfoRow(label = "Capturado em", value = report.capturedAt)
                DeviceInfoRow(label = "Versão do app", value = report.appVersion)
                DeviceInfoRow(label = "Android", value = "API ${report.androidApiLevel} (${report.androidRelease})")
                DeviceInfoRow(label = "Aparelho", value = report.handsetModel)
                DeviceInfoRow(label = "BLE suportado", value = report.bleSupported.yesNo())
                DeviceInfoRow(label = "Bluetooth ativo", value = report.bluetoothEnabled.yesNo())
                DeviceInfoRow(label = "Tela", value = report.screenState)
                DeviceInfoRow(label = "Conexão", value = report.connectionState)
                DeviceInfoRow(label = "Operação atual", value = report.activeOperation ?: "nenhuma")
                DeviceInfoRow(label = "Retry", value = report.retryAction ?: "nenhum")
                DeviceInfoRow(label = "Permissões concedidas", value = report.grantedPermissions.joinForUi())
                DeviceInfoRow(label = "Permissões ausentes", value = report.missingPermissions.joinForUi())
                DeviceInfoRow(label = "Candidatos visíveis", value = report.candidateCount.toString())
                DeviceInfoRow(label = "Candidato selecionado", value = report.selectedCandidatePresent.yesNo())
                report.associatedDevice?.let { device ->
                    DeviceInfoRow(label = "Modelo associado", value = device.modelName)
                    DeviceInfoRow(label = "Identificador seguro", value = device.safeIdentifier)
                    DeviceInfoRow(label = "Associado em", value = device.associatedAt)
                    DeviceInfoRow(label = "Última conexão", value = device.lastSuccessfulConnectionAt ?: "nunca")
                    DeviceInfoRow(label = "Última sincronização", value = device.lastImportedAt ?: "ainda não disponível")
                }
                DeviceInfoRow(label = "Categoria do último erro", value = report.lastErrorCategory ?: "nenhuma")
                DeviceInfoRow(label = "Erro recuperável", value = report.lastErrorRecoverable.yesNo())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
    )
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
                    text = "Gerencie busca, conexão, associação e informações seguras do Champion C001.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HealthCard(
                title = "Resumo da conexão",
                status = {
                    ConnectionStatus(
                        state = state.connectionState.toVisualState(),
                    )
                },
            ) {
                Text(
                    text = state.screenState.label(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.activeOperation?.let {
                    Text(
                        text = "Operação atual: ${it.label()}",
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

                DeviceManagementScreenState.PermissionsRequired -> DeviceFeedbackCard(
                    title = "Permissões BLE necessárias",
                    message = "Conceda as permissões para iniciar uma busca ou conectar o dispositivo.",
                    actionLabel = "Conceder permissões",
                    onAction = onRequestPermissions,
                )

                DeviceManagementScreenState.BluetoothDisabled -> DeviceFeedbackCard(
                    title = "Bluetooth desligado",
                    message = "Ative o Bluetooth para buscar ou conectar o Champion C001.",
                    actionLabel = "Abrir Bluetooth",
                    onAction = onOpenBluetoothSettings,
                )

                else -> Unit
            }

            state.error?.let { error ->
                if (state.shouldShowErrorCard()) {
                    DeviceFeedbackCard(
                        title = error.category.title(),
                        message = error.message,
                        actionLabel = if (error.isRecoverable) "Tentar novamente" else "Fechar",
                        onAction = if (error.isRecoverable) onRetry else onDismissError,
                        isError = true,
                    )
                }
            }

            if (associatedDevice != null) {
                AssociatedDeviceSection(
                    device = associatedDevice,
                    connectionState = state.connectionState,
                )
            } else if (state.candidates.isEmpty() && state.screenState != DeviceManagementScreenState.Scanning) {
                EmptyState(
                    title = "Nenhum dispositivo associado",
                    message = "Busque seu Champion C001 para conectar manualmente e manter uma associação segura.",
                )
            }

            if (
                state.connectionState != DeviceManagementConnectionState.CONNECTED &&
                (state.candidates.isNotEmpty() || state.screenState == DeviceManagementScreenState.Scanning)
            ) {
                ScanResultsSection(
                    candidates = state.candidates,
                    isScanning = state.screenState == DeviceManagementScreenState.Scanning,
                    isBusy = state.activeOperation != null && state.activeOperation != DeviceManagementActiveOperation.SCANNING,
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
                HealthCard(title = "PoC técnica de debug") {
                    Text(
                        text = "O fluxo técnico permanece separado do gerenciamento principal e fica disponível apenas em builds de debug.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ChampionSpacing.medium),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        OutlinedButton(
                            onClick = onOpenDebugPoc,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Abrir PoC técnica")
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
        Column(
            modifier = Modifier.padding(top = ChampionSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            DeviceInfoRow(
                label = "Última conexão",
                value = device.lastSuccessfulConnectionAt.formatTimestamp("Nunca conectado"),
            )
            DeviceInfoRow(
                label = "Última sincronização",
                value = device.lastImportedAt.formatTimestamp("Ainda não disponível"),
            )
            DeviceInfoRow(
                label = "Associado em",
                value = device.associatedAt.formatTimestamp("Não disponível"),
            )
        }
    }
}

@Composable
private fun ScanResultsSection(
    candidates: List<DeviceManagementScanCandidate>,
    isScanning: Boolean,
    isBusy: Boolean,
    selectedCandidate: DeviceManagementScanCandidate?,
    onSelectCandidate: (DeviceManagementCandidateId) -> Unit,
    onCancelScan: () -> Unit,
    onConnectSelectedCandidate: () -> Unit,
) {
    HealthCard(title = if (isScanning) "Buscando dispositivos" else "Dispositivos encontrados") {
        if (isScanning) {
            LoadingState(
                label = "Busca em andamento",
                message = "Os resultados aparecem durante esta janela única de busca.",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancelScan) {
                    Text("Cancelar busca")
                }
            }
        }

        if (candidates.isEmpty() && !isScanning) {
            EmptyState(
                title = "Nenhum Champion encontrado",
                message = "Inicie uma nova busca para procurar um dispositivo compatível por perto.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                candidates.forEach { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        enabled = !isBusy,
                        onClick = { onSelectCandidate(candidate.candidateId) },
                    )
                }
                selectedCandidate?.let {
                    PrimaryActionButton(
                        label = "Conectar dispositivo selecionado",
                        onClick = onConnectSelectedCandidate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning && !isBusy,
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: DeviceManagementScanCandidate,
    enabled: Boolean,
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
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
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
        Text(
            text = if (candidate.isSelected) "Pronto para conectar" else "Toque para selecionar este dispositivo",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
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

    HealthCard(title = "Ações") {
        Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium)) {
            PrimaryActionButton(
                label = if (hasAssociatedDevice) "Buscar outro dispositivo" else "Buscar dispositivo",
                onClick = onAddDevice,
                enabled = canStartScan,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.retryAction != null) {
                OutlinedButton(
                    onClick = onRetry,
                    enabled = state.activeOperation == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tentar novamente")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                OutlinedButton(
                    onClick = onConnectAssociatedDevice,
                    enabled = canConnectAssociated,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Conectar")
                }
                OutlinedButton(
                    onClick = onDisconnectDevice,
                    enabled = canDisconnect,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Desconectar")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            ) {
                OutlinedButton(
                    onClick = onForgetDevice,
                    enabled = hasAssociatedDevice && state.activeOperation == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Esquecer")
                }
                OutlinedButton(
                    onClick = onOpenDiagnostics,
                    enabled = hasAssociatedDevice && state.activeOperation == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Diagnóstico")
                }
            }

            OutlinedButton(
                onClick = onExportDiagnostics,
                enabled = hasAssociatedDevice && state.activeOperation == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exportar relatório")
            }
        }

        if (hasSelectedCandidate && !hasAssociatedDevice) {
            Text(
                text = "Selecione um dispositivo encontrado e conecte apenas quando confirmar que ele é o seu Champion.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeviceFeedbackCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    isError: Boolean = false,
) {
    val colors = if (isError) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
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
    DeviceManagementScreenState.PermissionsRequired -> "Permissões necessárias"
    DeviceManagementScreenState.BluetoothDisabled -> "Bluetooth desligado"
    DeviceManagementScreenState.ReadyToScan -> "Pronto para buscar"
    DeviceManagementScreenState.Scanning -> "Procurando dispositivos"
    DeviceManagementScreenState.ScanResults -> "Dispositivos encontrados"
    DeviceManagementScreenState.Connecting -> "Conectando"
    DeviceManagementScreenState.PreparingConnection -> "Preparando conexão"
    DeviceManagementScreenState.Connected -> "Conectado"
    DeviceManagementScreenState.Disconnecting -> "Desconectando"
    DeviceManagementScreenState.Disconnected -> "Desconectado"
    DeviceManagementScreenState.DeviceNotFound -> "Dispositivo não encontrado"
    DeviceManagementScreenState.ConnectionFailed -> "Falha de conexão"
    DeviceManagementScreenState.Forgetting -> "Esquecendo dispositivo"
    DeviceManagementScreenState.Error -> "Erro"
}

private fun DeviceManagementActiveOperation.label(): String = when (this) {
    DeviceManagementActiveOperation.REQUESTING_PERMISSIONS -> "Solicitando permissões"
    DeviceManagementActiveOperation.SCANNING -> "Executando scan"
    DeviceManagementActiveOperation.CONNECTING -> "Conectando dispositivo"
    DeviceManagementActiveOperation.DISCONNECTING -> "Desconectando dispositivo"
    DeviceManagementActiveOperation.FORGETTING -> "Removendo associação"
    DeviceManagementActiveOperation.OPENING_DIAGNOSTICS -> "Abrindo diagnóstico"
    DeviceManagementActiveOperation.EXPORTING_DIAGNOSTICS -> "Exportando diagnóstico"
}

private fun DeviceManagementErrorCategory.title(): String = when (this) {
    DeviceManagementErrorCategory.PREREQUISITES -> "Ajuste necessário"
    DeviceManagementErrorCategory.SCAN -> "Falha no scan"
    DeviceManagementErrorCategory.CONNECTION -> "Falha na conexão"
    DeviceManagementErrorCategory.HANDSHAKE -> "Falha na inicialização"
    DeviceManagementErrorCategory.DISCONNECT -> "Falha ao desconectar"
    DeviceManagementErrorCategory.FORGET -> "Falha ao esquecer dispositivo"
    DeviceManagementErrorCategory.DIAGNOSTICS -> "Falha em diagnóstico"
    DeviceManagementErrorCategory.INTERNAL -> "Falha interna"
}

private fun DeviceManagementUiState.shouldShowErrorCard(): Boolean {
    val currentError = error ?: return false
    return when (screenState) {
        DeviceManagementScreenState.PermissionsRequired,
        DeviceManagementScreenState.BluetoothDisabled,
        -> currentError.category != DeviceManagementErrorCategory.PREREQUISITES

        else -> true
    }
}

private fun buildDeviceDiagnosticsReport(
    context: android.content.Context,
    state: DeviceManagementUiState,
): DeviceDiagnosticsReport = DeviceDiagnosticsReport.from(
    state = state,
    environment = context.captureDeviceDiagnosticsEnvironment(),
)

private fun android.content.Context.captureDeviceDiagnosticsEnvironment(): DeviceDiagnosticsEnvironmentSnapshot {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val allPermissions = linkedMapOf(
        "BLUETOOTH_SCAN" to Manifest.permission.BLUETOOTH_SCAN,
        "BLUETOOTH_CONNECT" to Manifest.permission.BLUETOOTH_CONNECT,
        "ACCESS_FINE_LOCATION" to Manifest.permission.ACCESS_FINE_LOCATION,
    )
    val granted = allPermissions.filterValues { hasPermission(it) }.keys.toList()
    val missing = allPermissions.filterValues { !hasPermission(it) }.keys.toList()
    val bluetoothEnabled = runCatching {
        val manager = getSystemService(BluetoothManager::class.java)
        manager?.adapter?.isEnabled == true
    }.getOrDefault(false)
    return DeviceDiagnosticsEnvironmentSnapshot(
        appVersion = packageInfo.versionName ?: "unknown",
        androidApiLevel = Build.VERSION.SDK_INT,
        androidRelease = Build.VERSION.RELEASE ?: "unknown",
        handsetModel = listOfNotNull(Build.MANUFACTURER, Build.MODEL).joinToString(" ").ifBlank { "unknown" },
        bleSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
        bluetoothEnabled = bluetoothEnabled,
        grantedPermissions = granted,
        missingPermissions = missing,
    )
}

private fun android.content.Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Boolean.yesNo(): String = if (this) "Sim" else "Nao"

private fun List<String>.joinForUi(): String = if (isEmpty()) "nenhuma" else joinToString()

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
