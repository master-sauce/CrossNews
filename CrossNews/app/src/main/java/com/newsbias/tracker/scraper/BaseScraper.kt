package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

data class RssItem(
    val title: String,
    val link: String,
    val pubDate: String?,
    val author: String?,
    val categories: List<String>,
)

abstract class BaseScraper(protected val client: OkHttpClient) : NewsScraper {

    protected fun fetchDoc(url: String): Document? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept-Language", "he-IL,he;q=0.9")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (body != null) Jsoup.parse(body, url) else null
        } catch (e: Exception) { null }
    }

    protected fun fetchRss(url: String): List<RssItem> {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            val resp = client.newCall(req).execute()
            val xml = resp.body?.string() ?: return emptyList()
            parseRssXml(xml)
        } catch (e: Exception) { emptyList() }
    }

    private fun parseRssXml(xml: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        try {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false
            var tag = ""
            val fields = mutableMapOf<String, String>()
            val cats = mutableListOf<String>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        tag = parser.name ?: ""
                        if (tag == "item") { inItem = true; fields.clear(); cats.clear() }
                    }
                    XmlPullParser.TEXT -> if (inItem) {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) when (tag) {
                            "title"                  -> fields["title"] = text
                            "link"                   -> fields["link"] = (fields["link"] ?: "") + text
                            "pubDate"                -> fields["pubDate"] = text
                            "author", "dc:creator"   -> fields["author"] = text
                            "category"               -> cats.add(text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            items.add(RssItem(
                                title = fields["title"] ?: "",
                                link = fields["link"] ?: "",
                                pubDate = fields["pubDate"],
                                author = fields["author"],
                                categories = cats.toList(),
                            ))
                            inItem = false
                        }
                        if (inItem) tag = ""
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
        return items
    }

    protected fun parseDate(str: String?): Long {
        if (str == null) return System.currentTimeMillis()
        listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        ).forEach { pattern ->
            try { return SimpleDateFormat(pattern, Locale.ENGLISH).parse(str)!!.time }
            catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    /**
     * Extracts article title+URL pairs from a listing page.
     * No per-article HTTP requests — fast.
     */
    protected fun Document.extractArticleLinks(
        urlPattern: Regex,
        source: String,
        maxItems: Int = 25,
    ): List<NewsArticle> {
        val results = mutableListOf<NewsArticle>()
        val seen = mutableSetOf<String>()

        fun addIfNew(url: String, title: String, container: Element? = null) {
            if (results.size >= maxItems) return
            if (url in seen || !urlPattern.containsMatchIn(url)) return
            if (title.length < 8) return
            seen.add(url)
            val date = container?.select("time[datetime]")?.firstOrNull()
                ?.attr("datetime")?.let { parseDate(it) } ?: System.currentTimeMillis()
            val author = container?.select("[class*=author],[class*=byline]")
                ?.firstOrNull()?.text()?.trim() ?: ""
            val image = container?.select("img[src]")?.firstOrNull()
                ?.attr("abs:src")?.ifBlank { null }
            results.add(NewsArticle(
                url = url, title = title, content = "",
                source = source, publishedDate = date,
                author = author, imageUrl = image,
            ))
        }

        // Pass 1: semantic article containers
        select("article, [class*=news-item], [class*=article-item], [class*=story-item]")
            .forEach { container ->
                val a = container.select("a[href]").firstOrNull() ?: return@forEach
                val url = a.attr("abs:href").split("?")[0]
                val title = container.select("h1,h2,h3,h4").firstOrNull()?.text()?.trim()
                    ?: a.text().trim()
                addIfNew(url, title, container)
            }

        // Pass 2: any matching link with meaningful text
        if (results.isEmpty()) {
            select("a[href]").forEach { a ->
                val url = a.attr("abs:href").split("?")[0]
                val title = a.text().trim().ifBlank {
                    a.select("h1,h2,h3,h4").firstOrNull()?.text()?.trim() ?: ""
                }
                addIfNew(url, title, a.parent())
            }
        }

        return results
    }

    protected fun idFromUrl(url: String): String = url.hashCode().toString()

    companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile"
    }
}

// Top-level so ContentFetcher can import it too
fun Document.bodyText(vararg selectors: String): String {
    for (sel in selectors) {
        val el = select(sel).firstOrNull() ?: continue
        el.select("script, style, aside, figure, nav, ins, .advertisement").remove()
        val text = el.wholeText().trim()
        if (text.length > 100) return text
    }
    return ""
}