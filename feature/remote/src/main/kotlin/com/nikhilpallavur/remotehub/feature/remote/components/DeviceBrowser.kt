package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.feature.remote.RemoteUiState

/**
 * The pre-connection experience: saved devices, hostless infrared remotes, live discovery
 * results, a scan control, and a manual "add by IP" entry point. Tapping any row connects.
 */
@Composable
fun DeviceBrowser(
    state: RemoteUiState,
    irDrivers: List<DriverDescriptor>,
    onConnect: (RemoteDevice) -> Unit,
    onConnectDriver: (String) -> Unit,
    onScan: () -> Unit,
    onAddManual: () -> Unit,
    onForget: (RemoteDevice) -> Unit,
    onFavorite: (RemoteDevice) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { ScanHeader(isScanning = state.isScanning, onScan = onScan) }

        if (state.paired.isNotEmpty()) {
            item { SectionHeader("Saved devices") }
            items(state.paired, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    saved = true,
                    onConnect = { onConnect(device) },
                    onForget = { onForget(device) },
                    onFavorite = { onFavorite(device) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { SectionHeader(if (state.isScanning) "Searching nearby…" else "Found nearby") }
        if (state.newlyDiscovered.isEmpty()) {
            item { EmptyDiscovery(isScanning = state.isScanning) }
        } else {
            items(state.newlyDiscovered, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    saved = false,
                    onConnect = { onConnect(device) },
                    onForget = {},
                    onFavorite = {},
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (irDrivers.isNotEmpty()) {
            item { SectionHeader("On this phone") }
            items(irDrivers, key = { it.id }) { driver ->
                IrDriverRow(driver = driver, onConnect = { onConnectDriver(driver.id) })
            }
        }

        item {
            OutlinedButton(
                onClick = onAddManual,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(Spacing.xs))
                Text("Add a device by IP address")
            }
        }
    }
}

@Composable
private fun ScanHeader(isScanning: Boolean, onScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Your devices", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Control everything from one place",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onScan, enabled = !isScanning) {
            AnimatedContent(targetState = isScanning, label = "scanIcon") { scanning ->
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Wifi, contentDescription = null)
                }
            }
            Spacer(Modifier.size(Spacing.xs))
            Text(if (isScanning) "Scanning" else "Scan")
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
    )
}

@Composable
private fun DeviceRow(
    device: RemoteDevice,
    saved: Boolean,
    onConnect: () -> Unit,
    onForget: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onConnect,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        device.category.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    device.host?.let { "${device.category.displayName} · $it" } ?: device.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (saved) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (device.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (device.favorite) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** Hostless infrared remote (TV or AC) driven by the phone's own blaster — one tap to use. */
@Composable
private fun IrDriverRow(driver: DriverDescriptor, onConnect: () -> Unit) {
    Card(
        onClick = onConnect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        driver.category.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    driver.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "Tap to connect · Infrared",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyDiscovery(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isScanning) "Looking for devices on your Wi-Fi…" else "No devices found. Tap Scan, or add one by IP.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
