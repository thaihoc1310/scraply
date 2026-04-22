package com.example.scraply.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.scraply.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The stamp frame shipped in `assets/stamp.jpg` is a JPEG, so it has no alpha channel -- the inner
 * "window" of the frame is a solid bright white rectangle. This utility performs a one-time white
 * -> transparent conversion so we can composite the frame on top of the camera viewfinder (and on
 * top of saved stamps) and actually see through the window.
 *
 * Pixels whose RGB are all above [whiteThreshold] (default 235) are treated as fully transparent,
 * with a soft ramp for pixels just below that so edges aren't harshly clipped.
 *
 * The processed bitmap is cached per process.
 */
object StampFrameAsset {
    @Volatile
    private var cached: ImageBitmap? = null

    suspend fun load(context: Context): ImageBitmap {
        cached?.let { return it }
        val img = withContext(Dispatchers.IO) { build(context) }
        cached = img
        return img
    }

    private fun build(context: Context): ImageBitmap {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val src = BitmapFactory.decodeResource(
            context.resources, R.drawable.frame_postage_stamp, opts,
        ) ?: return emptyBitmap().asImageBitmap()

        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val threshold = 235
        val soft = 200

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val minBright = minOf(r, g, b)
            when {
                minBright >= threshold -> pixels[i] = 0
                minBright in soft until threshold -> {
                    val t = (minBright - soft).toFloat() / (threshold - soft)
                    val a = ((1f - t) * 255).toInt().coerceIn(0, 255)
                    pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        src.recycle()
        return out.asImageBitmap()
    }

    private fun emptyBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}
