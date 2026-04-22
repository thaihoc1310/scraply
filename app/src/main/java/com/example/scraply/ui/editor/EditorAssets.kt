package com.example.scraply.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.Stamp
import com.example.scraply.util.PostageStampShape

val BackgroundOptions = listOf(
    "paper" to "Paper",
    "soft_paper" to "Soft Paper",
    "plain" to "Plain",
    "grid" to "Grid",
)

data class AssetOption(val key: String, val label: String, val type: CanvasElementType)

val TapeAssets = (1..6).map { AssetOption("tape_$it", "Tape %02d".format(it), CanvasElementType.TAPE) }
val StickerAssets = (1..7).map { AssetOption("sticker_$it", "Sticker %02d".format(it), CanvasElementType.STICKER) }
val PaperCutAssets = (1..5).map { AssetOption("papercut_$it", "Paper %02d".format(it), CanvasElementType.PAPER_CUT) }

val FontPresets = listOf(
    "classic_serif" to "Classic Serif",
    "fz_kingshare" to "Fz Kingshare",
)

@Composable
fun BackgroundSurface(backgroundType: String, modifier: Modifier = Modifier) {
    when (backgroundType) {
        "plain" -> Box(modifier = modifier.background(Color(0xFFFFFDF9)))
        "grid" -> Box(modifier = modifier.background(Color(0xFFFBF7EF))) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 28f
                val strokeColor = Color(0xFFEFE6D8)
                var x = 0f
                while (x < size.width) {
                    drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
        }
        "soft_paper" -> Box(modifier = modifier.background(Color(0xFFF7F1E8)))
        else /* paper */ -> Box(modifier = modifier.background(Color(0xFFF1EADE)))
    }
}

/** Renders a placeholder visual for each element type. Real assets can replace these later. */
@Composable
fun CanvasElementView(
    element: CanvasElement,
    stamps: List<Stamp>,
    modifier: Modifier = Modifier,
) {
    when (element.type) {
        CanvasElementType.STAMP -> {
            val stamp = stamps.firstOrNull { it.id == element.stampId }
            Box(
                modifier = modifier
                    .size(width = 160.dp, height = 190.dp)
                    .clip(PostageStampShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (stamp != null) {
                    AsyncImage(
                        model = stamp.imageUri,
                        contentDescription = stamp.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        CanvasElementType.POLAROID -> {
            val stamp = stamps.firstOrNull { it.id == element.stampId }
            Box(
                modifier = modifier
                    .size(width = 170.dp, height = 210.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .padding(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(150.dp)
                        .background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (stamp != null) {
                        AsyncImage(
                            model = stamp.imageUri,
                            contentDescription = stamp.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        CanvasElementType.TAPE -> {
            val tint = tapeTint(element.assetKey)
            Box(
                modifier = modifier
                    .width(150.dp)
                    .height(42.dp)
                    .background(tint),
            )
        }
        CanvasElementType.STICKER -> {
            val color = stickerColor(element.assetKey)
            Canvas(modifier = modifier.size(86.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val petals = 8
                val r = size.minDimension / 2f
                val petalPath = Path()
                for (i in 0 until petals) {
                    val ang = (i * 2 * Math.PI / petals).toFloat()
                    val px = cx + kotlin.math.cos(ang) * r * 0.85f
                    val py = cy + kotlin.math.sin(ang) * r * 0.85f
                    petalPath.addOval(
                        androidx.compose.ui.geometry.Rect(
                            left = px - r * 0.32f, top = py - r * 0.2f,
                            right = px + r * 0.32f, bottom = py + r * 0.2f,
                        ),
                    )
                }
                drawPath(petalPath, color)
                drawCircle(Color(0xFFFFF2B0), radius = r * 0.28f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
        CanvasElementType.PAPER_CUT -> {
            Box(
                modifier = modifier
                    .size(width = 140.dp, height = 100.dp)
                    .background(paperCutTint(element.assetKey), RoundedCornerShape(4.dp)),
            )
        }
        CanvasElementType.TEXT -> {
            val family = when (element.font) {
                "fz_kingshare" -> FontFamily.Cursive
                else -> FontFamily.Serif
            }
            Text(
                text = element.text.ifBlank { "Double-tap to edit" },
                style = TextStyle(
                    fontFamily = family,
                    fontSize = 28.sp,
                    color = Color(element.color.toInt().toLong().let { if (it == 0L) 0xFF1F1B18 else it }),
                ),
                modifier = modifier.padding(4.dp),
            )
        }
    }
}

private fun tapeTint(key: String): Color = when (key) {
    "tape_1" -> Color(0xFFE6D3A7)
    "tape_2" -> Color(0xFFEFE4C7)
    "tape_3" -> Color(0xFFCFB89A)
    "tape_4" -> Color(0xFFD6C29F)
    "tape_5" -> Color(0xFFE2CBA5)
    "tape_6" -> Color(0xFFC9B084)
    else -> Color(0xFFE6D3A7)
}

private fun stickerColor(key: String): Color = when (key) {
    "sticker_1" -> Color(0xFFE77B2E)
    "sticker_2" -> Color(0xFFB9BDB1)
    "sticker_3" -> Color(0xFFF2DFA1)
    "sticker_4" -> Color(0xFFEBB8CC)
    "sticker_5" -> Color(0xFFA6C4E0)
    "sticker_6" -> Color(0xFFC6DFA4)
    "sticker_7" -> Color(0xFFE2A3B5)
    else -> Color(0xFFE77B2E)
}

private fun paperCutTint(key: String): Color = when (key) {
    "papercut_1" -> Color(0xFFF3EEDC)
    "papercut_2" -> Color(0xFFD8BE95)
    "papercut_3" -> Color(0xFFE6D9C4)
    "papercut_4" -> Color(0xFFCFA77A)
    "papercut_5" -> Color(0xFFE9D8BA)
    else -> Color(0xFFF3EEDC)
}
