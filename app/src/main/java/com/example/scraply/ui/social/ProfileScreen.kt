package com.example.scraply.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.model.CanvasState
import com.example.scraply.ui.common.ScraplyDialog
import com.example.scraply.ui.common.ScraplyDialogCancelButton
import com.example.scraply.ui.common.ScraplyDialogConfirmButton
import com.example.scraply.ui.common.ScraplyOutlinedTextField
import com.example.scraply.ui.common.ScraplyDropdownMenu
import com.example.scraply.ui.common.ScraplyDropdownMenuItem
import androidx.compose.ui.res.stringResource
import com.example.scraply.R
import com.example.scraply.ui.notifications.NotificationsViewModel
import com.example.scraply.ui.vm.scraplyViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun ProfileScreen(
    onOpenNotifications: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val vm: ProfileViewModel = scraplyViewModel()
    val notificationsVm: NotificationsViewModel = scraplyViewModel()
    val authState by vm.authState.collectAsState()
    val profileState by vm.profile.collectAsState()
    val notifState by notificationsVm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (authState.isSignedIn) {
                BellWithBadge(
                    unreadCount = notifState.unreadCount,
                    onClick = onOpenNotifications,
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.profile_settings))
                }
            }
        }

        AuthGate(
            vm = vm,
            signedOutHeadline = stringResource(R.string.profile_signed_out_headline),
            signedOutSubtext = stringResource(R.string.profile_signed_out_subtext),
        ) {
            ProfileBody(
                profile = profileState.profile,
                myPosts = profileState.myPosts,
                message = profileState.message,
                onDismissMessage = { vm.dismissMessage() },
                onOpenPost = { post -> onOpenPost(post.id) },
            )
        }
    }
}

@Composable
private fun ProfileBody(
    profile: ScraplyUser?,
    myPosts: List<FeedPost>,
    message: String?,
    onDismissMessage: () -> Unit,
    onOpenPost: (FeedPost) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 6.dp,
                end = 6.dp,
                top = 12.dp,
                bottom = 20.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalItemSpacing = 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ProfileHeader(profile, myPostsCount = myPosts.size)
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(Modifier.height(12.dp))
            }

            if (myPosts.isEmpty()) {
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
                items(myPosts, key = { it.id }) { post ->
                    MyPostThumb(post = post, onClick = { onOpenPost(post) })
                }
            }
        }
    }

    if (message != null) {
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            onDismissMessage()
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

// ProfileHeader remains here...

@Composable
private fun MyPostThumb(post: FeedPost, onClick: () -> Unit) {
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

@Composable
private fun ProfileHeader(
    profile: ScraplyUser?,
    myPostsCount: Int,
) {
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
            if (profile?.email != null) {
                Text(
                    profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(count = myPostsCount.toLong(), label = stringResource(R.string.profile_posts))
                Stat(count = profile?.followerCount ?: 0, label = stringResource(R.string.profile_followers))
                Stat(count = profile?.followingCount ?: 0, label = stringResource(R.string.profile_following))
            }
        }
    }

    if (!profile?.bio.isNullOrBlank()) {
        Text(
            profile!!.bio,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun Stat(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePostsFeedScreen(
    initialPostId: String,
    onBack: () -> Unit,
) {
    val vm: ProfileViewModel = scraplyViewModel()
    val profileState by vm.profile.collectAsState()
    val authState by vm.authState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeCommentsPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeLikesPost by remember { mutableStateOf<FeedPost?>(null) }

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
            Text(stringResource(R.string.profile_posts), style = MaterialTheme.typography.titleLarge)
        }

        AuthGate(
            vm = vm,
            signedOutHeadline = stringResource(R.string.profile_signed_out_headline),
            signedOutSubtext = stringResource(R.string.profile_signin_view_posts),
        ) {
            val posts = profileState.myPosts

            if (posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.profile_no_posts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val initialPostIndex = posts.indexOfFirst { it.id == initialPostId }
                    .takeIf { it >= 0 } ?: 0
                ProfilePostsFeedList(
                    posts = posts,
                    initialPostIndex = initialPostIndex,
                    currentUserId = authState.user?.uid,
                    onLike = { vm.toggleLike(it) },
                    onOpenComments = {
                        activeLikesPost = null
                        activeCommentsPost = it
                    },
                    onOpenLikes = {
                        activeCommentsPost = null
                        activeLikesPost = it
                    },
                    onEditPost = { post, title, description ->
                        vm.updatePostDetails(post, title, description)
                    },
                    onDeletePost = { vm.deletePost(it) },
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
}

@Composable
private fun ProfilePostsFeedList(
    posts: List<FeedPost>,
    initialPostIndex: Int,
    currentUserId: String?,
    onLike: (FeedPost) -> Unit,
    onOpenComments: (FeedPost) -> Unit,
    onOpenLikes: (FeedPost) -> Unit,
    onEditPost: (FeedPost, String, String) -> Unit,
    onDeletePost: (FeedPost) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPostIndex)

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(posts, key = { _, post -> post.id }) { _, post ->
            FeedCard(
                post = post,
                onLike = { onLike(post) },
                onOpenComments = { onOpenComments(post) },
                currentUserId = currentUserId,
                onEditPost = onEditPost,
                onDeletePost = onDeletePost,
                onOpenLikes = { onOpenLikes(post) },
            )
        }
    }
}

@Composable
private fun BellWithBadge(unreadCount: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notif_title))
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun copyUriToCache(ctx: android.content.Context, uri: Uri): String? {
    return runCatching {
        val dir = File(ctx.cacheDir, "avatars").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()
}
