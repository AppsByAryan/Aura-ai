package com.example.aura.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aura.data.PairedDeviceEntity
import com.example.aura.ui.AuraViewModel
import com.example.aura.ui.components.GlassCard
import com.example.ui.theme.AuraBlack
import com.example.ui.theme.AuraBorderGlow
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraElectricViolet
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    var showPairDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuraBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "UNIVERSAL REMOTE & DEVICES",
                        color = AuraCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showPairDialog = true },
                        modifier = Modifier.testTag("pair_new_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Pair New Device",
                            tint = AuraCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AuraDarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Security Disclosure Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "END-TO-END ENCRYPTED REMOTE CONTROL",
                                color = AuraCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AURA links to your workstation via authenticated TLS channel. Commands require explicit on-screen approval.",
                                color = AuraTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "MY DEVICES (${devices.size})",
                    color = AuraTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            items(devices, key = { it.id }) { dev ->
                DeviceItemCard(
                    device = dev,
                    onToggleOnline = { viewModel.toggleDeviceOnline(dev.id, dev.isOnline) },
                    onDelete = { viewModel.removeDevice(dev.id) },
                    onQuickCommand = { cmd -> viewModel.submitCommand(cmd) }
                )
            }
        }

        if (showPairDialog) {
            PairDeviceDialog(
                onDismiss = { showPairDialog = false },
                onPair = { name, type, host ->
                    viewModel.pairDevice(name, type, host)
                    showPairDialog = false
                }
            )
        }
    }
}

@Composable
fun DeviceItemCard(
    device: PairedDeviceEntity,
    onToggleOnline: () -> Unit,
    onDelete: () -> Unit,
    onQuickCommand: (String) -> Unit
) {
    val deviceIcon: ImageVector = when (device.type.uppercase()) {
        "LAPTOP" -> Icons.Default.Laptop
        "DESKTOP" -> Icons.Default.DesktopWindows
        "TABLET" -> Icons.Default.Tablet
        else -> Icons.Default.PhoneAndroid
    }

    val isThisPhone = device.id.contains("this_phone")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x3300E5FF),
                        border = BorderStroke(1.dp, AuraCyan)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = deviceIcon,
                                contentDescription = device.name,
                                tint = AuraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = device.name,
                            color = AuraTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = device.ipOrHost,
                            color = AuraTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Online/Offline Pill with Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (device.isOnline) AuraSuccess.copy(alpha = 0.15f) else AuraError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (device.isOnline) AuraSuccess.copy(alpha = 0.4f) else AuraError.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { if (!isThisPhone) onToggleOnline() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (device.isOnline) AuraSuccess else AuraError,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (device.isOnline) "ONLINE" else "OFFLINE",
                            color = if (device.isOnline) AuraSuccess else AuraError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick remote command buttons for laptop
            if (device.type.uppercase() == "LAPTOP" || device.type.uppercase() == "DESKTOP") {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "QUICK REMOTE DISPATCH",
                    color = AuraTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onQuickCommand("Open VS Code on my laptop") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132A4A))
                    ) {
                        Text("VS Code", fontSize = 11.sp, color = AuraCyan)
                    }

                    Button(
                        onClick = { onQuickCommand("Lock my laptop") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132A4A))
                    ) {
                        Text("Lock", fontSize = 11.sp, color = AuraTextPrimary)
                    }

                    Button(
                        onClick = { onQuickCommand("Play music on my laptop") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF132A4A))
                    ) {
                        Text("Music", fontSize = 11.sp, color = AuraTextPrimary)
                    }
                }
            }

            if (!isThisPhone) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = AuraError)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revoke Trust & Remove", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PairDeviceDialog(
    onDismiss: () -> Unit,
    onPair: (name: String, type: String, host: String) -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("LAPTOP") }
    var hostAddress by remember { mutableStateOf("192.168.1.150") }
    var pairingCode by remember { mutableStateOf("AURA-7892") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PAIR REMOTE WORKSTATION",
                color = AuraCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Establish an authenticated TLS connection with AURA Desktop Agent.",
                    color = AuraTextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name (e.g. Work Macbook)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraBorderGlow,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hostAddress,
                    onValueChange = { hostAddress = it },
                    label = { Text("Host IP or Local Domain") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraBorderGlow,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3300E5FF),
                    border = BorderStroke(1.dp, AuraCyan)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SECURITY PAIRING CODE:",
                            color = AuraTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = pairingCode,
                            color = AuraCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = if (deviceName.isNotBlank()) deviceName.trim() else "Remote Workstation"
                    onPair(name, deviceType, "$hostAddress (TLS 1.3)")
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
            ) {
                Text("PAIR DEVICE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CANCEL", color = AuraTextSecondary)
            }
        },
        containerColor = AuraDarkSurface
    )
}
