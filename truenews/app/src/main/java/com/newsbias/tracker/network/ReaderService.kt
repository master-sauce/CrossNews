package com.newsbias.tracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderService @Inject constructor(baseClient: OkHttpClient) {

    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchClean(
        url: String,
        maxChars: Int = 5000,
        maxRetries: Int = 2,
    ): String? = withContext(Dispatchers.IO) {
        repeat(maxRetries) { attempt ->
            if (attempt > 0) delay(1000L * attempt)
            val result = withTimeoutOrNull(55_000L) { fetchOnce(url, maxChars) }
            if (!result.isNullOrBlank()) return@withContext result
        }
        null
    }

    private fun fetchOnce(url: String, maxChars: Int): String? = try {
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
}