package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@Composable
fun HealthCard(
    title: String,
    modifier: Modifier = Modifier,
    status: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = ChampionShapeLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = ChampionElevation.card),
    ) {
        Column(modifier = Modifier.padding(ChampionSpacing.large)) {
            Row {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                status?.invoke()
            }
            content()
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    status: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(label, value, supportingText).joinToString(", ")
        },
        shape = ChampionShapeMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = ChampionElevation.card),
    ) {
        Column(modifier = Modifier.padding(ChampionSpacing.large)) {
            Row {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                )
                status?.invoke()
            }
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            supportingText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
