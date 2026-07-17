package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.model.RemoteDevice

/**
 * Connected-device header, presented as a raised info card: an accent icon badge, the device name,
 * and a live "connected" pulse. On the TV face it wears the neumorphic skin ([neumorphic] = true) so
 * it matches the remote below; on the themed climate remote it falls back to the Material scheme so
 * it stays readable in light or dark.
 */
@Composable
fun HeroDeviceHeader(
    device: RemoteDevice,
    neumorphic: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val cardBrush: Brush = if (neumorphic) neuSurfaceBrush() else SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    val badgeBrush: Brush = if (neumorphic) neuAccentBrush() else SolidColor(MaterialTheme.colorScheme.primaryContainer)
    val titleColor = if (neumorphic) Neu.Content else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (neumorphic) Neu.ContentDim else MaterialTheme.colorScheme.onSurfaceVariant
    val badgeIconTint = if (neumorphic) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    // The outer horizontal padding both aligns the card with the remote pad below and leaves room
    // for the neuRaised shadow (~14dp) so it isn't clipped at the screen edge.
    val surface = if (neumorphic) {
        Modifier.padding(horizontal = Spacing.md).neuRaised(cornerRadius = 24.dp)
    } else {
        Modifier.padding(horizontal = Spacing.md)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(surface)
            .clip(shape)
            .background(cardBrush)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(badgeBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                device.category.icon(),
                contentDescription = null,
                tint = badgeIconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.size(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot(color = Neu.Positive)
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    "Connected · ${device.category.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                )
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "connectedPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Motion.DURATION_LONG, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "connectedPulseAlpha",
    )
    Box(
        Modifier
            .size(8.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape),
    )
}
