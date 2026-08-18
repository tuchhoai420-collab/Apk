package com.example.gemini

import com.example.BuildConfig
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

class GeminiClient(
    private var customApiKey: String? = null,
    private var modelName: String = "gemini-3.5-flash"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getApiKey(): String {
        return customApiKey?.ifBlank { null } ?: BuildConfig.GEMINI_API_KEY
    }

    fun setCustomApiKey(key: String?) {
        this.customApiKey = key
    }

    fun setModel(model: String) {
        this.modelName = model
    }

    fun getModel(): String = modelName

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(
                false,
                "API Key no configurada. Agrega tu clave de Google AI Studio en el panel de secretos o en el campo de configuración."
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Hola, responde brevemente con 'OK'")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val start = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - start
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Pair(true, "Conectado a Google AI Studio ($modelName en ${elapsed}ms)")
                } else {
                    val err = response.body?.string() ?: ""
                    Pair(false, "Error de Gemini API (${response.code}): $err")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Fallo al conectar con Gemini: ${e.localizedMessage}")
        }
    }

    suspend fun queryAgentDecision(
        systemInstruction: String,
        userContextPrompt: String,
        temperature: Float = 0.2f
    ): AgentDecision = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Se requiere la clave API de Google AI Studio Gemini.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userContextPrompt)
                    })
                })
            })
        }

        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemInstruction)
                })
            })
        }

        val generationConfigObj = JSONObject().apply {
            put("temperature", temperature)
            put("responseMimeType", "application/json")
        }

        val payload = JSONObject().apply {
            put("systemInstruction", systemInstructionObj)
            put("contents", contentsArray)
            put("generationConfig", generationConfigObj)
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw RuntimeException("Respuesta vacía de Gemini API")
            if (!response.isSuccessful) {
                throw RuntimeException("Error en Gemini API (${response.code}): $responseBody")
            }

            parseGeminiResponse(responseBody)
        }
    }

    private fun parseGeminiResponse(rawJson: String): AgentDecision {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
            ?: throw RuntimeException("No candidates in Gemini response")

        if (candidates.length() == 0) {
            throw RuntimeException("Gemini no devolvió ninguna respuesta")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text") ?: ""

        val cleanText = if (text.startsWith("```json")) {
            text.substringAfter("```json").substringBefore("```").trim()
        } else if (text.startsWith("```")) {
            text.substringAfter("```").substringBefore("```").trim()
        } else {
            text.trim()
        }

        return try {
            val decisionObj = JSONObject(cleanText)
            val thought = decisionObj.optString("thought", "Analizando el entorno táctico de Android...")
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
                rawJson = cleanText
            )
        } catch (e: Exception) {
            AgentDecision(
                thought = cleanText,
                action = AgentActionType.FINISH,
                rawAction = "finish",
                params = emptyMap(),
                rawJson = cleanText
            )
        }
    }
}
