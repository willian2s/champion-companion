package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Fundação clara", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
private fun LightFoundationPreview() {
    FoundationPreview(darkTheme = false)
}

@Preview(name = "Fundação escura", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
private fun DarkFoundationPreview() {
    FoundationPreview(darkTheme = true)
}

@Preview(
    name = "Fundação fonte 1,3x",
    widthDp = 412,
    heightDp = 900,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun LargeFontFoundationPreview() {
    FoundationPreview(darkTheme = false)
}

@Preview(name = "Fundação landscape", widthDp = 800, heightDp = 420, showBackground = true)
@Composable
private fun LandscapeFoundationPreview() {
    FoundationPreview(darkTheme = false)
}

@Preview(name = "Fundação conteúdo longo", widthDp = 320, heightDp = 900, showBackground = true)
@Composable
private fun LongContentFoundationPreview() {
    FoundationPreview(darkTheme = false, useLongContent = true)
}

@Composable
private fun FoundationPreview(
    darkTheme: Boolean,
    useLongContent: Boolean = false,
) {
    ChampionCompanionTheme(darkTheme = darkTheme, useDynamicColors = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ChampionPage(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                header = {
                    ChampionPageHeader(
                        title = if (useLongContent) {
                            "Resumo de saúde e bem-estar para um período selecionado"
                        } else {
                            "Resumo de saúde"
                        },
                        supportingText = if (useLongContent) {
                            "Conteúdo genérico longo confirma quebra de linha sem ocultar informações essenciais."
                        } else {
                            "Informações locais organizadas por prioridade."
                        },
                        metadata = { PreviewMetadata("Atualizado localmente • Período atual") },
                    )
                },
            ) {
                ChampionSection(
                    title = "Métricas principais",
                    supportingText = "Valores demonstrativos, sem dados pessoais.",
                    metadata = { PreviewMetadata("Origem local") },
                ) {
                    ChampionMetricSummary(
                        title = "Passos",
                        value = "Sem dados",
                        supportingText = if (useLongContent) {
                            "A métrica permanece indisponível até existir informação local válida para este período."
                        } else {
                            "Nenhum registro no período"
                        },
                        metadata = { PreviewMetadata("Hoje • passos") },
                        onClick = {},
                    )
                }
                ChampionSection(title = "Ações") {
                    ChampionActionGroup(
                        primaryLabel = if (useLongContent) {
                            "Atualizar informações disponíveis neste dispositivo"
                        } else {
                            "Atualizar resumo"
                        },
                        onPrimaryClick = {},
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ChampionLayout.minimumTouchTarget),
                        ) {
                            Text("Ver detalhes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMetadata(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
}
