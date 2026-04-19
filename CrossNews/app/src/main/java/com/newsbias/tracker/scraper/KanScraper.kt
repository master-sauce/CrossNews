package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class KanScraper @Inject constructor(client: OkHttpClient) : BaseScraper(client) {

    private val BASE = "https://www.kan.org.il"
    private val ARTICLE_REGEX = Regex("/content/kan-news/[^/]+/\\d+")

    private val ENTRY_URLS = listOf(
        "$BASE/news/",
        "$BASE/",
        "$BASE/program/11/",
    )

    override suspend fun scrape(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val seen = mutableSetOf<String>()
        var links = emptySet<String>()

        for (entryUrl in ENTRY_URLS) {
            val doc = fetchDoc(entryUrl) ?: continue
            links = doc.select("a[href]")
                .map { it.attr("abs:href").split("?")[0] }
                .filter { ARTICLE_REGEX.containsMatchIn(it) && "kan.org.il" in it }
                .toSet()
            if (links.isNotEmpty()) break
        }

        coroutineScope {
            links.take(8).map { url ->
                async {
                    if (url in seen) return@async null
                    seen.add(url)
                    val doc = fetchDoc(url) ?: return@async null
                    val title = doc.select("h1").firstOrNull()?.text()?.trim()
                        ?: return@async null
                    val body = doc.bodyText(
                        "div[class*=article__content]",
                        "div[class*=article-body]",
                        "div[class*=content-text]",
                        "article",
                    )
                    val publishedDate = doc.select("time[datetime]").firstOrNull()
                        ?.attr("datetime")?.let { parseDate(it) }
                        ?: doc.select("meta[property=article:published_time]")
                            .firstOrNull()?.attr("content")?.let { parseDate(it) }
                        ?: System.currentTimeMillis()
                    NewsArticle(
                        url = url,
                        title = title,
                        content = body,
                        source = "Kan",
                        publishedDate = publishedDate,
                        contentFetched = body.isNotBlank(),
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }
}