package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class N12Scraper @Inject constructor(client: OkHttpClient) : BaseScraper(client) {

    // N12 = Channel 12 = Keshet/Mako. RSS lives on rcs.mako.co.il
    private val RSS_FEEDS = listOf(
        "https://rcs.mako.co.il/rss/news-military.xml",
        "https://rcs.mako.co.il/rss/news-law.xml",
        "https://rcs.mako.co.il/rss/news-politics.xml",
        "https://rcs.mako.co.il/rss/news-channel2.xml",
        "https://rcs.mako.co.il/rss/news-israel.xml",
        "https://rcs.mako.co.il/rss/news-world.xml",
    )

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
                    source = "N12",
                    publishedDate = parseDate(entry.pubDate),
                    author = entry.author ?: "",
                    tags = entry.categories,
                ))
            }
        }
        articles.take(40)
    }
}