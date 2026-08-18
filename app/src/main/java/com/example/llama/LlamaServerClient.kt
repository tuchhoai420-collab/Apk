package com.example.llama

import com.example.model.AgentActionType
import com.example.model.AgentDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlamaServerClient(
    private var baseUrl: String = "http://127.0.0.1:11434",
    private var defaultModel: String = "qwen3:8b"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun updateConfig(url: String, model: String) {
        this.baseUrl = url.trimEnd('/')
        this.defaultModel = model
    }

    fun getBaseUrl(): String = baseUrl
    fun getDefaultModel(): String = defaultModel

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.endsWith("/v1")) "$baseUrl/models" else "$baseUrl/v1/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val startTime = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Pair(true, "Conectado al servidor llama.cpp (${elapsed}ms)")
                } else {
                    // Try root endpoint
                    val rootReq = Request.Builder().url(baseUrl).get().build()
                    client.newCall(rootReq).execute().use { rootResp ->
                        if (rootResp.isSuccessful || rootResp.code < 500) {
                            Pair(true, "Servidor activo en $baseUrl (${elapsed}ms)")
                        } else {
                            Pair(false, "Código HTTP ${response.code}: ${response.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Pair(false, "No se pudo conectar a $baseUrl: ${e.localizedMessage}")
        }
    }

    suspend fun queryAgentDecision(
        systemPrompt: String,
        userContextPrompt: String,
        temperature: Double = 0.1
    ): AgentDecision = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.endsWith("/v1/chat/completions")) {
            baseUrl
        } else if (baseUrl.endsWith("/v1")) {
            "$baseUrl/chat/completions"
        } else {
            "$baseUrl/v1/chat/completions"
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userContextPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", defaultModel)
            put("messages", messages)
            put("temperature", temperature)
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
        }

        val body = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw RuntimeException("Error del servidor llama.cpp (${response.code}): $errorBody")
            }

            val responseBody = response.body?.string() ?: throw RuntimeException("Respuesta vacía del servidor")
            parseDecisionJson(responseBody)
        }
    }

    private fun parseDecisionJson(rawResponse: String): AgentDecision {
        val rootObj = JSONObject(rawResponse)
        val choices = rootObj.getJSONArray("choices")
        if (choices.length() == 0) {
            throw RuntimeException("El modelo no devolvió ninguna opción de respuesta")
        }

        val messageObj = choices.getJSONObject(0).getJSONObject("message")
        val content = messageObj.getString("content").trim()

        // Handle possible markdown JSON fence
        val cleanContent = if (content.startsWith("```json")) {
            content.substringAfter("```json").substringBefore("```").trim()
        } else if (content.startsWith("```")) {
            content.substringAfter("```").substringBefore("```").trim()
        } else {
            content
        }

        return try {
            val decisionObj = JSONObject(cleanContent)
            val thought = decisionObj.optString("thought", "Analizando el estado del sistema...")
            val actionStr = decisionObj.optString("action", "finish").lowercase()
            val paramsObj = decisionObj.optJSONObject("params") ?: JSONObject()

            val paramsMap = mutableMapOf<String, Any>()
            val keys = paramsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                paramsMap[key] = paramsObj.get(key)
            }

            val actionType = when (actionStr) {
                "tap" -> AgentActionType.TAP
                "swipe" -> AgentActionType.SWIPE
                "type" -> AgentActionType.TYPE
                "keyevent" -> AgentActionType.KEYEVENT
                "launch" -> AgentActionType.LAUNCH
                "shell" -> AgentActionType.SHELL
                "finish" -> AgentActionType.FINISH
                else -> AgentActionType.UNKNOWN
            }

            AgentDecision(
                thought = thought,
                action = actionType,
                rawAction = actionStr,
                params = paramsMap,
                rawJson = cleanContent
            )
        } catch (e: Exception) {
            // Fallback for unstructured text
            AgentDecision(
                thought = cleanContent,
                action = AgentActionType.FINISH,
                rawAction = "finish",
                params = emptyMap(),
                rawJson = cleanContent
            )
        }
    }
}
