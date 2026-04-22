package com.example.scraply.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.scraply.ui.collection.TextDialog
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.PillBadge
import com.example.scraply.ui.common.ScraplyCard

@Composable
fun EditorProjectsScreen(
    vm: EditorViewModel,
    onOpenProject: (String) -> Unit,
) {
    val projects by vm.projects.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "New project",
                onClick = { showCreate = true },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Editor",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Projects",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(projects, key = { it.project.id }) { card ->
                ProjectCardView(
                    card = card,
                    onOpen = { onOpenProject(card.project.id) },
                    onRename = { renaming = card.project.id },
                    onDelete = { vm.deleteProject(card.project.id) },
                )
            }
        }
    }

    if (showCreate) {
        TextDialog(
            title = "New project",
            placeholder = "Project name",
            confirmLabel = "Create",
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
            title = "Rename project",
            placeholder = "Project name",
            initial = current,
            confirmLabel = "Save",
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
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    ScraplyCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillBadge("Project")
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Brush,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${card.elementCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    CircleIconButton(
                        icon = Icons.Filled.MoreHoriz,
                        contentDescription = "More",
                        onClick = { menuOpen = true },
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuOpen = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    onClick = onOpen,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                card.project.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${card.elementCount} element${if (card.elementCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (card.elementCount == 0) "Empty canvas" else "Tap to edit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
