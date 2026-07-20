package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChampionActionAndStatusComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun primaryActionButton_clicks_and_has_minimum_touch_target() {
        var clicks = 0

        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                PrimaryActionButton(label = "Salvar", onClick = { clicks++ })
            }
        }

        composeRule.onNodeWithContentDescription("Salvar")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
        val height = composeRule.onNodeWithContentDescription("Salvar").fetchSemanticsNode().boundsInRoot.height
        val minimumHeight = with(composeRule.density) { 48.dp.toPx() }
        assertTrue("Expected height >= $minimumHeight, was $height", height >= minimumHeight)
    }

    @Test
    fun loading_action_is_described_and_disabled() {
        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                PrimaryActionButton(label = "Salvar", onClick = {}, isLoading = true)
            }
        }

        composeRule.onNodeWithContentDescription("Salvar, carregando")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun status_components_render_labels_and_safe_descriptions() {
        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                ConnectionStatus(ConnectionVisualState.Connected)
                SyncStatusBadge(SyncVisualState.Failure)
                ActivityStatusBadge(ActivityVisualState.Paused)
            }
        }

        composeRule.onNodeWithText("Conectado", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Falha na sincronização", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Atividade pausada", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dispositivo conectado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Não foi possível sincronizar. Tente novamente.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Atividade pausada").assertIsDisplayed()
    }
}
