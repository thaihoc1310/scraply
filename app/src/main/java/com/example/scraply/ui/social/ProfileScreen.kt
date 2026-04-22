package com.example.scraply.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.ui.vm.scraplyViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun ProfileScreen() {
    val vm: ProfileViewModel = scraplyViewModel()
    val authState by vm.authState.collectAsState()
    val profileState by vm.profile.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Profile", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            if (authState.isSignedIn) {
                IconButton(onClick = { vm.signOut() }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Sign out")
                }
            }
        }

        AuthGate(
            vm = vm,
            signedOutHeadline = "Your profile",
            signedOutSubtext = "Sign in to manage your profile and published scrapbooks.",
        ) {
            ProfileBody(
                profile = profileState.profile,
                myPosts = profileState.myPosts,
                saving = profileState.saving,
                editing = profileState.editing,
                message = profileState.message,
                onBeginEdit = { vm.beginEdit() },
                onCancelEdit = { vm.cancelEdit() },
                onSaveEdit = { dn, un, bio, path -> vm.saveProfile(dn, un, bio, path) },
                onDismissMessage = { vm.dismissMessage() },
                onDeletePost = { vm.deletePost(it) },
            )
        }
    }
}

@Composable
private fun ProfileBody(
    profile: ScraplyUser?,
    myPosts: List<FeedPost>,
    saving: Boolean,
    editing: Boolean,
    message: String?,
    onBeginEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (displayName: String, username: String, bio: String, newAvatarPath: String?) -> Unit,
    onDismissMessage: () -> Unit,
    onDeletePost: (FeedPost) -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 24.dp),
    ) {
        ProfileHeader(profile, myPostsCount = myPosts.size, onEdit = onBeginEdit)

        Spacer(Modifier.height(8.dp))
        Text(
            "Your posts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (myPosts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "You haven't published any scrapbooks yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MyPostsGrid(posts = myPosts, onDelete = onDeletePost)
        }
    }

    if (editing && profile != null) {
        EditProfileDialog(
            profile = profile,
            saving = saving,
            onDismiss = onCancelEdit,
            onSave = onSaveEdit,
        )
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

@Composable
private fun ProfileHeader(
    profile: ScraplyUser?,
    myPostsCount: Int,
    onEdit: () -> Unit,
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
                profile?.displayName ?: profile?.username ?: "Scraply user",
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
                Stat(count = myPostsCount.toLong(), label = "Posts")
                Stat(count = profile?.followerCount ?: 0, label = "Followers")
                Stat(count = profile?.followingCount ?: 0, label = "Following")
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

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit profile")
        }
    }
}

@Composable
private fun Stat(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MyPostsGrid(posts: List<FeedPost>, onDelete: (FeedPost) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height((((posts.size + 1) / 2) * 240).dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(posts, key = { it.id }) { post ->
            MyPostCard(post = post, onDelete = { onDelete(post) })
        }
    }
}

@Composable
private fun MyPostCard(post: FeedPost, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { showMenu = true }
            .padding(8.dp),
    ) {
        AsyncImage(
            model = post.imageUrl, contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22000000)),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("${post.likeCount}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("${post.commentCount}", style = MaterialTheme.typography.labelMedium)
        }
    }
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Post") },
            text = { Text("Remove this scrapbook from your published feed?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showMenu = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenu = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: ScraplyUser,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, bio: String, newAvatarPath: String?) -> Unit,
) {
    val ctx = LocalContext.current
    var displayName by remember { mutableStateOf(profile.displayName.orEmpty()) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pendingAvatarUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val model: Any? = pendingAvatarUri ?: profile.avatarUrl
                        if (model != null) {
                            AsyncImage(
                                model = model, contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .padding(4.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Tap to change photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(160) },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val localPath = pendingAvatarUri?.let { copyUriToCache(ctx, it) }
                    onSave(displayName, username, bio, localPath)
                },
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        },
    )
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
