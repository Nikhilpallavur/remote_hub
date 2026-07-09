package com.nikhilpallavur.remotehub.feature.remote.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.rememberPressFeedback

/**
 * Fixed palette for the soft-neumorphic remote face. The style only works against one known
 * background (both shadows are tuned to it), so the remote commits to this dark charcoal look
 * regardless of the app theme — like a physical remote, it doesn't change color at night.
 */
internal object Neu {
    val Background = Color(0xFF25282C)
    val SurfaceLight = Color(0xFF31353B)
    val SurfaceDark = Color(0xFF26292D)
    val ShadowLight = Color(0x30FFFFFF)
    val ShadowDark = Color(0xB3000000)
    val Content = Color(0xFFE3E6EA)
    val ContentDim = Color(0xFF959CA5)
    val PowerRed = Color(0xFFE5484D)
}

/** The subtle top-light gradient that makes a raised surface read as convex. */
internal fun neuSurfaceBrush(): Brush = Brush.linearGradient(listOf(Neu.SurfaceLight, Neu.SurfaceDark))

/**
 * Soft-neumorphism: a blurred light shadow up-left and a blurred dark shadow down-right, so the
 * control appears extruded from the background. Pass [cornerRadius] for rounded rectangles;
 * leave it unspecified for circles (radius follows the composable's size).
 */
internal fun Modifier.neuRaised(cornerRadius: Dp = Dp.Unspecified): Modifier = drawBehind {
    val radius = if (cornerRadius == Dp.Unspecified) size.minDimension / 2f else cornerRadius.toPx()
    val blur = 10.dp.toPx()
    val shift = 4.dp.toPx()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        paint.color = Neu.ShadowDark.toArgb()
        canvas.nativeCanvas.drawRoundRect(
            shift, shift, size.width + shift, size.height + shift, radius, radius, paint,
        )
        paint.color = Neu.ShadowLight.toArgb()
        canvas.nativeCanvas.drawRoundRect(
            -shift, -shift, size.width - shift, size.height - shift, radius, radius, paint,
        )
    }
}

/**
 * The base control of the remote face: a raised neumorphic surface that springs down when
 * pressed. No ripple — the press-scale plus the shadows carry the tactile feel.
 */
@Composable
internal fun NeuButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    cornerRadius: Dp = Dp.Unspecified,
    background: Brush = neuSurfaceBrush(),
    content: @Composable BoxScope.() -> Unit,
) {
    val press = rememberPressFeedback()
    Box(
        modifier = press.modifier
            .then(modifier)
            .neuRaised(cornerRadius)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
