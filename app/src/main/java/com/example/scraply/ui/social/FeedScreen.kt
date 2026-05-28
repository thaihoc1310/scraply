package com.example.scraply.ui.social

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import com.example.scraply.data.remote.FeedComment
import com.example.scraply.data.remote.FeedLikeUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.model.CanvasState
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.ScraplyDialog
import com.example.scraply.ui.common.ScraplyDialogCancelButton
import com.example.scraply.ui.common.ScraplyDialogConfirmButton
import com.example.scraply.ui.common.ScraplyDropdownMenu
import com.example.scraply.ui.common.ScraplyDropdownMenuItem
import com.example.scraply.ui.common.ScraplyOutlinedTextField
import com.example.scraply.ui.vm.scraplyViewModel
import com.example.scraply.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenPost: (String) -> Unit = {},
) {
    val vm: FeedViewModel = scraplyViewModel()
    val feedState by vm.feed.collectAsState()
    val authState by vm.authState.collectAsState()
    var activeCommentsPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeLikesPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeOptionsPost by remember { mutableStateOf<FeedPost?>(null) }
    var isDownloadingOption by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Feed",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))

        AuthGate(
            vm = vm,
            signedOutHeadline = "Welcome back",
            signedOutSubtext = "Sign in to explore scrapbooks from the community.",
        ) {
            if (feedState.isLoading && feedState.feed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                FeedList(
                    feed = feedState.feed,
                    onOpenPost = onOpenPost,
                    onMoreClick = { activeOptionsPost = it },
                )
            }
        }
    }

    val commentsPost = activeCommentsPost
    if (commentsPost != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { activeCommentsPost = null },
        ) {
            CommentsPanel(
                postId = commentsPost.id,
                onBack = null,
                showTopBar = false,
                header = null,
                modifier = Modifier.fillMaxHeight(0.6f),
            )
        }
    }

    val likesPost = activeLikesPost
    if (likesPost != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { activeLikesPost = null },
        ) {
            LikesPanel(
                postId = likesPost.id,
                showTopBar = false,
                modifier = Modifier.fillMaxHeight(0.6f),
            )
        }
    }

    val optionsPost = activeOptionsPost
    if (optionsPost != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { 
                if (!isDownloadingOption) {
                    activeOptionsPost = null
                }
            },
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            scrimColor = Color.Black.copy(alpha = 0.35f),
            dragHandle = null
        ) {
            PostOptionsPanel(
                post = optionsPost,
                onDismiss = { activeOptionsPost = null },
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this beautiful scrapbook on Scraply: ${optionsPost.imageUrl}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share scrapbook image")
                    context.startActivity(shareIntent)
                    activeOptionsPost = null
                },
                onDownload = {
                    isDownloadingOption = true
                    scope.launch {
                        val saved = savePostImageToGallery(context, optionsPost)
                        isDownloadingOption = false
                        Toast.makeText(
                            context,
                            if (saved) "Saved to Pictures/Scraply" else "Could not save image",
                            Toast.LENGTH_SHORT
                        ).show()
                        activeOptionsPost = null
                    }
                },
                isDownloading = isDownloadingOption
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPostScreen(
    postId: String,
    showComments: Boolean,
    onBack: () -> Unit,
) {
    val vm: FeedViewModel = scraplyViewModel()
    val feedState by vm.feed.collectAsState()
    val authState by vm.authState.collectAsState()
    val post = feedState.feed.firstOrNull { it.id == postId }
    var isCommentsVisible by remember { mutableStateOf(showComments) }
    var isLikesVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Text("Post", style = MaterialTheme.typography.titleLarge)
        }

        AuthGate(
            vm = vm,
            signedOutHeadline = "Welcome back",
            signedOutSubtext = "Sign in to explore scrapbooks from the community.",
        ) {
            when {
                feedState.isLoading && feedState.feed.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                post == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Post not found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(post.id) {
                            FeedCard(
                                post,
                                onLike = { vm.toggleLike(post) },
                                onOpenComments = {
                                    isLikesVisible = false
                                    isCommentsVisible = true
                                },
                                currentUserId = authState.user?.uid,
                                onEditPost = { item, title, description ->
                                    vm.updatePostDetails(item, title, description)
                                },
                                onDeletePost = {
                                    vm.deletePost(it)
                                    onBack()
                                },
                                onOpenLikes = {
                                    isCommentsVisible = false
                                    isLikesVisible = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (isCommentsVisible && post != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { isCommentsVisible = false },
        ) {
            CommentsPanel(
                postId = post.id,
                onBack = null,
                showTopBar = false,
                header = null,
                modifier = Modifier.fillMaxHeight(0.6f),
            )
        }
    }

    if (isLikesVisible && post != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { isLikesVisible = false },
        ) {
            LikesPanel(
                postId = post.id,
                showTopBar = false,
                modifier = Modifier.fillMaxHeight(0.6f),
            )
        }
    }
}

@Composable
private fun PinterestFeedCard(
    post: FeedPost,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val canvasState = remember(post.canvasJson) {
        CanvasState.fromJson(post.canvasJson)
    }
    val ratio = canvasState.aspectRatio

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Image matching the real canvas ratio - naked, beautifully clipped, clickable!
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
        )

        // Three-dot options menu aligned bottom-right under the image (highly compact)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedList(
    feed: List<FeedPost>,
    onOpenPost: (String) -> Unit,
    onMoreClick: (FeedPost) -> Unit,
) {
    if (feed.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No posts yet. Publish a scrapbook to start the feed!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 6.dp,
            end = 6.dp,
            top = 10.dp,
            bottom = 140.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 4.dp, // Sát nhau hơn giữa ảnh trên và ảnh dưới
        modifier = Modifier.fillMaxSize()
    ) {
        items(feed, key = { it.id }) { post ->
            PinterestFeedCard(
                post = post,
                onClick = { onOpenPost(post.id) },
                onMoreClick = { onMoreClick(post) }
            )
        }
    }
}

@Composable
private fun PostOptionsPanel(
    post: FeedPost,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    isDownloading: Boolean,
) {
    val canvasState = remember(post.canvasJson) {
        CanvasState.fromJson(post.canvasJson)
    }
    val ratio = canvasState.aspectRatio

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // 1. The Sheet Body Column (starts 80.dp down to make space for the floating preview)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(bottom = 40.dp)
        ) {
            // Drag handle at the top center of the sheet body
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterHorizontally)
            )

            // Close button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(54.dp)) // Leave space for the floating, overlapping image

            // Scrapbook author subtext
            Text(
                text = "Scrapbook created by ${post.username ?: "someone"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Option 1: Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDownloading, onClick = onShare)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Share",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Option 2: Download Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDownloading, onClick = onDownload)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download image",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (isDownloading) "Downloading..." else "Download image",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 2. The Overlapping Image (floats at the top center, popping out of the sheet!)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(160.dp)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun FeedCard(
    post: FeedPost,
    onLike: () -> Unit,
    onOpenComments: () -> Unit,
    currentUserId: String? = null,
    onEditPost: ((FeedPost, String, String) -> Unit)? = null,
    onDeletePost: ((FeedPost) -> Unit)? = null,
    onOpenLikes: (() -> Unit)? = null,
) {
    val canvasState = remember(post.canvasJson) {
        CanvasState.fromJson(post.canvasJson)
    }
    val ratio = canvasState.aspectRatio

    var showPostMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    val canManagePost = currentUserId != null &&
        currentUserId == post.userId &&
        onEditPost != null &&
        onDeletePost != null

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (post.avatarUrl != null) {
                    AsyncImage(
                        model = post.avatarUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        (post.username ?: "?").take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                post.username ?: post.userId.take(6),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            if (canManagePost) {
                Box {
                    IconButton(
                        onClick = { showPostMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Post options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    ScraplyDropdownMenu(
                        expanded = showPostMenu,
                        onDismissRequest = { showPostMenu = false },
                    ) {
                        ScraplyDropdownMenuItem(
                            label = "Edit",
                            icon = Icons.Filled.Edit,
                            onClick = {
                                showPostMenu = false
                                showEditDialog = true
                            },
                        )
                        ScraplyDropdownMenuItem(
                            label = "Delete",
                            icon = Icons.Filled.Delete,
                            destructive = true,
                            onClick = {
                                showPostMenu = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showImageViewer = true },
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike) {
                Icon(
                    if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.likedByMe) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "${post.likeCount}",
                fontWeight = FontWeight.SemiBold,
                modifier = if (onOpenLikes != null) {
                    Modifier.clickable(onClick = onOpenLikes)
                } else {
                    Modifier
                },
            )
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onOpenComments) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comments")
            }
            Spacer(Modifier.width(4.dp))
            Text("${post.commentCount}")
        }
        if (!post.title.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        if (!post.description.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        CommentPreviewSection(
            commentCount = post.commentCount,
            comments = post.previewComments,
            onOpenComments = onOpenComments,
        )
    }

    if (showEditDialog && onEditPost != null) {
        EditPostDialog(
            post = post,
            onDismiss = { showEditDialog = false },
            onSave = { title, description ->
                onEditPost(post, title, description)
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog && onDeletePost != null) {
        ScraplyDialog(
            title = "Delete post",
            onDismissRequest = { showDeleteDialog = false },
            content = {
                Text(
                    "Remove this scrapbook from your published feed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                ScraplyDialogCancelButton(onClick = { showDeleteDialog = false })
                Spacer(Modifier.width(8.dp))
                ScraplyDialogConfirmButton(
                    label = "Delete",
                    destructive = true,
                    onClick = {
                        onDeletePost(post)
                        showDeleteDialog = false
                    },
                )
            },
        )
    }

    if (showImageViewer) {
        PostImageViewer(
            post = post,
            onDismiss = { showImageViewer = false },
        )
    }
}

@Composable
private fun EditPostDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember(post.id) { mutableStateOf(post.title.orEmpty()) }
    var description by remember(post.id) { mutableStateOf(post.description.orEmpty()) }

    ScraplyDialog(
        title = "Edit post",
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScraplyOutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(1000) },
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ScraplyOutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(1000) },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
            }
        },
        actions = {
            ScraplyDialogCancelButton(onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            ScraplyDialogConfirmButton(label = "Save", onClick = { onSave(title, description) })
        },
    )
}

@Composable
private fun PostImageViewer(
    post: FeedPost,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val overlayInteraction = remember { MutableInteractionSource() }
    val imageInteraction = remember { MutableInteractionSource() }
    var imageAspect by remember(post.imageUrl) { mutableStateOf<Float?>(null) }
    var saving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(
                    interactionSource = overlayInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                val aspect = imageAspect
                val fittedModifier = if (aspect != null && aspect > 0f) {
                    val containerAspect = maxWidth.value / maxHeight.value
                    if (aspect > containerAspect) {
                        Modifier.fillMaxWidth().height(maxWidth / aspect)
                    } else {
                        Modifier.height(maxHeight).width(maxHeight * aspect)
                    }
                } else {
                    Modifier.fillMaxSize()
                }

                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val size = state.painter.intrinsicSize
                        if (size.width > 0f && size.height > 0f) {
                            imageAspect = size.width / size.height
                        }
                    },
                    modifier = fittedModifier.clickable(
                        interactionSource = imageInteraction,
                        indication = null,
                        onClick = {},
                    ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close image",
                    onClick = onDismiss,
                    background = Color.Black.copy(alpha = 0.55f),
                    tint = Color.White,
                )
                Spacer(Modifier.weight(1f))
                if (saving) {
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    CircleIconButton(
                        icon = Icons.Filled.Download,
                        contentDescription = "Save image",
                        onClick = {
                            saving = true
                            scope.launch {
                                val saved = savePostImageToGallery(context, post)
                                saving = false
                                Toast.makeText(
                                    context,
                                    if (saved) "Saved to Pictures/Scraply" else "Could not save image",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        background = Color.Black.copy(alpha = 0.55f),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

private suspend fun savePostImageToGallery(context: Context, post: FeedPost): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(post.imageUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching false
            val bitmap = result.drawable.toBitmap()
            val safeName = "scraply_post_${post.id.take(12).ifBlank { System.currentTimeMillis().toString() }}"
            ImageUtils.saveToGallery(context, bitmap, safeName) != null
        }.getOrDefault(false)
    }

@Composable
private fun CommentPreviewSection(
    commentCount: Long,
    comments: List<FeedComment>,
    onOpenComments: () -> Unit,
) {
    val visibleComments = comments.filter { it.text.isNotBlank() }.take(2)
    if (commentCount <= 0 && visibleComments.isEmpty()) return

    Spacer(Modifier.height(6.dp))
    if (commentCount > visibleComments.size) {
        Text(
            text = if (commentCount == 1L) "View 1 comment" else "View all $commentCount comments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onOpenComments),
        )
        if (visibleComments.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
        }
    }
    visibleComments.forEach { comment ->
        CommentPreviewRow(comment = comment, onClick = onOpenComments)
    }
}

@Composable
private fun CommentPreviewRow(
    comment: FeedComment,
    onClick: () -> Unit,
) {
    val username = comment.username?.takeIf { it.isNotBlank() }
        ?: comment.userId.take(6).ifBlank { "Someone" }

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(username)
            }
            append(" ")
            append(comment.text)
        },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
fun LikesPanel(
    postId: String,
    showTopBar: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val vm: LikesViewModel = scraplyViewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(postId) {
        vm.setPostId(postId)
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    "Likes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.likes.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No likes yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.likes, key = { it.userId }) { like ->
                            LikeUserRow(like = like)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikeUserRow(like: FeedLikeUser) {
    val name = like.displayName?.takeIf { it.isNotBlank() }
        ?: like.username?.takeIf { it.isNotBlank() }
        ?: like.userId.take(6)

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (like.avatarUrl != null) {
                AsyncImage(
                    model = like.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!like.username.isNullOrBlank() && like.username != name) {
                Spacer(Modifier.height(2.dp))
                Text(
                    like.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
