package com.newsbias.tracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderService @Inject constructor(private val client: OkHttpClient) {

    /** Jina Reader: any URL → clean plain text. Free, no key. */
    suspend fun fetchClean(url: String, maxChars: Int = 6000): String? =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("https://r.jina.ai/$url")
                    .header("Accept", "text/plain")
                    .header("X-Return-Format", "text")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    resp.body?.string()?.take(maxChars)
                }
            } catch (e: Exception) {
                null
            }
        }
}