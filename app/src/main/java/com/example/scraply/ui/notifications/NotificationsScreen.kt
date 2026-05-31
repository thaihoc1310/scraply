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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.remote.NotificationItem
import com.example.scraply.data.remote.NotificationType
import com.example.scraply.ui.vm.scraplyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.example.scraply.R

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenPost: (String, Boolean) -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
) {
    val vm: NotificationsViewModel = scraplyViewModel()
    val state by vm.state.collectAsState()

    fun handleClick(item: NotificationItem) {
        vm.markRead(item)
        // FOLLOW notifications navigate to the actor's profile
        if (item.type == NotificationType.FOLLOW && !item.actorId.isNullOrBlank()) {
            onOpenUserProfile(item.actorId)
            return
        }
        val postId = item.postId
        if (postId.isNullOrBlank()) return
        val showComments = item.type == NotificationType.COMMENT
        onOpenPost(postId, showComments)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.notif_back))
            }
            Text(
                stringResource(R.string.notif_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            if (state.unreadCount > 0) {
                IconButton(onClick = { vm.markAllRead() }) {
                    Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.notif_mark_all_read))
                }
            }
        }

        if (!state.isSignedIn) {
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.notif_empty_signed_out_title),
                subtitle = stringResource(R.string.notif_empty_signed_out_subtitle),
            )
            return
        }

        if (state.items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.notif_empty_title),
                subtitle = stringResource(R.string.notif_empty_subtitle),
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
        val showActorAvatar = item.type == NotificationType.LIKE || item.type == NotificationType.COMMENT
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (showActorAvatar) {
                    if (item.actorAvatar != null) {
                        AsyncImage(
                            model = item.actorAvatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            (item.actorName ?: "?").take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Icon(iconFor(item.type), contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            if (showActorAvatar) {
                Icon(
                    iconFor(item.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .background(tintFor(item.type), CircleShape)
                        .padding(3.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val displayNotificationText = when {
                item.type == NotificationType.LIKE || item.text == "liked your scrapbook" -> {
                    stringResource(R.string.notif_liked_your_scrapbook)
                }
                item.type == NotificationType.FOLLOW || item.text == "started following you" -> {
                    stringResource(R.string.notif_started_following)
                }
                item.type == NotificationType.COMMENT || item.text?.startsWith("commented:") == true -> {
                    val commentContent = item.text?.removePrefix("commented:")?.trim() ?: ""
                    if (commentContent.isNotEmpty()) {
                        stringResource(R.string.notif_left_comment) + ": $commentContent"
                    } else {
                        stringResource(R.string.notif_left_comment)
                    }
                }
                else -> item.text ?: defaultText(item.type)
            }
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(item.actorName ?: stringResource(R.string.notif_someone))
                }
                append(" ")
                append(displayNotificationText)
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun defaultText(type: String): String = when (type) {
    NotificationType.LIKE -> stringResource(R.string.notif_liked_your_scrapbook)
    NotificationType.COMMENT -> stringResource(R.string.notif_left_comment)
    NotificationType.FOLLOW -> stringResource(R.string.notif_started_following)
    else -> stringResource(R.string.notif_generic)
}

@Composable
private fun formatTime(epochMs: Long): String {
    val justNow = stringResource(R.string.time_just_now)
    if (epochMs == 0L) return justNow
    val diff = System.currentTimeMillis() - epochMs
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return when {
        diff < 60_000 -> justNow
        diff < 3_600_000 -> stringResource(R.string.time_minutes_ago, diff / 60_000)
        diff < 86_400_000 -> stringResource(R.string.time_hours_ago, diff / 3_600_000)
        diff < 7L * 86_400_000 -> stringResource(R.string.time_days_ago, diff / 86_400_000)
        else -> {
            val pattern = if (locale.language == "vi") "d 'thg' M" else "MMM d"
            SimpleDateFormat(pattern, locale).format(Date(epochMs))
        }
    }
}
