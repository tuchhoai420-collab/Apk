package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adb.AutoConnectStatus
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentLavenderContainer
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OnAccentLavender
import com.example.ui.theme.OnAccentLavenderContainer
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderSubtle
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdbScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val deviceState by viewModel.deviceState.collectAsState()
    val autoStatus by viewModel.adbAutoConnector.status.collectAsState()
    val autoLogs by viewModel.adbAutoConnector.logs.collectAsState()

    var hostInput by remember { mutableStateOf(deviceState.host) }
    var portInput by remember { mutableStateOf(deviceState.port.toString()) }

    var pairPortInput by remember { mutableStateOf("45397") }
    var pairCodeInput by remember { mutableStateOf("957198") }

    var rawShellCommand by remember { mutableStateOf("") }
    var shellOutput by remember { mutableStateOf<String?>(null) }
    var isExecutingShell by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val logsListState = rememberLazyListState()

    LaunchedEffect(autoLogs.size) {
        if (autoLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(autoLogs.size - 1)
        }
    }

    val quickAdbActions = listOf(
        "Presionar Inicio" to "input keyevent 3",
        "Presionar Atrás" to "input keyevent 4",
        "Subir Volumen" to "input keyevent 24",
        "Bajar Volumen" to "input keyevent 25",
        "Bloquear Pantalla" to "input keyevent 26",
        "Captura de Pantalla" to "screencap -p /sdcard/cometa_screen.png",
        "Listar Apps Instaladas" to "pm list packages -3",
        "Inspeccionar UI Dump" to "uiautomator dump /sdcard/window_dump.xml",
        "Consultar Batería" to "dumpsys battery",
        "Consultar Memoria" to "dumpsys meminfo"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Automatic Wireless Pairing & Connect Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_pairing_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(
                    1.dp,
                    if (deviceState.isConnected) EmeraldGreen.copy(alpha = 0.5f) else SophisticatedBorder
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (deviceState.isConnected) EmeraldGreen.copy(alpha = 0.15f) else AccentLavenderContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (deviceState.isConnected) Icons.Default.Wifi else Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = if (deviceState.isConnected) EmeraldGreen else AccentLavender,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vinculación Automática ADB",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Depuración inalámbrica sin cables en Android",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (deviceState.isConnected) EmeraldGreen else TextMuted
                                )
                            }
                        }

                        // Developer options quick trigger
                        OutlinedButton(
                            onClick = { viewModel.openDevSettings() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                            border = BorderStroke(1.dp, SophisticatedBorder),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_open_dev_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajustes Dev", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "1. Activa 'Depuración inalámbrica' en Ajustes.\n2. Toca 'Vincular con código' e ingresa el código de 6 dígitos y puerto:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Code and Port fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pairCodeInput,
                            onValueChange = { pairCodeInput = it },
                            modifier = Modifier.weight(1.3f).testTag("input_pair_code"),
                            label = { Text("Código de 6 Dígitos", fontSize = 11.sp) },
                            placeholder = { Text("Ej: 957198", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pairPortInput,
                            onValueChange = { pairPortInput = it },
                            modifier = Modifier.weight(1f).testTag("input_pair_port"),
                            label = { Text("Puerto Emparejar", fontSize = 11.sp) },
                            placeholder = { Text("Ej: 45397", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto pair & Auto discover buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = {
                                val p = pairPortInput.toIntOrNull() ?: 45397
                                viewModel.autoPairAndConnectAdb(p, pairCodeInput)
                            },
                            enabled = autoStatus != AutoConnectStatus.PAIRING && autoStatus != AutoConnectStatus.CONNECTING,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = AccentLavender,
                                contentColor = OnAccentLavender
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f).testTag("btn_auto_pair_connect")
                        ) {
                            if (autoStatus == AutoConnectStatus.PAIRING || autoStatus == AutoConnectStatus.CONNECTING) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnAccentLavender, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto-Vincular y Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.autoDiscoverAdbPorts() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                            border = BorderStroke(1.dp, SophisticatedBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.9f).testTag("btn_auto_scan_ports")
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Log Stream
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp, max = 130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TerminalBg)
                            .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(state = logsListState) {
                            items(autoLogs) { logLine ->
                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (logLine.contains("✓") || logLine.contains("🎉")) TerminalGreen else if (logLine.contains("❌")) CrimsonGlow else TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual Host/Port Connection & Device Telemetry
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adb_status_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conexión Manual y Telemetría",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            modifier = Modifier.weight(2f).testTag("input_adb_ip"),
                            label = { Text("IP Host", fontSize = 11.sp) },
                            placeholder = { Text("127.0.0.1", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            modifier = Modifier.weight(1f).testTag("input_adb_port"),
                            label = { Text("Puerto", fontSize = 11.sp) },
                            placeholder = { Text("5555", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = {
                                val p = portInput.toIntOrNull() ?: 5555
                                viewModel.connectWirelessAdb(hostInput, p)
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = AccentLavenderContainer,
                                contentColor = OnAccentLavenderContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_connect_manual")
                        ) {
                            Text("Conectar Puerto Manual", fontSize = 12.sp)
                        }

                        if (deviceState.isConnected) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setAdbConfig("127.0.0.1", 5555)
                                    viewModel.refreshDeviceState()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonGlow),
                                border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_disconnect_manual")
                            ) {
                                Text("Desconectar", fontSize = 12.sp)
                            }
                        }
                    }

                    // Device Telemetry
                    if (deviceState.isConnected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = SophisticatedBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("DISPOSITIVO", color = TextMuted, fontSize = 9.sp)
                                    Text(deviceState.deviceModel, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = SophisticatedBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("ANDROID OS", color = TextMuted, fontSize = 9.sp)
                                    Text(deviceState.androidVersion, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct ADB Shell Execution Console Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adb_terminal_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = AccentLavender,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Consola ADB Shell Directa",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            color = SophisticatedBorder,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Root / ADB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentLavender,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = rawShellCommand,
                            onValueChange = { rawShellCommand = it },
                            modifier = Modifier.weight(1f).testTag("input_raw_adb_command"),
                            placeholder = { Text("input tap 500 1000, pm list packages...", color = TextMuted, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (rawShellCommand.isNotBlank()) {
                                    isExecutingShell = true
                                    scope.launch {
                                        val res = viewModel.adbBridge.executeShell(rawShellCommand)
                                        shellOutput = res.output.ifEmpty { "[Código: ${res.exitCode}]" }
                                        isExecutingShell = false
                                    }
                                }
                            })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (rawShellCommand.isNotBlank()) {
                                    isExecutingShell = true
                                    scope.launch {
                                        val res = viewModel.adbBridge.executeShell(rawShellCommand)
                                        shellOutput = res.output.ifEmpty { "[Código: ${res.exitCode}]" }
                                        isExecutingShell = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentLavender)
                                .testTag("btn_send_shell_cmd")
                        ) {
                            if (isExecutingShell) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnAccentLavender, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Ejecutar", tint = OnAccentLavender)
                            }
                        }
                    }

                    shellOutput?.let { out ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TerminalBg)
                                .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = out,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TerminalGreen,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick ADB Utility Commands
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_adb_actions_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Acciones Rápidas del Dispositivo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAdbActions.forEach { (label, cmd) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    rawShellCommand = cmd
                                    isExecutingShell = true
                                    scope.launch {
                                        val res = viewModel.adbBridge.executeShell(cmd)
                                        shellOutput = "$ $cmd\n${res.output}"
                                        isExecutingShell = false
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp, color = TextPrimary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SophisticatedBg,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = SophisticatedBorderSubtle,
                                    enabled = true,
                                    selected = false
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("chip_action_${label.take(8)}")
                            )
                        }
                    }
                }
            }
        }
    }
}
