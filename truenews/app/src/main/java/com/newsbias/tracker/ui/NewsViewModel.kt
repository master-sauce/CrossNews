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
    val isRefreshing: Boolean = false,
    val filter: CorroborationFilter = CorroborationFilter.ALL,
    val selectionMode: Boolean = false,
    val selected: List<String> = emptyList(),   // URLs
    val error: String? = null,
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(CorroborationFilter.ALL)
    private val isRefreshing = MutableStateFlow(false)
    private val selectionMode = MutableStateFlow(false)
    private val selected = MutableStateFlow<List<String>>(emptyList())
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<FeedUiState> = combine(
        repository.allArticles,
        filter,
        isRefreshing,
        selectionMode,
        combine(selected, error) { a, b -> a to b },
    ) { articles, f, refreshing, selMode, (sel, err) ->
        val filtered = when (f) {
            CorroborationFilter.ALL       -> articles
            CorroborationFilter.FOUND     -> articles.filter { it.corroborationCount > 0 }
            CorroborationFilter.NOT_FOUND -> articles.filter { it.corroborationCount == 0 }
        }
        FeedUiState(filtered, refreshing, f, selMode, sel, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refreshNews().onFailure { error.value = it.message }
            isRefreshing.value = false
        }
    }

    fun setFilter(f: CorroborationFilter) { filter.value = f }

    fun toggleSelectionMode() {
        selectionMode.value = !selectionMode.value
        if (!selectionMode.value) selected.value = emptyList()
    }

    fun toggleSelected(url: String) {
        val cur = selected.value
        selected.value = when {
            url in cur     -> cur - url
            cur.size >= 2  -> listOf(cur.last(), url)   // keep only last 2
            else           -> cur + url
        }
    }

    fun clearSelection() { selected.value = emptyList() }

    fun clearError() { error.value = null }
}