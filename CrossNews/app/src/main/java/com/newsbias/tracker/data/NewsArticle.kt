package com.newsbias.tracker.data

import java.io.Serializable

data class NewsArticle(
    val url: String,
    val title: String,
    val content: String = "",
    val source: String,
    val publishedDate: Long,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val contentFetched: Boolean = false,
    val corroborationCount: Int = 0,
    val crossSourceMatches: List<CrossMatch> = emptyList(),
    val scoreReasons: List<String> = emptyList(),
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

data class CrossMatch(
    val url: String,
    val title: String,
    val source: String,
    val similarity: Float,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}