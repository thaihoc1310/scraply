package com.example.scraply.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.example.scraply.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.CanvasState
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.collection.TextDialog
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.ScraplyCard
import com.example.scraply.ui.common.ScraplyDropdownMenu
import com.example.scraply.ui.common.ScraplyDropdownMenuItem

@Composable
fun EditorProjectsScreen(
    vm: EditorViewModel,
    onOpenProject: (String) -> Unit,
) {
    val projects by vm.projects.collectAsState()
    val stamps by vm.stamps.collectAsState()
    val projectFocusRequest by vm.projectFocusRequest.collectAsState()
    val listState = rememberLazyListState()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }
    val topProjectId = projects.firstOrNull()?.project?.id

    LaunchedEffect(projectFocusRequest, topProjectId) {
        val requestedProjectId = projectFocusRequest ?: return@LaunchedEffect
        if (topProjectId == requestedProjectId) {
            listState.animateScrollToItem(0)
            vm.consumeProjectFocusRequest(requestedProjectId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.editor_new_project),
                onClick = { showCreate = true },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.editor_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            stringResource(R.string.editor_projects),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        LazyColumn(
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(projects, key = { it.project.id }) { card ->
                ProjectCardView(
                    card = card,
                    stamps = stamps,
                    onOpen = { onOpenProject(card.project.id) },
                    onRename = { renaming = card.project.id },
                    onDelete = { vm.deleteProject(card.project.id) },
                )
            }
        }
    }

    if (showCreate) {
        TextDialog(
            title = stringResource(R.string.editor_new_project),
            placeholder = stringResource(R.string.editor_project_name),
            confirmLabel = stringResource(R.string.collections_create),
            onConfirm = { name ->
                if (name.isNotBlank()) vm.createProject(name.trim())
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
    renaming?.let { id ->
        val current = projects.firstOrNull { it.project.id == id }?.project?.name.orEmpty()
        TextDialog(
            title = stringResource(R.string.editor_rename_project),
            placeholder = stringResource(R.string.editor_project_name),
            initial = current,
            confirmLabel = stringResource(R.string.collections_save),
            onConfirm = { name ->
                if (name.isNotBlank()) vm.renameProject(id, name.trim())
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun ProjectCardView(
    card: ProjectCard,
    stamps: List<Stamp>,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    ScraplyCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.project.name,
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 29.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = projectMeta(card),
                        style = MaterialTheme.typography.labelLarge.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box {
                    CircleIconButton(
                        icon = Icons.Filled.MoreHoriz,
                        contentDescription = stringResource(R.string.close),
                        onClick = { menuOpen = true },
                    )
                    ScraplyDropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        ScraplyDropdownMenuItem(
                            label = stringResource(R.string.collections_rename),
                            icon = Icons.Filled.Edit,
                            onClick = { menuOpen = false; onRename() },
                        )
                        ScraplyDropdownMenuItem(
                            label = stringResource(R.string.collections_delete),
                            icon = Icons.Filled.Delete,
                            destructive = true,
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ProjectPreview(card = card, stamps = stamps)
        }
    }
}

@Composable
private fun ProjectPreview(
    card: ProjectCard,
    stamps: List<Stamp>,
) {
    val canvasState = remember(card.project.canvasJson) {
        CanvasState.fromJson(card.project.canvasJson)
    }
    val ratio = canvasState.aspectRatio

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        val hasCustomBackground = card.project.backgroundType.isNotEmpty() && card.project.backgroundType != "paper"
        if (card.elements.isEmpty() && !hasCustomBackground) {
            Text(
                stringResource(R.string.editor_empty_canvas),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val canvasBaseHeight = 600.dp
            val localWidth = canvasBaseHeight * ratio
            val localHeight = canvasBaseHeight

            val maxW = maxWidth - 16.dp
            val maxH = maxHeight - 16.dp
            val fitScaleX = maxW.value / localWidth.value
            val fitScaleY = maxH.value / localHeight.value
            val previewScale = kotlin.math.min(fitScaleX, fitScaleY)

            val wPx = with(LocalDensity.current) { localWidth.toPx() }
            val hPx = with(LocalDensity.current) { localHeight.toPx() }

            Box(
                modifier = Modifier
                    .requiredSize(localWidth, localHeight)
                    .graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                    }
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                BackgroundSurface(backgroundType = card.project.backgroundType, modifier = Modifier.fillMaxSize())

                card.elements.sortedBy { it.zIndex }.forEach { element ->
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = element.x * wPx - size.width / 2f
                            translationY = element.y * hPx - size.height / 2f
                            scaleX = if (element.isFlipped) -element.scale else element.scale
                            scaleY = element.scale
                            rotationZ = element.rotation
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        CanvasElementView(element = element, stamps = stamps)
                    }
                }
            }
        }
    }
}

@Composable
private fun projectMeta(card: ProjectCard): String {
    val stampCount = card.elements.count {
        it.type == CanvasElementType.STAMP || it.type == CanvasElementType.POLAROID
    }
    val elementCount = card.elementCount
    val stampText = stringResource(
        if (stampCount == 1) R.string.editor_stamp_count_singular else R.string.editor_stamp_count_plural,
        stampCount
    )
    val elementText = stringResource(
        if (elementCount == 1) R.string.editor_element_count_singular else R.string.editor_element_count_plural,
        elementCount
    )
    return if (stampCount > 0) "$stampText - $elementText" else elementText
}
