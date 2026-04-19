package com.newsbias.tracker.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()
    private val _flow = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _flow.asStateFlow()

    private val fileName = "articles.bin"

    suspend fun load() = mutex.withLock {
        val list: List<NewsArticle> = withContext(Dispatchers.IO) { readAll() }
        val fresh = applyRetention(list).sortedByDescending { it.publishedDate }
        _flow.value = fresh
        if (fresh.size != list.size) writeAll(fresh)
    }

    suspend fun upsert(newOnes: List<NewsArticle>) = mutex.withLock {
        val existing = _flow.value
        val map = LinkedHashMap<String, NewsArticle>()
        existing.forEach { map[it.url] = it }
        newOnes.forEach { map[it.url] = it }
        val merged = applyRetention(map.values.toList())
            .sortedByDescending { it.publishedDate }
        _flow.value = merged
        withContext(Dispatchers.IO) { writeAll(merged) }
    }

    suspend fun clear() = mutex.withLock {
        _flow.value = emptyList()
        withContext(Dispatchers.IO) {
            // Delete internal copy
            runCatching { File(context.filesDir, fileName).delete() }

            // Delete external copy if folder selected
            val uri = settings.storageTreeUri.first()
            if (uri.isNotBlank()) {
                runCatching {
                    val tree = DocumentFile.fromTreeUri(context, Uri.parse(uri))
                    tree?.findFile(fileName)?.delete()
                }
            }
        }
    }

    private suspend fun readAll(): List<NewsArticle> {
        val uri = settings.storageTreeUri.first()
        return try {
            if (uri.isNotBlank()) {
                val doc = getDocFile(uri) ?: return emptyList()
                if (!doc.exists()) return emptyList()
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    ObjectInputStream(input).use {
                        @Suppress("UNCHECKED_CAST")
                        it.readObject() as List<NewsArticle>
                    }
                } ?: emptyList()
            } else {
                val f = File(context.filesDir, fileName)
                if (!f.exists()) return emptyList()
                ObjectInputStream(f.inputStream()).use {
                    @Suppress("UNCHECKED_CAST")
                    it.readObject() as List<NewsArticle>
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun writeAll(list: List<NewsArticle>) {
        try {
            val uriStr = settings.storageTreeUri.first()
            if (uriStr.isNotBlank()) {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return
                tree.findFile(fileName)?.delete()
                val doc = tree.createFile("application/octet-stream", fileName) ?: return
                context.contentResolver.openOutputStream(doc.uri, "w")?.use { out ->
                    ObjectOutputStream(out).use { it.writeObject(list) }
                }
            } else {
                val f = File(context.filesDir, fileName)
                ObjectOutputStream(f.outputStream()).use { it.writeObject(list) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getDocFile(uriStr: String): DocumentFile? {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) ?: return null
        return tree.findFile(fileName)
    }

    private suspend fun applyRetention(list: List<NewsArticle>): List<NewsArticle> {
        val hours = settings.retentionHours.first()
        val cutoff = System.currentTimeMillis() - hours * 3600_000L
        return list.filter { it.publishedDate >= cutoff }
    }
}