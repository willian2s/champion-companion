package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Visual contracts only. Technical connection and health models map to these at feature boundaries. */
enum class ConnectionVisualState {
    Connected,
    Disconnected,
}

/** Visual contracts only. Technical synchronization models map to these at feature boundaries. */
enum class SyncVisualState {
    Pending,
    Success,
    Failure,
}

/** Visual contracts only. Activity and health models map to these at feature boundaries. */
enum class ActivityVisualState {
    HeartRate,
    Running,
    Paused,
}

enum class ChampionStatusIcon {
    Connected,
    Disconnected,
    SyncPending,
    SyncSuccess,
    SyncFailure,
    HeartRate,
    ActivityRunning,
    ActivityPaused,
}

@Immutable
data class ChampionStatusVisualMetadata(
    val label: String,
    val contentDescription: String,
    val icon: ChampionStatusIcon,
)

fun ConnectionVisualState.visualMetadata(): ChampionStatusVisualMetadata = when (this) {
    ConnectionVisualState.Connected -> ChampionStatusVisualMetadata(
        label = "Conectado",
        contentDescription = "Dispositivo conectado",
        icon = ChampionStatusIcon.Connected,
    )
    ConnectionVisualState.Disconnected -> ChampionStatusVisualMetadata(
        label = "Desconectado",
        contentDescription = "Dispositivo desconectado",
        icon = ChampionStatusIcon.Disconnected,
    )
}

fun SyncVisualState.visualMetadata(): ChampionStatusVisualMetadata = when (this) {
    SyncVisualState.Pending -> ChampionStatusVisualMetadata(
        label = "Sincronização pendente",
        contentDescription = "Sincronização pendente",
        icon = ChampionStatusIcon.SyncPending,
    )
    SyncVisualState.Success -> ChampionStatusVisualMetadata(
        label = "Sincronizado",
        contentDescription = "Sincronização concluída",
        icon = ChampionStatusIcon.SyncSuccess,
    )
    SyncVisualState.Failure -> ChampionStatusVisualMetadata(
        label = "Falha na sincronização",
        contentDescription = "Não foi possível sincronizar. Tente novamente.",
        icon = ChampionStatusIcon.SyncFailure,
    )
}

fun ActivityVisualState.visualMetadata(): ChampionStatusVisualMetadata = when (this) {
    ActivityVisualState.HeartRate -> ChampionStatusVisualMetadata(
        label = "Frequência cardíaca",
        contentDescription = "Frequência cardíaca",
        icon = ChampionStatusIcon.HeartRate,
    )
    ActivityVisualState.Running -> ChampionStatusVisualMetadata(
        label = "Atividade em andamento",
        contentDescription = "Atividade em andamento",
        icon = ChampionStatusIcon.ActivityRunning,
    )
    ActivityVisualState.Paused -> ChampionStatusVisualMetadata(
        label = "Atividade pausada",
        contentDescription = "Atividade pausada",
        icon = ChampionStatusIcon.ActivityPaused,
    )
}

@Immutable
data class ChampionStatusColorPair(
    val container: Color,
    val content: Color,
)

@Immutable
data class ChampionStatusColors(
    val connected: ChampionStatusColorPair,
    val disconnected: ChampionStatusColorPair,
    val syncPending: ChampionStatusColorPair,
    val syncSuccess: ChampionStatusColorPair,
    val syncFailure: ChampionStatusColorPair,
    val heartRate: ChampionStatusColorPair,
    val activityRunning: ChampionStatusColorPair,
    val activityPaused: ChampionStatusColorPair,
)

val LightChampionStatusColors = ChampionStatusColors(
    connected = ChampionStatusColorPair(Color(0xFFC3F5C5), Color(0xFF002204)),
    disconnected = ChampionStatusColorPair(Color(0xFFE2E3E0), Color(0xFF1A1C1A)),
    syncPending = ChampionStatusColorPair(Color(0xFFAEEBFF), Color(0xFF001F29)),
    syncSuccess = ChampionStatusColorPair(Color(0xFFC3F5C5), Color(0xFF002204)),
    syncFailure = ChampionStatusColorPair(Color(0xFFFFDAD6), Color(0xFF410002)),
    heartRate = ChampionStatusColorPair(Color(0xFFFFDCC2), Color(0xFF2E1500)),
    activityRunning = ChampionStatusColorPair(Color(0xFFC2F1A6), Color(0xFF0B2000)),
    activityPaused = ChampionStatusColorPair(Color(0xFFE2E2EC), Color(0xFF1A1B23)),
)

val DarkChampionStatusColors = ChampionStatusColors(
    connected = ChampionStatusColorPair(Color(0xFF005315), Color(0xFFA6F8A6)),
    disconnected = ChampionStatusColorPair(Color(0xFF454745), Color(0xFFE2E3E0)),
    syncPending = ChampionStatusColorPair(Color(0xFF004D62), Color(0xFFAEEBFF)),
    syncSuccess = ChampionStatusColorPair(Color(0xFF005315), Color(0xFFA6F8A6)),
    syncFailure = ChampionStatusColorPair(Color(0xFF93000A), Color(0xFFFFDAD6)),
    heartRate = ChampionStatusColorPair(Color(0xFF713900), Color(0xFFFFB77F)),
    activityRunning = ChampionStatusColorPair(Color(0xFF1F5000), Color(0xFFC2F1A6)),
    activityPaused = ChampionStatusColorPair(Color(0xFF46464F), Color(0xFFE3E2EC)),
)

val LocalChampionStatusColors = staticCompositionLocalOf { LightChampionStatusColors }

fun ChampionStatusColors.colorsFor(state: ConnectionVisualState): ChampionStatusColorPair = when (state) {
    ConnectionVisualState.Connected -> connected
    ConnectionVisualState.Disconnected -> disconnected
}

fun ChampionStatusColors.colorsFor(state: SyncVisualState): ChampionStatusColorPair = when (state) {
    SyncVisualState.Pending -> syncPending
    SyncVisualState.Success -> syncSuccess
    SyncVisualState.Failure -> syncFailure
}

fun ChampionStatusColors.colorsFor(state: ActivityVisualState): ChampionStatusColorPair = when (state) {
    ActivityVisualState.HeartRate -> heartRate
    ActivityVisualState.Running -> activityRunning
    ActivityVisualState.Paused -> activityPaused
}
