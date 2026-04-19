package com.newsbias.tracker.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val _flow = MutableStateFlow<List<NewsArticle>>(emptyList())
    val bookmarks: StateFlow<List<NewsArticle>> = _flow.asStateFlow()

    private val file: File get() = File(context.filesDir, "bookmarks.bin")

    suspend fun load() = mutex.withLock {
        _flow.value = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext emptyList()
            try {
                ObjectInputStream(file.inputStream()).use {
                    @Suppress("UNCHECKED_CAST")
                    it.readObject() as List<NewsArticle>
                }
            } catch (e: Exception) { emptyList() }
        }
    }

    suspend fun isBookmarked(url: String): Boolean =
        _flow.value.any { it.url == url }

    suspend fun toggle(article: NewsArticle) = mutex.withLock {
        val current = _flow.value
        val next = if (current.any { it.url == article.url })
            current.filterNot { it.url == article.url }
        else current + article
        _flow.value = next
        write(next)
    }

    suspend fun remove(url: String) = mutex.withLock {
        val next = _flow.value.filterNot { it.url == url }
        _flow.value = next
        write(next)
    }

    private fun write(list: List<NewsArticle>) = try {
        ObjectOutputStream(file.outputStream()).use { it.writeObject(list) }
    } catch (e: Exception) { e.printStackTrace() }
}