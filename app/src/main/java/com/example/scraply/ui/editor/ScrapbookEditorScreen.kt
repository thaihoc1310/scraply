package com.example.scraply.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.ScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.ScraplyDialog
import com.example.scraply.ui.common.ScraplyDialogConfirmButton
import com.example.scraply.ui.common.ScraplyDropdownMenu
import com.example.scraply.ui.common.ScraplyDropdownMenuItem
import com.example.scraply.ui.common.StampImage
import com.example.scraply.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapbookEditorScreen(
    vm: EditorViewModel,
    projectId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val current by vm.current.collectAsState()
    val canvas by vm.canvas.collectAsState()
    val background by vm.background.collectAsState()
    val selectedId by vm.selectedId.collectAsState()
    val stamps by vm.stamps.collectAsState()
    val publishState by vm.publishState.collectAsState()

    LaunchedEffect(publishState) {
        when (val s = publishState) {
            is PublishState.Success -> {
                Toast.makeText(context, "Published to feed", Toast.LENGTH_SHORT).show()
                vm.dismissPublishState()
            }
            is PublishState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.dismissPublishState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(projectId) { vm.loadProject(projectId) }
    DisposableEffect(projectId) {
        onDispose { vm.saveCurrent() }
    }

    var showBackgrounds by remember { mutableStateOf(false) }
    var showAssetsSheet by remember { mutableStateOf(false) }
    var showTextStyle by remember { mutableStateOf(false) }
    var textStyleElementId by remember { mutableStateOf<String?>(null) }
    var showStampPicker by remember { mutableStateOf<PickerMode?>(null) }
    var editingText by remember { mutableStateOf<String?>(null) }
    var showPublishPreview by remember { mutableStateOf<Bitmap?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }

    // Collapse FAB when an element is selected
    LaunchedEffect(selectedId) {
        if (selectedId != null) fabExpanded = false
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val graphicsLayer = rememberGraphicsLayer()

    fun endInlineTextEdit() {
        if (editingText != null) {
            vm.onTransformEnd()
        }
        editingText = null
        focusManager.clearFocus()
    }

    fun beginInlineTextEdit(id: String) {
        vm.selectElement(id)
        if (editingText != id) {
            if (editingText != null) {
                vm.onTransformEnd()
            }
            vm.onTransformStart()
            editingText = id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = {
                        vm.saveCurrent()
                        onBack()
                    },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = current?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(
                        icon = Icons.Filled.Wallpaper,
                        contentDescription = "Backgrounds",
                        onClick = { showBackgrounds = true },
                    )
                    Spacer(Modifier.width(10.dp))

                    Box {
                        var topMenuOpen by remember { mutableStateOf(false) }
                        CircleIconButton(
                            icon = Icons.Filled.MoreHoriz,
                            contentDescription = "More options",
                            onClick = { topMenuOpen = true }
                        )
                        ScraplyDropdownMenu(
                            expanded = topMenuOpen,
                            onDismissRequest = { topMenuOpen = false }
                        ) {
                            ScraplyDropdownMenuItem(
                                label = "Share",
                                icon = Icons.Filled.IosShare,
                                onClick = { 
                                    topMenuOpen = false
                                    scope.launch {
                                        val bmp: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                        val uri = vm.shareBitmap(bmp)
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Scrapbook"))
                                        } else {
                                            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            ScraplyDropdownMenuItem(
                                label = "Save to photos",
                                icon = Icons.Filled.Download,
                                onClick = {
                                    topMenuOpen = false
                                    scope.launch {
                                        val bmp: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                        val saved = saveCanvasToPhotos(
                                            context = context,
                                            bitmap = bmp,
                                            projectName = current?.name,
                                        )
                                        Toast.makeText(
                                            context,
                                            if (saved) "Saved to Pictures/Scraply" else "Could not save image",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                            if (vm.canPublish) {
                                ScraplyDropdownMenuItem(
                                    label = "Publish to feed",
                                    icon = Icons.Filled.Public,
                                    onClick = { 
                                        topMenuOpen = false
                                        scope.launch {
                                            val bmp: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                            showPublishPreview = bmp
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(0.58f)
                    .clip(RoundedCornerShape(22.dp))
                    .onSizeChanged { canvasSize = it }
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            vm.selectElement(null)
                            endInlineTextEdit()
                        })
                    },
            ) {
                BackgroundSurface(backgroundType = background, modifier = Modifier.fillMaxSize())

                canvas.elements.sortedBy { it.zIndex }.forEach { element ->
                    CanvasElementOnBoard(
                        element = element,
                        stamps = stamps,
                        selected = element.id == selectedId,
                        canvasSize = canvasSize,
                        isEditing = element.id == editingText,
                        onSelect = {
                            vm.selectElement(element.id)
                            if (editingText != null && editingText != element.id) {
                                endInlineTextEdit()
                            }
                            if (element.type != CanvasElementType.TEXT) {
                                focusManager.clearFocus()
                            }
                        },
                        onBeginTextEdit = { beginInlineTextEdit(element.id) },
                        onUpdate = { updated -> vm.updateElement(element.id) { updated } },
                        onTransformStart = { vm.onTransformStart() },
                        onTransformEnd = { vm.onTransformEnd() },
                        onTextChange = { newText ->
                            vm.updateElement(element.id) { it.copy(text = newText) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { vm.undo() },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
                selectedId?.let { id ->
                    val sel = canvas.elements.firstOrNull { it.id == id }
                    if (sel != null) {
                        OutlinedButton(
                            onClick = { vm.bringToFront(id) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            Icon(Icons.Filled.FlipToFront, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { vm.sendToBack(id) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            Icon(Icons.Filled.FlipToBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        if (sel.type == CanvasElementType.TEXT) {
                            OutlinedButton(
                                onClick = {
                                    textStyleElementId = id
                                    showTextStyle = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Style")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    vm.updateElement(id) { it.copy(isFlipped = !it.isFlipped) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Icon(Icons.Filled.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Flip")
                            }
                        }
                        OutlinedButton(
                            onClick = { vm.deleteElement(id) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Expandable FAB — hide when an element is selected
        if (selectedId == null) {
            ExpandableFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onAddStamp = {
                    fabExpanded = false
                    showStampPicker = PickerMode.AddStamp
                },
                onAddAssets = {
                    fabExpanded = false
                    showAssetsSheet = true
                },
                onAddText = {
                    fabExpanded = false
                    val font = FontPresets.first().first
                    vm.addElement(type = CanvasElementType.TEXT, assetKey = font, text = "Text")
                    val id = vm.canvas.value.elements.lastOrNull()?.id
                    if (id != null) {
                        vm.updateElement(id) { it.copy(font = font) }
                        beginInlineTextEdit(id)
                    }
                },
            )
        }

        if (publishState is PublishState.Publishing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Publishing to feed…", color = Color.White)
                }
            }
        }
    }

    if (showPublishPreview != null) {
        val bmp = showPublishPreview!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var title by remember { mutableStateOf(current?.name ?: "") }
        var description by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showPublishPreview = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Publish to Feed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(canvasSize.width.toFloat() / canvasSize.height.toFloat())
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 1000) title = it },
                    label = { Text("Title") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${title.length}/1000") },
                    singleLine = true,
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1000) description = it },
                    label = { Text("Description") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 48.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${description.length}/1000") },
                    minLines = 3,
                    maxLines = 5,
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showPublishPreview = null }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            vm.publishToFeed(bmp, title, description)
                            showPublishPreview = null
                        }
                    ) {
                        Text("Publish")
                    }
                }
            }
        }
    }

    if (showBackgrounds) {
        BackgroundsSheet(
            current = background,
            onSelect = { vm.setBackground(it) },
            onDismiss = { showBackgrounds = false },
        )
    }

    if (showAssetsSheet) {
        AssetsOnlySheet(
            onPickAsset = { option ->
                if (option.type == CanvasElementType.POLAROID) {
                    showStampPicker = PickerMode.Polaroid
                } else {
                    vm.addElement(type = option.type, assetKey = option.key)
                }
                showAssetsSheet = false
            },
            onDismiss = { showAssetsSheet = false },
        )
    }

    if (showTextStyle) {
        val textEl = textStyleElementId?.let { id ->
            canvas.elements.firstOrNull { it.id == id && it.type == CanvasElementType.TEXT }
        }
        if (textEl != null) {
            TextStyleSheet(
                element = textEl,
                onFontChange = { font ->
                    vm.updateElement(textEl.id) { it.copy(font = font) }
                },
                onColorChange = { color ->
                    vm.updateElement(textEl.id) { it.copy(color = color) }
                },
                onDismiss = {
                    showTextStyle = false
                    textStyleElementId = null
                },
            )
        }
    }

    showStampPicker?.let { mode ->
        StampPickerDialog(
            stamps = stamps,
            usedStampIds = canvas.elements.mapNotNull { it.stampId }.toSet(),
            onPick = { stamp ->
                when (mode) {
                    PickerMode.AddStamp -> vm.addElement(
                        type = CanvasElementType.STAMP,
                        stampId = stamp.id,
                    )
                    PickerMode.Polaroid -> vm.addElement(
                        type = CanvasElementType.POLAROID,
                        stampId = stamp.id,
                    )
                }
                showStampPicker = null
            },
            onDismiss = { showStampPicker = null },
        )
    }


}

private enum class PickerMode { AddStamp, Polaroid }

private suspend fun saveCanvasToPhotos(
    context: Context,
    bitmap: Bitmap,
    projectName: String?,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ImageUtils.saveToGallery(
            context = context,
            bitmap = bitmap,
            displayName = scrapbookGalleryName(projectName),
        ) != null
    }.getOrDefault(false)
}

private fun scrapbookGalleryName(projectName: String?): String {
    val fallback = "scraply_${System.currentTimeMillis()}"
    val cleaned = projectName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace(Regex("""[\\/:*?"<>|]+"""), "_")
        ?.take(60)
        ?.trim(' ', '_')
        ?.takeIf { it.isNotBlank() }
        ?: fallback
    return if (cleaned.startsWith("scraply_", ignoreCase = true)) cleaned else "scraply_$cleaned"
}

@Composable
private fun CanvasElementOnBoard(
    element: CanvasElement,
    stamps: List<Stamp>,
    selected: Boolean,
    canvasSize: IntSize,
    isEditing: Boolean,
    onSelect: () -> Unit,
    onBeginTextEdit: () -> Unit,
    onUpdate: (CanvasElement) -> Unit,
    onTransformStart: () -> Unit,
    onTransformEnd: () -> Unit,
    onTextChange: (String) -> Unit,
) {
    if (canvasSize == IntSize.Zero) return
    val w = canvasSize.width.toFloat()
    val h = canvasSize.height.toFloat()

    // rememberUpdatedState giữ reference luôn trỏ tới giá trị mới nhất,
    // tránh stale closure trong pointerInput (key = element.id không đổi).
    val latestElement by rememberUpdatedState(element)

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = element.x * w - size.width / 2f
                translationY = element.y * h - size.height / 2f
                scaleX = if (element.isFlipped) -element.scale else element.scale
                scaleY = element.scale
                rotationZ = element.rotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
            .then(
                if (element.type == CanvasElementType.TEXT) {
                    Modifier.pointerInput(element.id) {
                        detectTapGestures(
                            onTap = { onSelect() },
                            onDoubleTap = { onBeginTextEdit() },
                        )
                    }
                } else {
                    Modifier.pointerInput(element.id) {
                        detectTapGestures(onTap = { onSelect() })
                    }
                },
            )
            .pointerInput(element.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onTransformStart()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onTransformEnd()
                }
            }
            .pointerInput(element.id) {
                detectTransformGestures(panZoomLock = false) { _, pan, zoom, rot ->
                    onSelect()
                    val el = latestElement
                    
                    // Convert local pan to screen pan by applying rotation and scale
                    val angleRad = el.rotation * Math.PI / 180.0
                    val cosA = kotlin.math.cos(angleRad).toFloat()
                    val sinA = kotlin.math.sin(angleRad).toFloat()
                    
                    val dx = (pan.x * cosA - pan.y * sinA) * el.scale
                    val dy = (pan.x * sinA + pan.y * cosA) * el.scale

                    val hwN = (size.width * el.scale / 2f) / w
                    val hhN = (size.height * el.scale / 2f) / h

                    val minX = kotlin.math.min(hwN, 1f - hwN)
                    val maxX = kotlin.math.max(hwN, 1f - hwN)
                    val minY = kotlin.math.min(hhN, 1f - hhN)
                    val maxY = kotlin.math.max(hhN, 1f - hhN)

                    val nx = (el.x + dx / w).coerceIn(minX, maxX)
                    val ny = (el.y + dy / h).coerceIn(minY, maxY)
                    val ns = (el.scale * zoom).coerceIn(0.2f, 4f)
                    val nr = el.rotation + rot
                    onUpdate(el.copy(x = nx, y = ny, scale = ns, rotation = nr))
                }
            }
            .then(
                if (selected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(6.dp),
                ) else Modifier,
            ),
    ) {
        CanvasElementView(
            element = element, 
            stamps = stamps,
            isEditing = isEditing,
            onStartTextEdit = onBeginTextEdit,
            onTextChange = onTextChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundsSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(64.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Backgrounds",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Done") }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                BackgroundsGrid(
                    current = current,
                    scrollState = scrollState,
                    sheetState = sheetState,
                    onSelect = {
                        scope.launch { sheetState.hide() }
                        onSelect(it)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetsOnlySheet(
    onPickAsset: (AssetOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(64.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Assets",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Done") }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                AssetCategoriesList(
                    scrollState = scrollState,
                    sheetState = sheetState,
                    onPick = { asset ->
                        scope.launch { sheetState.hide() }
                        onPickAsset(asset)
                        onDismiss()
                    }
                )
            }
        }
    }
}

private enum class TextStyleTab(val label: String) {
    Font("Font"),
    Color("Color"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextStyleSheet(
    element: CanvasElement,
    onFontChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(TextStyleTab.Font) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(64.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Text Style",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Done") }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                    .padding(4.dp),
            ) {
                TextStyleTab.entries.forEach { t ->
                    TabChip(
                        label = t.label,
                        selected = tab == t,
                        onClick = { tab = t },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when (tab) {
                TextStyleTab.Font -> FontPresetsContent(
                    currentFont = element.font,
                    onFontChange = onFontChange,
                )
                TextStyleTab.Color -> ColorPickerContent(
                    currentColor = element.color,
                    onColorChange = onColorChange,
                )
            }
        }
    }
}

@Composable
private fun FontPresetsContent(
    currentFont: String,
    onFontChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Font Presets",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        FontPresets.forEach { (key, label) ->
            val fontFamily = fontFamilyMap[key] ?: FontFamily.Serif
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFontChange(key) }
                    .background(
                        if (currentFont == key) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(14.dp),
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                    )
                    Text(
                        "The quick brown fox jumps over the lazy dog",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(selected = currentFont == key, onClick = { onFontChange(key) })
            }
        }
    }
}

@Composable
private fun ColorPickerContent(
    currentColor: Long,
    onColorChange: (Long) -> Unit,
) {
    val palette = listOf(
        // Row 1: neutrals
        0xFF1F1B18L, 0xFF3C3733L, 0xFF8A827AL, 0xFFB9BDB1L, 0xFFFFFFFFL,
        // Row 2: warm
        0xFFE77B2EL, 0xFFF59E0BL, 0xFFF2DFA1L, 0xFFE2A3B5L, 0xFFBFA98FL,
        // Row 3: cool
        0xFFA6C4E0L, 0xFF4FA3D9L, 0xFF6366F1L, 0xFF8B5CF6L, 0xFF14B8A6L,
        // Row 4: vivid
        0xFFD94F4FL, 0xFFEC4899L, 0xFF34D399L, 0xFFC6DFA4L, 0xFF6C5B4AL,
    )
    Column {
        Text(
            "Text Color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        palette.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                CircleShape,
                            )
                            .clickable { onColorChange(c) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (currentColor == c) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (c == 0xFFFFFFFFL.toLong()) Color.Black else Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddStamp: () -> Unit,
    onAddAssets: () -> Unit,
    onAddText: () -> Unit,
) {
    // Dim overlay when expanded
    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { onToggle() },
        )
    }

    // FAB stack at bottom-end
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Render sub-buttons individually with staggered slide-up from main FAB

        // 1. Add Text (highest, furthest from FAB)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 120, easing = FastOutSlowInEasing)) + 
                    slideInVertically(
                        initialOffsetY = { it * 4 }, // Slides up from bottom (near FAB)
                        animationSpec = tween(durationMillis = 300, delayMillis = 120, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)) + 
                   slideOutVertically(
                       targetOffsetY = { it * 4 },
                       animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                   ),
        ) {
            FabSubButton(
                icon = Icons.Filled.TextFields,
                label = "Add Text",
                onClick = onAddText,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
        }

        // 2. Add Assets (middle)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = 60, easing = FastOutSlowInEasing)) + 
                    slideInVertically(
                        initialOffsetY = { it * 3 }, // Slides up from bottom (near FAB)
                        animationSpec = tween(durationMillis = 250, delayMillis = 60, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)) + 
                   slideOutVertically(
                       targetOffsetY = { it * 3 },
                       animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                   ),
        ) {
            FabSubButton(
                icon = Icons.Filled.Dashboard,
                label = "Add Assets",
                onClick = onAddAssets,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
        }

        // 3. Add Stamp (lowest, closest to FAB)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 0, easing = FastOutSlowInEasing)) + 
                    slideInVertically(
                        initialOffsetY = { it * 2 }, // Slides up from FAB position
                        animationSpec = tween(durationMillis = 200, delayMillis = 0, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)) + 
                   slideOutVertically(
                       targetOffsetY = { it * 2 },
                       animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                   ),
        ) {
            FabSubButton(
                icon = Icons.Filled.PhotoLibrary,
                label = "Add Stamp",
                onClick = onAddStamp,
            )
        }

        Spacer(Modifier.height(12.dp))

        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            label = "fab_rotation",
        )
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = if (expanded) "Close" else "Add",
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun FabSubButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            label,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private enum class AssetsTab(val label: String) {
    Backgrounds("Backgrounds"),
    Assets("Assets"),
    Text("Text"),
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundsGrid(
    current: String,
    scrollState: ScrollState,
    sheetState: SheetState,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .scrollFirstThenDragSheet(scrollState, sheetState)
    ) {
        BackgroundOptions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { (key, label) ->
                    Column(
                        modifier = Modifier.weight(1f)
                            .clickable { onSelect(key) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (current == key)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                    else Modifier,
                                ),
                        ) {
                            BackgroundSurface(backgroundType = key, modifier = Modifier.fillMaxSize())
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            RadioButton(selected = current == key, onClick = { onSelect(key) })
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetCategoriesList(
    scrollState: ScrollState,
    sheetState: SheetState,
    onPick: (AssetOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .scrollFirstThenDragSheet(scrollState, sheetState)
    ) {
        AssetCategorySection(title = "Tape Pack", assets = TapeAssets, onPick = onPick)
        Spacer(Modifier.height(12.dp))
        AssetCategorySection(title = "Sticker Pack", assets = StickerAssets, onPick = onPick)
        Spacer(Modifier.height(12.dp))
        AssetCategorySection(
            title = "Polaroid Frame",
            assets = listOf(AssetOption("polaroid", "Classic Polaroid", CanvasElementType.POLAROID)),
            onPick = onPick,
        )
        Spacer(Modifier.height(12.dp))
        AssetCategorySection(title = "Paper Cuts", assets = PaperCutAssets, onPick = onPick)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AssetCategorySection(
    title: String,
    assets: List<AssetOption>,
    onPick: (AssetOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${assets.size} styles",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        assets.chunked(3).forEach { rowAssets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowAssets.forEach { a ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(a) }
                            .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                CanvasElementView(
                                    element = CanvasElement(
                                        id = a.key,
                                        type = a.type,
                                        assetKey = a.key,
                                    ),
                                    stamps = emptyList(),
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (rowAssets.size < 3) {
                    repeat(3 - rowAssets.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StampPickerDialog(
    stamps: List<Stamp>,
    usedStampIds: Set<String>,
    onPick: (Stamp) -> Unit,
    onDismiss: () -> Unit,
) {
    ScraplyDialog(
        title = "Pick a stamp",
        onDismissRequest = onDismiss,
        maxWidth = 460.dp,
        content = {
            if (stamps.isEmpty()) {
                Text(
                    "You have no stamps yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp),
                ) {
                    items(stamps, key = { it.id }) { s ->
                        val isUsed = usedStampIds.contains(s.id)
                        Box(
                            modifier = Modifier
                                .aspectRatio(147f / 190f)
                                .clickable(enabled = !isUsed) { onPick(s) },
                        ) {
                            StampImage(
                                imageUri = s.imageUri,
                                contentDescription = s.title,
                                modifier = Modifier.fillMaxSize().let {
                                    if (isUsed) it.alpha(0.5f) else it
                                },
                            )
                            if (isUsed) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        actions = {
            ScraplyDialogConfirmButton(label = "Close", onClick = onDismiss)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Modifier.scrollFirstThenDragSheet(
    scrollState: ScrollState,
    sheetState: SheetState,
): Modifier {
    return this.pointerInput(scrollState, sheetState) {
        detectVerticalDragGestures(
            onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                if (dragAmount < 0f) {
                    // Dragging up (scroll forward)
                    if (scrollState.value < scrollState.maxValue) {
                        scrollState.dispatchRawDelta(-dragAmount)
                        change.consume()
                    }
                } else if (dragAmount > 0f) {
                    // Dragging down (scroll backward)
                    if (scrollState.value > 0) {
                        scrollState.dispatchRawDelta(-dragAmount)
                        change.consume()
                    }
                }
            }
        )
    }
}
