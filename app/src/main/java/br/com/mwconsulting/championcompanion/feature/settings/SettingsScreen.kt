package br.com.mwconsulting.championcompanion.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import androidx.health.connect.client.PermissionController

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        androidx.compose.runtime.remember(activity) {
            ViewModelProvider(activity)[SettingsViewModel::class.java]
        }
    },
) {
    val state by viewModel.state.collectAsState()
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val missingPermissions = viewModel.requestedPermissions()
    val isConnected = state.availability == SettingsAvailability.AVAILABLE && missingPermissions.isEmpty()
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onIntent(SettingsIntent.PermissionsUpdated(granted))
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(SettingsIntent.Load)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(SettingsIntent.RefreshHealthConnectState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.large),
        ) {
            SettingsHeaderCard(state)
            HealthConnectStatusCard(
                state = state,
                isConnected = isConnected,
                onToggle = { viewModel.onIntent(SettingsIntent.ToggleHealthSync(it)) },
                onConnect = {
                    when (state.availability) {
                        SettingsAvailability.AVAILABLE -> if (missingPermissions.isNotEmpty()) {
                            permissionLauncher.launch(missingPermissions)
                        }
                        SettingsAvailability.PROVIDER_UPDATE_REQUIRED -> activity.openIntentSafely(viewModel.createProviderUpdateIntent())
                        SettingsAvailability.UNAVAILABLE -> viewModel.onIntent(SettingsIntent.DismissStatus)
                    }
                },
                onSyncNow = { viewModel.onIntent(SettingsIntent.SyncNow) },
                onManageAccess = { activity.openIntentSafely(viewModel.createManageAccessIntent()) },
                onDismissStatus = { viewModel.onIntent(SettingsIntent.DismissStatus) },
            )
            PermissionGroupsCard(state.permissionGroups)
            SyncQueueCard(state)
        }
    }
}

@Composable
private fun SettingsHeaderCard(state: SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Health Connect", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Controle disponibilidade, permissões e sincronização write-only do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(onClick = {}, enabled = false, label = { Text("Provider: ${state.providerPackageName}") })
        }
    }
}

@Composable
private fun HealthConnectStatusCard(
    state: SettingsUiState,
    isConnected: Boolean,
    onToggle: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onSyncNow: () -> Unit,
    onManageAccess: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
                    Text("Integração", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when (state.availability) {
                            SettingsAvailability.AVAILABLE -> "Health Connect disponível para sincronização foreground."
                            SettingsAvailability.PROVIDER_UPDATE_REQUIRED -> "Atualização do provider necessária antes de sincronizar."
                            SettingsAvailability.UNAVAILABLE -> "Health Connect indisponível neste dispositivo."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.healthSyncEnabled,
                    onCheckedChange = onToggle,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small)) {
                Button(
                    onClick = onConnect,
                    enabled = state.availability != SettingsAvailability.AVAILABLE || !isConnected,
                ) {
                    Text(
                        when {
                            isConnected -> "Conectado"
                            else -> when (state.availability) {
                            SettingsAvailability.AVAILABLE -> "Conectar"
                            SettingsAvailability.PROVIDER_UPDATE_REQUIRED -> "Atualizar provider"
                            SettingsAvailability.UNAVAILABLE -> "Conectar"
                            }
                        },
                    )
                }
                Button(onClick = onSyncNow, enabled = state.healthSyncEnabled && !state.isSyncNowRunning) {
                    Text(if (state.isSyncNowRunning) "Sincronizando" else "Sincronizar agora")
                }
                TextButton(onClick = onManageAccess) {
                    Text("Gerenciar acesso")
                }
            }
            state.syncStatusMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChampionShapeMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ChampionSpacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onDismissStatus) { Text("Fechar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGroupsCard(permissionGroups: List<HealthConnectPermissionGroupUiModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text("Permissões por capacidade", style = MaterialTheme.typography.titleLarge)
            permissionGroups.forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(group.title, style = MaterialTheme.typography.bodyLarge)
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(if (group.isGranted) "Concedida" else "Pendente") },
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncQueueCard(state: SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text("Fila local", style = MaterialTheme.typography.titleLarge)
            Text("Pendentes: ${state.pendingCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Falhas: ${state.failedCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Sincronizando: ${state.syncingCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Sincronizados: ${state.syncedCount}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Sincronizado significa escrita local aceita; o provider não é lido para confirmação.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Última sincronização: ${state.lastSyncedAt?.toString() ?: "ainda não registrada"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Context.openIntentSafely(intent: Intent) {
    runCatching { startActivity(intent) }.onFailure {
        if (it !is ActivityNotFoundException) throw it
    }
}
