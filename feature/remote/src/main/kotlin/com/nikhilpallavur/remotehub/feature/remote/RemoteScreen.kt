package com.nikhilpallavur.remotehub.feature.remote

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhilpallavur.remotehub.core.designsystem.motion.Motion
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ClimateMode
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

private enum class ScreenPhase { BROWSING, CONNECTING, CONNECTED }

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
    val snackbarHost = remember { SnackbarHostState() }
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
    val haptics = LocalHapticFeedback.current
    val hapticKey: (RemoteKey) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKey(key)
    }
    val climateActions = ClimateActions(
        onTogglePower = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggleAcPower()
        },
        onSetTemperature = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSetTemperature(it)
        },
        onSetMode = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSetMode(it)
        },
        onSetFanSpeed = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSetFanSpeed(it)
        },
        onSetSwing = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSetSwing(it)
        },
    )

    LaunchedEffect(state.failure) {
        state.failure?.let { snackbarHost.showSnackbar(it.reason) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("RemoteHub") },
                actions = {
                    if (connected != null) {
                        IconButton(onClick = onDisconnect) {
                            Icon(Icons.Filled.Close, contentDescription = "Disconnect")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val phase = when {
            connected != null -> ScreenPhase.CONNECTED
            state.connectingDevice != null -> ScreenPhase.CONNECTING
            else -> ScreenPhase.BROWSING
        }
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
                    ScreenPhase.CONNECTING -> Connecting(state.connectingDevice?.name.orEmpty())
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        state.connectedDevice?.let { device ->
            HeroDeviceHeader(device = device, modifier = Modifier.padding(top = Spacing.sm))
        }
        RemoteControlPad(
            capabilities = state.capabilities,
            onKey = onKey,
            onText = onText,
            climate = state.climate,
            temperatureRangeC = state.temperatureRangeC,
            climateActions = climateActions,
            modifier = Modifier.padding(bottom = Spacing.lg),
        )
    }
}

@Composable
private fun Connecting(name: String) {
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
    }
}
