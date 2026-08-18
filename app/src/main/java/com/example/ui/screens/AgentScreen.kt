package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
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
import com.example.agent.AIEngineType
import com.example.model.AgentActionType
import com.example.model.AgentStepLog
import com.example.model.SessionStatus
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.agentController.currentSession.collectAsState()
    val isExecuting by viewModel.agentController.isExecuting.collectAsState()
    val executionLogs by viewModel.agentController.executionLogs.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()
    val geminiModel by viewModel.geminiModel.collectAsState()

    var naturalPrompt by remember { mutableStateOf("") }
    val logsListState = rememberLazyListState()

    LaunchedEffect(executionLogs.size) {
        if (executionLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(executionLogs.size - 1)
        }
    }

    val quickCommands = listOf(
        "Abre YouTube y busca música lofi",
        "Consulta la batería y estado de memoria",
        "Abre Ajustes y activa modo oscuro",
        "Toma captura de pantalla y guarda en /sdcard",
        "Inicia juego de rol cometa sin restricciones"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Engine & Status Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_engine_switcher_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentLavenderContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeEngine == AIEngineType.LOCAL_LLAMA) Icons.Default.Memory else Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = AccentLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (activeEngine == AIEngineType.LOCAL_LLAMA) "Motor Local: Llama.cpp" else "Motor Nube: Google Gemini",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (activeEngine == AIEngineType.LOCAL_LLAMA) (selectedModel?.filename ?: "GGUF Nativo") else geminiModel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentLavender
                                )
                            }
                        }

                        // ADB Device Status Pill
                        Surface(
                            color = if (deviceState.isConnected) EmeraldGreen.copy(alpha = 0.15f) else CrimsonGlow.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (deviceState.isConnected) EmeraldGreen.copy(alpha = 0.4f) else CrimsonGlow.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (deviceState.isConnected) EmeraldGreen else CrimsonGlow)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (deviceState.isConnected) "ADB Listo" else "ADB Desconectado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (deviceState.isConnected) EmeraldGreen else CrimsonGlow
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Engine Selector Toggle Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeEngine == AIEngineType.LOCAL_LLAMA,
                            onClick = { viewModel.setAIEngine(AIEngineType.LOCAL_LLAMA) },
                            label = { Text("Llama.cpp (Local GGUF)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentLavenderContainer,
                                selectedLabelColor = OnAccentLavenderContainer,
                                containerColor = SophisticatedBg,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (activeEngine == AIEngineType.LOCAL_LLAMA) AccentLavender else SophisticatedBorderSubtle,
                                enabled = true,
                                selected = activeEngine == AIEngineType.LOCAL_LLAMA
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("chip_engine_llama")
                        )

                        FilterChip(
                            selected = activeEngine == AIEngineType.GEMINI_CLOUD,
                            onClick = { viewModel.setAIEngine(AIEngineType.GEMINI_CLOUD) },
                            label = { Text("Gemini Cloud (AI Studio)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentLavenderContainer,
                                selectedLabelColor = OnAccentLavenderContainer,
                                containerColor = SophisticatedBg,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (activeEngine == AIEngineType.GEMINI_CLOUD) AccentLavender else SophisticatedBorderSubtle,
                                enabled = true,
                                selected = activeEngine == AIEngineType.GEMINI_CLOUD
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("chip_engine_gemini")
                        )
                    }
                }
            }
        }

        // Natural Language Input Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nl_input_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "¿Qué deseas ejecutar en Android?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "El asistente razonará la pantalla activa y enviará los toques y comandos ADB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = naturalPrompt,
                        onValueChange = { naturalPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_natural_command"),
                        placeholder = { Text("Ej: 'Abre Spotify y reproduce mi playlist'...", color = TextMuted, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentLavender,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentLavender
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (naturalPrompt.isNotBlank() && !isExecuting) {
                                viewModel.executeNaturalCommand(naturalPrompt)
                            }
                        }),
                        trailingIcon = {
                            if (isExecuting) {
                                IconButton(
                                    onClick = { viewModel.cancelAgent() },
                                    modifier = Modifier.testTag("btn_cancel_agent")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Cancelar", tint = CrimsonGlow)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (naturalPrompt.isNotBlank()) {
                                            viewModel.executeNaturalCommand(naturalPrompt)
                                        }
                                    },
                                    enabled = naturalPrompt.isNotBlank(),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (naturalPrompt.isNotBlank()) AccentLavender else SophisticatedBorder)
                                        .testTag("btn_execute_agent")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Ejecutar",
                                        tint = if (naturalPrompt.isNotBlank()) OnAccentLavender else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Suggested quick commands
                    Text(
                        text = "Sugerencias rápidas:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCommands.forEach { cmd ->
                            Surface(
                                onClick = {
                                    naturalPrompt = cmd
                                    viewModel.executeNaturalCommand(cmd)
                                },
                                color = SophisticatedBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                                modifier = Modifier.testTag("chip_quick_${cmd.take(8)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = cmd, fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Execution Terminal Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_terminal_card"),
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Consola de Razonamiento y ADB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (isExecuting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = AccentLavender,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ejecutando...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentLavender
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TerminalBg)
                            .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        if (executionLogs.isEmpty()) {
                            Text(
                                text = "Esperando comando de lenguaje natural para iniciar el ciclo táctico...",
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        } else {
                            LazyColumn(state = logsListState) {
                                items(executionLogs) { logLine ->
                                    val textColor = when {
                                        logLine.contains("❌") || logLine.contains("🛑") -> CrimsonGlow
                                        logLine.contains("🎉") || logLine.contains("✓") -> EmeraldGreen
                                        logLine.contains("💭") || logLine.contains("🧠") -> AccentLavender
                                        logLine.contains("⚡") || logLine.contains("👉") -> AmberGlow
                                        else -> TerminalGreen
                                    }
                                    Text(
                                        text = logLine,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = textColor,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Session Step Breakdown Card
        currentSession?.let { session ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_steps_card"),
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
                            Text(
                                text = "Pasos Ejecutados (${session.steps.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            StatusBadge(status = session.status)
                        }

                        if (session.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = session.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentLavender
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        session.steps.forEach { step ->
                            StepItemView(step = step)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepItemView(step: AgentStepLog) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SophisticatedBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SophisticatedBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AccentLavenderContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Paso ${step.stepNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnAccentLavenderContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${step.action}]",
                        fontWeight = FontWeight.SemiBold,
                        color = AccentLavender,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (step.success) "✓ Éxito" else "⚠️ Advertencia",
                    color = if (step.success) EmeraldGreen else AmberGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = step.thought,
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (step.commandExecuted.isNotBlank() && step.commandExecuted != "NOOP") {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalBg)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$ ${step.commandExecuted}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalGreen
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: SessionStatus) {
    val (bgColor, textColor, label) = when (status) {
        SessionStatus.RUNNING -> Triple(AccentLavenderContainer, OnAccentLavenderContainer, "En Proceso")
        SessionStatus.SUCCESS -> Triple(EmeraldGreen.copy(alpha = 0.2f), EmeraldGreen, "Completado")
        SessionStatus.FAILED -> Triple(CrimsonGlow.copy(alpha = 0.2f), CrimsonGlow, "Error")
        SessionStatus.CANCELLED -> Triple(AmberGlow.copy(alpha = 0.2f), AmberGlow, "Cancelado")
        SessionStatus.IDLE -> Triple(SophisticatedBorder, TextMuted, "Inactivo")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
