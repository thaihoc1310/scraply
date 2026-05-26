package com.example.scraply.ui.stamp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.scraply.util.CutterGeometry

/**
 * Debug overlay showing the actual stamp hole bounds (innerRect)
 * inside the cutter frame, matching cutter_frame.png.
 *
 * The hole in cutter_frame is defined by CutterGeometry.innerX/Y/W/H proportions.
 */
@Composable
fun CutterDebugOverlay(
    modifier: Modifier = Modifier,
) {
    val cutterAspect = CutterGeometry.cutterWidth / CutterGeometry.cutterHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cutterAspect),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate innerRect bounds (this matches the hole in cutter_frame.png)
            // These proportions are extracted from the cutter_frame design
            val innerLeft = CutterGeometry.innerX * width
            val innerTop = CutterGeometry.innerY * height
            val innerRight = (CutterGeometry.innerX + CutterGeometry.innerW) * width
            val innerBottom = (CutterGeometry.innerY + CutterGeometry.innerH) * height

            val innerWidth = innerRight - innerLeft
            val innerHeight = innerBottom - innerTop

            // Draw filled area (red) - this is what the hole looks like
            drawRect(
                color = Color.Red.copy(alpha = 0.3f),
                topLeft = Offset(innerLeft, innerTop),
                size = Size(innerWidth, innerHeight),
            )

            // Draw border (red solid)
            drawRect(
                color = Color.Red,
                topLeft = Offset(innerLeft, innerTop),
                size = Size(innerWidth, innerHeight),
                style = Stroke(width = 3f)
            )

            // Draw center crosshair (green)
            val centerX = (innerLeft + innerRight) / 2
            val centerY = (innerTop + innerBottom) / 2
            val crossSize = 20f

            drawLine(
                color = Color.Green,
                start = Offset(centerX - crossSize, centerY),
                end = Offset(centerX + crossSize, centerY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Green,
                start = Offset(centerX, centerY - crossSize),
                end = Offset(centerX, centerY + crossSize),
                strokeWidth = 3f
            )

            // Draw corner markers (yellow)
            val cornerSize = 15f
            val stroke = 3f

            // Top-left
            drawLine(Color.Yellow, Offset(innerLeft, innerTop), Offset(innerLeft + cornerSize, innerTop), stroke)
            drawLine(Color.Yellow, Offset(innerLeft, innerTop), Offset(innerLeft, innerTop + cornerSize), stroke)

            // Top-right
            drawLine(Color.Yellow, Offset(innerRight - cornerSize, innerTop), Offset(innerRight, innerTop), stroke)
            drawLine(Color.Yellow, Offset(innerRight, innerTop), Offset(innerRight, innerTop + cornerSize), stroke)

            // Bottom-left
            drawLine(Color.Yellow, Offset(innerLeft, innerBottom - cornerSize), Offset(innerLeft, innerBottom), stroke)
            drawLine(Color.Yellow, Offset(innerLeft, innerBottom), Offset(innerLeft + cornerSize, innerBottom), stroke)

            // Bottom-right
            drawLine(Color.Yellow, Offset(innerRight - cornerSize, innerBottom), Offset(innerRight, innerBottom), stroke)
            drawLine(Color.Yellow, Offset(innerRight, innerBottom - cornerSize), Offset(innerRight, innerBottom), stroke)
        }
    }
}
