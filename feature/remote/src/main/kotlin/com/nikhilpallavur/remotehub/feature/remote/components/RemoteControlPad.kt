package com.nikhilpallavur.remotehub.feature.remote.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.motion.rememberHaptics
import com.nikhilpallavur.remotehub.core.designsystem.motion.rememberPressFeedback
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.designsystem.theme.margin
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.feature.remote.ClimateSettings
import com.nikhilpallavur.remotehub.feature.remote.DEFAULT_TEMPERATURE_RANGE_C
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The dynamic remote. It renders one section per [capabilities] entry, so the layout is generated
 * from the connected device's declared abilities — a TV shows the neumorphic remote face, while a
 * TEMPERATURE-capable device gets the climate remote, with no per-driver branching here.
 *
 * The TV face is styled as a soft-neumorphic physical remote (fixed dark palette in [Neu]) and is
 * sized to fit one screen: D-pad between slim volume/channel rockers, and the space-hungry
 * surfaces (touchpad, wireless keyboard, number pad) behind utility buttons that open bottom
 * sheets.
 */
@Composable
fun RemoteControlPad(
    capabilities: Set<DeviceCapability>,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    climate: ClimateSettings = ClimateSettings(),
    temperatureRangeC: IntRange = DEFAULT_TEMPERATURE_RANGE_C,
    climateActions: ClimateActions? = null,
) {
    Column(
        // animateContentSize clips to its own bounds (clipToBounds + size anim), so it must sit
        // BEFORE the margin — otherwise the clip hugs the content and slices the ~14dp neumorphic
        // shadows off every edge button. With it first, the margin's room lives inside the clip.
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing))
            .margin(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (DeviceCapability.TEMPERATURE in capabilities && climateActions != null) {
            ClimateRemote(capabilities, climate, temperatureRangeC, climateActions)
        } else {
            TvRemoteLayout(capabilities, onKey, onText)
        }
    }
}

/** Which full-screen surface is open in the bottom sheet, if any. */
private enum class RemoteSheet { TOUCHPAD, KEYBOARD, NUMBER_PAD }

@Composable
private fun TvRemoteLayout(
    capabilities: Set<DeviceCapability>,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Unit,
) {
    var sheet by remember { mutableStateOf<RemoteSheet?>(null) }

    UtilityRow(capabilities, onKey, onOpenSheet = { sheet = it })
    if (DeviceCapability.DPAD in capabilities) NavRow(onKey)
    if (
        DeviceCapability.DPAD in capabilities ||
        DeviceCapability.VOLUME in capabilities ||
        DeviceCapability.CHANNEL in capabilities
    ) {
        ControlCluster(capabilities, onKey)
    }
    if (DeviceCapability.MEDIA in capabilities) MediaRow(onKey)
    if (DeviceCapability.APP_SHORTCUTS in capabilities) AppShortcutRow(onKey)

    sheet?.let { active ->
        RemoteSheetHost(active, onKey, onText, onDismiss = { sheet = null })
    }
}

/**
 * Power on the left; on the right, one launcher per collapsed surface (number pad, keyboard,
 * touchpad) and the AV input toggle — so the main remote face never scrolls.
 */
@Composable
private fun UtilityRow(
    capabilities: Set<DeviceCapability>,
    onKey: (RemoteKey) -> Unit,
    onOpenSheet: (RemoteSheet) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (DeviceCapability.POWER in capabilities) {
            NeuButton(
                onClick = { onKey(RemoteKey.POWER) },
                label = "Power",
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = Neu.PowerRed,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (DeviceCapability.NUMBER_PAD in capabilities) {
            UtilityButton(Icons.Filled.Dialpad, "Number pad") { onOpenSheet(RemoteSheet.NUMBER_PAD) }
        }
        if (DeviceCapability.TEXT_INPUT in capabilities) {
            UtilityButton(Icons.Filled.Keyboard, "Keyboard") { onOpenSheet(RemoteSheet.KEYBOARD) }
        }
        if (DeviceCapability.TOUCHPAD in capabilities) {
            UtilityButton(Icons.Filled.TouchApp, "Touchpad") { onOpenSheet(RemoteSheet.TOUCHPAD) }
        }
        NeuButton(
            onClick = { onKey(RemoteKey.INPUT_SOURCE) },
            label = "Input source",
            modifier = Modifier.size(46.dp),
        ) {
            Text(
                "AV",
                color = Neu.Content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun UtilityButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    // Sheet launchers don't go through the haptic onKey wrapper, so they tick on their own.
    val tick = rememberHaptics()
    NeuButton(
        onClick = {
            tick()
            onClick()
        },
        label = label,
        modifier = Modifier.size(46.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Neu.ContentDim, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun NavRow(onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavButton(Icons.Filled.Menu, "Menu") { onKey(RemoteKey.MENU) }
        NavButton(Icons.Filled.Home, "Home") { onKey(RemoteKey.HOME) }
        NavButton(Icons.Filled.Search, "Search") { onKey(RemoteKey.SEARCH) }
        NavButton(Icons.Filled.ArrowBack, "Back", tint = Neu.Accent) { onKey(RemoteKey.BACK) }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    tint: Color = Neu.Content,
    onClick: () -> Unit,
) {
    NeuButton(onClick = onClick, label = label, modifier = Modifier.size(54.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
    }
}

/**
 * The heart of the remote: the circular D-pad gets the full row to itself with the VOL / CH
 * rockers on their own row beneath it. Flanking the pad with vertical rockers squeezed its
 * diameter badly on narrow phones — the arrows became fiddly pin-pricks — so the pad now grows to
 * the sheet width (capped at [DPAD_MAX_DIAMETER] so tablets don't get a comedy-sized disc).
 */
@Composable
private fun ControlCluster(capabilities: Set<DeviceCapability>, onKey: (RemoteKey) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (DeviceCapability.DPAD in capabilities) {
            DirectionPad(
                onKey = onKey,
                modifier = Modifier
                    .widthIn(max = DPAD_MAX_DIAMETER)
                    .fillMaxWidth(),
            )
        }
        if (DeviceCapability.VOLUME in capabilities || DeviceCapability.CHANNEL in capabilities) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (DeviceCapability.VOLUME in capabilities) {
                    HorizontalRocker(
                        label = "VOL",
                        leadingIcon = Icons.Filled.Remove,
                        trailingIcon = Icons.Filled.Add,
                        onLeading = { onKey(RemoteKey.VOLUME_DOWN) },
                        onTrailing = { onKey(RemoteKey.VOLUME_UP) },
                        onMiddle = { onKey(RemoteKey.MUTE) },
                        middleIcon = Icons.Filled.VolumeOff,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (DeviceCapability.CHANNEL in capabilities) {
                    HorizontalRocker(
                        label = "CH",
                        leadingIcon = Icons.Filled.KeyboardArrowDown,
                        trailingIcon = Icons.Filled.KeyboardArrowUp,
                        onLeading = { onKey(RemoteKey.CHANNEL_DOWN) },
                        onTrailing = { onKey(RemoteKey.CHANNEL_UP) },
                        onMiddle = null,
                        middleIcon = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionPad(onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neuRaised()
            .clip(CircleShape)
            .background(neuSurfaceBrush())
            // The arrows and OK consume their own taps; this catches everything else on the pad
            // so the whole quadrant acts as the arrow, not just the small icon hit box.
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val dx = offset.x - size.width / 2f
                    val dy = offset.y - size.height / 2f
                    onKey(
                        if (abs(dx) >= abs(dy)) {
                            if (dx > 0) RemoteKey.DPAD_RIGHT else RemoteKey.DPAD_LEFT
                        } else {
                            if (dy > 0) RemoteKey.DPAD_DOWN else RemoteKey.DPAD_UP
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        DpadArrow(Icons.Filled.KeyboardArrowUp, "Up", Modifier.align(Alignment.TopCenter)) {
            onKey(RemoteKey.DPAD_UP)
        }
        DpadArrow(Icons.Filled.KeyboardArrowDown, "Down", Modifier.align(Alignment.BottomCenter)) {
            onKey(RemoteKey.DPAD_DOWN)
        }
        DpadArrow(Icons.Filled.KeyboardArrowLeft, "Left", Modifier.align(Alignment.CenterStart)) {
            onKey(RemoteKey.DPAD_LEFT)
        }
        DpadArrow(Icons.Filled.KeyboardArrowRight, "Right", Modifier.align(Alignment.CenterEnd)) {
            onKey(RemoteKey.DPAD_RIGHT)
        }
        // A faint accent ring circles the OK, splitting the raised center from the arrow zone so
        // the pad reads like a physical remote's directional ring rather than a plain disc.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.46f)
                .aspectRatio(1f)
                .border(1.5.dp, Neu.Accent.copy(alpha = 0.45f), CircleShape),
        )
        NeuButton(
            onClick = { onKey(RemoteKey.DPAD_CENTER) },
            label = "OK",
            modifier = Modifier.size(72.dp),
            background = neuAccentBrush(),
        ) {
            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DpadArrow(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    // Arrows sit on the shared pad surface, so they're flat hit areas rather than raised buttons.
    // Generous 76dp targets so the ring is comfortably tappable, not a pin-prick around the glyph.
    PressIconButton(onClick = onClick, modifier = modifier.size(76.dp)) {
        Icon(icon, contentDescription = label, tint = Neu.Content, modifier = Modifier.size(34.dp))
    }
}

/**
 * [IconButton] with the same press-scale spring as the raised [NeuButton]s, so the flat hit areas
 * (D-pad arrows, rocker segments) answer a tap with the same motion as everything else.
 */
@Composable
private fun PressIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val press = rememberPressFeedback()
    IconButton(
        onClick = onClick,
        interactionSource = press.interactionSource,
        modifier = press.modifier.then(modifier),
        content = content,
    )
}

/**
 * A horizontal rocker bar: decrement on the left, increment on the right, an optional action (or
 * just the label) in the middle. Thumb-friendly full-width targets under the D-pad.
 */
@Composable
private fun HorizontalRocker(
    label: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector,
    onLeading: () -> Unit,
    onTrailing: () -> Unit,
    onMiddle: (() -> Unit)?,
    middleIcon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .neuRaised(cornerRadius = 30.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(neuSurfaceBrush())
            // The segment IconButtons are narrower than the bar, leaving dead strips at the
            // edges and in the gaps; this routes those taps to the segment third they fall in.
            .pointerInput(onMiddle != null) {
                detectTapGestures { offset ->
                    val third = offset.x / size.width
                    when {
                        third < 1 / 3f -> onLeading()
                        third > 2 / 3f -> onTrailing()
                        onMiddle != null -> onMiddle()
                        third < 0.5f -> onLeading()
                        else -> onTrailing()
                    }
                }
            }
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PressIconButton(onClick = onLeading, modifier = Modifier.size(44.dp)) {
            Icon(leadingIcon, contentDescription = "$label down", tint = Neu.Content, modifier = Modifier.size(24.dp))
        }
        if (onMiddle != null && middleIcon != null) {
            PressIconButton(onClick = onMiddle, modifier = Modifier.size(36.dp)) {
                Icon(middleIcon, contentDescription = "Mute", tint = Neu.ContentDim, modifier = Modifier.size(20.dp))
            }
        } else {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(label, color = Neu.ContentDim, style = MaterialTheme.typography.labelMedium)
            }
        }
        PressIconButton(onClick = onTrailing, modifier = Modifier.size(44.dp)) {
            Icon(trailingIcon, contentDescription = "$label up", tint = Neu.Content, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MediaRow(onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaButton(Icons.Filled.SkipPrevious, "Previous") { onKey(RemoteKey.PREVIOUS) }
        MediaButton(Icons.Filled.FastRewind, "Rewind") { onKey(RemoteKey.REWIND) }
        NeuButton(
            onClick = { onKey(RemoteKey.PLAY_PAUSE) },
            label = "Play or pause",
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Neu.Positive,
                modifier = Modifier.size(26.dp),
            )
        }
        MediaButton(Icons.Filled.FastForward, "Fast forward") { onKey(RemoteKey.FAST_FORWARD) }
        MediaButton(Icons.Filled.SkipNext, "Next") { onKey(RemoteKey.NEXT) }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    NeuButton(onClick = onClick, label = label, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, tint = Neu.ContentDim, modifier = Modifier.size(22.dp))
    }
}

/** One app shortcut: launch key, brand background, and either a glyph or a small icon. */
private data class BrandApp(
    val label: String,
    val key: RemoteKey,
    val background: Color,
    val glyph: String,
    val glyphColor: Color,
    val icon: ImageVector? = null,
)

private val BRAND_APPS = listOf(
    BrandApp("Netflix", RemoteKey.NETFLIX, Color(0xFF141414), "N", Color(0xFFE50914)),
    BrandApp("YouTube", RemoteKey.YOUTUBE, Color(0xFFFF0000), "", Color.White, Icons.Filled.PlayArrow),
    BrandApp("Prime Video", RemoteKey.PRIME_VIDEO, Color(0xFF00A8E1), "P", Color.White),
    BrandApp("Disney+", RemoteKey.DISNEY_PLUS, Color(0xFF113CCF), "D+", Color.White),
    BrandApp("JioHotstar", RemoteKey.HOTSTAR, Color(0xFF0B1F65), "★", Color(0xFFFFC107)),
    BrandApp("Spotify", RemoteKey.SPOTIFY, Color(0xFF1DB954), "S", Color(0xFF0E0E0E)),
    BrandApp("Apple TV", RemoteKey.APPLE_TV, Color(0xFF000000), "tv", Color.White),
)

@Composable
private fun AppShortcutRow(onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BRAND_APPS.forEach { app ->
            NeuButton(
                onClick = { onKey(app.key) },
                label = app.label,
                modifier = Modifier.size(42.dp),
                background = SolidColor(app.background),
            ) {
                if (app.icon != null) {
                    Icon(
                        app.icon,
                        contentDescription = app.label,
                        tint = app.glyphColor,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text(
                        app.glyph,
                        color = app.glyphColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

// ---------------- Bottom sheets ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteSheetHost(
    sheet: RemoteSheet,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Neu.Background,
        contentColor = Neu.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            when (sheet) {
                RemoteSheet.TOUCHPAD -> TouchpadSurface(onKey)
                RemoteSheet.KEYBOARD -> KeyboardInput(onKey, onText)
                RemoteSheet.NUMBER_PAD -> NumberPadGrid(onKey)
            }
        }
    }
}

/**
 * A trackpad surface that maps finger gestures onto the shared D-pad keys, so a phone acts like the
 * OnePlus-style touch remote: the TV has no absolute pointer, so a swipe past [TOUCHPAD_STEP_DP]
 * steps the focus one cell in the dominant direction (long swipes emit several), a tap selects, a
 * double-tap goes back and a long-press jumps Home. Everything travels as ordinary [RemoteKey]
 * presses, so it works on every driver that already supports the D-pad — no new wire protocol.
 */
@Composable
private fun TouchpadSurface(onKey: (RemoteKey) -> Unit) {
    val stepPx = with(LocalDensity.current) { TOUCHPAD_STEP_DP.dp.toPx() }
    Text("Touchpad", style = MaterialTheme.typography.titleMedium, color = Neu.Content)
    Spacer(Modifier.height(Spacing.sm))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .neuRaised(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(neuSurfaceBrush())
            .pointerInput(Unit) {
                var accX = 0f
                var accY = 0f
                detectDragGestures(
                    onDragStart = { accX = 0f; accY = 0f },
                    onDragEnd = { accX = 0f; accY = 0f },
                    onDragCancel = { accX = 0f; accY = 0f },
                ) { change, drag ->
                    change.consume()
                    accX += drag.x
                    accY += drag.y
                    // Emit one D-pad step per [stepPx] travelled along the dominant axis; the
                    // off-axis remainder is dropped so a mostly-horizontal swipe never leaks
                    // stray vertical presses (and vice-versa).
                    while (abs(accX) >= stepPx || abs(accY) >= stepPx) {
                        if (abs(accX) >= abs(accY)) {
                            if (accX > 0) { onKey(RemoteKey.DPAD_RIGHT); accX -= stepPx }
                            else { onKey(RemoteKey.DPAD_LEFT); accX += stepPx }
                            accY = 0f
                        } else {
                            if (accY > 0) { onKey(RemoteKey.DPAD_DOWN); accY -= stepPx }
                            else { onKey(RemoteKey.DPAD_UP); accY += stepPx }
                            accX = 0f
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onKey(RemoteKey.DPAD_CENTER) },
                    onDoubleTap = { onKey(RemoteKey.BACK) },
                    onLongPress = { onKey(RemoteKey.HOME) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Neu.ContentDim,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Swipe to move · Tap to select",
                style = MaterialTheme.typography.bodyMedium,
                color = Neu.ContentDim,
            )
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        "Double-tap · Back      Long-press · Home",
        style = MaterialTheme.typography.labelMedium,
        color = Neu.ContentDim,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
    )
}

/**
 * Live wireless keyboard: the field mirrors straight onto the TV as the user types. Every edit is
 * diffed against what the TV already has — removed characters go out as [RemoteKey.BACKSPACE]
 * presses, added ones as literal text — so editing and a second round of typing behave exactly
 * like typing on the TV itself (the old send-once model left stale text on the TV). The mic
 * button dictates through the system speech recognizer and pushes the result through the same
 * diff, so it also lands on the TV instantly.
 */
@Composable
private fun KeyboardInput(onKey: (RemoteKey) -> Unit, onText: (String) -> Unit) {
    // The field updates on every keystroke for a responsive feel; [synced] tracks what the TV has
    // actually received. The sheet starts fresh on every open. The field only ever holds
    // characters the wire protocol can type (see [typeableOnTv]) — anything else would be silently
    // dropped TV-side, desyncing the mirror: later diffs would then send too many BACKSPACEs, and
    // a BACKSPACE landing on an already-empty TV field navigates back on most TVs.
    var text by remember { mutableStateOf("") }
    var synced by remember { mutableStateOf("") }

    // Debounced mirror: a burst of fast typing is coalesced into a single diff rather than one
    // network command per character, so a flaky TV link isn't flooded and stays connected. The diff
    // sends removed characters as BACKSPACE and added ones as literal text, so edits behave exactly
    // like typing on the TV itself.
    //
    // One sequential collector (not an effect keyed on the text) because a keyed effect restarts on
    // every keystroke and can be cancelled BETWEEN emitting keys and recording [synced] — the next
    // diff then re-sends from a stale baseline and the TV ends up with duplicated/over-deleted
    // text. Here cancellation can only land on the delay: past it there are no suspension points,
    // so the emit + [synced] update always complete as one atomic step.
    LaunchedEffect(Unit) {
        snapshotFlow { text }.collectLatest { value ->
            if (value == synced) return@collectLatest
            delay(MIRROR_DEBOUNCE_MS)
            val target = text
            val prefix = synced.commonPrefixWith(target)
            repeat(synced.length - prefix.length) { onKey(RemoteKey.BACKSPACE) }
            if (target.length > prefix.length) onText(target.substring(prefix.length))
            synced = target
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { text = it.filter(::typeableOnTv) }
        }
    }

    // Focus the field once the sheet has animated in, so the system keyboard rises with it as one
    // deliberate motion instead of the user hunting for the field to pop it separately.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { focusRequester.requestFocus() }
    }

    // Typing already mirrors live, so "send" means confirm: ENTER submits the TV's focused field
    // (search box, login form…) exactly like the enter key of a keyboard docked to the TV.
    val send: () -> Unit = { onKey(RemoteKey.ENTER) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Wireless keyboard", style = MaterialTheme.typography.titleMedium, color = Neu.Content)
        // Keys only land in a focused text box; this jumps the TV to its search screen, which
        // focuses one — the one-tap remedy when typing seems to do nothing (or navigates!).
        TextButton(onClick = { onKey(RemoteKey.SEARCH) }) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = Neu.Accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text("TV search", color = Neu.Accent, style = MaterialTheme.typography.labelLarge)
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter(::typeableOnTv) },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Filled.Keyboard, contentDescription = null) },
            placeholder = { Text("Type to your TV…") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neu.Accent,
                unfocusedBorderColor = Neu.SurfaceLight,
                cursorColor = Neu.Accent,
                focusedTextColor = Neu.Content,
                unfocusedTextColor = Neu.Content,
                focusedContainerColor = Neu.SurfaceDark,
                unfocusedContainerColor = Neu.SurfaceDark,
                focusedPlaceholderColor = Neu.ContentDim,
                unfocusedPlaceholderColor = Neu.ContentDim,
                focusedLeadingIconColor = Neu.Accent,
                unfocusedLeadingIconColor = Neu.ContentDim,
                focusedTrailingIconColor = Neu.ContentDim,
                unfocusedTrailingIconColor = Neu.ContentDim,
            ),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear text on the TV too")
                    }
                }
            },
        )
        Spacer(Modifier.width(Spacing.sm))
        // Empty field: dictate with the mic. Once there's text, the slot becomes the accent send
        // button that confirms it on the TV.
        Crossfade(targetState = text.isEmpty(), label = "micToSend") { empty ->
            if (empty) {
                val press = rememberPressFeedback()
                FilledTonalIconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                            )
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to type on the TV")
                        // Devices without a speech recognizer just no-op instead of crashing.
                        runCatching { speechLauncher.launch(intent) }
                    },
                    interactionSource = press.interactionSource,
                    modifier = press.modifier.size(56.dp),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice input")
                }
            } else {
                val press = rememberPressFeedback()
                FilledTonalIconButton(
                    onClick = send,
                    interactionSource = press.interactionSource,
                    modifier = press.modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Neu.Accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send to TV")
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        "Mirrors to the TV as you type — letters, numbers and . , @ # punctuation. " +
            "If nothing appears, tap TV search first.",
        style = MaterialTheme.typography.labelMedium,
        color = Neu.ContentDim,
    )
}

/**
 * Characters the Android TV wire protocol can type as a plain (unshifted) key press — the only
 * ones allowed into the keyboard field. Must stay in sync with `AndroidTvProtocol.charKeyCode`;
 * letting an unsendable character in silently desyncs the phone-side mirror from the TV.
 */
private const val TV_TYPEABLE_PUNCTUATION = " *#,.`-=[]\\;'/@+"

private fun typeableOnTv(character: Char): Boolean =
    character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' ||
        character in TV_TYPEABLE_PUNCTUATION

@Composable
private fun NumberPadGrid(onKey: (RemoteKey) -> Unit) {
    val numberKeys = listOf(
        RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3,
        RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6,
        RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9,
    )
    Text("Number pad", style = MaterialTheme.typography.titleMedium, color = Neu.Content)
    Spacer(Modifier.height(Spacing.md))
    numberKeys.chunked(3).forEach { rowKeys ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            rowKeys.forEach { key -> NumberButton(key.name.removePrefix("NUM_"), key, onKey) }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
    ) {
        NumberButton("0", RemoteKey.NUM_0, onKey)
    }
}

@Composable
private fun NumberButton(label: String, key: RemoteKey, onKey: (RemoteKey) -> Unit) {
    NeuButton(onClick = { onKey(key) }, label = label, modifier = Modifier.size(60.dp)) {
        Text(label, color = Neu.Content, style = MaterialTheme.typography.titleLarge)
    }
}

/** Finger travel (in dp) that advances the touchpad one D-pad step. */
private const val TOUCHPAD_STEP_DP = 44

/** Ceiling for the D-pad circle so it fills small phones edge-to-edge without dwarfing tablets. */
private val DPAD_MAX_DIAMETER = 320.dp

/** Pause after the last keystroke before the keyboard mirror diffs and transmits. */
private const val MIRROR_DEBOUNCE_MS = 90L

@Composable
internal fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), content = content)
    }
}
