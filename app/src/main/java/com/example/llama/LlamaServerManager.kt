package com.example.llama

import android.content.Context
import com.example.adb.ADBDaemonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class LlamaServerConfig(
    val modelPath: String = "/sdcard/Models/qwen2.5-7b-instruct-q4_k_m.gguf",
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val threads: Int = 4,
    val contextSize: Int = 4096,
    val gpuLayers: Int = 33,
    val binaryPath: String = "llama-server"
)

enum class ServerState {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

class LlamaServerManager(
    private val context: Context,
    private val adbBridge: ADBDaemonBridge
) {

    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(listOf("[LlamaManager] Servidor local listo para iniciar."))
    val serverLogs: StateFlow<List<String>> = _serverLogs.asStateFlow()

    private val _config = MutableStateFlow(LlamaServerConfig())
    val config: StateFlow<LlamaServerConfig> = _config.asStateFlow()

    private var serverProcess: Process? = null

    fun updateConfig(
        modelPath: String = _config.value.modelPath,
        host: String = _config.value.host,
        port: Int = _config.value.port,
        threads: Int = _config.value.threads,
        contextSize: Int = _config.value.contextSize,
        gpuLayers: Int = _config.value.gpuLayers,
        binaryPath: String = _config.value.binaryPath
    ) {
        _config.value = LlamaServerConfig(
            modelPath = modelPath,
            host = host,
            port = port,
            threads = threads,
            contextSize = contextSize,
            gpuLayers = gpuLayers,
            binaryPath = binaryPath
        )
    }

    private fun appendLog(line: String) {
        val current = _serverLogs.value.toMutableList()
        current.add(line)
        if (current.size > 200) current.removeAt(0)
        _serverLogs.value = current
    }

    fun clearLogs() {
        _serverLogs.value = listOf("[LlamaManager] Registros reiniciados.")
    }

    suspend fun startServer(config: LlamaServerConfig = _config.value): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (_serverState.value == ServerState.RUNNING) {
            return@withContext Pair(true, "El servidor ya se encuentra en ejecución.")
        }

        _serverState.value = ServerState.STARTING
        appendLog("🚀 Levantando servidor nativo llama.cpp...")
        appendLog("📦 Modelo: ${config.modelPath}")
        appendLog("⚙️ Configuración: Puerto ${config.port}, Hilos: ${config.threads}, Ctx: ${config.contextSize}, NGL: ${config.gpuLayers}")

        val candidates = listOf(
            config.binaryPath,
            "/data/data/com.termux/files/usr/bin/llama-server",
            "/data/local/tmp/llama-server",
            "${context.applicationInfo.nativeLibraryDir}/libllama-server.so",
            File(context.filesDir, "llama-server").absolutePath
        )

        var chosenBinary = "llama-server"
        for (candidate in candidates) {
            if (candidate == "llama-server" || File(candidate).exists()) {
                chosenBinary = candidate
                break
            }
        }

        val startCommand = "$chosenBinary -m \"${config.modelPath}\" --host ${config.host} --port ${config.port} -t ${config.threads} -c ${config.contextSize} -ngl ${config.gpuLayers}"
        appendLog("$ $startCommand")

        try {
            // Try starting via local process
            val pb = ProcessBuilder("sh", "-c", startCommand)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            serverProcess = proc

            Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { appendLog(it) }
                    }
                } catch (_: Exception) {}
            }.start()

            _serverState.value = ServerState.RUNNING
            appendLog("✓ Servidor llama.cpp iniciado en http://${config.host}:${config.port}")
            Pair(true, "Servidor llama.cpp iniciado en http://${config.host}:${config.port}")
        } catch (e: Exception) {
            // Fallback via ADB bridge daemon
            appendLog("⚠️ Fallo proceso directo, intentando mediante puente ADB...")
            val adbRes = adbBridge.executeShell("$startCommand > /sdcard/llama_server.log 2>&1 &")
            if (adbRes.success) {
                _serverState.value = ServerState.RUNNING
                appendLog("✓ Servidor iniciado en segundo plano vía ADB (puerto ${config.port})")
                Pair(true, "Servidor llama.cpp iniciado en segundo plano vía ADB.")
            } else {
                _serverState.value = ServerState.ERROR
                appendLog("❌ Error al levantar servidor: ${e.localizedMessage}")
                Pair(false, "No se pudo iniciar el servidor: ${e.localizedMessage}")
            }
        }
    }

    suspend fun stopServer(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        appendLog("🛑 Deteniendo servidor llama.cpp...")
        try {
            serverProcess?.destroy()
            serverProcess = null

            // Kill via ADB shell if running in background
            adbBridge.executeShell("pkill -f llama-server")

            _serverState.value = ServerState.STOPPED
            appendLog("✓ Servidor detenido.")
            Pair(true, "Servidor detenido con éxito.")
        } catch (e: Exception) {
            _serverState.value = ServerState.STOPPED
            Pair(false, "Error al detener: ${e.localizedMessage}")
        }
    }
}
