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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.llama.ServerState
import com.example.model.GGUFModelInfo
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
fun ModelsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val detectedModels by viewModel.detectedModels.collectAsState()
    val isScanning by viewModel.isScanningModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val llamaUrl by viewModel.llamaUrl.collectAsState()
    val llamaModelName by viewModel.llamaModelName.collectAsState()
    val systemPersona by viewModel.systemPersona.collectAsState()

    val serverState by viewModel.llamaServerManager.serverState.collectAsState()
    val serverLogs by viewModel.llamaServerManager.serverLogs.collectAsState()
    val serverConfig by viewModel.llamaServerManager.config.collectAsState()

    val geminiModel by viewModel.geminiModel.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    var customScanPath by remember { mutableStateOf("") }
    var serverPortInput by remember { mutableStateOf(serverConfig.port.toString()) }
    var serverThreadsInput by remember { mutableStateOf(serverConfig.threads.toString()) }
    var serverCtxInput by remember { mutableStateOf(serverConfig.contextSize.toString()) }
    var serverNglInput by remember { mutableStateOf(serverConfig.gpuLayers.toString()) }

    var personaInput by remember { mutableStateOf(systemPersona) }
    var customApiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingGemini by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val logsListState = rememberLazyListState()

    LaunchedEffect(serverLogs.size) {
        if (serverLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(serverLogs.size - 1)
        }
    }

    val geminiModelOptions = listOf(
        "gemini-3.5-flash" to "Ultra rápido, razonamiento táctico y multimodal",
        "gemini-3.1-pro-preview" to "Máxima inteligencia y resolución compleja",
        "gemini-2.5-flash-image" to "Generación y análisis visual avanzado"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("llama_server_manager_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, if (serverState == ServerState.RUNNING) EmeraldGreen.copy(alpha = 0.5f) else SophisticatedBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentLavenderContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Memory, null, tint = AccentLavender, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Servidor Nativo llama.cpp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Motor de inferencia local sin censura", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Surface(
                            color = when (serverState) {
                                ServerState.RUNNING -> EmeraldGreen.copy(alpha = 0.15f)
                                ServerState.STARTING -> AmberGlow.copy(alpha = 0.15f)
                                ServerState.ERROR -> CrimsonGlow.copy(alpha = 0.15f)
                                ServerState.STOPPED -> SophisticatedBorder
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when (serverState) {
                                    ServerState.RUNNING -> "● ACTIVO"
                                    ServerState.STARTING -> "● INICIANDO"
                                    ServerState.ERROR -> "● ERROR"
                                    ServerState.STOPPED -> "○ DETENIDO"
                                },
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = when (serverState) {
                                    ServerState.RUNNING -> EmeraldGreen
                                    ServerState.STARTING -> AmberGlow
                                    ServerState.ERROR -> CrimsonGlow
                                    ServerState.STOPPED -> TextMuted
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = serverPortInput, onValueChange = { serverPortInput = it }, label = { Text("Puerto", fontSize = 10.sp) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLavender, unfocusedBorderColor = SophisticatedBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp), singleLine = true)
                        OutlinedTextField(value = serverThreadsInput, onValueChange = { serverThreadsInput = it }, label = { Text("Hilos", fontSize = 10.sp) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLavender, unfocusedBorderColor = SophisticatedBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp), singleLine = true)
                        OutlinedTextField(value = serverCtxInput, onValueChange = { serverCtxInput = it }, label = { Text("Ctx", fontSize = 10.sp) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLavender, unfocusedBorderColor = SophisticatedBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp), singleLine = true)
                        OutlinedTextField(value = serverNglInput, onValueChange = { serverNglInput = it }, label = { Text("NGL", fontSize = 10.sp) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLavender, unfocusedBorderColor = SophisticatedBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary), shape = RoundedCornerShape(12.dp), singleLine = true)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (serverState == ServerState.RUNNING) {
                        OutlinedButton(onClick = { viewModel.stopLlamaServer() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonGlow), border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.6f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detener Servidor", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        ElevatedButton(onClick = { viewModel.startLlamaServer() }, enabled = serverState != ServerState.STARTING, colors = ButtonDefaults.elevatedButtonColors(containerColor = AccentLavender, contentColor = OnAccentLavender), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Levantar Servidor llama-server", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("gemini_cloud_integration_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentLavenderContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Cloud, null, tint = AccentLavender, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Google AI Studio (Gemini)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Ingresá tu API Key manualmente abajo", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Selecciona el Modelo Gemini:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    geminiModelOptions.forEach { (modelKey, desc) ->
                        val isChosen = geminiModel == modelKey
                        Surface(
                            onClick = { viewModel.setGeminiConfig(modelKey, customApiKeyInput.ifBlank { null }) },
                            color = if (isChosen) AccentLavenderContainer else SophisticatedBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isChosen) AccentLavender else SophisticatedBorderSubtle),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(modelKey, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isChosen) OnAccentLavenderContainer else TextPrimary)
                                    Text(desc, fontSize = 11.sp, color = TextMuted)
                                }
                                if (isChosen) Icon(Icons.Default.CheckCircle, null, tint = AccentLavender, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🔑 Ingresá tu Clave API de Gemini (manual)", style = MaterialTheme.typography.labelMedium, color = AccentLavender, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = {
                            customApiKeyInput = it
                            viewModel.setGeminiConfig(geminiModel, it)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("input_gemini_api_key"),
                        placeholder = { Text("AIzaSy... (tu clave de AI Studio)", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Key, null, tint = AccentLavender, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentLavender, unfocusedBorderColor = SophisticatedBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            isTestingGemini = true
                            geminiTestResult = null
                            scope.launch {
                                val res = viewModel.geminiClient.testConnection()
                                geminiTestResult = res.second
                                isTestingGemini = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                        border = BorderStroke(1.dp, SophisticatedBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_gemini")
                    ) {
                        if (isTestingGemini) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentLavender, strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probar Conexión con Gemini", fontSize = 12.sp)
                        }
                    }
                    geminiTestResult?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(msg, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (msg.contains("Conectado")) TerminalGreen else AmberGlow)
                    }
                }
            }
        }

        item {
            Text("Modelos GGUF Detectados (${detectedModels.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(detectedModels) { model ->
            val isSelected = selectedModel?.path == model.path
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) SophisticatedSurface else SophisticatedBg),
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) AccentLavender else SophisticatedBorderSubtle),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isSelected) Icons.Default.CheckCircle else Icons.Default.DataObject, null, tint = if (isSelected) AccentLavender else TextMuted, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(model.filename, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        Text("${model.architecture} • ${model.parameters} • ${model.sizeFormatted}", fontSize = 11.sp, color = TextMuted)
                    }
                    if (!isSelected) {
                        FilledTonalButton(onClick = { viewModel.selectModel(model) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(10.dp)) {
                            Text("Cargar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
