package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChampionFeedbackComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun feedback_states_render_safe_text_and_semantics() {
        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                EmptyState(title = "Sem medições", message = "Conecte um dispositivo para começar.")
                ErrorState(title = "Não foi possível atualizar", message = "Tente novamente.")
                LoadingState(label = "Carregando medições", message = "Aguarde.")
            }
        }

        composeRule.onNodeWithContentDescription("Sem medições. Conecte um dispositivo para começar.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Não foi possível atualizar. Tente novamente.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Carregando medições, Aguarde.").assertIsDisplayed()
    }

    @Test
    fun feedback_error_action_is_optional_and_clickable() {
        var clicks = 0

        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                ErrorState(
                    title = "Não foi possível atualizar",
                    message = "Tente novamente.",
                    actionLabel = "Tentar novamente",
                    onAction = { clicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Tentar novamente")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun banners_describe_state_and_run_optional_actions() {
        var calibrationClicks = 0

        composeRule.setContent {
            ChampionCompanionTheme(useDynamicColors = false) {
                PermissionBanner(
                    title = "Permissão necessária",
                    message = "Autorize acesso para continuar.",
                )
                PendingReviewBanner(
                    message = "Uma atividade aguarda revisão.",
                )
                CalibrationBanner(
                    message = "Informe distância da esteira.",
                    actionLabel = "Calibrar",
                    onAction = { calibrationClicks++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Aviso: Permissão necessária. Autorize acesso para continuar.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Aviso: Revisão pendente. Uma atividade aguarda revisão.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Aviso: Calibração necessária. Informe distância da esteira.").assertIsDisplayed()
        composeRule.onNodeWithText("Calibrar")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, calibrationClicks)
        }
    }
}
