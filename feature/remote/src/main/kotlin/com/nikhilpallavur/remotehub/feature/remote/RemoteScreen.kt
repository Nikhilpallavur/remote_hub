package com.nikhilpallavur.remotehub.feature.remote

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.designsystem.theme.remoteHubDarkColorScheme
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.core.model.Transport
import com.nikhilpallavur.remotehub.feature.remote.components.ClimateActions
import com.nikhilpallavur.remotehub.feature.remote.components.ConfirmOnDeviceDialog
import com.nikhilpallavur.remotehub.feature.remote.components.DeviceBrowser
import com.nikhilpallavur.remotehub.feature.remote.components.HeroDeviceHeader
import com.nikhilpallavur.remotehub.feature.remote.components.ManualAddDialog
import com.nikhilpallavur.remotehub.feature.remote.components.Neu
import com.nikhilpallavur.remotehub.feature.remote.components.PairingCodeDialog
import com.nikhilpallavur.remotehub.feature.remote.components.RemoteControlPad

@Composable
fun RemoteRoute(viewModel: RemoteViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RemoteScreen(
        state = state,
        manualDrivers = viewModel.manualDrivers,
        onConnect = viewModel::connect,
        onConnectDriver = viewModel::connectDriver,
        onScan = viewModel::startScan,
        onManualConnect = viewModel::connectManual,
        onForget = viewModel::forget,
        onFavorite = viewModel::toggleFavorite,
        onDisconnect = viewModel::disconnect,
        onSubmitCode = viewModel::submitPairingCode,
        onKey = viewModel::press,
        onText = viewModel::sendText,
        onToggleAcPower = viewModel::toggleAcPower,
        onSetTemperature = viewModel::setTemperature,
        onSetMode = viewModel::setClimateMode,
        onSetFanSpeed = viewModel::setFanSpeed,
        onSetSwing = viewModel::setSwing,
    )
}

private enum class ScreenPhase { BROWSING, CONNECTING, CONNECTED, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    state: RemoteUiState,
    manualDrivers: List<DriverDescriptor>,
    onConnect: (RemoteDevice) -> Unit,
    onConnectDriver: (String) -> Unit,
    onScan: () -> Unit,
    onManualConnect: (String, String) -> Unit,
    onForget: (RemoteDevice) -> Unit,
    onFavorite: (RemoteDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSubmitCode: (String) -> Unit,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Unit,
    onToggleAcPower: () -> Unit,
    onSetTemperature: (Int) -> Unit,
    onSetMode: (ClimateMode) -> Unit,
    onSetFanSpeed: (FanSpeed) -> Unit,
    onSetSwing: (Boolean) -> Unit,
) {
    var showManualAdd by remember { mutableStateOf(false) }
    val connected = state.connectedDevice

    // Infrared devices are hostless — offered as one-tap rows instead of the add-by-IP dialog.
    val irDrivers = remember(manualDrivers) {
        manualDrivers.filter { it.transport == Transport.INFRARED }
    }
    val wifiDrivers = remember(manualDrivers) {
        manualDrivers.filter { it.transport != Transport.INFRARED }
    }

    // Every remote keypress gets a tactile tick; the visual press-scale lives on the buttons.
    // VIRTUAL_KEY is the platform's soft-key click — clearly felt yet smooth — and it respects
    // the system touch-feedback setting.
    val view = LocalView.current
    val tick: () -> Unit = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    val hapticKey: (RemoteKey) -> Unit = { key ->
        tick()
        onKey(key)
    }
    val climateActions = ClimateActions(
        onTogglePower = {
            tick()
            onToggleAcPower()
        },
        onSetTemperature = {
            tick()
            onSetTemperature(it)
        },
        onSetMode = {
            tick()
            onSetMode(it)
        },
        onSetFanSpeed = {
            tick()
            onSetFanSpeed(it)
        },
        onSetSwing = {
            tick()
            onSetSwing(it)
        },
    )

    // The TV remote face is a fixed dark neumorphic skin, so the whole screen (top bar included)
    // commits to the dark scheme while it's showing; browsing and the climate remote stay themed.
    val neuRemote = connected != null && DeviceCapability.TEMPERATURE !in state.capabilities
    val phase = when {
        connected != null -> ScreenPhase.CONNECTED
        state.connectingDevice != null -> ScreenPhase.CONNECTING
        state.failure != null -> ScreenPhase.FAILED
        else -> ScreenPhase.BROWSING
    }
    MaterialTheme(
        colorScheme = if (neuRemote) remoteHubDarkColorScheme() else MaterialTheme.colorScheme,
    ) {
        Scaffold(
            containerColor = if (neuRemote) Neu.Background else MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("RemoteHub") },
                    colors = if (neuRemote) {
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Neu.Background,
                            titleContentColor = Neu.Content,
                            navigationIconContentColor = Neu.Content,
                            actionIconContentColor = Neu.Content,
                        )
                    } else {
                        TopAppBarDefaults.topAppBarColors()
                    },
                    navigationIcon = {
                        // The one always-visible way back to the landing page from the remote.
                        if (phase != ScreenPhase.BROWSING) {
                            IconButton(onClick = onDisconnect) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to devices")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        (
                            fadeIn(tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing)) +
                                slideInVertically(tween(Motion.DURATION_MEDIUM, easing = Motion.EmphasizedEasing)) { it / 8 }
                            )
                            .togetherWith(fadeOut(tween(Motion.DURATION_SHORT)))
                    },
                    label = "remoteScreenPhase",
                ) { targetPhase ->
                    when (targetPhase) {
                        ScreenPhase.CONNECTED -> ConnectedContent(
                            state = state,
                            onKey = hapticKey,
                            onText = onText,
                            climateActions = climateActions,
                        )
                        ScreenPhase.CONNECTING -> Connecting(
                        name = state.connectingDevice?.name.orEmpty(),
                        onCancel = onDisconnect,
                    )
                    ScreenPhase.FAILED -> ConnectionFailed(
                        failure = state.failure,
                        onReconnect = onConnect,
                        onHome = onDisconnect,
                    )
                        ScreenPhase.BROWSING -> DeviceBrowser(
                            state = state,
                            irDrivers = irDrivers,
                            onConnect = onConnect,
                            onConnectDriver = onConnectDriver,
                            onScan = onScan,
                            onAddManual = { showManualAdd = true },
                            onForget = onForget,
                            onFavorite = onFavorite,
                        )
                    }
                }
            }
        }
    }

    state.awaitingPairing?.let { pairing ->
        when (pairing.mode) {
            PairingMode.PIN_CODE -> PairingCodeDialog(
                deviceName = pairing.device.name,
                onSubmit = onSubmitCode,
                onDismiss = onDisconnect,
            )
            PairingMode.CONFIRM_ON_DEVICE -> ConfirmOnDeviceDialog(
                deviceName = pairing.device.name,
                onDismiss = onDisconnect,
            )
        }
    }

    if (showManualAdd) {
        ManualAddDialog(
            drivers = wifiDrivers,
            onConnect = { host, driverId ->
                showManualAdd = false
                onManualConnect(host, driverId)
            },
            onDismiss = { showManualAdd = false },
        )
    }
}

@Composable
private fun ConnectedContent(
    state: RemoteUiState,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Unit,
    climateActions: ClimateActions,
) {
    // The remote is designed to fit without scrolling; the scroll modifier stays purely as a
    // safety net for very short screens and the taller climate layout. Padding sits after the
    // scroll (inside its clip) and covers all four sides so the neumorphic shadows (~14dp of
    // blur + shift) are never cut at the edges.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        state.connectedDevice?.let { device ->
            HeroDeviceHeader(device = device)
        }
        RemoteControlPad(
            capabilities = state.capabilities,
            onKey = onKey,
            onText = onText,
            climate = state.climate,
            temperatureRangeC = state.temperatureRangeC,
            climateActions = climateActions,
        )
    }
}

@Composable
private fun Connecting(name: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            "Connecting to $name…",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.md),
        )
        TextButton(onClick = onCancel, modifier = Modifier.padding(top = Spacing.sm)) {
            Text("Cancel")
        }
    }
}

/**
 * Full-screen connection failure: says what went wrong and offers the two ways forward —
 * try the same device again (Wake-on-LAN + the full retry ladder) or go back to the landing page.
 */
@Composable
private fun ConnectionFailed(
    failure: ConnectionState.Failed?,
    onReconnect: (RemoteDevice) -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        Text(
            failure?.device?.name?.let { "Couldn't connect to $it" } ?: "Couldn't connect",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            failure?.reason.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        failure?.device?.let { device ->
            Button(
                onClick = { onReconnect(device) },
                modifier = Modifier.padding(top = Spacing.lg),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("Reconnect", modifier = Modifier.padding(start = Spacing.xs))
            }
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.padding(top = Spacing.sm)) {
            Icon(Icons.Filled.Home, contentDescription = null)
            Text("Home", modifier = Modifier.padding(start = Spacing.xs))
        }
    }
}
