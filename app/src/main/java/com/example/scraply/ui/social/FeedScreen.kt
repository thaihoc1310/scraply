package com.example.scraply.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.ui.vm.scraplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    val vm: FeedViewModel = scraplyViewModel()
    val feedState by vm.feed.collectAsState()
    var activePost by remember { mutableStateOf<FeedPost?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Explore, contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            Text("Feed", style = MaterialTheme.typography.titleLarge)
        }

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
                    onLike = { vm.toggleLike(it) },
                    onOpenComments = { post -> activePost = post },
                )
            }
        }
    }

    val post = activePost
    if (post != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { activePost = null },
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
    val post = feedState.feed.firstOrNull { it.id == postId }
    var isCommentsVisible by remember { mutableStateOf(showComments) }
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
                                onOpenComments = { isCommentsVisible = true },
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
}

@Composable
private fun FeedList(
    feed: List<FeedPost>,
    onLike: (FeedPost) -> Unit,
    onOpenComments: (FeedPost) -> Unit,
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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(feed, key = { it.id }) { post ->
            FeedCard(
                post,
                onLike = { onLike(post) },
                onOpenComments = { onOpenComments(post) },
            )
        }
    }
}

@Composable
private fun FeedCard(
    post: FeedPost,
    onLike: () -> Unit,
    onOpenComments: () -> Unit,
) {
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
        }
        Spacer(Modifier.height(10.dp))
        AsyncImage(
            model = post.imageUrl, contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.8f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22000000)),
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
            Text("${post.likeCount}")
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
    }
}
