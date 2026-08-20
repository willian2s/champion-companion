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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeMedium
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.DarkColorScheme
import br.com.mwconsulting.championcompanion.core.designsystem.LightColorScheme
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
            SettingsHeaderCard()
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
private fun SettingsHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeLarge,
        colors = CardDefaults.cardColors(containerColor = LightColorScheme.primary),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = "Configurações",
                modifier = Modifier.semantics { heading() },
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Controle disponibilidade, permissões e sincronização write-only.",
                style = MaterialTheme.typography.bodyMedium,
                color = White,
            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, settingsPrimaryAccent()),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Integração Health Connect",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = settingsPrimaryAccent(),
            )
            Text(
                when (state.availability) {
                    SettingsAvailability.AVAILABLE -> "Disponível para sincronização foreground."
                    SettingsAvailability.PROVIDER_UPDATE_REQUIRED -> "Atualização do provider necessária antes de sincronizar."
                    SettingsAvailability.UNAVAILABLE -> "Indisponível neste dispositivo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Provider: ${state.providerPackageName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text("Sincronização", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.healthSyncEnabled) "Ativa quando ações foreground forem executadas."
                        else "Pausada até você ativar a integração.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.healthSyncEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (state.healthSyncEnabled) {
                            "Sincronização write-only ativa"
                        } else {
                            "Sincronização write-only pausada"
                        }
                    },
                )
            }
            Button(
                onClick = onConnect,
                enabled = state.availability != SettingsAvailability.AVAILABLE || !isConnected,
                modifier = Modifier.fillMaxWidth(),
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
            OutlinedButton(
                onClick = onSyncNow,
                enabled = state.healthSyncEnabled && !state.isSyncNowRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSyncNowRunning) "Sincronizando" else "Sincronizar agora")
            }
            TextButton(
                onClick = onManageAccess,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Gerenciar acesso")
            }
            state.syncStatusMessage?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChampionShapeMedium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(ChampionSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
                    ) {
                        Text(
                            it,
                            modifier = Modifier.semantics { contentDescription = "Status: $it" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, settingsSecondaryAccent()),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Permissões por capacidade",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = settingsSecondaryAccent(),
            )
            Text(
                "Somente permissões de escrita necessárias para projeções locais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            permissionGroups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
                    Text(group.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (group.isGranted) "✓ Concedida" else "! Pendente",
                        color = if (group.isGranted) settingsSecondaryAccent() else settingsTertiaryAccent(),
                        style = MaterialTheme.typography.labelLarge,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, settingsTertiaryAccent()),
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            Text(
                text = "Fila local",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = settingsTertiaryAccent(),
            )
            Text(
                "A fila concentra escritas foreground pendentes e seus resultados locais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsQueueRow("Pendentes", state.pendingCount, settingsTertiaryAccent())
            SettingsQueueRow("Falhas", state.failedCount, MaterialTheme.colorScheme.error)
            SettingsQueueRow("Sincronizando", state.syncingCount, settingsPrimaryAccent())
            SettingsQueueRow("Sincronizados", state.syncedCount, settingsSecondaryAccent())
            Text(
                "Sincronizado significa escrita local aceita; o provider não é lido para confirmação.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Última sincronização: ${state.lastSyncedAt?.toString() ?: "ainda não registrada"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsQueueRow(
    label: String,
    count: Int,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = count.toString(),
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun settingsPrimaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.primary
} else {
    LightColorScheme.primary
}

@Composable
private fun settingsSecondaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.secondary
} else {
    LightColorScheme.secondary
}

@Composable
private fun settingsTertiaryAccent(): Color = if (isSystemInDarkTheme()) {
    DarkColorScheme.tertiary
} else {
    LightColorScheme.tertiary
}

private fun Context.openIntentSafely(intent: Intent) {
    runCatching { startActivity(intent) }.onFailure {
        if (it !is ActivityNotFoundException) throw it
    }
}
