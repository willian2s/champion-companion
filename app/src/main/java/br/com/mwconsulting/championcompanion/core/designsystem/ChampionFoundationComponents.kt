package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Opta pela fundação visual em uma subárvore sem alterar tema raiz ou telas
 * ainda não migradas.
 */
@Composable
fun ChampionVisualFoundation(
    tokens: ChampionVisualTokens = DefaultChampionVisualTokens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalChampionVisualTokens provides tokens, content = content)
}

/** Cabeçalho editorial neutro, com slot de ações e retorno acessível opcional. */
@Composable
fun ChampionScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    backContentDescription: String = "Voltar",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = LocalChampionVisualTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics {
                        contentDescription = backContentDescription
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
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
    }
}

/** Linha de lista neutra; estado e formatação chegam prontos pelo chamador. */
@Composable
fun ChampionListItem(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    accessibilityLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = LocalChampionVisualTokens.current
    val itemModifier = modifier
        .fillMaxWidth()
        .heightIn(min = tokens.minimumTouchTarget)
        .then(
            if (onClick != null) {
                Modifier.semantics { role = Role.Button }
            } else {
                Modifier
            },
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
            } else {
                Modifier
            },
        )
        .then(
            if (accessibilityLabel != null) {
                Modifier.semantics { contentDescription = accessibilityLabel }
            } else {
                Modifier
            },
        )

    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
    ) {
        leadingContent?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        trailingContent?.invoke()
    }
}

/** Chip de escolha sem estado interno e com semântica explícita de seleção. */
@Composable
fun ChampionFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
    stateDescription: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalChampionVisualTokens.current
    FilterChip(
        selected = selected,
        onClick = { if (enabled) onClick() },
        label = { Text(text = label, maxLines = 1) },
        modifier = modifier
            .heightIn(min = tokens.minimumTouchTarget)
            .then(
                if (accessibilityLabel != null || stateDescription != null) {
                    Modifier.semantics {
                        accessibilityLabel?.let { contentDescription = it }
                        this.stateDescription = stateDescription
                            ?: if (selected) "Selecionado" else "Não selecionado"
                    }
                } else {
                    Modifier
                },
            ),
        enabled = enabled,
        leadingIcon = leadingContent,
    )
}

/** Vocabulário visual comum; o mapeamento para queries fica nas features. */
enum class ChampionPeriod(
    val code: String,
    val accessibleLabel: String,
) {
    DAY("D", "Dia"),
    WEEK("S", "Semana"),
    MONTH("M", "Mês"),
    THREE_MONTHS("3M", "3 meses"),
    YEAR("A", "Ano"),
}

@Immutable
data class ChampionPeriodOption(
    val period: ChampionPeriod,
    val enabled: Boolean = true,
    val visible: Boolean = true,
)

/**
 * Gera opções sem suporte ocultas por padrão. Quando exibidas, ficam
 * desabilitadas e nunca chamam [onSelected].
 */
fun championPeriodOptions(
    supported: Set<ChampionPeriod>,
    showUnsupported: Boolean = false,
): List<ChampionPeriodOption> = ChampionPeriod.entries.map { period ->
    val isSupported = period in supported
    ChampionPeriodOption(
        period = period,
        enabled = isSupported,
        visible = isSupported || showUnsupported,
    )
}

/** Seletor de período que só expõe callbacks para opções habilitadas. */
@Composable
fun ChampionPeriodSelector(
    selected: ChampionPeriod?,
    options: List<ChampionPeriodOption>,
    onSelected: (ChampionPeriod) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Período",
) {
    val tokens = LocalChampionVisualTokens.current
    val visibleOptions = options.filter { it.visible }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        if (visibleOptions.isEmpty()) {
            Text(
                text = "Nenhum período disponível",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
                verticalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
            ) {
                visibleOptions.forEach { option ->
                    ChampionFilterChip(
                        label = option.period.code,
                        selected = option.period == selected,
                        enabled = option.enabled,
                        onClick = { onSelected(option.period) },
                        accessibilityLabel = "${option.period.code}, ${option.period.accessibleLabel}",
                        stateDescription = when {
                            !option.enabled -> "Indisponível"
                            option.period == selected -> "Selecionado"
                            else -> "Não selecionado"
                        },
                    )
                }
            }
        }
    }
}

/** Estados de apresentação, independentes de workflow ou domínio. */
enum class ChampionContentState(
    val accessibleLabel: String,
) {
    LOADING("Carregando"),
    EMPTY("Sem dados"),
    ERROR("Erro"),
    UNAVAILABLE("Indisponível"),
    PENDING("Pendente"),
    SUCCESS("Concluído"),
}

@Composable
fun ChampionStateCard(
    state: ChampionContentState,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconContent: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalChampionVisualTokens.current
    val colors = state.colors()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $message"
                this.stateDescription = state.accessibleLabel
                if (state == ChampionContentState.LOADING) {
                    liveRegion = LiveRegionMode.Polite
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.content,
        ),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(tokens.componentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.controlSpacing),
        ) {
            iconContent?.invoke() ?: Text(
                text = state.glyph(),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

/** Diálogo neutro com callbacks explícitos e sem estado interno. */
@Composable
fun ChampionConfirmationDialog(
    title: String,
    message: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false,
) {
    val resolvedDismissLabel = dismissLabel
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = message?.let { { Text(text = it) } },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = resolvedDismissLabel?.let { label ->
            {
                TextButton(onClick = onDismiss ?: onDismissRequest) {
                    Text(text = label)
                }
            }
        },
    )
}

private data class ChampionStateColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun ChampionContentState.colors(): ChampionStateColors {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        ChampionContentState.LOADING,
        ChampionContentState.EMPTY,
        ChampionContentState.UNAVAILABLE,
        -> ChampionStateColors(colors.surfaceContainerLow, colors.onSurface)

        ChampionContentState.ERROR -> ChampionStateColors(colors.errorContainer, colors.onErrorContainer)
        ChampionContentState.PENDING -> ChampionStateColors(colors.tertiaryContainer, colors.onTertiaryContainer)
        ChampionContentState.SUCCESS -> ChampionStateColors(colors.primaryContainer, colors.onPrimaryContainer)
    }
}

private fun ChampionContentState.glyph(): String = when (this) {
    ChampionContentState.LOADING -> "…"
    ChampionContentState.EMPTY -> "○"
    ChampionContentState.ERROR -> "!"
    ChampionContentState.UNAVAILABLE -> "–"
    ChampionContentState.PENDING -> "!"
    ChampionContentState.SUCCESS -> "✓"
}
