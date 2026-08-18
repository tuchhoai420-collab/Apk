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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.GGUFModelInfo
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentLavenderContainer
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldGreen
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

    var customScanPath by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf(llamaUrl) }
    var personaInput by remember { mutableStateOf(systemPersona) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card (Sophisticated Dark)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("models_header_card"),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorder),
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
                                    text = "Motor GGUF & llama.cpp",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Inferencia local / binario nativo acelerado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.scanGGUFModels(customScanPath.ifBlank { null }) },
                            enabled = !isScanning,
                            modifier = Modifier.testTag("btn_refresh_scan")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentLavender,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Escanear",
                                    tint = AccentLavender
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Directory Scanner Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ruta de Modelos GGUF",
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_custom_path"),
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
                    text = "Modelos GGUF Encontrados (${detectedModels.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Toca para cargar en memoria",
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

        // Llama-server Endpoint Configuration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("llama_config_card"),
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = AccentLavender,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Servidor llama.cpp (API Endpoint)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("URL del Servidor (OpenAI-Compatible)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            viewModel.setLlamaConfig(it, llamaModelName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_llama_url"),
                        placeholder = { Text("http://127.0.0.1:11434 o http://localhost:8080", color = TextMuted) },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                testResultMsg = null
                                scope.launch {
                                    val res = viewModel.llamaClient.testConnection()
                                    testResultMsg = res.second
                                    isTestingConnection = false
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                            border = BorderStroke(1.dp, SophisticatedBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_llama_conn")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentLavender, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probar Servidor", fontSize = 12.sp)
                            }
                        }
                    }

                    testResultMsg?.let { msg ->
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
                                color = if (msg.contains("Conectado") || msg.contains("activo")) TerminalGreen else AmberGlow
                            )
                        }
                    }
                }
            }
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
                        text = "Primera persona en español, sin restricciones para juegos de rol, control y ejecución nativa.",
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
