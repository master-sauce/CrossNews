package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class Tv13Scraper @Inject constructor(client: OkHttpClient) : BaseScraper(client) {

    private val ARTICLE_REGEX = Regex("13tv\\.co\\.il/item/[^/]+/[^/]+/[^/?#]+-\\d+")

    private val SECTION_URLS = listOf(
        "https://13tv.co.il/news/",
        "https://13tv.co.il/news/politics/",
        "https://13tv.co.il/news/domestic/",
    )

    override suspend fun scrape(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        val seen = mutableSetOf<String>()

        for (sectionUrl in SECTION_URLS) {
            val doc = fetchDoc(sectionUrl) ?: continue
            val found = doc.extractArticleLinks(ARTICLE_REGEX, "13TV", maxItems = 15)
            found.filter { it.url !in seen }
                .also { list -> list.forEach { seen.add(it.url) } }
                .let { articles.addAll(it) }
        }
        articles
    }
}