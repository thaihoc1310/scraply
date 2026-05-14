package com.example.scraply.ui.stamp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.scraply.R
import com.example.scraply.util.CutterGeometry

/**
 * Renders the stamp cutter overlay on top of live camera preview.
 * Uses cutter_frame.png which has the frame borders with a transparent center.
 */
@Composable
fun StampFrameOverlay(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val aspectRatio = CutterGeometry.cutterWidth / CutterGeometry.cutterHeight

    // Load cutter_frame bitmap
    val frameBitmap = remember {
        loadCutterFrame(context)
    }

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio),
        contentAlignment = Alignment.Center,
    ) {
        frameBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Loads the cutter frame from resources.
 */
private fun loadCutterFrame(context: android.content.Context): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(context.resources, R.drawable.cutter_frame, options)
    } catch (e: Exception) {
        null
    }
}
