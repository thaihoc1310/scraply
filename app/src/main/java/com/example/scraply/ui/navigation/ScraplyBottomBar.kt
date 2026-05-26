package com.example.scraply.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.scraply.R

@Composable
fun ScraplyBottomBar(
    current: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = Color.Transparent,
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { onSelect(tab) },
                icon = {
                    BottomTabIcon(tab = tab)
                },
                colors = colors,
                alwaysShowLabel = false,
            )
        }
    }
}

@Composable
private fun BottomTabIcon(tab: BottomTab) {
    if (tab == BottomTab.STAMP) {
        Icon(
            painter = painterResource(R.drawable.seal_fill_icon),
            contentDescription = tab.label,
            modifier = Modifier.size(24.dp),
        )
    } else {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val BottomTab.icon: ImageVector
    get() = when (this) {
        BottomTab.STAMP -> Icons.Filled.Explore
        BottomTab.COLLECTIONS -> Icons.AutoMirrored.Filled.MenuBook
        BottomTab.EDITOR -> Icons.Filled.Edit
        BottomTab.FEED -> Icons.Filled.Explore
        BottomTab.PROFILE -> Icons.Filled.Person
    }
