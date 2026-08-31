package br.com.mwconsulting.championcompanion.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Immutable
data class ChampionNavigationItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
) {
    init {
        require(key.isNotBlank())
        require(label.isNotBlank())
    }
}

@Immutable
data class ChampionNavigationMenuItem(
    val key: String,
    val label: String,
    val onClick: () -> Unit,
) {
    init {
        require(key.isNotBlank())
        require(label.isNotBlank())
    }
}

/**
 * Shell visual sem estado: back stack, seleção e callbacks continuam sob
 * responsabilidade de [ChampionNavigation].
 */
@Composable
fun ChampionNavigationShell(
    primaryItems: List<ChampionNavigationItem>,
    moreItem: ChampionNavigationItem,
    moreItems: List<ChampionNavigationMenuItem>,
    moreMenuExpanded: Boolean,
    onMoreDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalChampionVisualTokens.current
    val colors = MaterialTheme.colorScheme
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.onPrimaryContainer,
        selectedTextColor = colors.primary,
        indicatorColor = colors.primaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )

    NavigationBar(
        modifier = modifier,
        containerColor = colors.surfaceContainer,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
    ) {
        val navigationBarScope = this
        primaryItems.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { androidx.compose.material3.Text(text = item.label) },
                colors = itemColors,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            navigationBarScope.NavigationBarItem(
                modifier = Modifier.fillMaxWidth(),
                selected = moreItem.selected,
                onClick = moreItem.onClick,
                icon = { Icon(imageVector = moreItem.icon, contentDescription = moreItem.label) },
                label = { androidx.compose.material3.Text(text = moreItem.label) },
                colors = itemColors,
            )
            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = onMoreDismissRequest,
                modifier = Modifier.align(Alignment.TopEnd),
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = colors.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                moreItems.forEach { item ->
                    DropdownMenuItem(
                        modifier = Modifier.heightIn(min = tokens.minimumTouchTarget),
                        text = { androidx.compose.material3.Text(text = item.label) },
                        onClick = {
                            onMoreDismissRequest()
                            item.onClick()
                        },
                    )
                }
            }
        }
    }
}
