package com.nikhilpallavur.remotehub.feature.remote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nikhilpallavur.remotehub.core.designsystem.theme.Spacing
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor

/** PIN-entry dialog for devices that show a code on screen (Android TV). */
@Composable
fun PairingCodeDialog(
    deviceName: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair with $deviceName") },
        text = {
            Column {
                Text(
                    "Enter the code shown on your TV.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim() },
                    label = { Text("Pairing code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(code) }, enabled = code.isNotBlank()) { Text("Pair") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Informational dialog for confirm-on-device pairing (Samsung/LG show an Allow prompt). */
@Composable
fun ConfirmOnDeviceDialog(deviceName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept on $deviceName") },
        text = {
            Text(
                "Look at the device and accept the connection request, then it will pair " +
                    "automatically. This is only needed the first time.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

/** Manual add: pick the device's driver and enter its IP. */
@Composable
fun ManualAddDialog(
    drivers: List<DriverDescriptor>,
    onConnect: (host: String, driverId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf(drivers.firstOrNull()?.id.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a device") },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP address") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
                Text("Device type", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                drivers.forEach { driver ->
                    DriverOption(
                        driver = driver,
                        selected = driver.id == selectedId,
                        onSelect = { selectedId = driver.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(host, selectedId) },
                enabled = host.isNotBlank() && selectedId.isNotBlank(),
            ) { Text("Connect") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DriverOption(driver: DriverDescriptor, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .selectable(selected = selected, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Icon(driver.category.icon(), contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(driver.displayName, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
