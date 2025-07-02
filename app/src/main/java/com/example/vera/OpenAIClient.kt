package com.example.vera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject

class OpenAIClient(private val apiKey: String) {

    private val client = OkHttpClient()
    private val url = "https://api.openai.com/v1/chat/completions"

    suspend fun ask(question: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
        body.put("model", "gpt-3.5-turbo")
        body.put("messages", listOf(mapOf("role" to "user", "content" to question)))

        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            body.toString()
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext "Nincs válasz."
        val json = JSONObject(responseBody)
        return@withContext json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
