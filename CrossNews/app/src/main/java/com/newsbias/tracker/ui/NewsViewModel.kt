package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CorroborationFilter { ALL, FOUND, NOT_FOUND }

data class FeedUiState(
    val articles: List<NewsArticle> = emptyList(),
    val allSources: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val filter: CorroborationFilter = CorroborationFilter.ALL,
    val searchQuery: String = "",
    val sourceFilter: Set<String> = emptySet(),   // empty = all sources
    val selectionMode: Boolean = false,
    val selected: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(CorroborationFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val sourceFilter = MutableStateFlow<Set<String>>(emptySet())
    private val isRefreshing = MutableStateFlow(false)
    private val selectionMode = MutableStateFlow(false)
    private val selected = MutableStateFlow<List<String>>(emptyList())
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<FeedUiState> = combine(
        repository.allArticles,
        combine(filter, searchQuery, sourceFilter) { f, q, s -> Triple(f, q, s) },
        combine(isRefreshing, selectionMode, selected, error) { r, sm, sel, err ->
            Quadruple(r, sm, sel, err)
        },
    ) { articles, (f, q, sources), (refreshing, selMode, sel, err) ->
        val allSources = articles.map { it.source }.distinct().sorted()

        val filtered = articles.asSequence()
            .filter {
                when (f) {
                    CorroborationFilter.ALL       -> true
                    CorroborationFilter.FOUND     -> it.corroborationCount > 0
                    CorroborationFilter.NOT_FOUND -> it.corroborationCount == 0
                }
            }
            .filter { sources.isEmpty() || it.source in sources }
            .filter {
                q.isBlank() ||
                        it.title.contains(q, ignoreCase = true) ||
                        it.source.contains(q, ignoreCase = true)
            }
            .toList()

        FeedUiState(
            articles = filtered,
            allSources = allSources,
            isRefreshing = refreshing,
            filter = f,
            searchQuery = q,
            sourceFilter = sources,
            selectionMode = selMode,
            selected = sel,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                repository.refreshNews()
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun setFilter(f: CorroborationFilter) { filter.value = f }
    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun toggleSource(src: String) {
        val cur = sourceFilter.value
        sourceFilter.value = if (src in cur) cur - src else cur + src
    }
    fun clearSources() { sourceFilter.value = emptySet() }

    fun toggleSelectionMode() {
        selectionMode.value = !selectionMode.value
        if (!selectionMode.value) selected.value = emptyList()
    }

    fun toggleSelected(url: String) {
        val cur = selected.value
        selected.value = when {
            url in cur     -> cur - url
            cur.size >= 2  -> listOf(cur.last(), url)
            else           -> cur + url
        }
    }

    fun clearError() { error.value = null }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)