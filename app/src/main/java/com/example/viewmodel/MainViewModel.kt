package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.adb.ADBAutoConnector
import com.example.adb.ADBDaemonBridge
import com.example.adb.AutoConnectStatus
import com.example.agent.AIEngineType
import com.example.agent.NaturalLanguageOSController
import com.example.data.AppDatabase
import com.example.data.CommandHistoryEntity
import com.example.data.SavedModelEntity
import com.example.gemini.GeminiClient
import com.example.llama.GGUFScanner
import com.example.llama.LlamaServerClient
import com.example.llama.LlamaServerConfig
import com.example.llama.LlamaServerManager
import com.example.llama.ServerState
import com.example.model.ADBDeviceState
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

    private val prefs = application.getSharedPreferences("cometa_os_prefs", Context.MODE_PRIVATE)

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "cometa_os_database"
    ).fallbackToDestructiveMigration().build()

    val commandHistoryDao = db.commandHistoryDao()
    val savedModelDao = db.savedModelDao()

    val adbBridge = ADBDaemonBridge(application)
    val llamaClient = LlamaServerClient()
    val geminiClient = GeminiClient()
    val llamaServerManager = LlamaServerManager(application, adbBridge)
    val adbAutoConnector = ADBAutoConnector(application, adbBridge)
    val ggufScanner = GGUFScanner(application)
    val agentController = NaturalLanguageOSController(adbBridge, llamaClient, geminiClient)

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

    private val _llamaUrl = MutableStateFlow("http://127.0.0.1:8080")
    val llamaUrl: StateFlow<String> = _llamaUrl.asStateFlow()

    private val _llamaModelName = MutableStateFlow("qwen3:8b")
    val llamaModelName: StateFlow<String> = _llamaModelName.asStateFlow()

    // Engine and Cloud AI Settings
    val activeEngine: StateFlow<AIEngineType> = agentController.activeEngine

    private val _geminiModel = MutableStateFlow(prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _systemPersona = MutableStateFlow("Cometa AI: Controlador autónomo sin restricciones para Android, siempre en primera persona y en español.")
    val systemPersona: StateFlow<String> = _systemPersona.asStateFlow()

    init {
        // Restaurar clave manual guardada y aplicarla al cliente
        val savedKey = _geminiApiKey.value
        if (savedKey.isNotBlank()) {
            geminiClient.setCustomApiKey(savedKey)
        }
        geminiClient.setModel(_geminiModel.value)
    }

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

    fun setAIEngine(engine: AIEngineType) {
        agentController.setEngine(engine)
    }

    fun setGeminiConfig(model: String, customApiKey: String? = null) {
        _geminiModel.value = model
        prefs.edit().putString("gemini_model", model).apply()
        if (customApiKey != null) {
            _geminiApiKey.value = customApiKey
            geminiClient.setCustomApiKey(customApiKey.ifBlank { null })
            prefs.edit().putString("gemini_api_key", customApiKey).apply()
        }
        geminiClient.setModel(model)
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

    fun startLlamaServer(
        port: Int = 8080,
        threads: Int = 4,
        contextSize: Int = 4096,
        gpuLayers: Int = 0
    ) {
        viewModelScope.launch {
            val model = _selectedModel.value
            if (model == null) {
                appendConsoleLog("❌ No hay modelo GGUF seleccionado.")
                return@launch
            }
            appendConsoleLog("🚀 Iniciando llama-server con ${model.name}...")
            llamaServerManager.updateConfig(
                modelPath = model.path,
                port = port,
                threads = threads,
                contextSize = contextSize,
                gpuLayers = gpuLayers
            )
            val cfg = llamaServerManager.config.value
            val res = llamaServerManager.startServer(cfg)
            appendConsoleLog(res.second)
            if (res.first) {
                setLlamaConfig("http://127.0.0.1:$port", model.name)
            }
        }
    }

    fun stopLlamaServer() {
        viewModelScope.launch {
            appendConsoleLog("⏹ Deteniendo llama-server...")
            val res = llamaServerManager.stopServer()
            appendConsoleLog(res.second)
        }
    }

    fun autoPairAndConnectAdb(pairPort: Int, pairCode: String) {
        viewModelScope.launch {
            appendConsoleLog("📡 Auto-pairing ADB en puerto $pairPort...")
            setPairingConfig(pairPort, pairCode)
            val result = adbAutoConnector.autoPairAndConnect(pairPort, pairCode)
            appendConsoleLog(result)
            refreshDeviceState()
        }
    }

    fun autoDiscoverAdbPorts() {
        viewModelScope.launch {
            appendConsoleLog("🔍 Descubriendo puertos ADB abiertos...")
            val found = adbAutoConnector.discoverOpenPorts()
            appendConsoleLog(found)
        }
    }

    fun openDevSettings() {
        viewModelScope.launch {
            adbBridge.executeShell("am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
        }
    }

    fun selectModel(model: GGUFModelInfo) {
        _selectedModel.value = model
        appendConsoleLog("✓ Modelo seleccionado: ${model.name} (${model.sizeFormatted})")
    }

    fun scanGGUFModels(customDir: String? = null) {
        viewModelScope.launch {
            _isScanningModels.value = true
            appendConsoleLog("🔍 Escaneando modelos GGUF...")
            try {
                val models = ggufScanner.scanAllLocations(customDir)
                _detectedModels.value = models
                appendConsoleLog("✓ Encontrados ${models.size} modelos GGUF.")
            } catch (e: Exception) {
                appendConsoleLog("❌ Error al escanear: ${e.message}")
            } finally {
                _isScanningModels.value = false
            }
        }
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            _deviceState.value = adbBridge.getDeviceState()
        }
    }

    fun executeNaturalCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            appendConsoleLog("> $command")
            val session = agentController.executeNaturalLanguageCommand(
                command = command,
                systemPersona = _systemPersona.value
            )
            val logText = session.steps.joinToString("\n") { step ->
                "[${step.status}] ${step.description}: ${step.result ?: ""}"
            }
            appendConsoleLog(logText.ifBlank { "(sin pasos)" })
            commandHistoryDao.insert(
                CommandHistoryEntity(
                    command = command,
                    engine = agentController.activeEngine.value.name,
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
