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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.CircleIconButton

private const val StampAspectRatio = 147f / 190f

@Composable
fun CollectionDetailScreen(
    vm: CollectionsViewModel,
    collectionId: String,
    onBack: () -> Unit,
    onOpenStamp: (String) -> Unit,
) {
    val stamps by vm.stampsIn(collectionId).collectAsState(initial = emptyList())
    val cards by vm.cards.collectAsState()
    val all by vm.allStamps.collectAsState()
    val current = cards.firstOrNull { it.collection.id == collectionId }
    val isDefault = current?.collection?.isDefault == true
    val title = current?.collection?.name ?: "Collection"

    var showAdd by remember { mutableStateOf(false) }

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
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.weight(1f))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (!isDefault) {
                CircleIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add stamps",
                    onClick = { showAdd = true },
                )
            } else {
                Spacer(Modifier.padding(start = 44.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (stamps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No stamps yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(stamps, key = { it.id }) { stamp ->
                    StampGridItem(stamp = stamp, onOpen = { onOpenStamp(stamp.id) })
                }
            }
        }
    }

    if (showAdd) {
        val existingIds = remember(stamps) { stamps.map { it.id }.toSet() }
        val selectable = all.filter { it.id !in existingIds }
        AddStampsDialog(
            candidates = selectable,
            onPick = { id ->
                vm.addStampToCollection(collectionId, id)
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun StampGridItem(stamp: Stamp, onOpen: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(StampAspectRatio),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = stamp.imageUri,
                contentDescription = stamp.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stamp.title ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddStampsDialog(
    candidates: List<Stamp>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add stamps") },
        text = {
            if (candidates.isEmpty()) {
                Text("Nothing left to add here.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp),
                ) {
                    items(candidates, key = { it.id }) { stamp ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(StampAspectRatio)
                                .clickable { onPick(stamp.id) },
                        ) {
                            AsyncImage(
                                model = stamp.imageUri,
                                contentDescription = stamp.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
