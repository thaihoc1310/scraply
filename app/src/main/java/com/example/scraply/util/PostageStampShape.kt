package com.example.scraply.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Classic postage-stamp shape with perforated half-circle edges on all four sides.
 */
object PostageStampShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(buildPath(size, perforationsAcross = 12))

    fun buildPath(size: Size, perforationsAcross: Int = 12): Path =
        buildAndroidPath(size.width, size.height, perforationsAcross).asComposePath()
}

fun buildAndroidPath(width: Float, height: Float, perforationsAcross: Int = 12): android.graphics.Path {
    val w = width
    val h = height
    val perfSize = w / (perforationsAcross + 1).toFloat()
    val inset = perfSize / 2f

    val perfH = (w - inset * 2f) / perforationsAcross
    val perfVCount = maxOf(1, ((h - inset * 2f) / perfH).toInt())
    val perfV = (h - inset * 2f) / perfVCount

    val path = android.graphics.Path()
    path.moveTo(inset, inset)

    var x = inset
    while (x < w - inset - 0.01f) {
        val next = (x + perfH).coerceAtMost(w - inset)
        val cx = (x + next) / 2f
        val r = (next - x) / 2f * 0.9f
        path.lineTo(cx - r, inset)
        path.arcTo(cx - r, inset - r, cx + r, inset + r, 180f, 180f, false)
        path.lineTo(next, inset)
        x = next
    }

    var y = inset
    while (y < h - inset - 0.01f) {
        val next = (y + perfV).coerceAtMost(h - inset)
        val cy = (y + next) / 2f
        val r = (next - y) / 2f * 0.9f
        path.lineTo(w - inset, cy - r)
        path.arcTo(w - inset - r, cy - r, w - inset + r, cy + r, 270f, 180f, false)
        path.lineTo(w - inset, next)
        y = next
    }

    x = w - inset
    while (x > inset + 0.01f) {
        val next = (x - perfH).coerceAtLeast(inset)
        val cx = (x + next) / 2f
        val r = (x - next) / 2f * 0.9f
        path.lineTo(cx + r, h - inset)
        path.arcTo(cx - r, h - inset - r, cx + r, h - inset + r, 0f, 180f, false)
        path.lineTo(next, h - inset)
        x = next
    }

    y = h - inset
    while (y > inset + 0.01f) {
        val next = (y - perfV).coerceAtLeast(inset)
        val cy = (y + next) / 2f
        val r = (y - next) / 2f * 0.9f
        path.lineTo(inset, cy + r)
        path.arcTo(inset - r, cy - r, inset + r, cy + r, 90f, 180f, false)
        path.lineTo(inset, next)
        y = next
    }

    path.close()
    return path
}
