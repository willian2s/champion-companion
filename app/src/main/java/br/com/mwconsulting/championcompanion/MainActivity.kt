package br.com.mwconsulting.championcompanion

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme
import br.com.mwconsulting.championcompanion.core.model.PhoneWorkoutPolicy
import br.com.mwconsulting.championcompanion.core.navigation.ChampionNavigation
import br.com.mwconsulting.championcompanion.feature.exercises.DebugPocLauncher
import br.com.mwconsulting.championcompanion.feature.exercises.StaleWorkoutReconciliation
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutSessionActivity
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutSessionOwnershipCoordinator
import br.com.mwconsulting.championcompanion.feature.exercises.WorkoutRecordingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var workoutSessionOwnershipCoordinator: WorkoutSessionOwnershipCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChampionCompanionTheme {
                ChampionNavigation(
                    onOpenDebugPoc = DebugPocLauncher.createCallback(this@MainActivity),
                )
            }
        }
        offerRecoverableWorkout()
    }

    private fun stopDiscontinuedWorkoutService(sessionId: String): Long? {
        if (!PhoneWorkoutPolicy.isDiscontinued()) return null
        val generation = PhoneWorkoutPolicy.requestShutdown()
        runCatching {
            startService(
                WorkoutRecordingService.shutdownDiscontinuedIntent(
                    context = this,
                    sessionId = sessionId,
                    shutdownGeneration = generation,
                ),
            )
        }
        return generation
    }

    private fun offerRecoverableWorkout() {
        lifecycleScope.launch {
            val recoverable = when (
                val reconciliation = workoutSessionOwnershipCoordinator.reconcileOpenSession()
            ) {
                is StaleWorkoutReconciliation.RecoverySurface -> reconciliation.session
                is StaleWorkoutReconciliation.ForegroundOwned -> reconciliation.session
                StaleWorkoutReconciliation.NoOpenSession -> null
                StaleWorkoutReconciliation.Unavailable -> {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.workout_recovery_unavailable,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    return@launch
                }
            }

            if (recoverable == null) return@launch
            val shutdownGeneration = stopDiscontinuedWorkoutService(recoverable.id.value.toString())
            if (shutdownGeneration == null || !PhoneWorkoutPolicy.awaitShutdown(shutdownGeneration)) {
                Toast.makeText(
                    this@MainActivity,
                    PhoneWorkoutPolicy.SHUTDOWN_UNCONFIRMED_MESSAGE,
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            if (!isFinishing && !isDestroyed) {
                startActivity(
                    WorkoutSessionActivity.recoveryIntent(
                        context = this@MainActivity,
                        sessionId = recoverable.id.value.toString(),
                        shutdownGeneration = shutdownGeneration,
                    ),
                )
            }
        }
    }
}
