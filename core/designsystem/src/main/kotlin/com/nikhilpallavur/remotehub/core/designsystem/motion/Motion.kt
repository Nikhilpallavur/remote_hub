package com.nikhilpallavur.remotehub.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Shared motion vocabulary so every screen animates with the same rhythm: short for touch
 * feedback, medium for content transitions, long for ambient/looping effects.
 */
object Motion {
    const val DURATION_SHORT = 120
    const val DURATION_MEDIUM = 220
    const val DURATION_LONG = 600

    /** Material 3 "emphasized decelerate" — fast start, gentle settle. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
