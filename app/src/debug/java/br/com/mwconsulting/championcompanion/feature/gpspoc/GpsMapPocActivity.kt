package br.com.mwconsulting.championcompanion.feature.gpspoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme

/**
 * Entry point técnico exclusivo de debug. Acesso primário pelo CTA técnico em
 * Exercícios; ADB permanece fallback diagnóstico. Não integra navegação de produto.
 */
class GpsMapPocActivity : ComponentActivity() {
    private val coordinator = GpsMapPocCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChampionCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GpsMapPocScreen(coordinator = coordinator)
                }
            }
        }
    }
}
