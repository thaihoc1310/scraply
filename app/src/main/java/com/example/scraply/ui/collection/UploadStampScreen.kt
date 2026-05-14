package com.example.scraply.ui.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.scraply.ui.stamp.StampFrameOverlay
import com.example.scraply.util.ImageUtils
import com.example.scraply.util.PostageStampShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UploadStampScreen(
    onClose: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        source = uri
        offsetX = 0f; offsetY = 0f; scale = 1f
    }

    LaunchedEffect(source) {
        val s = source
        bitmap = if (s == null) null else withContext(Dispatchers.IO) {
            ImageUtils.loadBitmap(context, s)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onClose,
                shape = RoundedCornerShape(18.dp),
            ) { Text("Close") }
            Spacer(Modifier.weight(1f))
            Text(
                "Upload Stamp",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(72.dp))
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Collections,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pick a photo to cut a stamp",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(PostageStampShape)
                        .background(Color.White)
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                scale = (scale * gestureZoom).coerceIn(0.5f, 4f)
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                offsetX = (offsetX + pan.x / w).coerceIn(-1f, 1f)
                                offsetY = (offsetY + pan.y / h).coerceIn(-1f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX * size.width
                                translationY = offsetY * size.height
                            },
                    )
                }
                StampFrameOverlay(
                    modifier = Modifier.fillMaxWidth(0.88f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp),
        ) {
            Text(
                "Drag image. Pinch to zoom.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                ) { Text("Choose Photo") }
                Button(
                    enabled = bitmap != null,
                    onClick = {
                        val current = bitmap ?: return@Button
                        scope.launch {
                            val out = withContext(Dispatchers.IO) {
                                val stamped = ImageUtils.renderPostageStamp(
                                    source = current,
                                    translateX = offsetX,
                                    translateY = offsetY,
                                    scale = scale,
                                )
                                ImageUtils.saveStampPng(context, stamped)
                            }
                            onCropped(out)
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text("Cut Stamp") }
            }
        }
    }
}
