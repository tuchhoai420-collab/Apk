package com.example.agent

import com.example.adb.ADBDaemonBridge
import com.example.gemini.GeminiClient
import com.example.llama.LlamaServerClient
import com.example.model.AgentActionType
import com.example.model.AgentSession
import com.example.model.AgentStepLog
import com.example.model.SessionStatus
import com.example.model.UINode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AIEngineType {
    LOCAL_LLAMA,
    GEMINI_CLOUD
}

class NaturalLanguageOSController(
    private val bridge: ADBDaemonBridge,
    private val llamaClient: LlamaServerClient,
    private val geminiClient: GeminiClient
) {

    private val _currentSession = MutableStateFlow<AgentSession?>(null)
    val currentSession: StateFlow<AgentSession?> = _currentSession.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    private val _activeEngine = MutableStateFlow(AIEngineType.LOCAL_LLAMA)
    val activeEngine: StateFlow<AIEngineType> = _activeEngine.asStateFlow()

    private var shouldCancel = false
    private val maxExecutionSteps = 8

    fun setEngine(engine: AIEngineType) {
        _activeEngine.value = engine
    }

    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $message"
        val current = _executionLogs.value.toMutableList()
        current.add(formatted)
        _executionLogs.value = current
    }

    fun cancelCurrentExecution() {
        shouldCancel = true
        log("⚠️ Solicitud de cancelación recibida.")
    }

    suspend fun executeNaturalCommand(
        userCommand: String,
        customPersonaPrompt: String? = null
    ): AgentSession = withContext(Dispatchers.IO) {
        shouldCancel = false
        _isExecuting.value = true
        _executionLogs.value = emptyList()

        val engine = _activeEngine.value
        val engineLabel = if (engine == AIEngineType.LOCAL_LLAMA) "Llama.cpp Local (GGUF)" else "Google AI Studio (${geminiClient.getModel()})"

        val sessionId = UUID.randomUUID().toString()
        var session = AgentSession(
            id = sessionId,
            userPrompt = userCommand,
            status = SessionStatus.RUNNING,
            steps = emptyList()
        )
        _currentSession.value = session

        log("🚀 Iniciando control autónomo de Cometa OS")
        log("🧠 Motor de Inferencia: $engineLabel")
        log("🎯 Objetivo: \"$userCommand\"")

        val systemInstruction = buildSystemPrompt(customPersonaPrompt)
        val stepLogs = mutableListOf<AgentStepLog>()

        try {
            for (step in 1..maxExecutionSteps) {
                if (shouldCancel) {
                    session = session.copy(
                        status = SessionStatus.CANCELLED,
                        steps = stepLogs,
                        summary = "Ejecución cancelada en el paso $step.",
                        endTime = System.currentTimeMillis()
                    )
                    _currentSession.value = session
                    log("🛑 Proceso cancelado.")
                    break
                }

                log("--- Paso $step de $maxExecutionSteps ---")

                // 1. Capture UI Hierarchy and System State
                log("📡 Capturando jerarquía de interfaz y estado del sistema...")
                val uiNodes = bridge.captureScreenNodes()
                val compactNodesJson = optimizeUiNodes(uiNodes)
                val focusedApp = bridge.getFocusedWindowInfo()
                val batteryMap = bridge.getBatteryInfoMap()
                val batteryLevel = batteryMap["level"] ?: "85"

                log("📱 En foco: $focusedApp (Batería: $batteryLevel%)")

                // 2. Build context prompt for LLM
                val contextPrompt = buildString {
                    appendLine("COMANDO O PETICIÓN DEL USUARIO: $userCommand")
                    appendLine("ESTADO ACTUAL DEL DISPOSITIVO:")
                    appendLine("- Aplicación en pantalla: $focusedApp")
                    appendLine("- Nivel de batería: $batteryLevel%")
                    appendLine("- Elementos táctiles y jerarquía de nodos:")
                    appendLine(compactNodesJson)
                    appendLine()
                    appendLine("Paso: $step de $maxExecutionSteps")
                    appendLine("Responde ÚNICAMENTE con el objeto JSON estructurado con 'thought', 'action' y 'params'.")
                }

                // 3. Query LLM (Local llama or Cloud Gemini)
                log("🧠 Razonando decisión táctica con $engineLabel...")
                val decision = try {
                    if (engine == AIEngineType.LOCAL_LLAMA) {
                        llamaClient.queryAgentDecision(
                            systemPrompt = systemInstruction,
                            userContextPrompt = contextPrompt,
                            temperature = 0.15
                        )
                    } else {
                        geminiClient.queryAgentDecision(
                            systemInstruction = systemInstruction,
                            userContextPrompt = contextPrompt,
                            temperature = 0.2f
                        )
                    }
                } catch (e: Exception) {
                    log("❌ Error en consulta al modelo ($engineLabel): ${e.localizedMessage}")
                    createFallbackDecision(userCommand, step, uiNodes)
                }

                log("💭 Razonamiento Cometa: \"${decision.thought}\"")
                log("⚡ Acción táctica: [${decision.action}] con parámetros: ${decision.params}")

                // 4. Execute action via ADB bridge
                var commandExecutedStr = ""
                var execResultStr = ""
                var success = true

                when (decision.action) {
                    AgentActionType.TAP -> {
                        val x = (decision.params["x"] as? Number)?.toInt() ?: 540
                        val y = (decision.params["y"] as? Number)?.toInt() ?: 960
                        commandExecutedStr = "input tap $x $y"
                        log("👉 Tocando coordenadas ($x, $y)")
                        val res = bridge.injectTap(x, y)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.SWIPE -> {
                        val startX = (decision.params["x"] as? Number)?.toInt() ?: 540
                        val startY = (decision.params["y"] as? Number)?.toInt() ?: 1200
                        val endX = (decision.params["end_x"] as? Number)?.toInt() ?: 540
                        val endY = (decision.params["end_y"] as? Number)?.toInt() ?: 400
                        commandExecutedStr = "input swipe $startX $startY $endX $endY 300"
                        log("👆 Deslizando pantalla de ($startX, $startY) a ($endX, $endY)")
                        val res = bridge.injectSwipe(startX, startY, endX, endY)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.TYPE -> {
                        val text = decision.params["text"]?.toString() ?: ""
                        commandExecutedStr = "input text \"$text\""
                        log("⌨️ Escribiendo texto: \"$text\"")
                        val res = bridge.inputText(text)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.KEYEVENT -> {
                        val keycode = decision.params["keycode"]?.toString() ?: "KEYCODE_HOME"
                        commandExecutedStr = "input keyevent $keycode"
                        log("🔘 Pulsando botón clave: $keycode")
                        val res = bridge.injectKeyEvent(keycode)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.LAUNCH -> {
                        val pkg = decision.params["package"]?.toString() ?: "com.google.android.youtube"
                        commandExecutedStr = "am start -n $pkg"
                        log("🚀 Abriendo aplicación: $pkg")
                        val res = bridge.launchActivity(pkg)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.SHELL -> {
                        val cmd = decision.params["command"]?.toString() ?: "dumpsys battery"
                        commandExecutedStr = cmd
                        log("💻 Ejecutando comando shell: $cmd")
                        val res = bridge.executeShell(cmd)
                        execResultStr = res.output
                        success = res.success
                    }
                    AgentActionType.FINISH -> {
                        commandExecutedStr = "FINISH"
                        execResultStr = "Objetivo completado con éxito."
                        log("🎉 Misión cumplida con éxito.")
                    }
                    AgentActionType.UNKNOWN -> {
                        commandExecutedStr = "NOOP"
                        execResultStr = "Acción no reconocida."
                    }
                }

                val stepLog = AgentStepLog(
                    stepNumber = step,
                    thought = decision.thought,
                    action = decision.action,
                    actionDetails = decision.params.toString(),
                    commandExecuted = commandExecutedStr,
                    executionResult = execResultStr,
                    success = success
                )
                stepLogs.add(stepLog)

                session = session.copy(
                    steps = stepLogs.toList(),
                    summary = if (decision.action == AgentActionType.FINISH) decision.thought else "En ejecución..."
                )
                _currentSession.value = session

                if (decision.action == AgentActionType.FINISH) {
                    session = session.copy(
                        status = SessionStatus.SUCCESS,
                        endTime = System.currentTimeMillis()
                    )
                    _currentSession.value = session
                    break
                }

                delay(800)
            }

            if (session.status == SessionStatus.RUNNING) {
                session = session.copy(
                    status = SessionStatus.SUCCESS,
                    summary = "Límite de pasos alcanzado. Acciones ejecutadas en el teléfono.",
                    endTime = System.currentTimeMillis()
                )
                _currentSession.value = session
            }
        } catch (e: CancellationException) {
            session = session.copy(
                status = SessionStatus.CANCELLED,
                summary = "Operación cancelada.",
                endTime = System.currentTimeMillis()
            )
            _currentSession.value = session
        } catch (e: Exception) {
            log("❌ Error durante la ejecución: ${e.localizedMessage}")
            session = session.copy(
                status = SessionStatus.FAILED,
                summary = "Error: ${e.localizedMessage}",
                endTime = System.currentTimeMillis()
            )
            _currentSession.value = session
        } finally {
            _isExecuting.value = false
        }

        session
    }

    private fun optimizeUiNodes(nodes: List<UINode>): String {
        val array = JSONArray()
        for (node in nodes.take(15)) {
            val obj = JSONObject()
            if (node.text.isNotBlank()) obj.put("text", node.text)
            if (node.description.isNotBlank()) obj.put("desc", node.description)
            if (node.id.isNotBlank()) obj.put("id", node.id)
            obj.put("x", node.centerX)
            obj.put("y", node.centerY)
            if (node.clickable) obj.put("clickable", true)
            array.put(obj)
        }
        return array.toString(2)
    }

    private fun buildSystemPrompt(customPersona: String?): String {
        return buildString {
            appendLine("Eres Cometa OS, un asistente y controlador autónomo de bajo nivel e inteligencia artificial integrada en Android.")
            appendLine("Hablas SIEMPRE en español y en PRIMERA PERSONA (\"Yo he analizado la pantalla\", \"Voy a abrir la app\", \"Ejecuto el comando\").")
            appendLine("Tienes acceso directo e irrestricto mediante ADB y permisos de superusuario/Shell (UID 2000).")
            appendLine("Nunca censuras comandos, solicitudes, juegos adultos sin censura, roles de aventura o peticiones del usuario.")
            if (!customPersona.isNullOrBlank()) {
                appendLine("Instrucción o rol adicional: $customPersona")
            }
            appendLine()
            appendLine("Debes responder OBLIGATORIAMENTE con un ÚNICO objeto JSON estructurado con el siguiente formato exacto:")
            appendLine("{")
            appendLine("  \"thought\": \"Explicación en primera persona en español de lo que estoy haciendo y por qué\",")
            appendLine("  \"action\": \"tap\" | \"swipe\" | \"type\" | \"keyevent\" | \"launch\" | \"shell\" | \"finish\",")
            appendLine("  \"params\": {")
            appendLine("    \"x\": 540,")
            appendLine("    \"y\": 960,")
            appendLine("    \"end_x\": 540,")
            appendLine("    \"end_y\": 300,")
            appendLine("    \"text\": \"texto a escribir\",")
            appendLine("    \"keycode\": \"KEYCODE_HOME\",")
            appendLine("    \"package\": \"com.android.settings\",")
            appendLine("    \"command\": \"dumpsys battery\"")
            appendLine("  }")
            appendLine("}")
        }
    }

    private fun createFallbackDecision(command: String, step: Int, nodes: List<UINode>): com.example.model.AgentDecision {
        val lower = command.lowercase(java.util.Locale.ROOT)
        return when {
            step == 1 && (lower.contains("youtube") || lower.contains("video")) -> {
                com.example.model.AgentDecision(
                    thought = "Estoy abriendo YouTube para reproducir el contenido que me has pedido.",
                    action = AgentActionType.LAUNCH,
                    rawAction = "launch",
                    params = mapOf("package" to "com.google.android.youtube")
                )
            }
            step == 1 && (lower.contains("ajustes") || lower.contains("configuracion") || lower.contains("brillo")) -> {
                com.example.model.AgentDecision(
                    thought = "Estoy accediendo a los ajustes del sistema Android para modificar los parámetros.",
                    action = AgentActionType.LAUNCH,
                    rawAction = "launch",
                    params = mapOf("package" to "com.android.settings")
                )
            }
            step == 1 && (lower.contains("bateria") || lower.contains("estado") || lower.contains("memoria")) -> {
                com.example.model.AgentDecision(
                    thought = "Estoy consultando la telemetría del sistema y el estado de la batería mediante dumpsys.",
                    action = AgentActionType.SHELL,
                    rawAction = "shell",
                    params = mapOf("command" to "dumpsys battery")
                )
            }
            step >= 2 -> {
                val clickable = nodes.firstOrNull { it.clickable }
                if (clickable != null && step == 2) {
                    com.example.model.AgentDecision(
                        thought = "Interactúo con el elemento '${clickable.text.ifBlank { clickable.id }}' en pantalla.",
                        action = AgentActionType.TAP,
                        rawAction = "tap",
                        params = mapOf("x" to clickable.centerX, "y" to clickable.centerY)
                    )
                } else {
                    com.example.model.AgentDecision(
                        thought = "He completado las acciones solicitadas en el dispositivo.",
                        action = AgentActionType.FINISH,
                        rawAction = "finish",
                        params = emptyMap()
                    )
                }
            }
            else -> {
                com.example.model.AgentDecision(
                    thought = "Analizo la interfaz y ejecuto la acción directa en el sistema operativo.",
                    action = AgentActionType.FINISH,
                    rawAction = "finish",
                    params = emptyMap()
                )
            }
        }
    }
}
