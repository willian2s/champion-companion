package br.com.mwconsulting.championcompanion.feature.exercises

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme
import br.com.mwconsulting.championcompanion.core.navigation.AppDestination
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class WorkoutSessionActivity : ComponentActivity() {
    private var request by mutableStateOf<WorkoutSessionRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        request = intent.toWorkoutSessionRequestOrNull()
        setContent {
            ChampionCompanionTheme {
                val viewModel = ViewModelProvider(this)[ExercisesViewModel::class.java]
                when (val current = request) {
                    is WorkoutSessionRequest.Active -> ActiveWorkoutShell(
                        sessionId = current.sessionId,
                        onNavigateTo = ::handleDestination,
                        onNavigateBack = ::finish,
                        exercisesViewModel = viewModel,
                    )
                    is WorkoutSessionRequest.Recovery -> WorkoutRecoveryShell(
                        sessionId = current.sessionId,
                        onNavigateTo = ::handleDestination,
                        onNavigateBack = ::finish,
                        exercisesViewModel = viewModel,
                    )
                    null -> finish()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        request = intent.toWorkoutSessionRequestOrNull()
    }

    private fun handleDestination(destination: AppDestination) {
        request = when (destination) {
            is AppDestination.ActiveWorkout -> WorkoutSessionRequest.Active(destination.sessionId)
            is AppDestination.WorkoutRecovery -> WorkoutSessionRequest.Recovery(destination.sessionId)
            else -> {
                finish()
                null
            }
        }
    }

    private fun Intent.toWorkoutSessionRequestOrNull(): WorkoutSessionRequest? {
        val sessionId = getStringExtra(EXTRA_SESSION_ID)
            ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
            ?: return null
        return when (getStringExtra(EXTRA_MODE)) {
            MODE_ACTIVE -> WorkoutSessionRequest.Active(sessionId)
            MODE_RECOVERY -> WorkoutSessionRequest.Recovery(sessionId)
            else -> null
        }
    }

    companion object {
        private const val EXTRA_MODE = "workout_session_mode"
        private const val EXTRA_SESSION_ID = "workout_session_id"
        private const val MODE_ACTIVE = "active"
        private const val MODE_RECOVERY = "recovery"

        fun activeIntent(context: Context, sessionId: String): Intent =
            Intent(context, WorkoutSessionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_ACTIVE)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

        fun recoveryIntent(context: Context, sessionId: String): Intent =
            Intent(context, WorkoutSessionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_RECOVERY)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
    }
}

private sealed interface WorkoutSessionRequest {
    val sessionId: String

    data class Active(override val sessionId: String) : WorkoutSessionRequest
    data class Recovery(override val sessionId: String) : WorkoutSessionRequest
}
