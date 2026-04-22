package com.example.scraply.ui.stamp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.scraply.util.StampFrameAsset
import com.example.scraply.util.buildAndroidPath

/**
 * Renders a postage-stamp viewfinder shape on top of live camera or photo content.
 *
 * Priority:
 *  1. The real `assets/stamp.jpg` frame is loaded once at runtime with near-white pixels converted
 *     to fully transparent, and overlaid as the actual frame. The inner window is see-through so
 *     the camera preview shows through it.
 *  2. As a fallback (until the bitmap finishes loading) we render the programmatic
 *     `PostageStampShape` with the outside dimmed, so the user always sees a stamp silhouette.
 */
@Composable
fun StampFrameOverlay(
    modifier: Modifier = Modifier,
    aspect: Float = 0.78f,
    dimOutside: Boolean = true,
) {
    val context = LocalContext.current
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        runCatching { frame = StampFrameAsset.load(context) }
    }

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(aspect),
        contentAlignment = Alignment.Center,
    ) {
        if (frame != null) {
            Image(
                painter = BitmapPainter(frame!!),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (dimOutside) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stampPath = buildAndroidPath(size.width, size.height).asComposePath()
                val outer = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                }
                val hole = Path.combine(
                    operation = androidx.compose.ui.graphics.PathOperation.Difference,
                    path1 = outer,
                    path2 = stampPath,
                )
                drawPath(hole, color = Color.Black.copy(alpha = 0.35f))
                drawPath(
                    path = stampPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}
