package com.example.scraply.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.example.scraply.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates the camera overlay bitmap (cutter frame).
 * Based on LiveStamp's CutterOverlayFactory implementation.
 */
object CutterOverlayFactory {

    /**
     * Create the overlay bitmap.
     * @param context Android context
     * @param stampHoleBlack If true, the hole is solid black (for black hole animation).
     */
    suspend fun create(context: Context, stampHoleBlack: Boolean = false): Bitmap = withContext(Dispatchers.Default) {
        createSync(context, stampHoleBlack)
    }

    /**
     * Synchronous version.
     */
    fun createSync(context: Context, stampHoleBlack: Boolean = false): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val cutterFrame = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.cutter_frame,
            options
        ) ?: throw IllegalStateException("cutter_frame not found")

        val result = cutterFrame.copy(Bitmap.Config.ARGB_8888, true)
        cutterFrame.recycle()

        // Calculate inner rect for this cutter size
        val innerRect = calculateInnerRect(result.width, result.height)

        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (stampHoleBlack) {
            paint.color = android.graphics.Color.BLACK
            paint.style = Paint.Style.FILL
            canvas.drawRect(innerRect, paint)
        } else {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            canvas.drawRect(innerRect, paint)
            paint.xfermode = null
            paint.color = android.graphics.Color.TRANSPARENT
            paint.style = Paint.Style.FILL
            canvas.drawRect(innerRect, paint)
        }

        return result
    }

    /**
     * Calculate inner rect for given cutter dimensions.
     */
    private fun calculateInnerRect(cutterWidth: Int, cutterHeight: Int): RectF {
        val w = cutterWidth.toFloat()
        val h = cutterHeight.toFloat()

        // Inner proportions from CutterGeometry
        val innerX = CutterGeometry.innerX
        val innerY = CutterGeometry.innerY
        val innerW = CutterGeometry.innerW
        val innerH = CutterGeometry.innerH

        val left = w * innerX
        val top = h * innerY
        val right = left + w * innerW
        val bottom = top + h * innerH

        return RectF(left, top, right, bottom)
    }
}

/**
 * Renders the stamp mask drawable to a bitmap.
 */
object StampMaskRenderer {
    fun renderMask(context: Context, width: Int, height: Int): Bitmap {
        val w = maxOf(width, 1)
        val h = maxOf(height, 1)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val drawable = context.getDrawable(R.drawable.stamp_mask)
            ?: throw IllegalStateException("stamp_mask not found")

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
}
