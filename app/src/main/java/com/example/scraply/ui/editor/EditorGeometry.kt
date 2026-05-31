package com.example.scraply.ui.editor

import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal object EditorGeometry {
    const val CanvasBaseHeightDp = 600f
    const val StampWidthDp = 160f
    const val StampAspectRatio = 147f / 190f

    fun fitStampInsideCanvas(element: CanvasElement, canvasAspectRatio: Float): CanvasElement {
        if (element.type != CanvasElementType.STAMP) return element

        val bounds = stampHalfExtents(
            canvasAspectRatio = canvasAspectRatio,
            scale = element.scale,
            rotation = element.rotation,
        )
        return element.copy(
            x = coerceCenterInsideCanvas(element.x, bounds.width),
            y = coerceCenterInsideCanvas(element.y, bounds.height),
        )
    }

    fun stampHalfExtents(
        canvasAspectRatio: Float,
        scale: Float,
        rotation: Float,
    ): HalfExtents {
        val stampHeightDp = StampWidthDp / StampAspectRatio
        val angleRad = rotation * Math.PI / 180.0
        val boundsCos = abs(cos(angleRad)).toFloat()
        val boundsSin = abs(sin(angleRad)).toFloat()
        val boundsWidthDp = (StampWidthDp * boundsCos + stampHeightDp * boundsSin) * scale
        val boundsHeightDp = (StampWidthDp * boundsSin + stampHeightDp * boundsCos) * scale

        return HalfExtents(
            width = boundsWidthDp / (CanvasBaseHeightDp * canvasAspectRatio) / 2f,
            height = boundsHeightDp / CanvasBaseHeightDp / 2f,
        )
    }

    fun coerceCenterInsideCanvas(value: Float, halfExtent: Float): Float {
        val minValue = min(halfExtent, 1f - halfExtent)
        val maxValue = max(halfExtent, 1f - halfExtent)
        return value.coerceIn(minValue, maxValue)
    }
}

internal data class HalfExtents(
    val width: Float,
    val height: Float,
)
