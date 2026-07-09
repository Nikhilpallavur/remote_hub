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
import androidx.compose.material.icons.filled.SettingsRemote
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
import androidx.compose.runtime.remember
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

/**
 * The pre-connection experience, ordered by what the user reaches for most: saved devices
 * (favorites, then TVs) up top, the infrared TV remote next so the TV is controllable even with
 * no Wi-Fi, then live discovery, and finally the second-priority remotes (AC and friends) and
 * the manual add-by-IP escape hatch. Tapping any row connects.
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
    val tvIrDrivers = remember(irDrivers) { irDrivers.filter { it.category in TV_CATEGORIES } }
    val otherIrDrivers = remember(irDrivers) { irDrivers.filterNot { it.category in TV_CATEGORIES } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { ScanHeader(isScanning = state.isScanning, onScan = onScan) }

        if (savedSorted.isNotEmpty()) {
            item { SectionHeader("Saved devices") }
            items(savedSorted, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    saved = true,
                    onConnect = { onConnect(device) },
                    onFavorite = { onFavorite(device) },
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
                    modifier = Modifier.animateItem(),
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
        } else {
            TrailingChevron()
        }
    }
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
