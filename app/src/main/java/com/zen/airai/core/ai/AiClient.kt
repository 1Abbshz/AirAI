package com.zen.airai.core.ai

import com.zen.airai.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

@Serializable
data class AiMessage(
    val role: String,
    val content: String,
    val name: String? = null
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<AiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null
)

class AiClient(private val preferences: PreferencesManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val sseFactory = EventSources.createFactory(httpClient)

    suspend fun chat(
        messages: List<AiMessage>,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val allMessages = buildList {
            if (!systemPrompt.isNullOrBlank()) {
                add(AiMessage(role = "system", content = systemPrompt))
            }
            addAll(messages)
        }

        val request = ChatCompletionRequest(
            model = preferences.getModel(),
            messages = allMessages,
            stream = false
        )

        val body = json.encodeToString(ChatCompletionRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("${preferences.getEndpoint()}/chat/completions")
            .addHeader("Authorization", "Bearer ${preferences.getApiKey()}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
        jsonResponse["choices"]
            ?.jsonArray?.get(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw Exception("Invalid response format")
    }

    fun chatStream(
        messages: List<AiMessage>,
        systemPrompt: String? = null
    ): Flow<String> = callbackFlow {
        val allMessages = buildList {
            if (!systemPrompt.isNullOrBlank()) {
                add(AiMessage(role = "system", content = systemPrompt))
            }
            addAll(messages)
        }

        val request = ChatCompletionRequest(
            model = preferences.getModel(),
            messages = allMessages,
            stream = true
        )

        val body = json.encodeToString(ChatCompletionRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("${preferences.getEndpoint()}/chat/completions")
            .addHeader("Authorization", "Bearer ${preferences.getApiKey()}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data.trim() == "[DONE]") {
                    close()
                    return
                }
                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val delta = chunk["choices"]
                        ?.jsonArray?.get(0)
                        ?.jsonObject?.get("delta")
                        ?.jsonObject?.get("content")
                        ?.jsonPrimitive?.content
                    if (delta != null) {
                        trySend(delta)
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed: ${response?.code}"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = sseFactory.newEventSource(httpRequest, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
