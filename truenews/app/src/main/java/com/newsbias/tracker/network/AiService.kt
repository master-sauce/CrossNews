package com.newsbias.tracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(private val client: OkHttpClient) {

    /** Single-turn prompt via Pollinations (free, no key). */
    suspend fun prompt(systemPrompt: String, userPrompt: String): String? =
        withContext(Dispatchers.IO) {
            try {
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
                    if (!resp.isSuccessful) return@withContext null
                    val raw = resp.body?.string() ?: return@withContext null
                    // Response is OpenAI-compatible JSON
                    try {
                        JSONObject(raw)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                    } catch (e: Exception) {
                        raw.trim()  // fallback: plain text
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    /** Returns true if AI says same topic. */
    suspend fun isSameTopic(titleA: String, sourceA: String, titleB: String, sourceB: String): Boolean? {
        val answer = prompt(
            systemPrompt = "אתה עוזר לבדוק האם שתי כותרות חדשות ישראליות מדווחות על אותו אירוע. ענה במילה אחת בלבד: כן או לא.",
            userPrompt = "כותרת 1 ($sourceA): $titleA\nכותרת 2 ($sourceB): $titleB\n\nהאם שתי הכותרות מדווחות על אותו אירוע? ענה רק: כן / לא",
        ) ?: return null
        return answer.contains("כן") && !answer.contains("לא")
    }
}