package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null,
) {
    FeedbackState(
        title = title,
        message = message,
        modifier = modifier,
        icon = "○",
        iconContent = iconContent,
    )
}

/** Error text must be a safe, user-facing message supplied by caller. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    FeedbackState(
        title = title,
        message = message,
        modifier = modifier,
        icon = "!",
        iconContent = iconContent,
        actionLabel = actionLabel,
        onAction = onAction,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
fun LoadingState(
    label: String,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(label, message).joinToString(", ")
                liveRegion = LiveRegionMode.Polite
            }
            .padding(ChampionSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
    ) {
        CircularProgressIndicator()
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun PermissionBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconContent: @Composable (() -> Unit)? = null,
) {
    InformationBanner(
        title = title,
        message = message,
        modifier = modifier,
        icon = "i",
        actionLabel = actionLabel,
        onAction = onAction,
        iconContent = iconContent,
    )
}

@Composable
fun PendingReviewBanner(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Revisão pendente",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconContent: @Composable (() -> Unit)? = null,
) {
    InformationBanner(
        title = title,
        message = message,
        modifier = modifier,
        icon = "!",
        actionLabel = actionLabel,
        onAction = onAction,
        iconContent = iconContent,
    )
}

@Composable
fun CalibrationBanner(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Calibração necessária",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconContent: @Composable (() -> Unit)? = null,
) {
    InformationBanner(
        title = title,
        message = message,
        modifier = modifier,
        icon = "↔",
        actionLabel = actionLabel,
        onAction = onAction,
        iconContent = iconContent,
    )
}

@Composable
private fun FeedbackState(
    title: String,
    message: String,
    modifier: Modifier,
    icon: String,
    iconContent: @Composable (() -> Unit)?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title. $message" },
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = ChampionShapeLarge,
    ) {
        Column(
            modifier = Modifier.padding(ChampionSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ChampionSpacing.small),
        ) {
            iconContent?.invoke() ?: Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun InformationBanner(
    title: String,
    message: String,
    modifier: Modifier,
    icon: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    iconContent: @Composable (() -> Unit)?,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Aviso: $title. $message" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = ChampionShapeMedium,
    ) {
        Row(
            modifier = Modifier.padding(ChampionSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(ChampionSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            iconContent?.invoke() ?: Text(text = icon, style = MaterialTheme.typography.titleLarge)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ChampionSpacing.extraSmall),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                if (actionLabel != null && onAction != null) {
                    Button(onClick = onAction) { Text(actionLabel) }
                }
            }
        }
    }
}
