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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SessionStatus
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.InfoCode
import com.example.ui.theme.OnAccentLavender
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val currentSession by viewModel.agentController.currentSession.collectAsState()
    val isExecuting by viewModel.agentController.isExecuting.collectAsState()
    val executionLogs by viewModel.agentController.executionLogs.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto scroll logs
    LaunchedEffect(executionLogs.size) {
        if (executionLogs.isNotEmpty()) {
            listState.animateScrollToItem(executionLogs.size - 1)
        }
    }

    val quickMacros = listOf(
        "Verificar batería y memoria" to Icons.Default.BatteryChargingFull,
        "Abrir Ajustes del dispositivo" to Icons.Default.Settings,
        "Abrir YouTube y buscar" to Icons.Default.Videocam,
        "Volver a Inicio" to Icons.Default.Home,
        "Inspeccionar app activa" to Icons.Default.Search
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Model & Telemetry Card (Sophisticated Dark Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_header_card"),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MODELO ACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentLavender,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = selectedModel?.filename ?: "Meta-Llama-3-8B",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${selectedModel?.quantization ?: "Q4_K_M"} • ${selectedModel?.sizeFormatted ?: "4.92 GB"} • ${selectedModel?.architecture ?: "LLaMA"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SophisticatedBorder,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (selectedModel != null) "Cargado" else "Listo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("btn_open_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Ver Historial",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry mini boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniTelemetryBox(label = "BATERÍA", value = deviceState.batteryLevel, modifier = Modifier.weight(1f))
                    MiniTelemetryBox(label = "RESOLUCIÓN", value = deviceState.screenResolution.take(9), modifier = Modifier.weight(1f))
                    MiniTelemetryBox(label = "CONEXIÓN", value = if (deviceState.isConnected) "5555" else "Local", modifier = Modifier.weight(1f))
                }
            }
        }

        // Quick Macro Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickMacros.forEach { (macroText, icon) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        promptInput = macroText
                        viewModel.executeNaturalCommand(macroText)
                    },
                    label = { Text(macroText, fontSize = 11.sp, color = TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentLavender,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SophisticatedSurface,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = SophisticatedBorderSubtle,
                        enabled = true,
                        selected = false
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("macro_chip_${macroText.take(10)}")
                )
            }
        }

        // Terminal / Console View (Sophisticated Dark Terminal: #000000 with #313033 border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(TerminalBg)
                .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(18.dp))
                .padding(14.dp)
                .testTag("agent_session_card")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal Header / Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusColor, statusText) = when (currentSession?.status) {
                            SessionStatus.RUNNING -> AccentLavender to "EJECUTANDO..."
                            SessionStatus.SUCCESS -> EmeraldGreen to "FINALIZADO CON ÉXITO"
                            SessionStatus.CANCELLED -> AmberGlow to "CANCELADO"
                            SessionStatus.FAILED -> CrimsonGlow to "ERROR"
                            else -> TextMuted to "TERMINAL LISTO"
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    if (isExecuting) {
                        OutlinedButton(
                            onClick = { viewModel.cancelAgent() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonGlow),
                            border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("btn_cancel_agent")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Detener",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detener", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (executionLogs.isEmpty() && currentSession == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "$ llama.cpp --model ${selectedModel?.filename ?: "./models/llama-3.gguf"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = InfoCode
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[INFO] Initializing KV cache...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AccentLavender
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[SYSTEM] Native binary active via wireless debugging port 5555. Ready for commands.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "> ", color = EmeraldGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            Text(text = "_", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("execution_logs_list"),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(executionLogs) { logLine ->
                            val color = when {
                                logLine.contains("🚀") || logLine.contains("---") -> AccentLavender
                                logLine.contains("💭") -> TextSecondary
                                logLine.contains("👉") || logLine.contains("🔘") || logLine.contains("⌨️") -> EmeraldGreen
                                logLine.contains("❌") -> CrimsonGlow
                                logLine.contains("🎉") -> TerminalGreen
                                else -> TextPrimary
                            }
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = color,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Pill Input Field & Circular Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            TextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("natural_prompt_input"),
                placeholder = {
                    Text(
                        "Pregunta a Llama o ejecuta comando...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SophisticatedSurface,
                    unfocusedContainerColor = SophisticatedSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentLavender,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (promptInput.isNotBlank() && !isExecuting) {
                        val cmd = promptInput
                        promptInput = ""
                        viewModel.executeNaturalCommand(cmd)
                    }
                })
            )

            // Round action button placed directly inside right edge of pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank() && !isExecuting) {
                            val cmd = promptInput
                            promptInput = ""
                            viewModel.executeNaturalCommand(cmd)
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isExecuting,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (promptInput.isNotBlank()) AccentLavender else SophisticatedBorder)
                        .testTag("btn_send_prompt")
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = OnAccentLavender,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Enviar",
                            tint = if (promptInput.isNotBlank()) OnAccentLavender else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SophisticatedSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial de Comandos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showHistorySheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (historyList.isEmpty()) {
                    Text(
                        "No hay comandos registrados aún.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("history_item_${item.id}"),
                                colors = CardDefaults.cardColors(containerColor = SophisticatedBg),
                                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.userPrompt,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = item.status,
                                            color = if (item.status == "SUCCESS") EmeraldGreen else AmberGlow,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.stepsCount} pasos ejecutados",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniTelemetryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SophisticatedBg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
