package br.com.mwconsulting.championcompanion.feature.device

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBlePrerequisites
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleReadiness
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionBleError
import br.com.mwconsulting.championcompanion.core.bluetooth.ChampionRuntimePermission
import br.com.mwconsulting.championcompanion.core.bluetooth.android.DefaultAndroidBleEnvironment

@Composable
fun DeviceScreen() {
    val context = LocalContext.current
    if (!context.isDebugBuild()) {
        DeviceReleasePlaceholder()
        return
    }

    DeviceDebugEntrypoint()
}

@Composable
private fun DeviceReleasePlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Dispositivo",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Fluxo técnico BLE disponível apenas na variante de depuração nesta fase.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DeviceDebugEntrypoint() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val environment = remember(context) { DefaultAndroidBleEnvironment(context.applicationContext) }
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Entrada técnica BLE",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Preparação da PoC física. Sem scan automático, sem conexão automática e sem fluxo funcional de A1/D0 nesta task.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (missingRuntimePermissions.isNotEmpty()) {
            InfoCard(
                title = "Permissões necessárias",
                message = "Conceda permissões BLE somente após ação explícita para liberar scan e conexão técnica.",
                actionLabel = "Conceder permissões",
                onAction = { permissionLauncher.launch(missingRuntimePermissions.toTypedArray()) },
            )
        }

        SectionCard(title = "Estado do ambiente") {
            PermissionStatusRow(
                label = "Bluetooth LE suportado",
                value = if (scanReadiness.prerequisiteSnapshot.isBleSupported) "Sim" else "Não",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "Bluetooth habilitado",
                value = if (scanReadiness.prerequisiteSnapshot.isBluetoothEnabled) "Sim" else "Não",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "Scan pronto",
                value = if (scanReadiness.isReady) "Sim" else "Não",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "Conexão pronta",
                value = if (connectReadiness.isReady) "Sim" else "Não",
            )
        }

        SectionCard(title = "Permissões observadas") {
            val snapshot = environment.snapshot().permissions
            PermissionStatusRow(
                label = "BLUETOOTH_SCAN",
                value = snapshot.bluetoothScanGranted.formatGranted(),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "BLUETOOTH_CONNECT",
                value = snapshot.bluetoothConnectGranted.formatGranted(),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "ACCESS_FINE_LOCATION",
                value = snapshot.fineLocationGranted.formatLegacyPermission(snapshot.sdkInt),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "BLUETOOTH (legado)",
                value = snapshot.bluetoothGranted.formatLegacyPermission(snapshot.sdkInt),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PermissionStatusRow(
                label = "BLUETOOTH_ADMIN (legado)",
                value = snapshot.bluetoothAdminGranted.formatLegacyPermission(snapshot.sdkInt),
            )
        }

        SectionCard(title = "Bloqueios atuais") {
            BlockingReasons(
                title = "Scan",
                readiness = scanReadiness,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            BlockingReasons(
                title = "Conexão",
                readiness = connectReadiness,
            )
        }

        SectionCard(title = "Entrypoint da PoC") {
            Text(
                text = "Tela técnica completa será implementada na task 005-09 sobre este entrypoint. Nesta task, o objetivo é preparar permissões e um ponto explícito de entrada debug.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { permissionLauncher.launch(missingRuntimePermissions.toTypedArray()) },
                enabled = missingRuntimePermissions.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(if (missingRuntimePermissions.isEmpty()) "Ambiente preparado" else "Solicitar permissões BLE")
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
                text = "• ${missing.error.safePermissionMessage()}",
                style = MaterialTheme.typography.bodyMedium,
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

private fun android.content.Context.isDebugBuild(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

private fun Boolean.formatGranted(): String = if (this) "Concedida" else "Pendente"

private fun Boolean.formatLegacyPermission(sdkInt: Int): String =
    if (sdkInt >= Build.VERSION_CODES.S) {
        "Não aplicável"
    } else {
        formatGranted()
    }

private fun ChampionBleError.safePermissionMessage(): String = when (this) {
    ChampionBleError.BleNotSupported -> "Bluetooth BLE não suportado neste dispositivo."
    ChampionBleError.BluetoothDisabled -> "Ative o Bluetooth para continuar."
    ChampionBleError.ScanPermissionMissing -> "Permissão de busca BLE pendente."
    ChampionBleError.ConnectPermissionMissing -> "Permissão de conexão BLE pendente."
    ChampionBleError.LegacyPermissionMissing -> "Permissões legadas de Bluetooth ainda não estão disponíveis."
    ChampionBleError.LocationPermissionRequired -> "Permissão de localização pendente para dispositivos Android 11 ou inferiores."
    else -> "Pré-requisito BLE ainda não atendido."
}
