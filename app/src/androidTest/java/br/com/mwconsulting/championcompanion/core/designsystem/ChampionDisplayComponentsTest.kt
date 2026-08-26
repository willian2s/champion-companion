package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ChampionDisplayComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cards_render_caller_formatted_content_with_semantics() {
        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                HealthCard(title = "Resumo") { androidx.compose.material3.Text("Conteúdo fornecido") }
                MetricCard(label = "Passos", value = "8.420", supportingText = "Hoje")
            }
        }

        composeRule.onNodeWithText("Resumo").assertIsDisplayed()
        composeRule.onNodeWithText("Conteúdo fornecido").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Passos, 8.420, Hoje").assertIsDisplayed()
    }

}
