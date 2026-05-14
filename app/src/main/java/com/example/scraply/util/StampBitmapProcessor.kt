package com.example.scraply.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.media.ExifInterface
import androidx.compose.ui.unit.IntSize
import com.example.scraply.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Processes captured images into stamp-shaped bitmaps.
 * Based on LiveStamp's StampBitmapProcessor implementation.
 */
object StampBitmapProcessor {

    /**
     * Process a captured bitmap: crop + apply mask.
     *
     * @param source Source bitmap from camera capture
     * @param previewSize Preview view dimensions
     * @param cutterScale Scale factor for repositioning
     */
    suspend fun cut(
        context: Context,
        source: Bitmap,
        previewSize: IntSize,
        cutterScale: Float = 1f,
    ): Bitmap = withContext(Dispatchers.Default) {
        cutRaw(context, source, previewSize.width, previewSize.height, cutterScale)
    }

    /**
     * Cut raw bitmap based on cutter geometry and apply mask.
     * Logic matches LiveStamp's StampBitmapProcessor.cutRaw()
     */
    fun cutRaw(
        context: Context,
        source: Bitmap,
        previewWidth: Int,
        previewHeight: Int,
        cutterScale: Float = 1f,
    ): Bitmap {
        if (previewWidth <= 0 || previewHeight <= 0) {
            return applyMask(context, source)
        }

        // Calculate crop region using LiveStamp's logic
        val cropRegion = CutterGeometry.calculateCropRegion(
            sourceWidth = source.width,
            sourceHeight = source.height,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
            cutterScale = cutterScale,
        )

        // Convert to integers
        val left = cropRegion.left.roundToInt().coerceIn(0, source.width - 1)
        val top = cropRegion.top.roundToInt().coerceIn(0, source.height - 1)
        val width = cropRegion.width().roundToInt().coerceAtLeast(1)
        val height = cropRegion.height().roundToInt().coerceAtLeast(1)

        // Clamp to source bounds
        val right = (left + width).coerceAtMost(source.width)
        val bottom = (top + height).coerceAtMost(source.height)
        val finalWidth = (right - left).coerceAtLeast(1)
        val finalHeight = (bottom - top).coerceAtLeast(1)

        // Create cropped bitmap
        val cropped = Bitmap.createBitmap(source, left, top, finalWidth, finalHeight)

        // Apply stamp mask
        return applyMask(context, cropped)
    }

    /**
     * Apply stamp mask using PorterDuff.DST_IN.
     */
    private fun applyMask(context: Context, source: Bitmap): Bitmap {
        val maskDrawable = context.getDrawable(R.drawable.stamp_mask)
            ?: throw IllegalStateException("Stamp mask drawable missing")

        val mask = renderMaskDrawable(maskDrawable, source.width, source.height)

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, 0f, 0f, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)

        mask.recycle()

        return result
    }

    /**
     * Render drawable to bitmap.
     */
    private fun renderMaskDrawable(
        drawable: android.graphics.drawable.Drawable,
        width: Int,
        height: Int,
    ): Bitmap {
        val w = maxOf(width, 1)
        val h = maxOf(height, 1)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val drawableW = drawable.intrinsicWidth.takeIf { it > 0 } ?: w
        val drawableH = drawable.intrinsicHeight.takeIf { it > 0 } ?: h

        val scaleX = w.toFloat() / drawableW
        val scaleY = h.toFloat() / drawableH
        val scale = minOf(scaleX, scaleY)

        canvas.save()
        canvas.scale(scale, scale)

        val scaledW = (drawableW * scale).toInt()
        val scaledH = (drawableH * scale).toInt()
        val offsetX = (w - scaledW) / 2f / scale
        val offsetY = (h - scaledH) / 2f / scale

        drawable.setBounds(
            offsetX.toInt(),
            offsetY.toInt(),
            (offsetX + drawableW).toInt(),
            (offsetY + drawableH).toInt()
        )
        drawable.draw(canvas)
        canvas.restore()

        return bitmap
    }

    /**
     * Blocking version.
     */
    fun cutBlocking(
        context: Context,
        source: Bitmap,
        previewSize: IntSize,
        cutterScale: Float = 1f,
    ): Bitmap {
        return cutRaw(context, source, previewSize.width, previewSize.height, cutterScale)
    }

    /**
     * Load bitmap with EXIF rotation handling.
     */
    fun loadBitmapWithExifRotation(filePath: String): Bitmap? {
        val options = BitmapFactory.Options().apply { inMutable = false }
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        options.inJustDecodeBounds = false
        options.inSampleSize = 1

        val bitmap = BitmapFactory.decodeFile(filePath, options) ?: return null

        val exif = ExifInterface(filePath)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()
        return rotated
    }
}
