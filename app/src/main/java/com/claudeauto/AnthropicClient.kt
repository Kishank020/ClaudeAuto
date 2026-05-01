package com.claudeauto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Message(val role: String, val content: String)

class AnthropicClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-20250514"
        private const val MAX_TOKENS = 1024
        private val SYSTEM_PROMPT = """
            You are Claude, an AI assistant integrated into Android Auto for hands-free voice conversations.
            The user is driving, so:
            - Keep responses concise and clear (2-4 sentences when possible)
            - Avoid long lists or complex formatting — this will be read aloud
            - Be conversational and natural, like talking to a passenger
            - If asked something complex, give a brief answer and offer to elaborate
            - Prioritize safety: never encourage distracted driving
        """.trimIndent()
    }

    suspend fun sendMessage(history: List<Message>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            for (msg in history) {
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("system", SYSTEM_PROMPT)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response from API")
            )

            if (!response.isSuccessful) {
                val errorJson = JSONObject(responseBody)
                val errorMsg = errorJson.optJSONObject("error")?.optString("message") ?: "API error ${response.code}"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            val text = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
