package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cabeçalho reutilizável de superfície, com slots para voltar, navegação e
 * ações. O título é exposto como heading. Slots aceitam conteúdo genérico; para
 * qualquer ação interativa, use [ChampionRedesignHeaderAction] ou
 * [ChampionRedesignHeaderNavigationAction], que fornecem semântica obrigatória
 * e alvo nativo mínimo de 48 dp.
 */
@Composable
fun ChampionRedesignSurfaceHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationContent: @Composable (() -> Unit)? = null,
    actionsContent: @Composable RowScope.() -> Unit = {},
    supportingText: String? = null,
) {
    val tokens = LocalChampionRedesignTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compact),
    ) {
        navigationContent?.let { content ->
            Box(modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { content() }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = tokens.typography.headlineSmall,
            )
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = tokens.typography.bodyMedium,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compact),
            content = actionsContent,
        )
    }
}

/**
 * Ação interativa segura para [ChampionRedesignSurfaceHeader]. O wrapper usa
 * [IconButton] nativo, exige descrição acessível e garante alvo mínimo de 48 dp.
 */
@Composable
fun ChampionRedesignHeaderAction(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    require(contentDescription.isNotBlank()) { "contentDescription must not be blank" }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { this.contentDescription = contentDescription },
        content = content,
    )
}

/**
 * Ação de navegação segura para [ChampionRedesignSurfaceHeader]. Mantém mesma
 * garantia de semântica e alvo de [ChampionRedesignHeaderAction].
 */
@Composable
fun ChampionRedesignHeaderNavigationAction(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    ChampionRedesignHeaderAction(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * Card stateless de resumo ou métrica, com conteúdo principal e slots de apoio.
 * [onClick] opcional transforma card em ação sem exigir estado de negócio; o
 * alvo mínimo do card interativo permanece 48 dp.
 */
@Composable
fun ChampionRedesignSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val tokens = LocalChampionRedesignTokens.current
    val cardModifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    if (onClick == null) {
        Card(
            modifier = cardModifier,
            shape = tokens.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevation.card),
        ) {
            ChampionRedesignSummaryCardContent(
                title = title,
                value = value,
                supportingText = supportingText,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                content = content,
                padding = tokens.spacing.card,
                compactSpacing = tokens.spacing.compact,
                typography = tokens.typography,
            )
        }
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = tokens.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevation.card),
        ) {
            ChampionRedesignSummaryCardContent(
                title = title,
                value = value,
                supportingText = supportingText,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                content = content,
                padding = tokens.spacing.card,
                compactSpacing = tokens.spacing.compact,
                typography = tokens.typography,
            )
        }
    }
}

@Composable
private fun ChampionRedesignSummaryCardContent(
    title: String,
    value: String,
    supportingText: String?,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    padding: Dp,
    compactSpacing: Dp,
    typography: Typography,
) {
    Column(modifier = Modifier.padding(padding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(compactSpacing),
        ) {
            leadingContent?.let { content -> content() }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = typography.labelLarge,
                )
                Text(
                    text = value,
                    style = typography.headlineSmall,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = typography.bodyMedium,
                    )
                }
            }
            trailingContent()
        }
        content()
    }
}
