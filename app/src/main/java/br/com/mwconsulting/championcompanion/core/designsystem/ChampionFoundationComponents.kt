package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Ritmo de página opt-in. Rolagem e insets permanecem sob responsabilidade do
 * chamador para permitir uso em shells diferentes sem estado próprio de UI.
 */
@Composable
fun ChampionPage(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(
            horizontal = ChampionLayout.pageHorizontalPadding,
            vertical = ChampionLayout.pageVerticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(ChampionLayout.sectionSpacing),
    ) {
        header?.invoke()
        content()
    }
}

/** Cabeçalho editorial com quebra natural de texto e metadados opcionais do chamador. */
@Composable
fun ChampionPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    metadata: @Composable (() -> Unit)? = null,
    leadingContent: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            leadingContent()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            actions()
        }
        metadata?.invoke()
    }
}

/** Seção nomeada. Formatação de domínio permanece no conteúdo fornecido pelo chamador. */
@Composable
fun ChampionSection(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    metadata: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionLayout.contentSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            metadata?.invoke()
        }
        content()
    }
}

/**
 * Resumo tonal de métrica. Valores e unidades chegam pré-formatados; callback
 * opcional torna todo o resumo acionável sem alterar o uso não acionável.
 */
@Composable
fun ChampionMetricSummary(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    metadata: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier.heightIn(min = ChampionLayout.minimumTouchTarget)
            } else {
                Modifier
            },
        )
        .semantics(mergeDescendants = true) {}
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            metadata?.invoke()
        }
    }

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = ChampionShapeLarge,
            content = cardContent,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = ChampionShapeLarge,
            content = cardContent,
        )
    }
}

/** Ação primária seguida por ações secundárias ou destrutivas definidas pelo chamador. */
@Composable
fun ChampionActionGroup(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryActions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
    ) {
        PrimaryActionButton(
            label = primaryLabel,
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = primaryEnabled,
        )
        secondaryActions()
    }
}
