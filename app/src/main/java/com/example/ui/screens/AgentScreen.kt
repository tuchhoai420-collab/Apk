package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AIEngineType
import com.example.model.AgentStepLog
import com.example.model.SessionStatus
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentLavenderContainer
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OnAccentLavenderContainer
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderSubtle
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun AgentScreen(viewModel: MainViewModel) {
    val consoleOutput by viewModel.consoleOutput.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    // Agent session from controller
    val session by viewModel.agentController.currentSession.collectAsState()
    val isExecuting by viewModel.agentController.isExecuting.collectAsState()
    val executionLogs by viewModel.agentController.executionLogs.collectAsState()

    var naturalPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(consoleOutput.size, session?.steps?.size) {
        if (consoleOutput.isNotEmpty()) {
            listState.animateScrollToItem(consoleOutput.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Engine selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = { viewModel.setAIEngine(AIEngineType.LOCAL_LLAMA) },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (activeEngine == AIEngineType.LOCAL_LLAMA) AccentLavenderContainer else SophisticatedBg
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Llama Local", fontSize = 12.sp, color = if (activeEngine == AIEngineType.LOCAL_LLAMA) OnAccentLavenderContainer else TextSecondary)
            }
            ElevatedButton(
                onClick = { viewModel.setAIEngine(AIEngineType.GEMINI_CLOUD) },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (activeEngine == AIEngineType.GEMINI_CLOUD) AccentLavenderContainer else SophisticatedBg
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Gemini Cloud", fontSize = 12.sp, color = if (activeEngine == AIEngineType.GEMINI_CLOUD) OnAccentLavenderContainer else TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt input
        OutlinedTextField(
            value = naturalPrompt,
            onValueChange = { naturalPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("natural_prompt_field"),
            placeholder = { Text("Ej: revisá la batería y la memoria RAM", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentLavender,
                unfocusedBorderColor = SophisticatedBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentLavender
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = {
                    if (naturalPrompt.isNotBlank()) {
                        viewModel.executeNaturalCommand(naturalPrompt)
                    }
                },
                enabled = !isExecuting && naturalPrompt.isNotBlank(),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = AccentLavender),
                modifier = Modifier.weight(1f)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(if (isExecuting) "Ejecutando..." else "Ejecutar", color = Color.White)
            }
            if (isExecuting) {
                ElevatedButton(
                    onClick = { viewModel.cancelAgent() },
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = CrimsonGlow)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = { viewModel.clearConsole() }) {
                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick commands
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "batería y memoria" to "revisá el estado de la batería y la memoria RAM",
                "brillo al máximo" to "poné el brillo al máximo",
                "abrir YouTube" to "abrí YouTube"
            ).forEach { (label, cmd) ->
                ElevatedButton(
                    onClick = {
                        naturalPrompt = cmd
                        viewModel.executeNaturalCommand(cmd)
                    },
                    enabled = !isExecuting,
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = SophisticatedBg),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(label, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Session steps card
        session?.let { sess ->
            if (sess.steps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .testTag("session_steps_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SophisticatedBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pasos Ejecutados (${sess.steps.size})",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            StatusBadge(status = sess.status)
                        }

                        if (sess.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sess.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentLavender
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        sess.steps.forEach { step ->
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

            // Mostrar salida real del comando / resultado de la acción
            if (step.executionResult.isNotBlank()
                && step.executionResult != "Objetivo completado con éxito."
                && step.executionResult != "Acción no reconocida."
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalBg.copy(alpha = 0.7f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = step.executionResult.take(2500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
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
