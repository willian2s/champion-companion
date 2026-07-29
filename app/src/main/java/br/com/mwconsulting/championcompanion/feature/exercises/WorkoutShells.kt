package br.com.mwconsulting.championcompanion.feature.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val workoutTypes = listOf(
    "WALKING" to "Caminhada",
    "RUNNING" to "Corrida",
    "FREE_WORKOUT" to "Treino livre",
    "TREADMILL" to "Esteira",
)

@Composable
fun WorkoutTypeSelectionShell(
    onSelectType: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    WorkoutShellCard(
        title = "Selecionar treino",
        description = "Escolha tipo de treino para preparar sessão.",
        onNavigateBack = onNavigateBack,
    ) {
        workoutTypes.forEach { (type, label) ->
            Button(
                onClick = { onSelectType(type) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun WorkoutPreparationShell(
    workoutType: String,
    onNavigateBack: () -> Unit,
) {
    WorkoutShellCard(
        title = "Preparar treino",
        description = "Tipo selecionado: ${workoutType.toWorkoutTypeLabel()}.",
        onNavigateBack = onNavigateBack,
    ) {
        Text(
            text = "Nenhuma sessão, rota ou serviço foi iniciado.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun ActiveWorkoutShell(
    sessionId: String,
    onNavigateBack: () -> Unit,
) {
    WorkoutShellCard(
        title = "Treino ativo",
        description = "Sessão $sessionId disponível para implementação futura.",
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun WorkoutRecoveryShell(
    sessionId: String?,
    onNavigateBack: () -> Unit,
) {
    WorkoutShellCard(
        title = "Recuperar treino",
        description = sessionId?.let { "Sessão $it aguarda decisão." }
            ?: "Não há identificador de sessão informado.",
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun WorkoutShellCard(
    title: String,
    description: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onNavigateBack) {
            Text("Voltar")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                content()
            }
        }
    }
}

private fun String.toWorkoutTypeLabel(): String = workoutTypes.firstOrNull { it.first == this }?.second
    ?: "desconhecido"
