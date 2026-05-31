package com.example.scraply.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.scraply.R

@Composable
fun ScraplyBottomBar(
    current: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp + navBarInsets.calculateBottomPadding())
                .padding(horizontal = 8.dp)
                .padding(bottom = navBarInsets.calculateBottomPadding()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                CompactNavItem(
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                ) {
                    BottomTabIcon(tab = tab, selected = tab == current)
                }
            }
        }
    }
}

/**
 * A compact navigation item that always shows a pill-shaped background
 * when selected (Google Play style). The ripple/touch area is confined
 * to the small pill shape, not the entire cell.
 */
@Composable
private fun CompactNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val pillShape = RoundedCornerShape(50)

    // Always show background for selected item
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "navItemBg",
    )

    // Center the pill within the cell, but only the pill is clickable
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(backgroundColor, pillShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    role = Role.Tab,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun BottomTabIcon(tab: BottomTab, selected: Boolean) {
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "navIconTint",
    )

    if (tab == BottomTab.STAMP) {
        Icon(
            painter = painterResource(R.drawable.seal_fill_icon),
            contentDescription = stringResource(tab.labelResId),
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
    } else {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.labelResId),
            modifier = Modifier.size(24.dp),
            tint = tint,
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
