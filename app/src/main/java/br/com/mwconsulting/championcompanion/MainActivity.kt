package br.com.mwconsulting.championcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionMutation
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionRepository
import br.com.mwconsulting.championcompanion.core.model.AppWorkoutSessionState
import br.com.mwconsulting.championcompanion.core.model.RepositoryResult
import br.com.mwconsulting.championcompanion.core.navigation.ChampionNavigation
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutRecordingService
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutSessionActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var workoutSessionRepository: AppWorkoutSessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChampionCompanionTheme {
                ChampionNavigation()
            }
        }
        offerRecoverableWorkout()
    }

    private fun offerRecoverableWorkout() {
        lifecycleScope.launch {
            if (WorkoutRecordingService.isAlive) return@launch

            val active = workoutSessionRepository.observeActive().first()
            val recoverable = when {
                active?.state == AppWorkoutSessionState.ACTIVE ||
                    active?.state == AppWorkoutSessionState.PAUSED -> {
                    when (
                        val result = workoutSessionRepository.interrupt(
                            AppWorkoutSessionMutation(
                                sessionId = active.id,
                                expectedRevision = active.revision,
                                at = Instant.now(),
                            ),
                        )
                    ) {
                        is RepositoryResult.Success -> result.value
                        RepositoryResult.Conflict -> {
                            delay(100L)
                            workoutSessionRepository.observeRecoverable().first().firstOrNull()
                        }
                        else -> null
                    }
                }
                else -> workoutSessionRepository.observeRecoverable().first().firstOrNull()
            }

            if (recoverable != null && !isFinishing && !isDestroyed) {
                startActivity(
                    WorkoutSessionActivity.recoveryIntent(
                        context = this@MainActivity,
                        sessionId = recoverable.id.value.toString(),
                    ),
                )
            }
        }
    }
}
