package com.newsbias.tracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderService @Inject constructor(baseClient: OkHttpClient) {

    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchClean(
        url: String,
        maxChars: Int = 5000,
        maxRetries: Int = 2,
    ): String? = withContext(Dispatchers.IO) {
        // Site-specific API first (bypasses Cloudflare HTML challenges)
        if (url.contains("c14.co.il")) {
            val viaApi = withTimeoutOrNull(30_000L) { fetchC14Api(url, maxChars) }
            if (!viaApi.isNullOrBlank()) return@withContext viaApi
        }

        // Default: Jina Reader with retries
        repeat(maxRetries) { attempt ->
            if (attempt > 0) delay(1000L * attempt)
            val result = withTimeoutOrNull(55_000L) { fetchJina(url, maxChars) }
            if (!result.isNullOrBlank()) return@withContext result
        }
        null
    }

    private fun fetchJina(url: String, maxChars: Int): String? = try {
        val req = Request.Builder()
            .url("https://r.jina.ai/$url")
            .header("Accept", "text/plain")
            .header("X-Return-Format", "text")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null
            else resp.body?.string()?.take(maxChars)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Channel 14 WordPress REST API fallback.
     * Extracts post slug/id from URL and fetches clean JSON content.
     */
    private fun fetchC14Api(url: String, maxChars: Int): String? {
        return try {
            // C14 URL patterns:
            //   https://www.c14.co.il/article/123456
            //   https://www.c14.co.il/some-slug-here
            val path = url.substringAfter("c14.co.il/").trimEnd('/')
            val lastSegment = path.substringAfterLast('/')

            // Try by ID first (if numeric)
            val byId = lastSegment.toLongOrNull()?.let { id ->
                fetchC14Json("https://www.c14.co.il/wp-json/wp/v2/posts/$id")
            }
            if (byId != null) return extractFromWpPost(byId, maxChars)

            // Try by slug
            val bySlug = fetchC14Json(
                "https://www.c14.co.il/wp-json/wp/v2/posts?slug=$lastSegment"
            )
            if (bySlug != null) {
                // ?slug= returns an array
                val arr = try { JSONArray(bySlug) } catch (e: Exception) { null }
                if (arr != null && arr.length() > 0) {
                    return extractFromWpPost(arr.getJSONObject(0).toString(), maxChars)
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchC14Json(apiUrl: String): String? = try {
        val req = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/json")
            .header("User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "he-IL,he;q=0.9")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    } catch (e: Exception) {
        null
    }

    private fun extractFromWpPost(json: String, maxChars: Int): String? = try {
        val obj = JSONObject(json)
        val title = obj.optJSONObject("title")?.optString("rendered").orEmpty()
        val content = obj.optJSONObject("content")?.optString("rendered").orEmpty()
        val excerpt = obj.optJSONObject("excerpt")?.optString("rendered").orEmpty()

        // Strip HTML from content
        val cleanContent = Jsoup.parse(content).text()
        val cleanExcerpt = Jsoup.parse(excerpt).text()
        val cleanTitle = Jsoup.parse(title).text()

        val combined = buildString {
            if (cleanTitle.isNotBlank()) appendLine(cleanTitle).appendLine()
            if (cleanExcerpt.isNotBlank() && !cleanContent.contains(cleanExcerpt)) {
                appendLine(cleanExcerpt).appendLine()
            }
            append(cleanContent)
        }.trim()

        combined.take(maxChars).takeIf { it.length > 100 }
    } catch (e: Exception) {
        null
    }
}