package com.example.scraply.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.model.CanvasState
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.ui.vm.scraplyViewModel
import androidx.compose.ui.res.stringResource
import com.example.scraply.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) {
    val vm: UserProfileViewModel = scraplyViewModel()
    val state by vm.state.collectAsState()
    val authState by vm.authState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeCommentsPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeLikesPost by remember { mutableStateOf<FeedPost?>(null) }

    LaunchedEffect(userId) {
        vm.loadUser(userId)
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
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.calendar_back))
            }
            Text(
                text = state.profile?.displayName ?: state.profile?.username ?: stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }

        AuthGate(
            vm = vm,
            signedOutHeadline = stringResource(R.string.profile_signed_out_headline),
            signedOutSubtext = stringResource(R.string.profile_signed_out_subtext),
        ) {
            UserProfileBody(
                profile = state.profile,
                posts = state.posts,
                isPostsLoading = state.isPostsLoading,
                onOpenPost = onOpenPost,
                onLike = { vm.toggleLike(it) },
                onOpenComments = { activeCommentsPost = it },
                onOpenLikes = { activeLikesPost = it },
                currentUserId = authState.user?.uid,
            )
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
}

private val UserProfilePlaceholderRatios = listOf(1.18f, 0.82f, 0.96f, 1.3f, 0.86f, 1.08f)

@Composable
private fun UserProfileBody(
    profile: ScraplyUser?,
    posts: List<FeedPost>,
    isPostsLoading: Boolean,
    onOpenPost: (String) -> Unit,
    onLike: (FeedPost) -> Unit,
    onOpenComments: (FeedPost) -> Unit,
    onOpenLikes: (FeedPost) -> Unit,
    currentUserId: String?,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 6.dp,
            end = 6.dp,
            top = 12.dp,
            bottom = 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 6.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            UserProfileHeader(
                profile = profile,
                postsCount = posts.size.takeUnless { isPostsLoading && posts.isEmpty() },
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(12.dp))
        }

        if (isPostsLoading && posts.isEmpty()) {
            items(UserProfilePlaceholderRatios) { ratio ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                )
            }
        } else if (posts.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.profile_no_posts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                UserPostThumb(post = post, onClick = { onOpenPost(post.id) })
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    profile: ScraplyUser?,
    postsCount: Int?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (profile?.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        (profile?.displayName ?: profile?.username ?: "?").take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: profile?.username ?: stringResource(R.string.profile_default_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (!profile?.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile!!.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    postsCount?.toString() ?: "--",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.profile_posts),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UserPostThumb(post: FeedPost, onClick: () -> Unit) {
    val canvasState = remember(post.canvasJson) {
        CanvasState.fromJson(post.canvasJson)
    }
    val ratio = canvasState.aspectRatio

    AsyncImage(
        model = post.imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
    )
}
