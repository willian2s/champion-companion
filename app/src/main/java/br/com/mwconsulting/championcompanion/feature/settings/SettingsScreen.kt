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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.health.connect.client.PermissionController
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignHeaderNavigationAction
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSummaryCard
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignSurfaceHeader
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionRedesignTheme
import br.com.mwconsulting.championcompanion.core.designsystem.LocalChampionRedesignTokens
import br.com.mwconsulting.championcompanion.core.healthconnect.HealthConnectWritePermission

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = run {
        val activity = checkNotNull(LocalActivity.current) as ComponentActivity
        androidx.compose.runtime.remember(activity) {
            ViewModelProvider(activity)[SettingsViewModel::class.java]
        }
    },
    onNavigateBack: (() -> Unit)? = null,
    backLabel: String = "Voltar",
) {
    val state by viewModel.state.collectAsState()
    val activity = checkNotNull(LocalActivity.current) as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val weightPermission = HealthConnectWritePermission.WEIGHT
    val weightGranted = state.permissionGroups
        .firstOrNull { weightPermission in it.permissions }
        ?.isGranted == true
    val missingPermissions = buildSet {
        addAll(viewModel.requestedPermissions())
        if (!weightGranted) add(weightPermission.rawValue)
    }
    val allManagedPermissions = viewModel.allRequestedPermissions() + weightPermission.rawValue
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

    ChampionRedesignTheme {
        val tokens = LocalChampionRedesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screen, vertical = tokens.spacing.compact),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.item),
            ) {
                SettingsHeader(
                    onNavigateBack = onNavigateBack,
                    backLabel = backLabel,
                )
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
                    onManageAccess = {
                        allManagedPermissions.takeIf { it.isNotEmpty() }?.let {
                            permissionLauncher.launch(it)
                        }
                    },
                    onDismissStatus = { viewModel.onIntent(SettingsIntent.DismissStatus) },
                )
                PermissionGroupsCard(state.permissionGroups)
                SyncQueueCard(state)
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    onNavigateBack: (() -> Unit)?,
    backLabel: String,
) {
    ChampionRedesignSurfaceHeader(
        title = "Configurações",
        supportingText = "Controle disponibilidade, permissões e sincronização write-only.",
        navigationContent = onNavigateBack?.let { navigateBack ->
            {
                ChampionRedesignHeaderNavigationAction(
                    contentDescription = backLabel,
                    onClick = navigateBack,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
        },
    )
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
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = LocalChampionRedesignTokens.current.elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
        ) {
            Text(
                text = "Integração Health Connect",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
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
                    verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
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
                    shape = LocalChampionRedesignTokens.current.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.item),
                        verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact),
                    ) {
                        Text(
                            it,
                            modifier = Modifier.semantics {
                                contentDescription = "Status: $it"
                                liveRegion = LiveRegionMode.Polite
                            },
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
        shape = LocalChampionRedesignTokens.current.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = LocalChampionRedesignTokens.current.elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(LocalChampionRedesignTokens.current.spacing.card),
            verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item),
        ) {
            Text(
                text = "Permissões por capacidade",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Somente permissões de escrita necessárias para projeções locais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            permissionGroups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.compact)) {
                    Text(group.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (group.isGranted) "✓ Concedida" else "! Pendente",
                        color = if (group.isGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncQueueCard(state: SettingsUiState) {
    ChampionRedesignSummaryCard(
        title = "Fila local",
        value = state.pendingCount.toString(),
        modifier = Modifier.fillMaxWidth(),
        supportingText = "A fila concentra escritas foreground pendentes e seus resultados locais.",
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalChampionRedesignTokens.current.spacing.item)) {
            SettingsQueueRow("Pendentes", state.pendingCount, MaterialTheme.colorScheme.tertiary)
            SettingsQueueRow("Falhas", state.failedCount, MaterialTheme.colorScheme.error)
            SettingsQueueRow("Sincronizando", state.syncingCount, MaterialTheme.colorScheme.primary)
            SettingsQueueRow("Sincronizados", state.syncedCount, MaterialTheme.colorScheme.secondary)
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

private fun Context.openIntentSafely(intent: Intent) {
    runCatching { startActivity(intent) }.onFailure {
        if (it !is ActivityNotFoundException) throw it
    }
}
