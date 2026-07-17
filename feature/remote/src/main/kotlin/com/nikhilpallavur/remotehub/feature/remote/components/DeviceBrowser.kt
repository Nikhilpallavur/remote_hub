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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.Transport
import com.nikhilpallavur.remotehub.feature.remote.RemoteUiState

/** Categories the landing page treats as "a TV" — the app's first-priority remotes. */
private val TV_CATEGORIES = setOf(
    DeviceCategory.TELEVISION,
    DeviceCategory.ANDROID_TV,
    DeviceCategory.STREAMING_DEVICE,
    DeviceCategory.SET_TOP_BOX,
    DeviceCategory.PROJECTOR,
)

/** The two landing-page tabs — network remotes vs. blaster remotes. */
private enum class BrowserTab(val label: String) { WIFI("Wi-Fi"), INFRARED("Infrared") }

/**
 * The pre-connection experience, split by how a remote reaches the device: the Wi-Fi tab holds
 * saved network devices, live discovery and the add-by-IP escape hatch, while the Infrared tab
 * holds the blaster-driven remotes (TV first, then AC and friends) that need no network at all.
 * Tapping any row connects.
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
    val savedSorted = remember(state.paired) {
        state.paired.sortedWith(
            compareByDescending<RemoteDevice> { it.favorite }
                .thenBy { it.category !in TV_CATEGORIES }
                .thenByDescending { it.lastConnectedEpochMs },
        )
    }
    val savedWifi = remember(savedSorted) { savedSorted.filter { it.transport != Transport.INFRARED } }
    val savedIr = remember(savedSorted) { savedSorted.filter { it.transport == Transport.INFRARED } }
    val tvIrDrivers = remember(irDrivers) { irDrivers.filter { it.category in TV_CATEGORIES } }
    val otherIrDrivers = remember(irDrivers) { irDrivers.filterNot { it.category in TV_CATEGORIES } }

    var tab by rememberSaveable { mutableStateOf(BrowserTab.WIFI) }

    // Forgetting is destructive (a paired TV needs the PIN dance again), so rows only *request*
    // it and one shared dialog does the confirming.
    var pendingForget by remember { mutableStateOf<RemoteDevice?>(null) }
    pendingForget?.let { device ->
        ForgetDeviceDialog(
            device = device,
            onConfirm = {
                onForget(device)
                pendingForget = null
            },
            onDismiss = { pendingForget = null },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tab.ordinal) {
            BrowserTab.entries.forEach { candidate ->
                Tab(
                    selected = tab == candidate,
                    onClick = { tab = candidate },
                    text = { Text(candidate.label) },
                    icon = {
                        Icon(
                            when (candidate) {
                                BrowserTab.WIFI -> Icons.Filled.Wifi
                                BrowserTab.INFRARED -> Icons.Filled.SettingsRemote
                            },
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        AnimatedContent(targetState = tab, label = "browserTab") { activeTab ->
            when (activeTab) {
                BrowserTab.WIFI -> WifiTab(
                    state = state,
                    saved = savedWifi,
                    onConnect = onConnect,
                    onScan = onScan,
                    onAddManual = onAddManual,
                    onFavorite = onFavorite,
                    onForgetRequest = { pendingForget = it },
                    contentPadding = contentPadding,
                )
                BrowserTab.INFRARED -> InfraredTab(
                    saved = savedIr,
                    tvIrDrivers = tvIrDrivers,
                    otherIrDrivers = otherIrDrivers,
                    onConnect = onConnect,
                    onConnectDriver = onConnectDriver,
                    onFavorite = onFavorite,
                    onForgetRequest = { pendingForget = it },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun WifiTab(
    state: RemoteUiState,
    saved: List<RemoteDevice>,
    onConnect: (RemoteDevice) -> Unit,
    onScan: () -> Unit,
    onAddManual: () -> Unit,
    onFavorite: (RemoteDevice) -> Unit,
    onForgetRequest: (RemoteDevice) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { ScanHeader(isScanning = state.isScanning, onScan = onScan) }

        if (saved.isNotEmpty()) {
            item { SectionHeader("Saved devices") }
            items(saved, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    saved = true,
                    onConnect = { onConnect(device) },
                    onFavorite = { onFavorite(device) },
                    onForgetRequest = { onForgetRequest(device) },
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
                    onFavorite = {},
                    onForgetRequest = {},
                    modifier = Modifier.animateItem(),
                )
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
private fun InfraredTab(
    saved: List<RemoteDevice>,
    tvIrDrivers: List<DriverDescriptor>,
    otherIrDrivers: List<DriverDescriptor>,
    onConnect: (RemoteDevice) -> Unit,
    onConnectDriver: (String) -> Unit,
    onFavorite: (RemoteDevice) -> Unit,
    onForgetRequest: (RemoteDevice) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { InfraredHeader() }

        if (saved.isNotEmpty()) {
            item { SectionHeader("Saved remotes") }
            items(saved, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    saved = true,
                    onConnect = { onConnect(device) },
                    onFavorite = { onFavorite(device) },
                    onForgetRequest = { onForgetRequest(device) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (tvIrDrivers.isNotEmpty()) {
            item { SectionHeader("TV remote") }
            items(tvIrDrivers, key = { it.id }) { driver ->
                DriverRow(
                    driver = driver,
                    subtitle = "No Wi-Fi needed · point the phone at the TV",
                    onConnect = { onConnectDriver(driver.id) },
                )
            }
        }

        if (otherIrDrivers.isNotEmpty()) {
            item { SectionHeader("Other remotes") }
            items(otherIrDrivers, key = { it.id }) { driver ->
                DriverRow(
                    driver = driver,
                    subtitle = "Uses this phone's infrared blaster",
                    onConnect = { onConnectDriver(driver.id) },
                )
            }
        }
    }
}

@Composable
private fun InfraredHeader() {
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(
            "Infrared remotes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Driven by this phone's IR blaster — no network needed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScanHeader(isScanning: Boolean, onScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Your devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Every remote in one place",
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
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
    )
}

@Composable
private fun DeviceRow(
    device: RemoteDevice,
    saved: Boolean,
    onConnect: () -> Unit,
    onFavorite: () -> Unit,
    onForgetRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrowserRow(
        icon = device.category.icon(),
        title = device.name,
        subtitle = device.host?.let { "${device.category.displayName} · $it" }
            ?: device.category.displayName,
        transport = device.transport,
        onClick = onConnect,
        modifier = modifier,
    ) {
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
            IconButton(onClick = onForgetRequest) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Forget ${device.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TrailingChevron()
        }
    }
}

/** Confirms forgetting a saved device — destructive, since paired devices must re-pair to return. */
@Composable
private fun ForgetDeviceDialog(
    device: RemoteDevice,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Forget ${device.name}?") },
        text = {
            Text(
                if (device.paired) {
                    "It will be removed from your saved devices, and you'll need to pair with it " +
                        "again to reconnect."
                } else {
                    "It will be removed from your saved devices."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Forget", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Hostless infrared remote driven by the phone's own blaster — one tap to use. */
@Composable
private fun DriverRow(driver: DriverDescriptor, subtitle: String, onConnect: () -> Unit) {
    BrowserRow(
        icon = driver.category.icon(),
        title = driver.displayName,
        subtitle = subtitle,
        transport = driver.transport,
        onClick = onConnect,
    ) {
        TrailingChevron()
    }
}

/**
 * The one visual shape every browser entry shares — icon badge, name, detail line, transport
 * chip, then a caller-supplied trailing control — so the page reads as a single tidy list
 * rather than a patchwork of card styles.
 */
@Composable
private fun BrowserRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    transport: Transport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            TransportBadge(transport)
            trailing()
        }
    }
}

@Composable
private fun TransportBadge(transport: Transport) {
    val (icon, label) = when (transport) {
        Transport.INFRARED -> Icons.Filled.SettingsRemote to "IR"
        else -> Icons.Filled.Wifi to transport.displayName
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.size(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun TrailingChevron() {
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.xs),
    )
}

@Composable
private fun EmptyDiscovery(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isScanning) {
                "Looking for devices on your Wi-Fi…"
            } else {
                "No devices found. Tap Scan, or add one by IP."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
