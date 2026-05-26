package com.example.scraply.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.remote.NotificationItem
import com.example.scraply.data.remote.NotificationType
import com.example.scraply.ui.vm.scraplyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenPost: (String, Boolean) -> Unit,
) {
    val vm: NotificationsViewModel = scraplyViewModel()
    val state by vm.state.collectAsState()

    fun handleClick(item: NotificationItem) {
        vm.markRead(item)
        val postId = item.postId
        if (postId.isNullOrBlank()) return
        val showComments = item.type == NotificationType.COMMENT
        onOpenPost(postId, showComments)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            if (state.unreadCount > 0) {
                IconButton(onClick = { vm.markAllRead() }) {
                    Icon(Icons.Filled.DoneAll, contentDescription = "Mark all read")
                }
            }
        }

        if (!state.isSignedIn) {
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = "No notifications yet",
                subtitle = "Sign in to see activity on your scrapbooks.",
            )
            return
        }

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = "You're all caught up",
                subtitle = "Likes, comments and new followers will show up here.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onClick = { handleClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val bgColor = if (item.read) MaterialTheme.colorScheme.surface
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.actorAvatar != null) {
                AsyncImage(
                    model = item.actorAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(iconFor(item.type), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Icon(
                iconFor(item.type),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(tintFor(item.type), CircleShape)
                    .padding(3.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    item.actorName ?: "Someone",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    item.text ?: defaultText(item.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatTime(item.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.postImageUrl != null) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = item.postImageUrl,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22000000)),
            )
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun iconFor(type: String): ImageVector = when (type) {
    NotificationType.LIKE -> Icons.Filled.Favorite
    NotificationType.COMMENT -> Icons.Filled.ChatBubbleOutline
    NotificationType.FOLLOW -> Icons.Filled.PersonAdd
    else -> Icons.Filled.Notifications
}

@Composable
private fun tintFor(type: String): Color = when (type) {
    NotificationType.LIKE -> MaterialTheme.colorScheme.error
    NotificationType.COMMENT -> MaterialTheme.colorScheme.tertiary
    NotificationType.FOLLOW -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.secondary
}

private fun defaultText(type: String): String = when (type) {
    NotificationType.LIKE -> "liked your scrapbook"
    NotificationType.COMMENT -> "left a comment"
    NotificationType.FOLLOW -> "started following you"
    else -> "sent you a notification"
}

private fun formatTime(epochMs: Long): String {
    if (epochMs == 0L) return "just now"
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 7L * 86_400_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
