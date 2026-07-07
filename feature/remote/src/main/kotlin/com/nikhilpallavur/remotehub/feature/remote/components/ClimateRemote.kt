package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.motion.rememberPressFeedback
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import com.nikhilpallavur.remotehub.feature.remote.ClimateSettings

/** Bundles the climate callbacks so the control pad's signature grows by one param, not five. */
class ClimateActions(
    val onTogglePower: () -> Unit,
    val onSetTemperature: (Int) -> Unit,
    val onSetMode: (ClimateMode) -> Unit,
    val onSetFanSpeed: (FanSpeed) -> Unit,
    val onSetSwing: (Boolean) -> Unit,
)

/**
 * The air-conditioner remote: temperature dial, mode, fan and swing — driven entirely by the
 * device's declared capabilities, mirroring the optimistic [ClimateSettings]. Everything except
 * power is disabled while the AC is (assumed) off, since one-way IR frames always carry the full
 * state.
 */
@Composable
internal fun ClimateRemote(
    capabilities: Set<DeviceCapability>,
    climate: ClimateSettings,
    temperatureRangeC: IntRange,
    actions: ClimateActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ClimatePowerRow(isOn = climate.power, onToggle = actions.onTogglePower)
        if (DeviceCapability.TEMPERATURE in capabilities) {
            TemperatureDial(
                temperatureC = climate.temperatureC,
                range = temperatureRangeC,
                enabled = climate.power,
                onSet = actions.onSetTemperature,
            )
        }
        if (DeviceCapability.MODE in capabilities) {
            ModeSelector(selected = climate.mode, enabled = climate.power, onSelect = actions.onSetMode)
        }
        if (DeviceCapability.FAN_SPEED in capabilities) {
            FanSpeedSelector(selected = climate.fan, enabled = climate.power, onSelect = actions.onSetFanSpeed)
        }
        if (DeviceCapability.SWING in capabilities) {
            SwingToggle(enabled = climate.power, swinging = climate.swing, onToggle = actions.onSetSwing)
        }
    }
}

@Composable
private fun ClimatePowerRow(isOn: Boolean, onToggle: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing),
        label = "acPowerContainer",
    )
    val content by animateColorAsState(
        targetValue = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing),
        label = "acPowerContent",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val press = rememberPressFeedback()
        FilledIconButton(
            onClick = onToggle,
            interactionSource = press.interactionSource,
            modifier = press.modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = container,
                contentColor = content,
            ),
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = "Power",
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            if (isOn) "On" else "Standby",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun TemperatureDial(
    temperatureC: Int,
    range: IntRange,
    enabled: Boolean,
    onSet: (Int) -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val minusPress = rememberPressFeedback()
            FilledIconButton(
                onClick = { onSet(temperatureC - 1) },
                enabled = enabled && temperatureC > range.first,
                interactionSource = minusPress.interactionSource,
                modifier = minusPress.modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Cooler")
            }
            AnimatedContent(
                targetState = temperatureC,
                transitionSpec = {
                    // Odometer roll: new value slides in from the direction of change.
                    val towards = if (targetState > initialState) -1 else 1
                    (slideInVertically(tween(Motion.DURATION_MEDIUM)) { towards * it } +
                        fadeIn(tween(Motion.DURATION_MEDIUM)))
                        .togetherWith(
                            slideOutVertically(tween(Motion.DURATION_MEDIUM)) { -towards * it } +
                                fadeOut(tween(Motion.DURATION_SHORT)),
                        )
                },
                label = "temperature",
            ) { temp ->
                Text(
                    "$temp°",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            val plusPress = rememberPressFeedback()
            FilledIconButton(
                onClick = { onSet(temperatureC + 1) },
                enabled = enabled && temperatureC < range.last,
                interactionSource = plusPress.interactionSource,
                modifier = plusPress.modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Warmer")
            }
        }
        Text(
            "Target temperature",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.xs),
        )
    }
}

private val ClimateMode.icon: ImageVector
    get() = when (this) {
        ClimateMode.AUTO -> Icons.Filled.AutoMode
        ClimateMode.COOL -> Icons.Filled.AcUnit
        ClimateMode.HEAT -> Icons.Filled.WbSunny
        ClimateMode.DRY -> Icons.Filled.WaterDrop
        ClimateMode.FAN -> Icons.Filled.Air
    }

private val ClimateMode.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(selected: ClimateMode, enabled: Boolean, onSelect: (ClimateMode) -> Unit) {
    SectionCard {
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.padding(top = Spacing.xs))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ClimateMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, ClimateMode.entries.size),
                    icon = {},
                    label = {
                        Icon(
                            mode.icon,
                            contentDescription = mode.label,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
        Text(
            selected.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun FanSpeedSelector(selected: FanSpeed, enabled: Boolean, onSelect: (FanSpeed) -> Unit) {
    SectionCard {
        Text("Fan speed", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FanSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = speed == selected,
                    onClick = { onSelect(speed) },
                    enabled = enabled,
                    label = { Text(speed.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (speed == selected) {
                        { Icon(Icons.Filled.Air, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SwingToggle(enabled: Boolean, swinging: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.SwapVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "Swing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = swinging, onCheckedChange = onToggle, enabled = enabled)
        }
    }
}
