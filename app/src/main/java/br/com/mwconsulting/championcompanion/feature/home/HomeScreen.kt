package br.com.mwconsulting.championcompanion.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeExtraLarge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionShapeSmall
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionSpacing
import br.com.mwconsulting.championcompanion.core.designsystem.EmptyState
import br.com.mwconsulting.championcompanion.feature.device.DeviceManagementConnectionState
import br.com.mwconsulting.championcompanion.feature.device.DeviceManagementIntent
import br.com.mwconsulting.championcompanion.feature.device.DeviceManagementViewModel

@Composable
fun HomeScreen(
    onOpenDevice: () -> Unit,
    deviceManagementViewModel: DeviceManagementViewModel = run {
        val activity = LocalContext.current as ComponentActivity
        remember(activity) {
            ViewModelProvider(activity)[DeviceManagementViewModel::class.java]
        }
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val deviceState by deviceManagementViewModel.state.collectAsState()
    val watchStatus = if (deviceState.connectionState == DeviceManagementConnectionState.CONNECTED) {
        HomeWatchStatus.Connected
    } else {
        HomeWatchStatus.Pending
    }

    LaunchedEffect(deviceManagementViewModel) {
        deviceManagementViewModel.onIntent(DeviceManagementIntent.Load)
    }

    DisposableEffect(lifecycleOwner, deviceManagementViewModel) {
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ChampionSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraLarge),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium)) {
                HomeTopBar(
                    status = watchStatus,
                    onOpenDevice = onOpenDevice,
                )
                Column(
                    modifier = Modifier.padding(horizontal = ChampionSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
                ) {
                    Text(
                        text = watchStatus.subtitle(deviceState.associatedDevice?.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Painel em preparação",
                    message = "Acompanhe status do relógio pelo ícone acima e abra Dispositivo para gerenciar conexão e sincronização.",
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    status: HomeWatchStatus,
    onOpenDevice: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ChampionShapeExtraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = ChampionSpacing.small),
        ) {
            WatchStatusButton(
                status = status,
                onClick = onOpenDevice,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = "Início",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            SpacerBalance(modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun WatchStatusButton(
    status: HomeWatchStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(ChampionShapeExtraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Abrir Dispositivo. Status do relógio: ${status.label}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Watch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(10.dp)
                .clip(ChampionShapeSmall)
                .background(status.color()),
        )
    }
}

@Composable
private fun SpacerBalance(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(44.dp))
}

private enum class HomeWatchStatus(
    val label: String,
) {
    Connected("conectado"),
    Pending("pendente"),
}

@Composable
private fun HomeWatchStatus.color() = when (this) {
    HomeWatchStatus.Connected -> MaterialTheme.colorScheme.primary
    HomeWatchStatus.Pending -> MaterialTheme.colorScheme.tertiary
}

private fun HomeWatchStatus.subtitle(deviceName: String?): String = when (this) {
    HomeWatchStatus.Connected -> "Relógio conectado${deviceName?.let { " • $it" } ?: ""}"
    HomeWatchStatus.Pending -> if (deviceName.isNullOrBlank()) {
        "Relógio desconectado ou pendente"
    } else {
        "Relógio desconectado ou pendente • $deviceName"
    }
}
