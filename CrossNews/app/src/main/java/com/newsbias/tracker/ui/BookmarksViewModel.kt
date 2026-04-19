package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.BookmarkStore
import com.newsbias.tracker.data.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarksState(
    val bookmarks: List<NewsArticle> = emptyList(),
    val selectionMode: Boolean = false,
    val selected: List<String> = emptyList(),
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val store: BookmarkStore,
) : ViewModel() {

    private val _selectionMode = MutableStateFlow(false)
    private val _selected = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<BookmarksState> = combine(
        store.bookmarks, _selectionMode, _selected,
    ) { b, sel, selList -> BookmarksState(b, sel, selList) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookmarksState())

    init { viewModelScope.launch { store.load() } }

    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        _selected.value = emptyList()
    }

    fun toggleSelected(url: String) {
        val cur = _selected.value
        _selected.value = when {
            url in cur -> cur - url
            cur.size < 2 -> cur + url
            else -> cur
        }
    }

    fun remove(article: NewsArticle) = viewModelScope.launch { store.toggle(article) }
}