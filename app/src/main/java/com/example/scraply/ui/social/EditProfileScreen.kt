package com.example.scraply.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scraply.R
import com.example.scraply.ui.common.ScraplyOutlinedTextField
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel,
) {
    val profileState by viewModel.profile.collectAsState()
    val ctx = LocalContext.current

    val user = profileState.profile
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var displayName by remember { mutableStateOf(user.displayName.orEmpty()) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pendingAvatarUri = uri }

    // Tự động đóng màn hình khi lưu thành công
    LaunchedEffect(profileState.message) {
        if (profileState.message == "Profile updated.") {
            onBack()
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_profile_title),
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
                    TextButton(
                        enabled = !profileState.saving,
                        onClick = {
                            val localPath = pendingAvatarUri?.let { copyUriToCache(ctx, it) }
                            viewModel.saveProfile(displayName, username, bio, localPath)
                        }
                    ) {
                        if (profileState.saving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.edit_profile_save),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (profileState.saving) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Avatar Area ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val model: Any? = pendingAvatarUri ?: user.avatarUrl
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (displayName.takeIf { it.isNotBlank() } ?: username).take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Camera Overlay Icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                )
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = stringResource(R.string.edit_profile_avatar_hint),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp, end = 4.dp)
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(5.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.edit_profile_avatar_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(28.dp))

            // ── Display Name ─────────────────────────────────────────
            ScraplyOutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = stringResource(R.string.edit_profile_display_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Username ─────────────────────────────────────────────
            ScraplyOutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.edit_profile_username),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Bio ──────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                ScraplyOutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(160) },
                    label = stringResource(R.string.edit_profile_bio),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${bio.length}/160",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
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
