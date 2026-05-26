package com.example.scraply.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import com.example.scraply.R
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    enum class StampStyle(val label: String) {
        CLASSIC("Classic"),
        VINTAGE("Vintage"),
        VINTAGE_2("Vintage 2"),
    }

    enum class StampFrameStyle(val label: String) {
        FULL_BLEED("Full"),
        CLEAN_FRAME("Frame"),
        NO_STROKE("No stroke"),
    }

    /** Load a bitmap from a content Uri, correcting orientation. */
    fun loadBitmap(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        input.use { BitmapFactory.decodeStream(it, null, opts) }
        val sample = run {
            var s = 1
            val longer = maxOf(opts.outWidth, opts.outHeight)
            while (longer / s > maxDim) s *= 2
            s
        }
        val input2 = context.contentResolver.openInputStream(uri) ?: return null
        val bmp = input2.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        return bmp
    }

    /**
     * Render the given source bitmap into a postage-stamp shaped output bitmap.
     * [translate] and [scale] describe how the photo is positioned inside the frame
     * (normalized coordinates relative to the stamp output size).
     */
    fun renderPostageStamp(
        source: Bitmap,
        outSizePx: Int = 1024,
        translateX: Float = 0f,
        translateY: Float = 0f,
        scale: Float = 1f,
    ): Bitmap {
        val aspect = 0.78f
        val outW = outSizePx
        val outH = (outSizePx / aspect).toInt()
        val result = createBitmap(outW, outH)
        val canvas = Canvas(result)

        val mask = createBitmap(outW, outH)
        val maskCanvas = Canvas(mask)
        val androidPath = buildAndroidPath(outW.toFloat(), outH.toFloat(), 12)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        maskCanvas.drawPath(androidPath, maskPaint)

        val srcBmp = createBitmap(outW, outH)
        val srcCanvas = Canvas(srcBmp)
        srcCanvas.drawColor(android.graphics.Color.WHITE)

        val srcAspect = source.width.toFloat() / source.height.toFloat()
        val outAspect = outW.toFloat() / outH.toFloat()
        val baseScale = if (srcAspect > outAspect) {
            outH.toFloat() / source.height.toFloat()
        } else {
            outW.toFloat() / source.width.toFloat()
        }
        val finalScale = baseScale * scale
        val drawW = source.width * finalScale
        val drawH = source.height * finalScale
        val cx = outW / 2f + translateX * outW
        val cy = outH / 2f + translateY * outH
        val dst = RectF(cx - drawW / 2f, cy - drawH / 2f, cx + drawW / 2f, cy + drawH / 2f)
        srcCanvas.drawBitmap(source, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(srcBmp, 0f, 0f, paint)
        paint.xfermode = null

        srcBmp.recycle()
        mask.recycle()
        return result
    }

    /** Save a bitmap to app internal files/stamps and return the file URI string. */
    fun saveStampPng(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "stamps").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.toURI().toString()
    }

    /** Save a bitmap to the device's MediaStore (Pictures/Scraply). */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Scraply")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    /** Rotate a bitmap by degrees. */
    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    /**
     * Apply gesture transform (pan + zoom) to a bitmap.
     * Used for repositioning stamp in crop screen.
     *
     * @param source Source bitmap
     * @param scale Zoom factor (1f = original size)
     * @param offsetX Horizontal pan offset (-1 to 1, normalized to bitmap width)
     * @param offsetY Vertical pan offset (-1 to 1, normalized to bitmap height)
     * @param viewportWidth Width of the display viewport
     * @param viewportHeight Height of the display viewport
     */
    fun applyGestureTransform(
        source: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        viewportWidth: Int,
        viewportHeight: Int,
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fill with white background
        canvas.drawColor(android.graphics.Color.WHITE)

        val matrix = Matrix()

        // Calculate the center offset based on gesture
        val centerOffsetX = offsetX * source.width
        val centerOffsetY = offsetY * source.height

        // Scale around center
        matrix.setScale(scale, scale, source.width / 2f, source.height / 2f)

        // Translate to new position
        matrix.postTranslate(centerOffsetX, centerOffsetY)

        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        return result
    }

    fun renderStyledStamp(
        context: Context,
        source: Bitmap,
        style: StampStyle,
        frameStyle: StampFrameStyle,
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val fullRect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())

        drawStyledBitmap(
            canvas = canvas,
            source = source,
            dst = fullRect,
            style = style,
        )

        if (frameStyle == StampFrameStyle.CLEAN_FRAME) {
            val insetX = source.width * 0.13f
            val insetY = source.height * 0.12f
            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(250, 246, 235)
            }
            canvas.drawRect(0f, 0f, source.width.toFloat(), insetY, framePaint)
            canvas.drawRect(0f, source.height - insetY, source.width.toFloat(), source.height.toFloat(), framePaint)
            canvas.drawRect(0f, insetY, insetX, source.height - insetY, framePaint)
            canvas.drawRect(source.width - insetX, insetY, source.width.toFloat(), source.height - insetY, framePaint)
        }

        drawStyleTexture(context, canvas, fullRect, style)

        val mask = renderStampMask(context, source.width, source.height)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(mask, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        mask.recycle()

        if (frameStyle != StampFrameStyle.NO_STROKE) {
            drawStampStroke(context, canvas, source.width, source.height)
        }

        return result
    }

    private fun renderStampMask(context: Context, width: Int, height: Int): Bitmap {
        val drawable = context.getDrawable(R.drawable.stamp_mask)
            ?: throw IllegalStateException("Stamp mask drawable missing")
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun drawStyledBitmap(
        canvas: Canvas,
        source: Bitmap,
        dst: RectF,
        style: StampStyle,
    ) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, null, dst, paint)
    }

    private fun drawStyleTexture(
        context: Context,
        canvas: Canvas,
        dst: RectF,
        style: StampStyle,
    ) {
        val textureRes = when (style) {
            StampStyle.CLASSIC -> return
            StampStyle.VINTAGE -> R.drawable.stamp_filter1
            StampStyle.VINTAGE_2 -> R.drawable.stamp_vintage2
        }
        val texture = BitmapFactory.decodeResource(context.resources, textureRes) ?: return
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            alpha = when (style) {
                StampStyle.CLASSIC -> 0
                StampStyle.VINTAGE -> (255 * 0.6f).toInt()
                StampStyle.VINTAGE_2 -> 255
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = when (style) {
                    StampStyle.CLASSIC -> null
                    StampStyle.VINTAGE -> BlendMode.OVERLAY
                    StampStyle.VINTAGE_2 -> BlendMode.COLOR_DODGE
                }
            } else {
                xfermode = when (style) {
                    StampStyle.CLASSIC -> null
                    StampStyle.VINTAGE -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    StampStyle.VINTAGE_2 -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                }
            }
        }
        canvas.drawBitmap(texture, null, dst, paint)
        paint.xfermode = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = null
        }
        texture.recycle()
    }

    private fun drawStampStroke(
        context: Context,
        canvas: Canvas,
        width: Int,
        height: Int,
    ) {
        val stroke = BitmapFactory.decodeResource(context.resources, R.drawable.stamp_stroke) ?: return
        canvas.drawBitmap(
            stroke,
            null,
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        stroke.recycle()
    }
}

