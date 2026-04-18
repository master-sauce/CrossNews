package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    repository: NewsRepository,
) : ViewModel() {

    val articles: StateFlow<List<NewsArticle>> = repository.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}