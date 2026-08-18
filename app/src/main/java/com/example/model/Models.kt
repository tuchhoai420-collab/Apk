package com.example.model

data class GGUFModelInfo(
    val filename: String,
    val path: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val parameters: String,
    val quantization: String,
    val architecture: String,
    val contextLength: Int = 4096,
    val lastModified: Long = System.currentTimeMillis()
)

data class UINode(
    val text: String,
    val description: String,
    val id: String,
    val className: String,
    val clickable: Boolean,
    val bounds: String,
    val centerX: Int,
    val centerY: Int,
    val width: Int = 0,
    val height: Int = 0
)

data class ADBDeviceState(
    val isConnected: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 5555,
    val deviceModel: String = "Android Device",
    val androidVersion: String = "Android 14+",
    val batteryLevel: String = "--",
    val batteryStatus: String = "Desconocido",
    val currentFocusedApp: String = "Desconocida",
    val screenResolution: String = "--",
    val lastPingMs: Long = 0L
)

enum class AgentActionType {
    TAP,
    SWIPE,
    TYPE,
    KEYEVENT,
    LAUNCH,
    SHELL,
    FINISH,
    UNKNOWN
}

data class AgentDecision(
    val thought: String,
    val action: AgentActionType,
    val rawAction: String,
    val params: Map<String, Any>,
    val rawJson: String = ""
)

data class AgentStepLog(
    val stepNumber: Int,
    val thought: String,
    val action: AgentActionType,
    val actionDetails: String,
    val commandExecuted: String,
    val executionResult: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentSession(
    val id: String,
    val userPrompt: String,
    val status: SessionStatus,
    val steps: List<AgentStepLog> = emptyList(),
    val summary: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

enum class SessionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}
