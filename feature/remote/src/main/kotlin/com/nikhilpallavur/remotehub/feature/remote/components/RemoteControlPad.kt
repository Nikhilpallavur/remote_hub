package com.nikhilpallavur.remotehub.feature.remote.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.feature.remote.ClimateSettings
import com.nikhilpallavur.remotehub.feature.remote.DEFAULT_TEMPERATURE_RANGE_C
import kotlin.math.abs

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
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing)),
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
    val view = LocalView.current
    NeuButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
        NavButton(Icons.Filled.ArrowBack, "Back") { onKey(RemoteKey.BACK) }
        NavButton(Icons.Filled.Home, "Home") { onKey(RemoteKey.HOME) }
        NavButton(Icons.Filled.Search, "Search") { onKey(RemoteKey.SEARCH) }
        NavButton(Icons.Filled.Menu, "Menu") { onKey(RemoteKey.MENU) }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    NeuButton(onClick = onClick, label = label, modifier = Modifier.size(54.dp)) {
        Icon(icon, contentDescription = label, tint = Neu.Content, modifier = Modifier.size(23.dp))
    }
}

/** The heart of the remote: slim VOL / CH rockers flanking the circular D-pad, one shared row. */
@Composable
private fun ControlCluster(capabilities: Set<DeviceCapability>, onKey: (RemoteKey) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (DeviceCapability.VOLUME in capabilities) {
            RockerPill(
                label = "VOL",
                topIcon = Icons.Filled.Add,
                bottomIcon = Icons.Filled.Remove,
                onTop = { onKey(RemoteKey.VOLUME_UP) },
                onBottom = { onKey(RemoteKey.VOLUME_DOWN) },
                onMiddle = { onKey(RemoteKey.MUTE) },
                middleIcon = Icons.Filled.VolumeOff,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (DeviceCapability.DPAD in capabilities) DirectionPad(onKey)
        }
        if (DeviceCapability.CHANNEL in capabilities) {
            RockerPill(
                label = "CH",
                topIcon = Icons.Filled.KeyboardArrowUp,
                bottomIcon = Icons.Filled.KeyboardArrowDown,
                onTop = { onKey(RemoteKey.CHANNEL_UP) },
                onBottom = { onKey(RemoteKey.CHANNEL_DOWN) },
                onMiddle = null,
                middleIcon = null,
            )
        }
    }
}

@Composable
private fun DirectionPad(onKey: (RemoteKey) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .neuRaised()
            .clip(CircleShape)
            .background(neuSurfaceBrush()),
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
        NeuButton(
            onClick = { onKey(RemoteKey.DPAD_CENTER) },
            label = "OK",
            modifier = Modifier.size(64.dp),
        ) {
            Text("OK", color = Neu.Content, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DpadArrow(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    // Arrows sit on the shared pad surface, so they're flat hit areas rather than raised buttons.
    IconButton(onClick = onClick, modifier = modifier.size(56.dp)) {
        Icon(icon, contentDescription = label, tint = Neu.Content, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun RockerPill(
    label: String,
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    onMiddle: (() -> Unit)?,
    middleIcon: ImageVector?,
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .neuRaised(cornerRadius = 28.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(neuSurfaceBrush())
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconButton(onClick = onTop, modifier = Modifier.size(44.dp)) {
            Icon(topIcon, contentDescription = "$label up", tint = Neu.Content, modifier = Modifier.size(24.dp))
        }
        if (onMiddle != null && middleIcon != null) {
            IconButton(onClick = onMiddle, modifier = Modifier.size(36.dp)) {
                Icon(middleIcon, contentDescription = "Mute", tint = Neu.ContentDim, modifier = Modifier.size(20.dp))
            }
        } else {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(label, color = Neu.ContentDim, style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(onClick = onBottom, modifier = Modifier.size(44.dp)) {
            Icon(bottomIcon, contentDescription = "$label down", tint = Neu.Content, modifier = Modifier.size(24.dp))
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
                tint = Neu.Content,
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
    var text by remember { mutableStateOf("") }
    // What the TV has received so far this session; the sheet starts fresh on every open.
    var synced by remember { mutableStateOf("") }
    val sync: (String) -> Unit = { value ->
        val prefix = synced.commonPrefixWith(value)
        repeat(synced.length - prefix.length) { onKey(RemoteKey.BACKSPACE) }
        if (value.length > prefix.length) onText(value.substring(prefix.length))
        synced = value
        text = value
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(sync)
        }
    }
    Text("Wireless keyboard", style = MaterialTheme.typography.titleMedium, color = Neu.Content)
    Spacer(Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = sync,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Type here — it appears on the TV") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { sync("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear text on the TV too")
                    }
                }
            },
        )
        Spacer(Modifier.width(Spacing.sm))
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
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice input")
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        "Every change is sent to the TV instantly",
        style = MaterialTheme.typography.labelMedium,
        color = Neu.ContentDim,
    )
}

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
