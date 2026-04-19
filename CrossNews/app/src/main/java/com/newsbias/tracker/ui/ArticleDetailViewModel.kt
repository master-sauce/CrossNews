package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class DetailUiState(val article: NewsArticle? = null)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: NewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    fun load(encodedUrl: String) {
        val url = URLDecoder.decode(encodedUrl, "UTF-8")
        viewModelScope.launch {
            _state.value = DetailUiState(repository.getArticle(url))
        }
    }
}