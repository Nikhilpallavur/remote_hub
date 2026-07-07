package com.nikhilpallavur.remotehub.core.designsystem.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Tactile press feedback: pass [interactionSource] to the button and chain [modifier] onto its
 * own so the whole control springs down while pressed.
 */
class PressFeedback(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier,
)

/** Remembers a press-scale effect for one control; each pressable control needs its own. */
@Composable
fun rememberPressFeedback(scaleDown: Float = 0.92f): PressFeedback {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = tween(Motion.DURATION_SHORT, easing = Motion.EmphasizedEasing),
        label = "pressScale",
    )
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return PressFeedback(interactionSource, modifier)
}
