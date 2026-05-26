package com.example.scraply.ui.calendar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.util.PostageStampShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    onOpenDate: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val stamps by vm.stamps.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var compact by remember { mutableStateOf(false) }

    val grouped = remember(stamps, month) {
        val zone = ZoneId.systemDefault()
        stamps.groupBy {
            LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.createdAt), zone)
        }.filterKeys { it.month == month.month && it.year == month.year }
    }

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
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "%02d".format(month.monthValue),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${month.year}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                onClick = { month = month.minusMonths(1) },
            )
            Spacer(Modifier.width(4.dp))
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                onClick = { month = month.plusMonths(1) },
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                .padding(4.dp),
        ) {
            CompactFullChip(
                label = "Compact",
                selected = compact,
                onClick = { compact = true },
                modifier = Modifier.weight(1f),
            )
            CompactFullChip(
                label = "Full",
                selected = !compact,
                onClick = { compact = false },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        MonthGrid(
            month = month,
            compact = compact,
            stampsByDay = grouped,
            onDateClick = { day ->
                onOpenDate(day.toEpochDay())
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CompactFullChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
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
private fun MonthGrid(
    month: YearMonth,
    compact: Boolean,
    stampsByDay: Map<LocalDate, List<Stamp>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstOfMonth = month.atDay(1)
    val daysBefore = firstOfMonth.dayOfWeek.value % 7
    val totalDays = month.lengthOfMonth()
    val gridCells = mutableListOf<LocalDate?>()
    val prevMonth = month.minusMonths(1)
    val prevMonthLen = prevMonth.lengthOfMonth()
    for (i in daysBefore downTo 1) gridCells.add(prevMonth.atDay(prevMonthLen - i + 1))
    for (d in 1..totalDays) gridCells.add(month.atDay(d))
    val nextMonth = month.plusMonths(1)
    var nextIdx = 1
    while (gridCells.size < 42) {
        gridCells.add(nextMonth.atDay(nextIdx))
        nextIdx += 1
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(gridCells.size) { idx ->
            val date = gridCells[idx] ?: return@items
            val inMonth = date.month == month.month && date.year == month.year
            val stamps = stampsByDay[date].orEmpty()
            DateCell(
                date = date,
                inMonth = inMonth,
                compact = compact,
                stamps = stamps,
                onClick = { if (inMonth) onDateClick(date) },
            )
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    inMonth: Boolean,
    compact: Boolean,
    stamps: List<Stamp>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(if (compact) 1f else 0.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            color = if (inMonth) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        if (stamps.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = if (compact) 22.dp else 30.dp, height = if (compact) 26.dp else 36.dp)
                        .clip(PostageStampShape)
                        .background(Color.White),
                ) {
                    AsyncImage(
                        model = stamps.first().imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarDateDetailScreen(
    vm: CalendarViewModel,
    epochDay: Long,
    onBack: () -> Unit,
    onOpenStamp: (String) -> Unit,
) {
    val stamps by vm.stamps.collectAsState()
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val zone = ZoneId.systemDefault()
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val list = stamps.filter { it.createdAt in start..end }

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
            Spacer(Modifier.width(12.dp))
            Text(
                "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No stamps from this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(list.size) { idx ->
                    val s = list[idx]
                    Column(
                        modifier = Modifier.clickable { onOpenStamp(s.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .clip(PostageStampShape)
                                .background(Color.White),
                        ) {
                            AsyncImage(
                                model = s.imageUri,
                                contentDescription = s.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            s.title.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!s.caption.isNullOrBlank()) {
                            Text(
                                s.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
