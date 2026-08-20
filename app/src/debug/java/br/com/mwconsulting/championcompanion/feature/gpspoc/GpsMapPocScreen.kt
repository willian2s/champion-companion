package br.com.mwconsulting.championcompanion.feature.gpspoc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.model.WorkoutRoutePoint
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

private const val OPEN_FREE_MAP_POSITRON_STYLE =
    "https://tiles.openfreemap.org/styles/positron"

private val ROUTE_LINE_COLOR = Color(0xFF1565C0)
private val ROUTE_START_COLOR = Color(0xFF2E7D32)
private val ROUTE_END_COLOR = Color(0xFFC62828)
private val CURRENT_POSITION_COLOR = Color(0xFFFF8F00)

@Composable
fun GpsMapPocScreen(
    coordinator: GpsMapPocCoordinator,
    onPrepareLocation: () -> Unit,
) {
    val state by coordinator.state.collectAsState()
    val cameraState = rememberCameraState()
    val moveReason = cameraState.moveReason
    val lastHandledRecenterRequestId = remember { mutableLongStateOf(0L) }

    LaunchedEffect(moveReason) {
        if (moveReason == CameraMoveReason.GESTURE) {
            coordinator.onManualCameraGesture()
        }
    }

    LaunchedEffect(
        state.currentPosition,
        state.followUser,
        state.status,
        state.mapStatus,
        state.recenterRequestId,
    ) {
        if (state.mapStatus !is GpsMapPocMapStatus.Ready) return@LaunchedEffect

        if (state.recenterRequestId != lastHandledRecenterRequestId.longValue) {
            lastHandledRecenterRequestId.longValue = state.recenterRequestId
            state.currentPosition?.let { point ->
                val currentCamera = cameraState.position
                cameraState.animateTo(
                    currentCamera.copy(
                        target = point.toMapPosition(),
                        zoom = if (state.routePoints.size == 1) {
                            URBAN_ACTIVITY_ZOOM
                        } else {
                            currentCamera.zoom.forLocation()
                        },
                    ),
                    duration = RECENTER_ANIMATION_MILLIS.milliseconds,
                )
            }
            return@LaunchedEffect
        }

        if (state.status == GpsMapPocStatus.Stopped) {
            state.routePoints.toMapBounds()?.let { bounds ->
                cameraState.animateTo(
                    bounds,
                    padding = FINAL_ROUTE_PADDING,
                    duration = FINAL_ROUTE_ANIMATION_MILLIS.milliseconds,
                )
                return@LaunchedEffect
            }
            if (state.routePoints.size == 1) {
                val point = state.routePoints.single()
                val currentCamera = cameraState.position
                cameraState.animateTo(
                    currentCamera.copy(
                        target = point.toMapPosition(),
                        zoom = URBAN_ACTIVITY_ZOOM,
                    ),
                    duration = FINAL_ROUTE_ANIMATION_MILLIS.milliseconds,
                )
            }
            return@LaunchedEffect
        }

        val point = state.currentPosition ?: return@LaunchedEffect
        if (!state.followUser || cameraState.moveReason == CameraMoveReason.GESTURE) {
            return@LaunchedEffect
        }

        val currentCamera = cameraState.position
        cameraState.animateTo(
            currentCamera.copy(
                target = point.toMapPosition(),
                zoom = if (state.routePoints.size == 1) URBAN_ACTIVITY_ZOOM else currentCamera.zoom.forLocation(),
            ),
            duration = FOLLOW_ANIMATION_MILLIS.milliseconds,
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        cameraState = cameraState,
                        onMapLoadFinished = coordinator::markMapReady,
                        onMapLoadFailed = { coordinator.markMapError() },
                    ) {
                        val routeSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(
                                state.toRouteGeoJson(),
                            ),
                            options = GeoJsonOptions(synchronousUpdate = false),
                        )
                        val startMarkerSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(
                                state.toStartMarkerGeoJson(),
                            ),
                            options = GeoJsonOptions(synchronousUpdate = false),
                        )
                        val endMarkerSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(
                                state.toEndMarkerGeoJson(),
                            ),
                            options = GeoJsonOptions(synchronousUpdate = false),
                        )
                        val currentPositionSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(
                                state.toCurrentPositionGeoJson(),
                            ),
                            options = GeoJsonOptions(synchronousUpdate = false),
                        )

                        if (state.routePoints.size >= 2) {
                            LineLayer(
                                id = "gps-poc-route",
                                source = routeSource,
                                color = const(ROUTE_LINE_COLOR),
                                width = const(5.dp),
                                cap = const(LineCap.Round),
                                join = const(LineJoin.Round),
                            )
                        }

                        if (state.routePoints.isNotEmpty()) {
                            CircleLayer(
                                id = "gps-poc-route-start",
                                source = startMarkerSource,
                                color = const(ROUTE_START_COLOR),
                                radius = const(6.dp),
                                strokeColor = const(Color.White),
                                strokeWidth = const(2.dp),
                            )
                        }

                        if (state.status == GpsMapPocStatus.Stopped && state.routePoints.size >= 2) {
                            CircleLayer(
                                id = "gps-poc-route-end",
                                source = endMarkerSource,
                                color = const(ROUTE_END_COLOR),
                                radius = const(7.dp),
                                strokeColor = const(Color.White),
                                strokeWidth = const(2.dp),
                            )
                        }

                        if (state.currentPosition != null && state.status != GpsMapPocStatus.Stopped) {
                            CircleLayer(
                                id = "gps-poc-current-position",
                                source = currentPositionSource,
                                color = const(CURRENT_POSITION_COLOR),
                                radius = const(7.dp),
                                strokeColor = const(Color.White),
                                strokeWidth = const(2.dp),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
            if (GpsMapPocMockGeometryDebugMode.isEnabled) {
                Text(
                    text = GpsMapPocMockGeometryDebugMode.banner,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = GpsMapPocMockGeometryDebugMode.disclaimer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = GpsMapPocMockGeometryDebugMode.requirements,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
            enabled = state.mapStatus is GpsMapPocMapStatus.Ready &&
                state.status.canRecenter() && state.currentPosition != null,
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

private fun GpsMapPocUiState.toRouteGeoJson(): String =
    if (routePoints.size >= 2) {
        featureCollectionJson(
            """{"type":"Feature","geometry":{"type":"LineString","coordinates":[${
                routePoints.joinToString(",") { it.toGeoJsonCoordinate() }
            }]},"properties":{}}""",
        )
    } else {
        emptyFeatureCollectionJson()
    }

private fun GpsMapPocUiState.toStartMarkerGeoJson(): String =
    routePoints.firstOrNull()?.let { point ->
        featureCollectionJson(point.toPointFeatureJson())
    } ?: emptyFeatureCollectionJson()

private fun GpsMapPocUiState.toEndMarkerGeoJson(): String =
    routePoints.lastOrNull()?.let { point ->
        featureCollectionJson(point.toPointFeatureJson())
    } ?: emptyFeatureCollectionJson()

private fun GpsMapPocUiState.toCurrentPositionGeoJson(): String =
    currentPosition?.let { point ->
        featureCollectionJson(point.toPointFeatureJson())
    } ?: emptyFeatureCollectionJson()

private fun featureCollectionJson(vararg features: String): String =
    """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

private fun emptyFeatureCollectionJson(): String = featureCollectionJson()

private fun GpsMapPocCurrentPosition.toPointFeatureJson(): String =
    """{"type":"Feature","geometry":{"type":"Point","coordinates":${toGeoJsonCoordinate()}},"properties":{}}"""

private fun WorkoutRoutePoint.toPointFeatureJson(): String =
    """{"type":"Feature","geometry":{"type":"Point","coordinates":${toGeoJsonCoordinate()}},"properties":{}}"""

private fun GpsMapPocCurrentPosition.toGeoJsonCoordinate(): String =
    "[$longitudeDegrees,$latitudeDegrees]"

private fun WorkoutRoutePoint.toGeoJsonCoordinate(): String =
    "[$longitudeDegrees,$latitudeDegrees]"

private fun GpsMapPocCurrentPosition.toMapPosition(): Position =
    Position(longitudeDegrees, latitudeDegrees)

private fun WorkoutRoutePoint.toMapPosition(): Position =
    Position(longitudeDegrees, latitudeDegrees)

private fun List<WorkoutRoutePoint>.toMapBounds(): BoundingBox? {
    if (size < 2) return null
    val minLongitude = minOf { it.longitudeDegrees }
    val maxLongitude = maxOf { it.longitudeDegrees }
    val minLatitude = minOf { it.latitudeDegrees }
    val maxLatitude = maxOf { it.latitudeDegrees }
    return BoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude)
}

private const val URBAN_ACTIVITY_ZOOM = 16.0
private const val FOLLOW_ANIMATION_MILLIS = 350L
private const val RECENTER_ANIMATION_MILLIS = 450L
private const val FINAL_ROUTE_ANIMATION_MILLIS = 600L
private val FINAL_ROUTE_PADDING = PaddingValues(48.dp)

private fun Double.forLocation(): Double = if (this < MIN_USABLE_LOCATION_ZOOM) {
    URBAN_ACTIVITY_ZOOM
} else {
    this
}

private const val MIN_USABLE_LOCATION_ZOOM = 1.0
