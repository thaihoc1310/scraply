package com.example.scraply.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.IntrinsicSize
import coil.compose.AsyncImage
import com.example.scraply.R
import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.Stamp
import com.example.scraply.ui.common.StampImage

private const val StampAspectRatio = 147f / 190f

val BackgroundOptions = listOf(
    "bg_chalkboard" to "Chalkboard",
    "bg_cork_board" to "Cork Board",
    "bg_crumpled_white" to "Crumpled White",
    "bg_doodle_galaxy" to "Doodle Galaxy",
    "bg_floral_paper" to "Floral Paper",
    "bg_floral_paper_2" to "Floral Paper 2",
    "bg_grid_green" to "Grid Green",
    "bg_kraft_brown" to "Kraft Brown",
    "bg_lavender" to "Lavender Dream",
    "bg_vintage_old" to "Vintage Old",
    "bg_lined_notebook" to "Lined Notebook",
    "bg_linen_beige" to "Linen Beige",
    "bg_midnight_blue" to "Midnight Blue",
    "bg_mint_pastel" to "Mint Pastel",
    "bg_old_newspaper" to "Old Newspaper",
    "bg_pastel_pink" to "Pastel Pink",
    "bg_purple_pastel" to "Purple Pastel",
    "bg_floral_script" to "Floral Script",
    "bg_red_grid" to "Red Grid Note",
    "bg_starry_night" to "Starry Night",
    "bg_vintage_cream" to "Vintage Cream",
    "bg_wood_plank" to "Wood Plank",
)

/** Maps background key to its asset file path in assets/backgrounds/ */
private val backgroundAssetMap = mapOf(
    "bg_chalkboard" to "backgrounds/chalkboard_texture_dark.jpg",
    "bg_cork_board" to "backgrounds/cork_board_texture.jpg",
    "bg_crumpled_white" to "backgrounds/crumpled_white_paper.jpg",
    "bg_doodle_galaxy" to "backgrounds/doodle_galaxy.jpg",
    "bg_floral_paper" to "backgrounds/floral_paper.jpg",
    "bg_floral_paper_2" to "backgrounds/floral_paper_2.jpg",
    "bg_grid_green" to "backgrounds/grid_green.jpg",
    "bg_kraft_brown" to "backgrounds/kraft_paper_brown_texture.jpg",
    "bg_lavender" to "backgrounds/lavender_dream.jpg",
    "bg_vintage_old" to "backgrounds/lilydust-old-2012064.jpg",
    "bg_lined_notebook" to "backgrounds/lined_notebook_paper.jpg",
    "bg_linen_beige" to "backgrounds/linen_fabric_texture_beige.jpg",
    "bg_midnight_blue" to "backgrounds/midnight_collage_blue.jpg",
    "bg_mint_pastel" to "backgrounds/mint_green_pastel_texture.jpg",
    "bg_old_newspaper" to "backgrounds/old_newspaper_texture.jpg",
    "bg_pastel_pink" to "backgrounds/pastel_pink_paper_soft.jpg",
    "bg_purple_pastel" to "backgrounds/purple_pastel.jpg",
    "bg_floral_script" to "backgrounds/ractapopulous-background-2009164_1920.jpg",
    "bg_red_grid" to "backgrounds/red_grid_note.jpg",
    "bg_starry_night" to "backgrounds/starry_night_yellow.jpg",
    "bg_vintage_cream" to "backgrounds/vintage_paper_texture_cream.jpg",
    "bg_wood_plank" to "backgrounds/wood_plank_texture.jpg",
)

data class AssetOption(val key: String, val label: String, val type: CanvasElementType)

val TapeAssets = (1..8).map { AssetOption("tape_$it", "Tape %02d".format(it), CanvasElementType.TAPE) }
val StickerAssets = (1..17).map { AssetOption("sticker_$it", "Sticker %02d".format(it), CanvasElementType.STICKER) }
val PaperCutAssets = (1..6).map { AssetOption("papercut_$it", "Paper Cut %02d".format(it), CanvasElementType.PAPER_CUT) }

/** Maps sticker key to its asset file path in assets/stickers/ */
private val stickerAssetMap = (1..17).associate { "sticker_$it" to "stickers/sticker_%02d.png".format(it) }

/** Maps papercut key to its asset file path in assets/papercuts/ */
private val paperCutAssetMap = (1..6).associate { "papercut_$it" to "paper_cuts/paper_cut_%02d.png".format(it) }

/** Maps tape key to its asset file path in assets/tapes/ */
private val tapeAssetMap = mapOf(
    "tape_1" to "tapes/Tape_01.png",
    "tape_2" to "tapes/Tape_02.png",
    "tape_3" to "tapes/Tape_03.png",
    "tape_4" to "tapes/Tape_04.png",
    "tape_5" to "tapes/Tape_05.png",
    "tape_6" to "tapes/Tape_06.png",
    "tape_7" to "tapes/Tape_07.png",
    "tape_8" to "tapes/Tape_08.png",
)

val FontPresets = listOf(
    "classic_serif" to "Classic Serif",
    "fz_kingshare" to "Cursive",
    "dancing_script" to "Dancing Script",
    "pacifico" to "Pacifico",
    "caveat" to "Caveat",
    "playfair" to "Playfair Display",
    "lobster" to "Lobster",
    "indie_flower" to "Indie Flower",
    "great_vibes" to "Great Vibes",
    "sacramento" to "Sacramento",
    "permanent_marker" to "Permanent Marker",
    "architects_daughter" to "Architects Daughter",
)

/** Maps font key to its FontFamily */
internal val fontFamilyMap = mapOf(
    "classic_serif" to FontFamily.Serif,
    "fz_kingshare" to FontFamily.Cursive,
    "dancing_script" to FontFamily(Font(R.font.dancingscript_variablefont_wght)),
    "pacifico" to FontFamily(Font(R.font.pacifico_regular)),
    "caveat" to FontFamily(Font(R.font.caveat_variablefont_wght)),
    "playfair" to FontFamily(Font(R.font.playfairdisplaysc_blackitalic)),
    "lobster" to FontFamily(Font(R.font.lobster_regular)),
    "indie_flower" to FontFamily(Font(R.font.indieflower_regular)),
    "great_vibes" to FontFamily(Font(R.font.greatvibes_regular)),
    "sacramento" to FontFamily(Font(R.font.sacramento_regular)),
    "permanent_marker" to FontFamily(Font(R.font.permanentmarker_regular)),
    "architects_daughter" to FontFamily(Font(R.font.architectsdaughter_regular)),
)

@Composable
fun BackgroundSurface(backgroundType: String, modifier: Modifier = Modifier) {
    val assetPath = backgroundAssetMap[backgroundType]
    if (assetPath != null) {
        // Load real image from assets/ folder
        Box(modifier = modifier.background(Color(0xFFF1EADE))) {
            AsyncImage(
                model = "file:///android_asset/$assetPath",
                contentDescription = backgroundType,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        // Original code-drawn backgrounds
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
}

/** Renders a placeholder visual for each element type. Real assets can replace these later. */
@Composable
fun CanvasElementView(
    element: CanvasElement,
    stamps: List<Stamp>,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onStartTextEdit: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
) {
    when (element.type) {
        CanvasElementType.STAMP -> {
            val stamp = stamps.firstOrNull { it.id == element.stampId }
            Box(
                modifier = modifier
                    .width(160.dp)
                    .aspectRatio(StampAspectRatio),
                contentAlignment = Alignment.Center,
            ) {
                if (stamp != null) {
                    StampImage(
                        imageUri = stamp.imageUri,
                        contentDescription = stamp.title,
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
                        StampImage(
                            imageUri = stamp.imageUri,
                            contentDescription = stamp.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        CanvasElementType.TAPE -> {
            val assetPath = tapeAssetMap[element.assetKey]
            if (assetPath != null) {
                AsyncImage(
                    model = "file:///android_asset/$assetPath",
                    contentDescription = element.assetKey,
                    contentScale = ContentScale.Fit,
                    modifier = modifier.width(150.dp),
                )
            } else {
                val tint = tapeTint(element.assetKey)
                Box(
                    modifier = modifier
                        .width(150.dp)
                        .height(42.dp)
                        .background(tint),
                )
            }
        }
        CanvasElementType.STICKER -> {
            val assetPath = stickerAssetMap[element.assetKey]
            if (assetPath != null) {
                AsyncImage(
                    model = "file:///android_asset/$assetPath",
                    contentDescription = element.assetKey,
                    contentScale = ContentScale.Fit,
                    modifier = modifier.sizeIn(maxWidth = 150.dp, maxHeight = 150.dp),
                )
            } else {
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
        }
        CanvasElementType.PAPER_CUT -> {
            val assetPath = paperCutAssetMap[element.assetKey]
            if (assetPath != null) {
                AsyncImage(
                    model = "file:///android_asset/$assetPath",
                    contentDescription = element.assetKey,
                    contentScale = ContentScale.Fit,
                    modifier = modifier.sizeIn(maxWidth = 160.dp, maxHeight = 160.dp),
                )
            } else {
                Box(
                    modifier = modifier
                        .size(width = 140.dp, height = 100.dp)
                        .background(paperCutTint(element.assetKey), RoundedCornerShape(4.dp)),
                )
            }
        }
        CanvasElementType.TEXT -> {
            val family = fontFamilyMap[element.font] ?: FontFamily.Serif
            val textStyle = TextStyle(
                fontFamily = family,
                fontSize = 28.sp,
                color = Color(element.color.toInt().toLong().let { if (it == 0L) 0xFF1F1B18 else it }),
            )

            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                val textFieldValue = remember(element.id) {
                    androidx.compose.runtime.mutableStateOf(
                        androidx.compose.ui.text.input.TextFieldValue(
                            text = element.text,
                            selection = androidx.compose.ui.text.TextRange(0, element.text.length),
                        ),
                    )
                }

                androidx.compose.runtime.LaunchedEffect(element.text) {
                    if (element.text != textFieldValue.value.text) {
                        val selStart = textFieldValue.value.selection.start.coerceIn(0, element.text.length)
                        val selEnd = textFieldValue.value.selection.end.coerceIn(0, element.text.length)
                        textFieldValue.value = textFieldValue.value.copy(
                            text = element.text,
                            selection = androidx.compose.ui.text.TextRange(selStart, selEnd),
                        )
                    }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                androidx.compose.foundation.text.BasicTextField(
                    value = textFieldValue.value,
                    onValueChange = {
                        textFieldValue.value = it
                        if (it.text != element.text) {
                            onTextChange(it.text)
                        }
                    },
                    textStyle = textStyle,
                    modifier = modifier
                        .padding(4.dp)
                        .defaultMinSize(minWidth = 20.dp)
                        .width(IntrinsicSize.Min)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (textFieldValue.value.text.isBlank()) {
                                Text(
                                    text = "Tap to edit",
                                    style = textStyle.copy(color = textStyle.color.copy(alpha = 0.55f)),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            } else {
                // Read-only text display - no keyboard, no focus
                Text(
                    text = element.text.ifBlank { "Tap to edit" },
                    style = textStyle.let {
                        if (element.text.isBlank()) it.copy(color = it.color.copy(alpha = 0.55f)) else it
                    },
                    modifier = modifier.padding(4.dp),
                )
            }
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
