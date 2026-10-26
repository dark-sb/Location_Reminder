package com.example.location_reminder.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Standard motion timing constants to guarantee fast, lightweight feedback (100–180ms)
 * without affecting scrolling performance or feeling bouncy.
 */
object MotionTokens {
    const val DURATION_PRESS = 120
    const val DURATION_COMPLETION = 150
    const val DURATION_TRANSITION = 160
    val Easing = FastOutSlowInEasing
}

/**
 * Applies a subtle, fast scale press effect driven by an [InteractionSource].
 * By listening directly to the component's [InteractionSource], it completely eliminates
 * raw pointer input interceptors, respecting Android's touch slop and scroll container delay.
 * During list scrolling, no press animations or state changes are triggered, guaranteeing
 * buttery smooth 60/120fps scrolling.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
    durationMs: Int = MotionTokens.DURATION_PRESS
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = durationMs, easing = MotionTokens.Easing),
        label = "pressScaleInteraction"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Applies a subtle, fast scale press effect (e.g. 0.96f down) when touched.
 * Uses [graphicsLayer] to operate on the draw layer without triggering re-layout or re-measurement,
 * ensuring 60/120fps smooth scrolling.
 *
 * Uses tap-timeout and touch-slop awareness so that scroll gestures in LazyColumn or verticalScroll
 * containers do NOT trigger premature press animations or micro-stutters during scrolling.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    durationMs: Int = MotionTokens.DURATION_PRESS
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = durationMs, easing = MotionTokens.Easing),
        label = "pressScale"
    )

    if (isPressed) {
        androidx.compose.runtime.LaunchedEffect(isPressed) {
            delay(durationMs.toLong())
            isPressed = false
        }
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(pressedScale, durationMs) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val up = waitForUpOrCancellation()
                if (up != null && !up.isConsumed) {
                    isPressed = true
                }
            }
        }
}

/**
 * Clickable modifier combining a subtle press scale with Material 3 ripple feedback.
 * Ideal for clickable rows, cards, options, and chips that need tactile feedback.
 */
fun Modifier.bounceClickable(
    pressedScale: Float = 0.97f,
    durationMs: Int = MotionTokens.DURATION_PRESS,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = tween(durationMillis = durationMs, easing = MotionTokens.Easing),
        label = "bounceClickableScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
}
