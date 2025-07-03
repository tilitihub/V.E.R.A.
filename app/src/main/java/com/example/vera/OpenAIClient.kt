package com.example.vera

import android.util.Log
import androidx.privacysandbox.tools.core.generator.build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// Egy Result wrapper osztály a sikeres válasz vagy hiba egyértelmű kezelésére
sealed class ApiResult {
    data class Success(val data: String) : ApiResult()
    data class Error(
        val
        errorMessage: String, val statusCode: Int? = null
    ) : ApiResult()
}

class OpenAIClient(private val apiKey: String) {

    // Konfigurálhatóbb OkHttpClient időtúllépésekkel
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Kapcsolódási időtúllépés
        .readTimeout(30, TimeUnit.SECONDS)    // Olvasási időtúllépés
        .writeTimeout(30, TimeUnit.SECONDS)   // Írási időtúllépés
        .build()

    // Konstansok a jobb karbantarthatóságért
    companion object {
        private const val BASE_URL = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
        private const val TAG = "OpenAIClient" // Logoláshoz
    }

    suspend fun ask(question: String, model: String = DEFAULT_MODEL): ApiResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (apiKey.isBlank()) {
                Log.e(TAG, "OpenAI API kulcs hiányzik vagy üres.")
                return@withContext ApiResult.Error("API kulcs nincs beállítva.")
            }

            val requestJson = JSONObject()
            try {
                requestJson.put("model", model)
                val messagesArray = JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                }
                requestJson.put("messages", listOf(messagesArray))
            } catch (e: JSONException) {
                Log.e(TAG, "Hiba a JSON kérés összeállításakor: ", e)
                return@withContext ApiResult.Error("Belső hiba a kérés összeállításakor.")
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json") // Jó gyakorlat expliciten megadni
                .post(requestBody)
                .build()

            try {
                Log.d(TAG, "OpenAI kérés indítása a(z) $BASE_URL címre, model: $model")
                client.newCall(request).execute()
                    .use { response -> // 'use' blokk az erőforrás-biztos lezáráshoz
                        val responseBodyString = response.body?.string()

                        if (!response.isSuccessful || responseBodyString == null) {
                            val errorMsg =
                                "OpenAI API hiba: ${response.code} ${response.message}. Válasz: $responseBodyString"
                            Log.e(TAG, errorMsg)
                            return@withContext ApiResult.Error(
                                "Hiba a szerverrel való kommunikáció során (kód: ${response.code}).",
                                response.code
                            )
                        }

                        Log.d(TAG, "OpenAI válasz (${response.code}): $responseBodyString")

                        try {
                            val jsonResponse = JSONObject(responseBodyString)
                            // Robusztusabb JSON feldolgozás
                            val choices = jsonResponse.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val firstChoice = choices.optJSONObject(0)
                                if (firstChoice != null) {
                                    val message = firstChoice.optJSONObject("message")
                                    if (message != null) {
                                        val content = message.optString("content")
                                        if (content.isNotEmpty()) {
                                            return@withContext ApiResult.Success(content)
                                        }
                                    }
                                }
                            }
                            Log.e(
                                TAG,
                                "Nem található 'content' a várt struktúrában a JSON válaszban."
                            )
                            return@withContext ApiResult.Error("Nem sikerült feldolgozni a szerver válaszát (formátumhiba).")
                        } catch (e: JSONException) {
                            Log.e(TAG, "Hiba a JSON válasz feldolgozásakor: ", e)
                            return@withContext ApiResult.Error("Hiba a szerver válaszának feldolgozásakor.")
                        }
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Hálózati hiba az OpenAI kérés során: ", e)
                return@withContext ApiResult.Error("Hálózati hiba. Ellenőrizd az internetkapcsolatot.")
            } catch (e: Exception) {
                Log.e(TAG, "Ismeretlen hiba az OpenAI kérés során: ", e)
                return@withContext ApiResult.Error("Ismeretlen hiba történt.")
            }
        }
}
lifecycleScope.launch {
    val result = openAI.ask("Mesélj egy viccet!")
    when (result) {
        is ApiResult.Success -> {
            val answer = result.data
            speakAndShow(answer)
        }

        is ApiResult.Error -> {
            val errorMessage = result.errorMessage
            Log.e("MainActivity", "OpenAI hiba: $errorMessage, Kód: ${result.statusCode}")
            speakAndShow("Hoppá, hiba történt: $errorMessage")
        }
    }
}