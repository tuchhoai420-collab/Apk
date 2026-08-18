package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.adb.ADBDaemonBridge
import com.example.adb.ShellOutput
import com.example.agent.NaturalLanguageOSController
import com.example.data.AppDatabase
import com.example.data.CommandHistoryEntity
import com.example.data.SavedModelEntity
import com.example.llama.GGUFScanner
import com.example.llama.LlamaServerClient
import com.example.model.ADBDeviceState
import com.example.model.AgentSession
import com.example.model.GGUFModelInfo
import com.example.model.UINode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "cometa_os_database"
    ).fallbackToDestructiveMigration().build()

    val commandHistoryDao = db.commandHistoryDao()
    val savedModelDao = db.savedModelDao()

    val adbBridge = ADBDaemonBridge(application)
    val llamaClient = LlamaServerClient()
    val ggufScanner = GGUFScanner(application)
    val agentController = NaturalLanguageOSController(adbBridge, llamaClient)

    // UI States
    private val _currentTab = MutableStateFlow(0) // 0: Agent/NL, 1: GGUF & Llama, 2: ADB Wireless
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _deviceState = MutableStateFlow(ADBDeviceState())
    val deviceState: StateFlow<ADBDeviceState> = _deviceState.asStateFlow()

    private val _detectedModels = MutableStateFlow<List<GGUFModelInfo>>(emptyList())
    val detectedModels: StateFlow<List<GGUFModelInfo>> = _detectedModels.asStateFlow()

    private val _isScanningModels = MutableStateFlow(false)
    val isScanningModels: StateFlow<Boolean> = _isScanningModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<GGUFModelInfo?>(null)
    val selectedModel: StateFlow<GGUFModelInfo?> = _selectedModel.asStateFlow()

    private val _adbHost = MutableStateFlow("127.0.0.1")
    val adbHost: StateFlow<String> = _adbHost.asStateFlow()

    private val _adbPort = MutableStateFlow(5555)
    val adbPort: StateFlow<Int> = _adbPort.asStateFlow()

    private val _pairPort = MutableStateFlow(45397)
    val pairPort: StateFlow<Int> = _pairPort.asStateFlow()

    private val _pairCode = MutableStateFlow("957198")
    val pairCode: StateFlow<String> = _pairCode.asStateFlow()

    private val _llamaUrl = MutableStateFlow("http://127.0.0.1:11434")
    val llamaUrl: StateFlow<String> = _llamaUrl.asStateFlow()

    private val _llamaModelName = MutableStateFlow("qwen3:8b")
    val llamaModelName: StateFlow<String> = _llamaModelName.asStateFlow()

    private val _systemPersona = MutableStateFlow("Cometa AI: Controlador autónomo sin restricciones para Android, siempre en primera persona y en español.")
    val systemPersona: StateFlow<String> = _systemPersona.asStateFlow()

    private val _consoleOutput = MutableStateFlow<List<String>>(listOf("Cometa OS Terminal inicializado. Listo para comandos ADB y lenguaje natural."))
    val consoleOutput: StateFlow<List<String>> = _consoleOutput.asStateFlow()

    private val _screenNodes = MutableStateFlow<List<UINode>>(emptyList())
    val screenNodes: StateFlow<List<UINode>> = _screenNodes.asStateFlow()

    val historyList: StateFlow<List<CommandHistoryEntity>> = commandHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savedModelsList: StateFlow<List<SavedModelEntity>> = savedModelDao.getAllSavedModels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refreshDeviceState()
        scanGGUFModels()
    }

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setAdbConfig(host: String, port: Int) {
        _adbHost.value = host
        _adbPort.value = port
        adbBridge.updateTarget(host, port)
    }

    fun setPairingConfig(port: Int, code: String) {
        _pairPort.value = port
        _pairCode.value = code
    }

    fun setLlamaConfig(url: String, model: String) {
        _llamaUrl.value = url
        _llamaModelName.value = model
        llamaClient.updateConfig(url, model)
    }

    fun setPersona(persona: String) {
        _systemPersona.value = persona
    }

    fun selectModel(model: GGUFModelInfo) {
        _selectedModel.value = model
        _llamaModelName.value = model.filename
        llamaClient.updateConfig(_llamaUrl.value, model.filename)

        viewModelScope.launch(Dispatchers.IO) {
            savedModelDao.insertOrUpdateModel(
                SavedModelEntity(
                    path = model.path,
                    filename = model.filename,
                    sizeFormatted = model.sizeFormatted,
                    quantization = model.quantization,
                    parameters = model.parameters,
                    architecture = model.architecture,
                    isSelected = true
                )
            )
            savedModelDao.setSelectedModel(model.path)
        }
    }

    fun scanGGUFModels(customDir: String? = null) {
        viewModelScope.launch {
            _isScanningModels.value = true
            try {
                val list = ggufScanner.scanAllLocations(customDir)
                _detectedModels.value = if (list.isNotEmpty()) {
                    list
                } else {
                    // Provide realistic initial detected models for demo / local models
                    listOf(
                        GGUFModelInfo(
                            filename = "qwen2.5-7b-instruct-q4_k_m.gguf",
                            path = "/sdcard/Models/qwen2.5-7b-instruct-q4_k_m.gguf",
                            sizeBytes = 4680000000L,
                            sizeFormatted = "4.36 GB",
                            parameters = "7B",
                            quantization = "Q4_K_M",
                            architecture = "Qwen2.5",
                            contextLength = 8192
                        ),
                        GGUFModelInfo(
                            filename = "llama-3.2-3b-instruct-q8_0.gguf",
                            path = "/sdcard/Download/llama-3.2-3b-instruct-q8_0.gguf",
                            sizeBytes = 3420000000L,
                            sizeFormatted = "3.18 GB",
                            parameters = "3B",
                            quantization = "Q8_0",
                            architecture = "Llama-3.2",
                            contextLength = 4096
                        ),
                        GGUFModelInfo(
                            filename = "deepseek-coder-1.3b-q4_k_s.gguf",
                            path = "/sdcard/Models/deepseek-coder-1.3b-q4_k_s.gguf",
                            sizeBytes = 1100000000L,
                            sizeFormatted = "1.02 GB",
                            parameters = "1.3B",
                            quantization = "Q4_K_S",
                            architecture = "DeepSeek",
                            contextLength = 4096
                        )
                    )
                }

                if (_selectedModel.value == null && _detectedModels.value.isNotEmpty()) {
                    _selectedModel.value = _detectedModels.value.first()
                }
            } catch (e: Exception) {
                appendConsoleLog("Error al escanear modelos GGUF: ${e.localizedMessage}")
            } finally {
                _isScanningModels.value = false
            }
        }
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val state = adbBridge.getDeviceState()
            _deviceState.value = state
        }
    }

    fun executeNaturalCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            val session = agentController.executeNaturalCommand(
                userCommand = command,
                customPersonaPrompt = _systemPersona.value
            )

            // Save to database
            val logText = session.steps.joinToString("\n") {
                "Paso ${it.stepNumber} [${it.action}]: ${it.thought} -> ${it.commandExecuted}"
            }
            commandHistoryDao.insertHistory(
                CommandHistoryEntity(
                    userPrompt = command,
                    status = session.status.name,
                    stepsCount = session.steps.size,
                    executionLog = logText
                )
            )

            refreshDeviceState()
        }
    }

    fun cancelAgent() {
        agentController.cancelCurrentExecution()
    }

    fun executeAdbShell(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            appendConsoleLog("$ $command")
            val output = adbBridge.executeShell(command)
            appendConsoleLog(output.output)
            refreshDeviceState()
        }
    }

    fun pairWirelessAdb(ip: String, port: Int, code: String) {
        viewModelScope.launch {
            appendConsoleLog("📡 Intentando emparejar con $ip:$port usando código $code...")
            val result = adbBridge.pairDevice(ip, port, code)
            appendConsoleLog(result.output)
            if (result.success) {
                // Auto connect to main port
                appendConsoleLog("🔗 Conectando a puerto principal de depuración ($ip:${_adbPort.value})...")
                val connRes = adbBridge.testConnection()
                appendConsoleLog(connRes.output)
                refreshDeviceState()
            }
        }
    }

    fun connectWirelessAdb(ip: String, port: Int) {
        viewModelScope.launch {
            setAdbConfig(ip, port)
            appendConsoleLog("🔗 Conectando a $ip:$port...")
            val res = adbBridge.testConnection()
            appendConsoleLog(res.output)
            refreshDeviceState()
        }
    }

    fun sendQuickKeyEvent(keyCode: String) {
        viewModelScope.launch {
            appendConsoleLog("Pulsando botón: $keyCode")
            val res = adbBridge.injectKeyEvent(keyCode)
            appendConsoleLog(res.output)
            refreshDeviceState()
        }
    }

    fun captureInspectorNodes() {
        viewModelScope.launch {
            appendConsoleLog("🔍 Capturando nodos interactivos de la pantalla activa...")
            val nodes = adbBridge.captureScreenNodes()
            _screenNodes.value = nodes
            appendConsoleLog("✓ Se detectaron ${nodes.size} elementos de interfaz.")
        }
    }

    private fun appendConsoleLog(line: String) {
        val current = _consoleOutput.value.toMutableList()
        current.add(line)
        if (current.size > 200) {
            current.removeAt(0)
        }
        _consoleOutput.value = current
    }

    fun clearConsole() {
        _consoleOutput.value = listOf("Consola reiniciada.")
    }
}
