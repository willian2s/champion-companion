package br.com.mwconsulting.championcompanion.feature.gpspoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle

private const val OPEN_FREE_MAP_POSITRON_STYLE =
    "https://tiles.openfreemap.org/styles/positron"

@Composable
fun GpsMapPocScreen(
    coordinator: GpsMapPocCoordinator,
    onPrepareLocation: () -> Unit,
) {
    val state by coordinator.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GPS + MapLibre — PoC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    MaplibreMap(
                        modifier = Modifier.fillMaxSize(),
                        baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_POSITRON_STYLE),
                        onMapLoadFinished = coordinator::markMapReady,
                        onMapLoadFailed = { coordinator.markMapError() },
                    )
                }
            }

            GpsMapPocStatusCard(state = state)
            GpsMapPocMetricsCard(state = state)
            GpsMapPocActions(
                state = state,
                onPrepareLocation = onPrepareLocation,
                onRecenter = coordinator::recenter,
                onStart = coordinator::start,
                onStop = coordinator::stop,
                onClear = coordinator::clear,
            )
        }
    }
}

@Composable
private fun GpsMapPocStatusCard(state: GpsMapPocUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Estado técnico", style = MaterialTheme.typography.titleMedium)
            StatusRow(label = "Mapa-base", value = state.mapStatus.label())
            StatusRow(label = "GPS", value = state.gpsReadiness.label())
            StatusRow(label = "PoC", value = state.status.label())
            state.mapStatus.errorMessageOrNull()?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.status.errorMessageOrNull()?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "OpenFreeMap via MapLibre. Atribuição cartográfica permanece no mapa.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GpsMapPocMetricsCard(state: GpsMapPocUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Métricas", style = MaterialTheme.typography.titleMedium)
            StatusRow(
                label = "Precisão horizontal",
                value = state.horizontalAccuracyMeters?.let { "${it.formatMeters()} m" } ?: "não disponível",
            )
            StatusRow(label = "Fixes recebidos", value = state.receivedFixCount.toString())
            StatusRow(label = "Distância", value = state.distanceMeters.formatDistance())
            StatusRow(label = "Pontos aceitos", value = state.acceptedPointCount.toString())
            StatusRow(label = "Pontos rejeitados", value = state.rejectedPointCount.toString())
        }
    }
}

@Composable
private fun GpsMapPocActions(
    state: GpsMapPocUiState,
    onPrepareLocation: () -> Unit,
    onRecenter: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onPrepareLocation,
            enabled = state.status != GpsMapPocStatus.Tracking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Preparar GPS")
        }
        OutlinedButton(
            onClick = onRecenter,
            enabled = state.mapStatus is GpsMapPocMapStatus.Ready && state.status.canRecenter(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Recentralizar")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onStart,
                enabled = state.status == GpsMapPocStatus.Ready &&
                    state.mapStatus is GpsMapPocMapStatus.Ready &&
                    state.gpsReadiness == GpsMapPocGpsReadiness.Ready,
                modifier = Modifier.weight(1f),
            ) {
                Text("Iniciar")
            }
            OutlinedButton(
                onClick = onStop,
                enabled = state.status == GpsMapPocStatus.Tracking ||
                    state.status == GpsMapPocStatus.GpsLost,
                modifier = Modifier.weight(1f),
            ) {
                Text("Parar")
            }
        }
            OutlinedButton(
                onClick = onClear,
                enabled = state.status.canClear(),
                modifier = Modifier.fillMaxWidth(),
            ) {
            Text("Limpar")
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun GpsMapPocMapStatus.label(): String = when (this) {
    GpsMapPocMapStatus.Loading -> "Carregando"
    GpsMapPocMapStatus.Ready -> "Pronto"
    is GpsMapPocMapStatus.Error -> "Indisponível"
}

private fun GpsMapPocGpsReadiness.label(): String = when (this) {
    GpsMapPocGpsReadiness.PermissionRequired -> "Permissão necessária"
    GpsMapPocGpsReadiness.PrecisePermissionRequired -> "Permissão precisa necessária"
    GpsMapPocGpsReadiness.LocationDisabled -> "Localização desativada"
    GpsMapPocGpsReadiness.Searching -> "Procurando GPS"
    GpsMapPocGpsReadiness.Ready -> "Pronto"
    GpsMapPocGpsReadiness.Lost -> "GPS perdido"
    GpsMapPocGpsReadiness.Unavailable -> "Indisponível"
}

private fun GpsMapPocStatus.label(): String = when (this) {
    GpsMapPocStatus.Initializing -> "Inicializando"
    GpsMapPocStatus.PermissionRequired -> "Permissão necessária"
    GpsMapPocStatus.SearchingGps -> "Procurando GPS"
    GpsMapPocStatus.Ready -> "Pronto"
    GpsMapPocStatus.Tracking -> "Capturando"
    GpsMapPocStatus.GpsLost -> "GPS perdido"
    GpsMapPocStatus.Stopped -> "Parado"
    is GpsMapPocStatus.Error -> "Erro"
}

private fun GpsMapPocMapStatus.errorMessageOrNull(): String? =
    (this as? GpsMapPocMapStatus.Error)?.details?.message

private fun GpsMapPocStatus.errorMessageOrNull(): String? =
    (this as? GpsMapPocStatus.Error)?.details?.message

private fun GpsMapPocStatus.canRecenter(): Boolean = this in setOf(
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Tracking,
    GpsMapPocStatus.GpsLost,
    GpsMapPocStatus.Stopped,
)

private fun GpsMapPocStatus.canClear(): Boolean = this in setOf(
    GpsMapPocStatus.PermissionRequired,
    GpsMapPocStatus.SearchingGps,
    GpsMapPocStatus.Ready,
    GpsMapPocStatus.Stopped,
) || this is GpsMapPocStatus.Error

private fun Float.formatMeters(): String = "%.0f".format(java.util.Locale.ROOT, this)

private fun Double.formatDistance(): String = when {
    this >= 1000.0 -> "%.2f km".format(java.util.Locale.ROOT, this / 1000.0)
    else -> "%.0f m".format(java.util.Locale.ROOT, this)
}
