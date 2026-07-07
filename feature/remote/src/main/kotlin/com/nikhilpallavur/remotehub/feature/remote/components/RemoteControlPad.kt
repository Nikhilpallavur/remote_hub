package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.motion.rememberPressFeedback
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.feature.remote.ClimateSettings
import com.nikhilpallavur.remotehub.feature.remote.DEFAULT_TEMPERATURE_RANGE_C
import kotlin.math.abs

/**
 * The dynamic remote. It renders one section per [capabilities] entry, so the layout is generated
 * from the connected device's declared abilities — a TV shows a D-pad and channels, while a
 * TEMPERATURE-capable device gets the climate remote, with no per-driver branching here.
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
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing)),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (DeviceCapability.TEMPERATURE in capabilities && climateActions != null) {
            ClimateRemote(capabilities, climate, temperatureRangeC, climateActions)
        } else {
            if (DeviceCapability.POWER in capabilities) PowerRow(onKey)
            if (DeviceCapability.DPAD in capabilities) DirectionPad(onKey)
            if (DeviceCapability.TOUCHPAD in capabilities) TouchpadSection(onKey)
            if (DeviceCapability.VOLUME in capabilities || DeviceCapability.CHANNEL in capabilities) {
                RockerRow(capabilities, onKey)
            }
            if (DeviceCapability.MEDIA in capabilities) MediaRow(onKey)
            if (DeviceCapability.NUMBER_PAD in capabilities) NumberPad(onKey)
            if (DeviceCapability.APP_SHORTCUTS in capabilities) AppShortcuts(onKey)
            if (DeviceCapability.TEXT_INPUT in capabilities) KeyboardSection(onText)
        }
    }
}

@Composable
private fun PowerRow(onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = { onKey(RemoteKey.MENU) }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu")
        }
        val press = rememberPressFeedback()
        FilledIconButton(
            onClick = { onKey(RemoteKey.POWER) },
            interactionSource = press.interactionSource,
            modifier = press.modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(32.dp))
        }
        FilledTonalIconButton(onClick = { onKey(RemoteKey.HOME) }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Home, contentDescription = "Home")
        }
    }
}

@Composable
private fun DirectionPad(onKey: (RemoteKey) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { onKey(RemoteKey.BACK) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(Spacing.xs))
                Text("Back")
            }
            TextButton(onClick = { onKey(RemoteKey.HOME) }) {
                Text("Home")
                Spacer(Modifier.width(Spacing.xs))
                Icon(Icons.Filled.Home, contentDescription = null)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            DpadButton(Icons.Filled.KeyboardArrowUp, "Up", Modifier.align(Alignment.TopCenter)) {
                onKey(RemoteKey.DPAD_UP)
            }
            DpadButton(Icons.Filled.KeyboardArrowDown, "Down", Modifier.align(Alignment.BottomCenter)) {
                onKey(RemoteKey.DPAD_DOWN)
            }
            DpadButton(Icons.Filled.KeyboardArrowLeft, "Left", Modifier.align(Alignment.CenterStart)) {
                onKey(RemoteKey.DPAD_LEFT)
            }
            DpadButton(Icons.Filled.KeyboardArrowRight, "Right", Modifier.align(Alignment.CenterEnd)) {
                onKey(RemoteKey.DPAD_RIGHT)
            }
            FilledIconButton(
                onClick = { onKey(RemoteKey.DPAD_CENTER) },
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
            ) {
                Text("OK", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DpadButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val press = rememberPressFeedback()
    IconButton(
        onClick = onClick,
        interactionSource = press.interactionSource,
        modifier = modifier.size(64.dp).then(press.modifier),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun RockerRow(capabilities: Set<DeviceCapability>, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (DeviceCapability.VOLUME in capabilities) {
            RockerColumn(
                label = "VOL",
                topIcon = Icons.Filled.VolumeUp,
                bottomIcon = Icons.Filled.VolumeDown,
                onTop = { onKey(RemoteKey.VOLUME_UP) },
                onBottom = { onKey(RemoteKey.VOLUME_DOWN) },
                onMiddle = { onKey(RemoteKey.MUTE) },
                middleIcon = Icons.Filled.VolumeOff,
                modifier = Modifier.weight(1f),
            )
        }
        if (DeviceCapability.CHANNEL in capabilities) {
            RockerColumn(
                label = "CH",
                topIcon = Icons.Filled.Add,
                bottomIcon = Icons.Filled.Remove,
                onTop = { onKey(RemoteKey.CHANNEL_UP) },
                onBottom = { onKey(RemoteKey.CHANNEL_DOWN) },
                onMiddle = null,
                middleIcon = null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RockerColumn(
    label: String,
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    onMiddle: (() -> Unit)?,
    middleIcon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onTop, modifier = Modifier.size(56.dp)) {
                Icon(topIcon, contentDescription = "$label up", modifier = Modifier.size(30.dp))
            }
            if (onMiddle != null && middleIcon != null) {
                IconButton(onClick = onMiddle) {
                    Icon(middleIcon, contentDescription = "Mute")
                }
            } else {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onBottom, modifier = Modifier.size(56.dp)) {
                Icon(bottomIcon, contentDescription = "$label down", modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun MediaRow(onKey: (RemoteKey) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onKey(RemoteKey.PREVIOUS) }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = { onKey(RemoteKey.REWIND) }) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Rewind")
            }
            FilledTonalIconButton(
                onClick = { onKey(RemoteKey.PLAY_PAUSE) },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
            }
            IconButton(onClick = { onKey(RemoteKey.FAST_FORWARD) }) {
                Icon(Icons.Filled.FastForward, contentDescription = "Fast forward")
            }
            IconButton(onClick = { onKey(RemoteKey.NEXT) }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = { onKey(RemoteKey.STOP) }) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(Spacing.xs))
                Text("Stop")
            }
        }
    }
}

@Composable
private fun NumberPad(onKey: (RemoteKey) -> Unit) {
    val numberKeys = listOf(
        RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3,
        RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6,
        RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9,
    )
    SectionCard {
        numberKeys.chunked(3).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowKeys.forEach { key -> NumberButton(key.name.removePrefix("NUM_"), key, onKey) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.Center,
        ) {
            NumberButton("0", RemoteKey.NUM_0, onKey)
        }
    }
}

@Composable
private fun NumberButton(label: String, key: RemoteKey, onKey: (RemoteKey) -> Unit) {
    FilledTonalIconButton(onClick = { onKey(key) }, modifier = Modifier.size(60.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AppShortcuts(onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AppChip("Netflix", RemoteKey.NETFLIX, onKey, Modifier.weight(1f))
        AppChip("YouTube", RemoteKey.YOUTUBE, onKey, Modifier.weight(1f))
        AppChip("Prime", RemoteKey.PRIME_VIDEO, onKey, Modifier.weight(1f))
    }
}

@Composable
private fun AppChip(label: String, key: RemoteKey, onKey: (RemoteKey) -> Unit, modifier: Modifier) {
    FilledTonalButton(onClick = { onKey(key) }, modifier = modifier) {
        Text(label, maxLines = 1)
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
private fun TouchpadSection(onKey: (RemoteKey) -> Unit) {
    val stepPx = with(LocalDensity.current) { TOUCHPAD_STEP_DP.dp.toPx() }
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Touchpad", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Swipe to move · Tap to select",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Double-tap · Back      Long-press · Home",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun KeyboardSection(onText: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotEmpty()) {
            onText(text)
            text = ""
        }
    }
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Wireless keyboard", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Type here — sent to the TV") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )
            Spacer(Modifier.width(Spacing.sm))
            FilledTonalIconButton(onClick = send, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Send, contentDescription = "Send text")
            }
        }
    }
}

/** Finger travel (in dp) that advances the touchpad one D-pad step. */
private const val TOUCHPAD_STEP_DP = 44

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
