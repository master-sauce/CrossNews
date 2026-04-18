package com.newsbias.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "news_articles")
@TypeConverters(Converters::class)
data class NewsArticle(
    @PrimaryKey val url: String,
    val title: String,
    val content: String,
    val source: String,
    val publishedDate: Long,
    val imageUrl: String? = null,
    val biasScore: Float? = null,
    val fakeNewsProbability: Float? = null,
    val author: String = "",
    val tags: List<String> = emptyList(),
    val crossSourceMatches: List<CrossMatch> = emptyList(),
    val sensationalTitle: Boolean = false,
    val corroborationCount: Int = 0,
    val contentFetched: Boolean = false,
    val scoreReasons: List<String> = emptyList(),
)