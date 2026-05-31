package com.example.scraply.ui.stamp

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.scraply.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.ScraplyOutlinedButton
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.calendar_back),
                    onClick = onBack,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stamp_adjust),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScraplyOutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.stamp_retake))
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
                    Text(if (isProcessing) stringResource(R.string.stamp_processing) else stringResource(R.string.stamp_next))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
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
                title = stringResource(R.string.stamp_style),
                options = ImageUtils.StampStyle.entries,
                selected = selectedStyle,
                onSelect = { selectedStyle = it },
                scrollable = true,
                label = { style ->
                    when (style) {
                        ImageUtils.StampStyle.CLASSIC -> stringResource(R.string.stamp_style_classic)
                        ImageUtils.StampStyle.VINTAGE -> stringResource(R.string.stamp_style_vintage)
                        ImageUtils.StampStyle.VINTAGE_2 -> stringResource(R.string.stamp_style_vintage2)
                        ImageUtils.StampStyle.BLACK_AND_WHITE -> stringResource(R.string.stamp_style_black_and_white)
                        ImageUtils.StampStyle.FADED -> stringResource(R.string.stamp_style_faded)
                        ImageUtils.StampStyle.FILM -> stringResource(R.string.stamp_style_film)
                        ImageUtils.StampStyle.SEPIA -> stringResource(R.string.stamp_style_sepia)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            OptionSection(
                title = stringResource(R.string.stamp_frame),
                options = ImageUtils.StampFrameStyle.entries,
                selected = selectedFrame,
                onSelect = { selectedFrame = it },
                label = { frame ->
                    when (frame) {
                        ImageUtils.StampFrameStyle.FULL_BLEED -> stringResource(R.string.stamp_frame_full)
                        ImageUtils.StampFrameStyle.CLEAN_FRAME -> stringResource(R.string.stamp_frame_frame)
                        ImageUtils.StampFrameStyle.NO_STROKE -> stringResource(R.string.stamp_frame_none)
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun <T> OptionSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    scrollable: Boolean = false,
    label: @Composable (T) -> String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (scrollable) it.horizontalScroll(rememberScrollState()) else it },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            OptionButton(
                label = label(option),
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = if (scrollable) Modifier.widthIn(min = 92.dp) else Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val selectedContainer = MaterialTheme.colorScheme.surfaceVariant
    val unselectedContainer = MaterialTheme.colorScheme.surface
    val selectedBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) selectedContainer else unselectedContainer)
            .border(
                width = 1.dp,
                color = if (selected) selectedBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
