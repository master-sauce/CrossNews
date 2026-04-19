package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.BookmarkStore
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class DetailUiState(val article: NewsArticle? = null)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val bookmarkStore: BookmarkStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    private val _currentUrl = MutableStateFlow<String?>(null)

    val isBookmarked: StateFlow<Boolean> = combine(
        _currentUrl, bookmarkStore.bookmarks,
    ) { url, list -> url != null && list.any { it.url == url } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch { bookmarkStore.load() }
    }

    fun load(encodedUrl: String) {
        val url = URLDecoder.decode(encodedUrl, "UTF-8")
        _currentUrl.value = url
        viewModelScope.launch {
            _state.value = DetailUiState(repository.getArticle(url))
        }
    }

    fun toggleBookmark() {
        val article = _state.value.article ?: return
        viewModelScope.launch { bookmarkStore.toggle(article) }
    }
}