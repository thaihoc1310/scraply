package com.example.scraply.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Animation parameters for the stamp falling effect.
 */
object StampAnimations {

    /**
     * Black hole animation - camera shrinks into a black circle.
     * Returns progress (0-1) for animation.
     */
    @Composable
    fun rememberBlackHoleAnimation(
        trigger: Boolean,
        durationMs: Int = 300,
    ): Float {
        val progress = androidx.compose.runtime.remember { Animatable(0f) }

        LaunchedEffect(trigger) {
            if (trigger) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
                )
            } else {
                progress.snapTo(0f)
            }
        }

        return progress.value
    }

    /**
     * Falling card animation with physics-based motion.
     * Simulates a stamp falling with rotation and bounce.
     */
    @Composable
    fun rememberFallingCardAnimation(
        trigger: Boolean,
        onComplete: () -> Unit,
    ): FallingCardState {
        val offsetY = androidx.compose.runtime.remember { Animatable(-300f) }
        val offsetX = androidx.compose.runtime.remember { Animatable(0f) }
        val rotation = androidx.compose.runtime.remember { Animatable(0f) }
        val scale = androidx.compose.runtime.remember { Animatable(1f) }
        val alpha = androidx.compose.runtime.remember { Animatable(1f) }

        LaunchedEffect(trigger) {
            if (trigger) {
                // Start from top
                offsetY.snapTo(-300f)
                offsetX.snapTo(0f)
                rotation.snapTo(0f)
                scale.snapTo(1f)
                alpha.snapTo(1f)

                // Fall with spring physics
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )

                // Slight sway
                offsetX.animateTo(
                    targetValue = 20f,
                    animationSpec = tween(200),
                )
                offsetX.animateTo(
                    targetValue = -10f,
                    animationSpec = tween(150),
                )
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(100),
                )

                // Rotation
                rotation.animateTo(
                    targetValue = 5f,
                    animationSpec = tween(300),
                )
                rotation.animateTo(
                    targetValue = -3f,
                    animationSpec = tween(200),
                )
                rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(100),
                )

                // Scale bounce
                scale.animateTo(
                    targetValue = 1.05f,
                    animationSpec = tween(100),
                )
                scale.animateTo(
                    targetValue = 0.98f,
                    animationSpec = tween(80),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(60),
                )

                // Fade out slightly then complete
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200),
                )

                onComplete()
            }
        }

        return FallingCardState(
            offsetX = offsetX.value,
            offsetY = offsetY.value,
            rotation = rotation.value,
            scale = scale.value,
            alpha = alpha.value,
        )
    }

    /**
     * Overlay pulse animation for the cutter frame.
     */
    @Composable
    fun rememberOverlayPulseAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_scale",
        )
        return pulse
    }

    /**
     * Shimmer animation for loading states.
     */
    @Composable
    fun rememberShimmerAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_progress",
        )
        return shimmer
    }

    /**
     * Capture button press animation.
     */
    @Composable
    fun rememberCaptureButtonAnimation(isPressed: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (isPressed) 0.9f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "capture_scale",
        ).value
    }

    /**
     * Zoom level indicator fade animation.
     */
    @Composable
    fun rememberZoomIndicatorAnimation(zoomLevel: Float): Float {
        return animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(200),
            label = "zoom_indicator",
        ).value
    }
}

/**
 * State holder for falling card animation.
 */
data class FallingCardState(
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
)

/**
 * Compose modifier extension for applying falling card animation.
 */
fun Modifier.fallingCardAnimation(
    state: FallingCardState,
): Modifier = this.graphicsLayer {
    translationX = state.offsetX
    translationY = state.offsetY
    rotationZ = state.rotation
    scaleX = state.scale
    scaleY = state.scale
    this.alpha = state.alpha
}
