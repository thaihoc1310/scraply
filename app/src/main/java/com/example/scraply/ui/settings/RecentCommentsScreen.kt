package com.example.scraply.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scraply.R
import com.example.scraply.data.remote.UserCommentGroup
import com.example.scraply.ui.vm.scraplyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCommentsScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: RecentCommentsViewModel = scraplyViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val currentUser = authState.user

    // Observe lifecycle events to refresh comments when screen is resumed (e.g. returning from post detail)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val listState = rememberLazyListState()

    // Lắng nghe sự kiện cuộn để kích hoạt Infinite Scroll tải thêm bình luận
    LaunchedEffect(listState, uiState.hasMore, uiState.isLoading) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (totalItems > 0 && lastVisibleIndex >= totalItems - 3) {
                    if (uiState.hasMore && !uiState.isLoading) {
                        viewModel.loadMoreComments()
                    }
                }
            }
    }

    val totalSelectedComments = uiState.selectedCommentIds.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recent_comments_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.calendar_back)
                        )
                    }
                },
                actions = {
                    if (uiState.commentGroups.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.setSelectionMode(!uiState.isSelectionMode)
                            }
                        ) {
                            Text(
                                text = if (uiState.isSelectionMode) {
                                    stringResource(R.string.recent_comments_cancel)
                                } else {
                                    stringResource(R.string.recent_comments_select)
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading && uiState.commentGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.commentGroups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.recent_comments_empty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.commentGroups, key = { it.postId }) { group ->
                        RecentCommentGroupCard(
                            group = group,
                            currentUserAvatarUrl = currentUser?.avatarUrl,
                            currentUsername = currentUser?.displayName ?: currentUser?.username ?: "giang11022",
                            isSelectionMode = uiState.isSelectionMode,
                            selectedCommentIds = uiState.selectedCommentIds,
                            onToggleSelect = { commentId ->
                                viewModel.toggleCommentSelection(commentId)
                            },
                            onOpenPost = onOpenPost
                        )
                    }

                    // Vòng xoay tải thêm phần tử dưới LazyColumn
                    if (uiState.isLoading && uiState.commentGroups.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // Thanh xóa hàng loạt dưới đáy
            AnimatedVisibility(
                visible = uiState.isSelectionMode && totalSelectedComments > 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.recent_comments_delete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.deleteSelectedComments() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.recent_comments_delete_btn,
                                    totalSelectedComments
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCommentGroupCard(
    group: UserCommentGroup,
    currentUserAvatarUrl: String?,
    currentUsername: String,
    isSelectionMode: Boolean,
    selectedCommentIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onOpenPost: (String) -> Unit,
) {
    val locale = Locale.getDefault()
    val isVietnamese = locale.language == "vi"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPost(group.postId) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Tác giả bài viết & thông tin scrapbook gốc ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (!group.postAuthorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = group.postAuthorAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = (group.postAuthorName ?: "A").take(1).uppercase(locale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.postAuthorName ?: stringResource(R.string.notif_someone),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = group.postTitle ?: stringResource(R.string.pinch_to_zoom),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!group.postImageUrl.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = group.postImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                    )
                }
            }

            // Đường chỉ đứng kết nối từ avatar tác giả xuống danh sách bình luận bên dưới
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                }
                Spacer(Modifier.width(12.dp))
                Spacer(Modifier.weight(1f))
            }

            // ── Danh sách bình luận thụt lề dưới dạng Thread Line ─────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                group.comments.forEachIndexed { index, comment ->
                    val isSelected = selectedCommentIds.contains(comment.commentId)
                    val isFirst = index == 0
                    val isLast = index == group.comments.lastIndex

                    RecentCommentSubRow(
                        comment = comment,
                        currentUserAvatarUrl = currentUserAvatarUrl,
                        currentUsername = currentUsername,
                        isFirst = isFirst,
                        isLast = isLast,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = { onToggleSelect(comment.commentId) },
                        isVietnamese = isVietnamese
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCommentSubRow(
    comment: com.example.scraply.data.remote.CommentSubItem,
    currentUserAvatarUrl: String?,
    currentUsername: String,
    isFirst: Boolean,
    isLast: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    isVietnamese: Boolean,
) {
    val locale = Locale.getDefault()
    val timeLabel = formatCommentTime(comment.createdAt, isVietnamese)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Cột bên trái: Thiết kế đường chỉ Thread Line chạy dọc động
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
        ) {
            // Đường chỉ trên nối từ avatar tác giả bài viết hoặc comment trước
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Avatar của bạn
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!currentUserAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentUserAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = currentUsername.take(1).uppercase(locale),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Đường chỉ dưới nối tiếp sang comment tiếp theo trong nhóm
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Cột bên phải: Tên bạn, thời gian, nội dung comment và checkbox chọn
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentUsername,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelectionMode) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Select comment",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun formatCommentTime(epochMs: Long, isVietnamese: Boolean): String {
    if (epochMs == 0L) return if (isVietnamese) "vừa xong" else "just now"
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> if (isVietnamese) "vừa xong" else "just now"
        diff < 3_600_000 -> "${diff / 60_000}${if (isVietnamese) "p" else "m"}"
        diff < 86_400_000 -> "${diff / 3_600_000}${if (isVietnamese) "h" else "h"}"
        diff < 7L * 86_400_000 -> "${diff / 86_400_000}${if (isVietnamese) "d" else "d"}"
        else -> "${diff / (7L * 86_400_000)}${if (isVietnamese) "t" else "w"}"
    }
}
