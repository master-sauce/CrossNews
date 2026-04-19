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

    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Models to try in order (better Hebrew → fallback)
    private val models = listOf("openai-large", "openai", "mistral")

    private val hebrewStyleRules = """
        חוקים קריטיים לניסוח התשובה:
        - כתוב בעברית תקנית בלבד, ללא שגיאות כתיב או דקדוק.
        - הקפד על התאמת מין ומספר (זכר/נקבה, יחיד/רבים).
        - השתמש בניקוד רק כשחובה להבנה.
        - אל תערבב אנגלית בתוך המשפט העברי (שמות לועזיים מותר).
        - אל תתרגם מילולית מאנגלית - נסח בעברית טבעית וזורמת.
        - הימנע מצורות ארכאיות או ספרותיות מדי - שפה עיתונאית שוטפת.
        - סיים משפטים בנקודה, השתמש בפסיקים נכון.
        - שמור על טון ענייני, קצר וברור.
    """.trimIndent()

    suspend fun prompt(
        systemPrompt: String,
        userPrompt: String,
        maxRetries: Int = 3,
    ): String? = withContext(Dispatchers.IO) {
        val enrichedSystem = "$systemPrompt\n\n$hebrewStyleRules"

        repeat(maxRetries) { attempt ->
            if (attempt > 0) delay(1500L * attempt)

            val model = models[attempt.coerceAtMost(models.size - 1)]
            val result = withTimeoutOrNull(90_000L) {
                callOnce(enrichedSystem, userPrompt, model)
            }
            if (!result.isNullOrBlank()) return@withContext result
        }
        null
    }

    private fun callOnce(systemPrompt: String, userPrompt: String, model: String): String? {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system"); put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user"); put("content", userPrompt)
                    })
                })
                put("temperature", 0.3)  // lower = fewer hallucinations / typos
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