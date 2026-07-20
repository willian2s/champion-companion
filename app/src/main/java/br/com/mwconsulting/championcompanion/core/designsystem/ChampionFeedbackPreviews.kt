package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Feedback claro", showBackground = true)
@Composable
private fun LightFeedbackPreview() {
    FeedbackPreview(darkTheme = false)
}

@Preview(name = "Feedback escuro", showBackground = true)
@Composable
private fun DarkFeedbackPreview() {
    FeedbackPreview(darkTheme = true)
}

@Preview(name = "Feedback fonte ampliada", fontScale = 1.3f, showBackground = true)
@Composable
private fun LargeFontFeedbackPreview() {
    FeedbackPreview(darkTheme = false)
}

@Composable
private fun FeedbackPreview(darkTheme: Boolean) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
        ) {
            EmptyState(title = "Nada por aqui", message = "Novos dados aparecerão aqui.")
            ErrorState(title = "Não foi possível atualizar", message = "Tente novamente.", actionLabel = "Tentar", onAction = {})
            LoadingState(label = "Carregando dados", message = "Isso pode levar alguns instantes.")
            PermissionBanner(title = "Permissão necessária", message = "Autorize acesso para continuar.", actionLabel = "Autorizar", onAction = {})
            PendingReviewBanner(message = "Há uma atividade aguardando revisão.", actionLabel = "Revisar", onAction = {})
            CalibrationBanner(message = "Informe distância da esteira.", actionLabel = "Calibrar", onAction = {})
        }
    }
}
