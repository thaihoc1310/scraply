package com.example.scraply.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.util.PostageStampShape

@Composable
fun StampDetailScreen(
    vm: CollectionsViewModel,
    stampId: String,
    onBack: () -> Unit,
) {
    var stamp by remember { mutableStateOf<Stamp?>(null) }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(stampId) {
        vm.loadStamp(stampId)?.let {
            stamp = it
            title = it.title.orEmpty()
            caption = it.caption.orEmpty()
        }
    }

    val current = stamp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Delete",
                onClick = {
                    vm.deleteStamp(stampId)
                    onBack()
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        if (current != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .clip(PostageStampShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = current.imageUri,
                        contentDescription = current.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Title", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; dirty = true },
                placeholder = { Text("Give it a title") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("Caption", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it; dirty = true },
                placeholder = { Text("What do you see?") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    enabled = dirty,
                    onClick = {
                        vm.updateStamp(stampId, title, caption)
                        dirty = false
                        onBack()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text("Save changes") }
            }
        }
    }
}
