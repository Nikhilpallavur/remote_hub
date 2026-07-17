package com.nikhilpallavur.remotehub.core.designsystem.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Generous rounding for a soft, modern, tactile surface language.
internal val RemoteHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Consistent spacing scale used across every screen so layouts stay visually aligned. */
@Immutable
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

/**
 * Outer spacing for a composable. Compose has no first-class margin, so this delegates to
 * [padding] — the distinction only matters when it is applied *before* a background/border/draw
 * modifier, in which case the spacing stays outside the drawn surface, i.e. a true margin.
 */
fun Modifier.margin(all: Dp): Modifier = padding(all)

fun Modifier.margin(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier =
    padding(horizontal = horizontal, vertical = vertical)
