package com.example.scraply.ui.collection

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
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
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.ui.common.PillBadge
import com.example.scraply.ui.common.ScraplyCard
import com.example.scraply.ui.common.ScraplyDialog
import com.example.scraply.ui.common.ScraplyDialogCancelButton
import com.example.scraply.ui.common.ScraplyDialogConfirmButton
import com.example.scraply.ui.common.ScraplyDropdownMenu
import com.example.scraply.ui.common.ScraplyDropdownMenuItem
import com.example.scraply.ui.common.ScraplyOutlinedTextField
import com.example.scraply.ui.common.StampImage

private const val StampAspectRatio = 147f / 190f

@Composable
fun CollectionsScreen(
    vm: CollectionsViewModel,
    onOpenCollection: (String) -> Unit,
    onOpenUpload: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val cards by vm.cards.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }

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
            CircleIconButton(
                icon = Icons.Filled.AddPhotoAlternate,
                contentDescription = "Upload stamp",
                onClick = onOpenUpload,
            )
            Spacer(Modifier.width(8.dp))
            CircleIconButton(
                icon = Icons.Filled.CalendarViewMonth,
                contentDescription = "Calendar",
                onClick = onOpenCalendar,
            )
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "New collection",
                onClick = { showCreate = true },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Collections",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(cards, key = { it.collection.id }) { card ->
                CollectionCardView(
                    card = card,
                    onOpen = { onOpenCollection(card.collection.id) },
                    onRename = { renaming = card.collection.id },
                    onDelete = { vm.deleteCollection(card.collection.id) },
                )
            }
        }
    }

    if (showCreate) {
        TextDialog(
            title = "New collection",
            placeholder = "Collection name",
            confirmLabel = "Create",
            onConfirm = { name ->
                if (name.isNotBlank()) vm.createCollection(name.trim())
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    renaming?.let { id ->
        val current = cards.firstOrNull { it.collection.id == id }?.collection?.name.orEmpty()
        TextDialog(
            title = "Rename collection",
            placeholder = "Collection name",
            initial = current,
            confirmLabel = "Save",
            onConfirm = { name ->
                if (name.isNotBlank()) vm.renameCollection(id, name.trim())
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun CollectionCardView(
    card: CollectionCard,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    ScraplyCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillBadge(label = if (card.collection.isDefault) "Default" else "Custom")
                Spacer(Modifier.weight(1f))
                if (!card.collection.isDefault) {
                    Box {
                        CircleIconButton(
                            icon = Icons.Filled.MoreHoriz,
                            contentDescription = "More",
                            onClick = { menuOpen = true },
                        )
                        ScraplyDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            ScraplyDropdownMenuItem(
                                label = "Rename",
                                icon = Icons.Filled.Edit,
                                onClick = { menuOpen = false; onRename() },
                            )
                            ScraplyDropdownMenuItem(
                                label = "Delete",
                                icon = Icons.Filled.Delete,
                                destructive = true,
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    onClick = onOpen,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                card.collection.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${card.stampCount} stamp${if (card.stampCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onOpen)
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (card.thumbUris.isEmpty()) {
                    Text(
                        "No stamps yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(card.thumbUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .height(106.dp)
                                    .aspectRatio(StampAspectRatio),
                                contentAlignment = Alignment.Center,
                            ) {
                                StampImage(
                                    imageUri = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextDialog(
    title: String,
    placeholder: String,
    initial: String = "",
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    ScraplyDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            ScraplyOutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = placeholder,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            ScraplyDialogCancelButton(onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            ScraplyDialogConfirmButton(label = confirmLabel, onClick = { onConfirm(value) })
        },
    )
}
