package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class YnetScraper @Inject constructor(client: OkHttpClient) : BaseScraper(client) {

    private val RSS_FEEDS = listOf(
        "https://www.ynet.co.il/Integration/StoryRss2.xml",
        "https://www.ynet.co.il/Integration/StoryRss1854.xml",
        "https://www.ynet.co.il/Integration/StoryRss1090.xml",
        "https://www.ynet.co.il/Integration/StoryRss3082.xml",
    )

    override suspend fun scrape(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        val seen = mutableSetOf<String>()
        for (rssUrl in RSS_FEEDS) {
            for (entry in fetchRss(rssUrl)) {
                val url = entry.link.trim()
                if (url.isBlank() || url in seen) continue
                seen.add(url)
                articles.add(NewsArticle(
                    url = url,
                    title = entry.title,
                    content = "",
                    source = "Ynet",
                    publishedDate = parseDate(entry.pubDate),
                    tags = entry.categories,
                ))
            }
        }
        articles
    }
}