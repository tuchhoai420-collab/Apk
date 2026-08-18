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

    // Llama Server Manager state
    val serverState by viewModel.llamaServerManager.serverState.collectAsState()
    val serverLogs by viewModel.llamaServerManager.serverLogs.collectAsState()
    val serverConfig by viewModel.llamaServerManager.config.collectAsState()

    // Gemini Cloud states
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
        // Native Llama.cpp Server Lifecycle Manager Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("llama_server_manager_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(
                    1.dp,
                    if (serverState == ServerState.RUNNING) EmeraldGreen.copy(alpha = 0.5f) else SophisticatedBorder
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentLavenderContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = AccentLavender,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Servidor Nativo llama.cpp",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Motor de inferencia local sin censura",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // State Pill
                        Surface(
                            color = when (serverState) {
                                ServerState.RUNNING -> EmeraldGreen.copy(alpha = 0.15f)
                                ServerState.STARTING -> AmberGlow.copy(alpha = 0.15f)
                                ServerState.ERROR -> CrimsonGlow.copy(alpha = 0.15f)
                                ServerState.STOPPED -> SophisticatedBorder
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                when (serverState) {
                                    ServerState.RUNNING -> EmeraldGreen.copy(alpha = 0.4f)
                                    ServerState.STARTING -> AmberGlow.copy(alpha = 0.4f)
                                    ServerState.ERROR -> CrimsonGlow.copy(alpha = 0.4f)
                                    ServerState.STOPPED -> SophisticatedBorderSubtle
                                }
                            )
                        ) {
                            Text(
                                text = when (serverState) {
                                    ServerState.RUNNING -> "● ACTIVO"
                                    ServerState.STARTING -> "● INICIANDO"
                                    ServerState.ERROR -> "● ERROR"
                                    ServerState.STOPPED -> "○ DETENIDO"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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

                    // Server parameters configuration grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serverPortInput,
                            onValueChange = { serverPortInput = it },
                            label = { Text("Puerto", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("input_server_port"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = serverThreadsInput,
                            onValueChange = { serverThreadsInput = it },
                            label = { Text("Hilos (-t)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("input_server_threads"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = serverCtxInput,
                            onValueChange = { serverCtxInput = it },
                            label = { Text("Ctx (-c)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("input_server_ctx"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = serverNglInput,
                            onValueChange = { serverNglInput = it },
                            label = { Text("NGL (GPU)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("input_server_ngl"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start / Stop Server Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (serverState == ServerState.RUNNING) {
                            OutlinedButton(
                                onClick = { viewModel.stopLlamaServer() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonGlow),
                                border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).testTag("btn_stop_llama_server")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detener Servidor", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            ElevatedButton(
                                onClick = {
                                    val port = serverPortInput.toIntOrNull() ?: 8080
                                    val threads = serverThreadsInput.toIntOrNull() ?: 4
                                    val ctx = serverCtxInput.toIntOrNull() ?: 4096
                                    val ngl = serverNglInput.toIntOrNull() ?: 33
                                    viewModel.startLlamaServer(port, threads, ctx, ngl)
                                },
                                enabled = serverState != ServerState.STARTING,
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = AccentLavender,
                                    contentColor = OnAccentLavender
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).testTag("btn_start_llama_server")
                            ) {
                                if (serverState == ServerState.STARTING) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnAccentLavender, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Levantar Servidor llama-server", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-time server log console
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TerminalBg)
                            .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(state = logsListState) {
                            items(serverLogs) { logLine ->
                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (logLine.contains("✓") || logLine.contains("iniciado")) TerminalGreen else if (logLine.contains("❌") || logLine.contains("Error")) CrimsonGlow else TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Google AI Studio (Gemini Cloud) Integration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_cloud_integration_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentLavenderContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = AccentLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google AI Studio (Gemini)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Modelos en la nube de alta velocidad",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Surface(
                            color = AccentLavenderContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Nube Gemini",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentLavenderContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Selecciona el Modelo Gemini:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Gemini Model selector chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        geminiModelOptions.forEach { (modelKey, desc) ->
                            val isChosen = geminiModel == modelKey
                            Surface(
                                onClick = { viewModel.setGeminiConfig(modelKey, customApiKeyInput.ifBlank { null }) },
                                color = if (isChosen) AccentLavenderContainer else SophisticatedBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isChosen) AccentLavender else SophisticatedBorderSubtle),
                                modifier = Modifier.fillMaxWidth().testTag("gemini_option_$modelKey")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = modelKey,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isChosen) OnAccentLavenderContainer else TextPrimary
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = if (isChosen) OnAccentLavenderContainer.copy(alpha = 0.8f) else TextMuted
                                        )
                                    }
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AccentLavender,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key input (Optional custom key override)
                    Text("Clave API de Google AI Studio (Opcional si se inyecta por Secretos):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = {
                            customApiKeyInput = it
                            viewModel.setGeminiConfig(geminiModel, it)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("input_gemini_api_key"),
                        placeholder = { Text("Clave API de AI Studio...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentLavender,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
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
                        if (isTestingGemini) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentLavender, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probar Conexión con Gemini ($geminiModel)", fontSize = 12.sp)
                        }
                    }

                    geminiTestResult?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TerminalBg)
                                .border(1.dp, SophisticatedBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = msg,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (msg.contains("Conectado") || msg.contains("OK")) TerminalGreen else AmberGlow
                            )
                        }
                    }
                }
            }
        }

        // Custom GGUF File Scanner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Escanear Modelos GGUF en Almacenamiento",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentLavender
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customScanPath,
                            onValueChange = { customScanPath = it },
                            modifier = Modifier.weight(1f).testTag("input_custom_path"),
                            placeholder = { Text("/sdcard/Models o /sdcard/Download", color = TextMuted, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentLavender,
                                unfocusedBorderColor = SophisticatedBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ElevatedButton(
                            onClick = { viewModel.scanGGUFModels(customScanPath.ifBlank { null }) },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = AccentLavenderContainer,
                                contentColor = OnAccentLavenderContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("btn_scan_dir")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Escanear", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buscar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Detected Models List Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Modelos GGUF Detectados (${detectedModels.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Toca para cargar en llama-server",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentLavender
                )
            }
        }

        // Model Cards
        items(detectedModels) { model ->
            val isSelected = selectedModel?.path == model.path
            ModelItemCard(
                model = model,
                isSelected = isSelected,
                onSelect = { viewModel.selectModel(model) }
            )
        }

        // Uncensored Persona / Roleplay System Prompt Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("persona_prompt_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = AccentLavender,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personalidad & Rol Cometa (Sin Censura)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Primera persona en español, juegos de rol y control táctico sin restricciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = personaInput,
                        onValueChange = {
                            personaInput = it
                            viewModel.setPersona(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_persona_prompt"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentLavender,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelItemCard(
    model: GGUFModelInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_card_${model.filename}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SophisticatedSurface else SophisticatedBg
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentLavender else SophisticatedBorderSubtle
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) AccentLavenderContainer else SophisticatedBorder.copy(alpha = 0.5f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.DataObject,
                            contentDescription = null,
                            tint = if (isSelected) AccentLavender else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = model.filename,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AccentLavender else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            text = model.path,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                if (!isSelected) {
                    FilledTonalButton(
                        onClick = onSelect,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentLavenderContainer,
                            contentColor = OnAccentLavenderContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_select_model_${model.filename}")
                    ) {
                        Text("Cargar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Surface(
                        color = SophisticatedBorder,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Activo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentLavender,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BadgeChip(label = model.architecture, color = AccentLavender)
                BadgeChip(label = model.parameters, color = TextSecondary)
                BadgeChip(label = model.quantization, color = AmberGlow)
                BadgeChip(label = model.sizeFormatted, color = EmeraldGreen)
                BadgeChip(label = "Ctx: ${model.contextLength}", color = TextMuted)
            }
        }
    }
}

@Composable
fun BadgeChip(label: String, color: Color) {
    Surface(
        color = SophisticatedBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, SophisticatedBorderSubtle)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
