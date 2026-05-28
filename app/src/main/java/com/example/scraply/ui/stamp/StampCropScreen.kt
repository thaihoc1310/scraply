package com.example.scraply.ui.stamp

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.scraply.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StampCropScreen(
    sourceUriEncoded: String,
    onBack: () -> Unit,
    onCropped: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourceUri = remember(sourceUriEncoded) { Uri.parse(Uri.decode(sourceUriEncoded)) }

    // Already cropped bitmap - just load and display
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedStyle by remember { mutableStateOf(ImageUtils.StampStyle.CLASSIC) }
    var selectedFrame by remember { mutableStateOf(ImageUtils.StampFrameStyle.FULL_BLEED) }

    LaunchedEffect(sourceUri) {
        bitmap = withContext(Dispatchers.IO) { ImageUtils.loadBitmap(context, sourceUri) }
    }

    LaunchedEffect(bitmap, selectedStyle, selectedFrame) {
        val source = bitmap
        previewBitmap = if (source == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                ImageUtils.renderStyledStamp(context, source, selectedStyle, selectedFrame)
            }
        }
    }

    // Stamp aspect ratio (147:190)
    val stampAspect = 147f / 190f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Adjust stamp",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Preview area - fixed stamp centered on screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Stamp preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .aspectRatio(stampAspect),
                )
            }
        }

        OptionSection(
            title = "Stamp Style",
            options = ImageUtils.StampStyle.entries,
            selected = selectedStyle,
            onSelect = { selectedStyle = it },
            label = { it.label },
        )

        Spacer(modifier = Modifier.height(12.dp))

        OptionSection(
            title = "Stamp Frame",
            options = ImageUtils.StampFrameStyle.entries,
            selected = selectedFrame,
            onSelect = { selectedFrame = it },
            label = { it.label },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text("Retake")
            }
            Button(
                onClick = {
                    val source = bitmap
                    if (source != null) {
                        isProcessing = true
                        scope.launch {
                            val savedUri = withContext(Dispatchers.IO) {
                                val rendered = ImageUtils.renderStyledStamp(
                                    context = context,
                                    source = source,
                                    style = selectedStyle,
                                    frameStyle = selectedFrame,
                                )
                                val uri = ImageUtils.saveStampPng(context, rendered)
                                rendered.recycle()
                                uri
                            }
                            onCropped(Uri.encode(savedUri))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f),
                enabled = !isProcessing && bitmap != null,
            ) {
                Text(if (isProcessing) "Processing..." else "Next")
            }
        }
    }
}

@Composable
private fun <T> OptionSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val isDark = isSystemInDarkTheme()
            val selectedContainer = if (isDark) Color(0xFF242424) else Color(0xFFD6D6D6)
            val unselectedContainer = if (isDark) Color(0xFF343434) else Color(0xFFF1F1F1)
            val selectedBorder = if (isDark) Color(0xFF5A5A5A) else Color(0xFFB8B8B8)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) {
                            selectedContainer
                        } else {
                            unselectedContainer
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            selectedBorder
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
