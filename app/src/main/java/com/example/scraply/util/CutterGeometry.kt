package com.example.scraply.util

import android.graphics.RectF
import androidx.compose.ui.unit.IntSize

/**
 * Geometry calculations for stamp cutter overlay positioning.
 * Based on LiveStamp's CutterGeometry implementation.
 */
object CutterGeometry {

    // Reference dimensions (designed for 393dp screen width)
    const val refScreenWidth = 393f
    const val cutterWidth = 282f
    const val cutterHeight = 372f

    // Stamp output dimensions (the hole size)
    const val stampWidth = 147f
    const val stampHeight = 190f

    // Inner window proportions (relative to cutter dimensions)
    // These define the actual hole size in cutter_frame.png
    const val innerX = 0.2393617f
    const val innerY = 0.24462366f
    const val innerW = 0.5212766f
    const val innerH = 0.5107527f

    /**
     * Calculate stamp window rect for given preview size.
     * Uses stamp aspect ratio (147:190).
     */
    fun stampWindowRect(previewWidth: Int, previewHeight: Int): RectF {
        val w = previewWidth.toFloat()
        val h = previewHeight.toFloat()

        val stampW = w * stampWidth / refScreenWidth
        val stampH = stampW * stampHeight / stampWidth

        val left = (w - stampW) / 2f
        val top = (h - stampH) / 2f

        return RectF(left, top, left + stampW, top + stampH)
    }

    /**
     * Calculate stamp window rect with IntSize.
     */
    fun stampWindowRect(previewSize: IntSize): RectF {
        return stampWindowRect(previewSize.width, previewSize.height)
    }

    /**
     * Calculate the crop region in source coordinates using innerRect bounds.
     * This matches the hole position in cutter_frame.png.
     *
     * @param sourceWidth Source image width
     * @param sourceHeight Source image height
     * @param previewWidth Preview view width
     * @param previewHeight Preview view height
     * @param cutterScale Scale factor for repositioning
     */
    fun calculateCropRegion(
        sourceWidth: Int,
        sourceHeight: Int,
        previewWidth: Int,
        previewHeight: Int,
        cutterScale: Float = 1f,
    ): RectF {
        val srcW = sourceWidth.toFloat()
        val srcH = sourceHeight.toFloat()
        val prevW = previewWidth.toFloat()
        val prevH = previewHeight.toFloat()

        // Get cutter frame dimensions in preview coordinates
        val cutterW = prevW * cutterWidth / refScreenWidth
        val cutterH = prevW * cutterHeight / refScreenWidth

        // Center the cutter frame
        val cutterLeft = (prevW - cutterW) / 2f
        val cutterTop = (prevH - cutterH) / 2f

        // Calculate innerRect bounds in preview coordinates
        val innerLeft = cutterLeft + innerX * cutterW
        val innerTop = cutterTop + innerY * cutterH
        val innerRight = innerLeft + innerW * cutterW
        val innerBottom = innerTop + innerH * cutterH

        // Apply cutterScale to innerRect
        val scaledWidth = (innerRight - innerLeft) * cutterScale
        val scaledHeight = (innerBottom - innerTop) * cutterScale
        val centerX = (innerLeft + innerRight) / 2f
        val centerY = (innerTop + innerBottom) / 2f

        val scaledLeft = centerX - scaledWidth / 2f
        val scaledTop = centerY - scaledHeight / 2f
        val scaledRight = centerX + scaledWidth / 2f
        val scaledBottom = centerY + scaledHeight / 2f

        // Calculate baseScale - how much to scale preview to fill source
        // Use maxOf because we want the limiting dimension
        val scaleX = srcW / prevW
        val scaleY = srcH / prevH
        val baseScale = maxOf(scaleX, scaleY)

        // Calculate offset to center scaled preview in source
        val scaledW = prevW * baseScale
        val scaledH = prevH * baseScale
        val offsetX = (srcW - scaledW) / 2f
        val offsetY = (srcH - scaledH) / 2f

        // Map preview coordinates to source coordinates
        val cropLeft = scaledLeft * baseScale + offsetX
        val cropTop = scaledTop * baseScale + offsetY
        val cropRight = scaledRight * baseScale + offsetX
        val cropBottom = scaledBottom * baseScale + offsetY

        return RectF(cropLeft, cropTop, cropRight, cropBottom)
    }
}
