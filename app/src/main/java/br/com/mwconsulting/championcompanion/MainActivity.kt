package br.com.mwconsulting.championcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme
import br.com.mwconsulting.championcompanion.core.navigation.ChampionNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChampionCompanionTheme {
                ChampionNavigation()
            }
        }
    }
}
