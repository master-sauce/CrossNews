package com.newsbias.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ComparisonGroup(
    val primary: NewsArticle,
    val related: List<NewsArticle>,
)

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    repository: NewsRepository,
) : ViewModel() {

    val groups: StateFlow<List<ComparisonGroup>> = repository.allArticles
        .map { articles ->
            val byUrl = articles.associateBy { it.url }
            val seen = mutableSetOf<String>()
            val result = mutableListOf<ComparisonGroup>()

            for (article in articles.filter { it.crossSourceMatches.isNotEmpty() }) {
                if (article.url in seen) continue
                val related = article.crossSourceMatches
                    .mapNotNull { byUrl[it.url] }
                    .filter { it.url !in seen }
                if (related.isEmpty()) continue

                seen.add(article.url)
                related.forEach { seen.add(it.url) }
                result.add(ComparisonGroup(article, related))
            }
            result
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}