package com.example.scraply.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    icon = Icons.Filled.IosShare,
                    contentDescription = "Export",
                    onClick = {
                        scope.launch {
                            val bmp: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            val ok = vm.exportToGallery(bmp)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to gallery" else "Export failed",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
                if (vm.canPublish) {
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(
                        icon = Icons.Filled.Public,
                        contentDescription = "Publish to feed",
                        onClick = {
                            scope.launch {
                                val bmp: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                vm.publishToFeed(bmp)
                            }
                        },
                    )
                }
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
                        icon = Icons.Filled.PhotoLibrary,
                        contentDescription = "Add stamp",
                        onClick = { showStampPicker = PickerMode.AddStamp },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.Tune,
                        contentDescription = "Edit assets",
                        onClick = { showAssets = true },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.Palette,
                        contentDescription = "Palette",
                        onClick = { showPalette = true },
                    )
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
                        detectTapGestures(onTap = { vm.selectElement(null) })
                    },
            ) {
                BackgroundSurface(backgroundType = background, modifier = Modifier.fillMaxSize())

                canvas.elements.sortedBy { it.zIndex }.forEach { element ->
                    CanvasElementOnBoard(
                        element = element,
                        stamps = stamps,
                        selected = element.id == selectedId,
                        canvasSize = canvasSize,
                        onSelect = { vm.selectElement(element.id) },
                        onUpdate = { updated -> vm.updateElement(element.id) { updated } },
                        onCommit = { vm.commitTransform() },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { vm.undo() },
                    shape = RoundedCornerShape(14.dp),
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
                        ) {
                            Icon(Icons.Filled.FlipToFront, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { vm.sendToBack(id) },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.FlipToBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                if (sel.type == CanvasElementType.TEXT) editingText = id
                            },
                            shape = RoundedCornerShape(14.dp),
                            enabled = sel.type == CanvasElementType.TEXT,
                        ) { Text("Edit") }
                        OutlinedButton(
                            onClick = { vm.deleteElement(id) },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
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

    editingText?.let { id ->
        val sel = canvas.elements.firstOrNull { it.id == id }
        if (sel != null) {
            var value by remember(id) { mutableStateOf(sel.text) }
            AlertDialog(
                onDismissRequest = { editingText = null },
                title = { Text("Edit text") },
                text = {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = { Text("Your text…") },
                        shape = RoundedCornerShape(12.dp),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateElement(id) { it.copy(text = value) }
                        editingText = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editingText = null }) { Text("Cancel") }
                },
            )
        }
    }
}

private enum class PickerMode { AddStamp, Polaroid }

@Composable
private fun CanvasElementOnBoard(
    element: CanvasElement,
    stamps: List<Stamp>,
    selected: Boolean,
    canvasSize: IntSize,
    onSelect: () -> Unit,
    onUpdate: (CanvasElement) -> Unit,
    onCommit: () -> Unit,
) {
    if (canvasSize == IntSize.Zero) return
    val w = canvasSize.width.toFloat()
    val h = canvasSize.height.toFloat()

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
                detectTransformGestures(panZoomLock = false) { _, pan, zoom, rot ->
                    onSelect()
                    val nx = (element.x + pan.x / w).coerceIn(-0.1f, 1.1f)
                    val ny = (element.y + pan.y / h).coerceIn(-0.1f, 1.1f)
                    val ns = (element.scale * zoom).coerceIn(0.2f, 4f)
                    val nr = element.rotation + rot
                    onUpdate(element.copy(x = nx, y = ny, scale = ns, rotation = nr))
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
        CanvasElementView(element = element, stamps = stamps)
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
    var tab by remember { mutableStateOf(AssetsTab.Backgrounds) }
    var newText by remember { mutableStateOf("") }
    var font by remember { mutableStateOf(FontPresets.first().first) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onDismiss,
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
                    onSelect = onBackground,
                )
                AssetsTab.Assets -> AssetCategoriesList(onPick = onPickAsset)
                AssetsTab.Text -> TextTab(
                    text = newText,
                    onTextChange = { newText = it },
                    font = font,
                    onFontChange = { font = it },
                    onAdd = {
                        onAddText(newText.ifBlank { "New text" }, font)
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
    Column {
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
    Row {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text("${assets.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(assets) { a ->
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onPick(a) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CanvasElementView(
                        element = CanvasElement(
                            id = a.key,
                            type = a.type,
                            assetKey = a.key,
                        ),
                        stamps = emptyList(),
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        a.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Add Text Layer") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text("Font Presets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) { Text("Add Text Layer") }
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
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.8f)
                                .clip(PostageStampShape)
                                .background(Color.White)
                                .clickable { onPick(s) },
                        ) {
                            AsyncImage(
                                model = s.imageUri,
                                contentDescription = s.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
