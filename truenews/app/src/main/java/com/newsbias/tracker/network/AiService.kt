package com.newsbias.tracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(baseClient: OkHttpClient) {

    // Dedicated client with longer timeouts for AI calls
    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun prompt(
        systemPrompt: String,
        userPrompt: String,
        maxRetries: Int = 3,
    ): String? = withContext(Dispatchers.IO) {
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            if (attempt > 0) delay(1500L * attempt)  // backoff

            val result = withTimeoutOrNull(90_000L) { callOnce(systemPrompt, userPrompt) }
            when {
                result != null && result.isNotBlank() -> return@withContext result
                else -> lastError = "ניסיון ${attempt + 1} נכשל"
            }
        }
        null
    }

    private fun callOnce(systemPrompt: String, userPrompt: String): String? {
        return try {
            val body = JSONObject().apply {
                put("model", "openai")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system"); put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user"); put("content", userPrompt)
                    })
                })
                put("private", true)
            }.toString()

            val req = Request.Builder()
                .url("https://text.pollinations.ai/openai")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string() ?: return null
                parseResponse(raw)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseResponse(raw: String): String? {
        return try {
            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            raw.trim().takeIf { it.isNotBlank() && !it.startsWith("{") }
        }
    }

    suspend fun isSameTopic(titleA: String, sourceA: String, titleB: String, sourceB: String): Boolean? {
        val answer = prompt(
            "אתה עוזר לבדוק האם שתי כותרות חדשות ישראליות מדווחות על אותו אירוע. ענה במילה אחת בלבד: כן או לא.",
            "כותרת 1 ($sourceA): $titleA\nכותרת 2 ($sourceB): $titleB\n\nהאם שתי הכותרות מדווחות על אותו אירוע? ענה רק: כן / לא",
        ) ?: return null
        return answer.contains("כן") && !answer.contains("לא")
    }
}