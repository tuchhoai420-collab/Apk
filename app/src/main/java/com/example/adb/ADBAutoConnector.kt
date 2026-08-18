package com.example.adb

import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class AutoConnectStatus {
    IDLE,
    SCANNING_PORTS,
    PAIRING,
    CONNECTING,
    CONNECTED,
    FAILED
}

class ADBAutoConnector(
    private val context: Context,
    private val adbBridge: ADBDaemonBridge
) {

    private val _status = MutableStateFlow(AutoConnectStatus.IDLE)
    val status: StateFlow<AutoConnectStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(listOf("Asistente de Vinculación Inalámbrica preparado."))
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private fun log(msg: String) {
        val current = _logs.value.toMutableList()
        current.add(msg)
        if (current.size > 100) current.removeAt(0)
        _logs.value = current
    }

    fun openDeveloperSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            log("📲 Abriendo Opciones de Desarrollador...")
        } catch (e: Exception) {
            log("⚠️ No se pudo abrir Ajustes directamente: ${e.localizedMessage}")
        }
    }

    suspend fun autoDiscoverAndConnect(
        host: String = "127.0.0.1",
        preferredPort: Int = 5555
    ): Boolean = withContext(Dispatchers.IO) {
        _status.value = AutoConnectStatus.SCANNING_PORTS
        log("🔍 Buscando puertos ADB activos en $host...")

        // 1. Check preferred port first
        if (isPortReachable(host, preferredPort, 1200)) {
            log("✓ Puerto principal $preferredPort detectado.")
            return@withContext attemptConnect(host, preferredPort)
        }

        // 2. Scan standard wireless debugging port ranges (e.g., 37000..45000)
        val testPorts = listOf(5555, 37001, 38000, 39000, 40000, 41000, 42000, 43000, 44000, 45000)
        for (port in testPorts) {
            if (isPortReachable(host, port, 400)) {
                log("✓ Puerto ADB inalámbrico detectado en $host:$port")
                return@withContext attemptConnect(host, port)
            }
        }

        _status.value = AutoConnectStatus.FAILED
        log("❌ No se encontró ningún puerto ADB inalámbrico abierto. Por favor, asegúrate de activar 'Depuración inalámbrica' en Opciones de Desarrollador.")
        false
    }

    suspend fun autoPairAndConnect(
        host: String = "127.0.0.1",
        pairPort: Int,
        pairCode: String,
        targetConnectPort: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        _status.value = AutoConnectStatus.PAIRING
        log("🔗 Iniciando vinculación automática con código $pairCode en puerto $pairPort...")

        val pairRes = adbBridge.pairDevice(host, pairPort, pairCode)
        log(pairRes.output)

        // Now discover active connect port or use target
        val connectPort = targetConnectPort ?: findActiveConnectPort(host) ?: (pairPort - 1)
        return@withContext attemptConnect(host, connectPort)
    }

    private suspend fun attemptConnect(host: String, port: Int): Boolean {
        _status.value = AutoConnectStatus.CONNECTING
        log("🔌 Conectando puente ADB en $host:$port...")
        adbBridge.updateTarget(host, port)
        val connRes = adbBridge.testConnection()
        log(connRes.output)

        return if (connRes.success) {
            _status.value = AutoConnectStatus.CONNECTED
            log("🎉 Dispositivo vinculado y conectado correctamente en $host:$port.")
            true
        } else {
            _status.value = AutoConnectStatus.FAILED
            log("❌ Error de conexión al puerto $port.")
            false
        }
    }

    private fun findActiveConnectPort(host: String): Int? {
        val candidates = listOf(5555, 37000, 38000, 39000, 40000, 41000, 42000, 43000, 44000)
        for (p in candidates) {
            if (isPortReachable(host, p, 500)) return p
        }
        return null
    }

    private fun isPortReachable(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
