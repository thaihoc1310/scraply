package com.example.scraply.ui.editor

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
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.util.PostageStampShape
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapbookEditorScreen(
    vm: EditorViewModel,
    projectId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
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

    var showAssets by remember { mutableStateOf(false) }
    var showPalette by remember { mutableStateOf(false) }
    var showStampPicker by remember { mutableStateOf<PickerMode?>(null) }
    var editingText by remember { mutableStateOf<String?>(null) }
    var showPublishPreview by remember { mutableStateOf<Bitmap?>(null) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val graphicsLayer = rememberGraphicsLayer()

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
                Spacer(Modifier.weight(1f))
                Text(
                    current?.name?.let { if (it.length > 14) it.take(12) + "…" else it } ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(
                        icon = Icons.Filled.Tune,
                        contentDescription = "Edit assets",
                        onClick = { showAssets = true },
                    )
                    
                    val isTextSelected = canvas.elements.find { it.id == selectedId }?.type == CanvasElementType.TEXT
                    if (isTextSelected) {
                        CircleIconButton(
                            icon = Icons.Filled.Palette,
                            contentDescription = "Palette",
                            onClick = { showPalette = true },
                        )
                    }

                    Box {
                        var topMenuOpen by remember { mutableStateOf(false) }
                        CircleIconButton(
                            icon = Icons.Filled.MoreHoriz,
                            contentDescription = "More options",
                            onClick = { topMenuOpen = true }
                        )
                        DropdownMenu(
                            expanded = topMenuOpen,
                            onDismissRequest = { topMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add stamp") },
                                leadingIcon = { Icon(Icons.Filled.PhotoLibrary, null) },
                                onClick = { 
                                    topMenuOpen = false
                                    showStampPicker = PickerMode.AddStamp 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.IosShare, null) },
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
                            if (vm.canPublish) {
                                DropdownMenuItem(
                                    text = { Text("Publish to feed") },
                                    leadingIcon = { Icon(Icons.Filled.Public, null) },
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
                            if (editingText != null) {
                                vm.onTransformEnd()
                            }
                            editingText = null
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
                                vm.onTransformEnd()
                                editingText = null
                            }
                        },
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
                        OutlinedButton(
                            onClick = {
                                if (sel.type == CanvasElementType.TEXT) {
                                    vm.onTransformStart()
                                    editingText = id
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = sel.type == CanvasElementType.TEXT,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) { Text("Edit") }
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

    if (showAssets) {
        EditAssetsSheet(
            background = background,
            onBackground = { vm.setBackground(it) },
            onPickAsset = { option ->
                if (option.type == CanvasElementType.POLAROID) {
                    showStampPicker = PickerMode.Polaroid
                } else {
                    vm.addElement(type = option.type, assetKey = option.key)
                }
            },
            onAddText = { initial, font ->
                vm.addElement(type = CanvasElementType.TEXT, assetKey = font, text = initial)
                val id = vm.canvas.value.elements.lastOrNull()?.id
                if (id != null) {
                    vm.updateElement(id) { it.copy(font = font) }
                }
            },
            onDismiss = { showAssets = false },
        )
    }

    if (showPalette) {
        PaletteSheet(
            onPickColor = { color ->
                val id = selectedId ?: return@PaletteSheet
                vm.updateElement(id) { it.copy(color = color.toLong()) }
            },
            onDismiss = { showPalette = false },
        )
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

@Composable
private fun CanvasElementOnBoard(
    element: CanvasElement,
    stamps: List<Stamp>,
    selected: Boolean,
    canvasSize: IntSize,
    isEditing: Boolean,
    onSelect: () -> Unit,
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
                scaleX = element.scale
                scaleY = element.scale
                rotationZ = element.rotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
            .pointerInput(element.id) {
                detectTapGestures(onTap = { onSelect() })
            }
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
            onTextChange = onTextChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAssetsSheet(
    background: String,
    onBackground: (String) -> Unit,
    onPickAsset: (AssetOption) -> Unit,
    onAddText: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(AssetsTab.Backgrounds) }
    var newText by remember { mutableStateOf("") }
    var font by remember { mutableStateOf(FontPresets.first().first) }

    fun dismissWithAction(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
            onDismiss()
        }
    }

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
                OutlinedButton(
                    onClick = { dismissWithAction {} },
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Done") }
                Spacer(Modifier.weight(1f))
                Text(
                    "Edit Assets",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                    .padding(4.dp),
            ) {
                AssetsTab.entries.forEach { t ->
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
                AssetsTab.Backgrounds -> BackgroundsGrid(
                    current = background,
                    onSelect = { dismissWithAction { onBackground(it) } },
                )
                AssetsTab.Assets -> AssetCategoriesList(onPick = { dismissWithAction { onPickAsset(it) } })
                AssetsTab.Text -> TextTab(
                    text = newText,
                    onTextChange = { newText = it },
                    font = font,
                    onFontChange = { font = it },
                    onAdd = {
                        val textToAdd = newText.ifBlank { "New text" }
                        val fontToAdd = font
                        dismissWithAction { onAddText(textToAdd, fontToAdd) }
                        newText = ""
                    },
                )
            }
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

@Composable
private fun BackgroundsGrid(current: String, onSelect: (String) -> Unit) {
    Text(
        "Project Background",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

@Composable
private fun AssetCategoriesList(onPick: (AssetOption) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
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
                            .background(Color(0xFF26211E)) // Dark frame color from screenshot
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
                                    modifier = Modifier.size(64.dp), // slightly larger
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                // Fill empty spots in the last row to maintain grid alignment
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
private fun TextTab(
    text: String,
    onTextChange: (String) -> Unit,
    font: String,
    onFontChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column {
        Text("Typography UI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Enter text...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onAdd,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) { Text("Add") }
        }
        
        Spacer(Modifier.height(16.dp))
        Text("Font Presets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FontPresets.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFontChange(key) }
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "The quick brown fox jumps over the lazy dog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(selected = font == key, onClick = { onFontChange(key) })
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteSheet(onPickColor: (Long) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = listOf(
        0xFF1F1B18L, 0xFFFFFFFFL, 0xFFE77B2EL, 0xFFE2A3B5L,
        0xFFA6C4E0L, 0xFFC6DFA4L, 0xFFF2DFA1L, 0xFFB9BDB1L,
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text("Color", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            .clickable { onPickColor(c); onDismiss() },
                    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a stamp") },
        text = {
            if (stamps.isEmpty()) {
                Text("You have no stamps yet.")
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
                                .aspectRatio(0.8f)
                                .clip(PostageStampShape)
                                .background(Color.White)
                                .clickable(enabled = !isUsed) { onPick(s) },
                        ) {
                            AsyncImage(
                                model = s.imageUri,
                                contentDescription = s.title,
                                contentScale = ContentScale.Crop,
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
