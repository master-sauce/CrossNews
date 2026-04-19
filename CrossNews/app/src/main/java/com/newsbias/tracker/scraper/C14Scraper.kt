package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class C14Scraper @Inject constructor(client: OkHttpClient) : BaseScraper(client) {

    private val RSS_FEEDS = listOf(
        "https://www.c14.co.il/feed/",
        "https://www.c14.co.il/category/news/feed/",
    )
    private val ARTICLE_REGEX = Regex("/article/\\d+")

    override suspend fun scrape(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        val seen = mutableSetOf<String>()

        for (rssUrl in RSS_FEEDS) {
            for (entry in fetchRss(rssUrl)) {
                val url = entry.link.trim()
                if (url.isBlank() || url in seen || entry.title.isBlank()) continue
                seen.add(url)
                articles.add(NewsArticle(
                    url = url,
                    title = entry.title,
                    content = "",
                    source = "Channel 14",
                    publishedDate = parseDate(entry.pubDate),
                    author = entry.author ?: "",
                    tags = entry.categories,
                ))
            }
            if (articles.isNotEmpty()) break
        }

        // Fallback: homepage link scraping
        if (articles.isEmpty()) {
            val home = fetchDoc("https://www.c14.co.il") ?: return@withContext articles
            val found = home.extractArticleLinks(ARTICLE_REGEX, "Channel 14", maxItems = 20)
            found.filter { it.url !in seen }.let { articles.addAll(it) }
        }
        articles
    }
}