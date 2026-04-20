package com.newsbias.tracker.data

import com.newsbias.tracker.analysis.NewsAnalyzer
import com.newsbias.tracker.scraper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class NewsRepository @Inject constructor(
    private val store: ArticleFileStore,
    private val ynetScraper: YnetScraper,
    private val n12Scraper: N12Scraper,
    private val kanScraper: KanScraper,
    private val c14Scraper: C14Scraper,
    private val tv13Scraper: Tv13Scraper,
    private val analyzer: NewsAnalyzer,
) {
    val allArticles: StateFlow<List<NewsArticle>> = store.articles

    suspend fun init() {
        store.load()
        rematchAll()
    }

    fun getArticle(url: String): NewsArticle? =
        store.articles.value.firstOrNull { it.url == url }

    suspend fun refreshNews() = withContext(Dispatchers.IO) {
        coroutineScope {
            val scrapers = listOf(ynetScraper, n12Scraper, kanScraper, c14Scraper, tv13Scraper)
            val scraped = scrapers.map { async { it.scrape() } }.awaitAll().flatten()

            val dedupedScraped = scraped.distinctBy {
                it.url.substringBefore("?").trimEnd('/')
            }

            val existing = store.articles.value
            val pool = LinkedHashMap<String, NewsArticle>()
            existing.forEach { pool[it.url] = it }

            val now = System.currentTimeMillis()
            dedupedScraped.forEach { fresh ->
                val prior = pool[fresh.url]
                pool[fresh.url] = if (prior != null) {
                    // Existing article: preserve first-seen timestamp + analysis
                    fresh.copy(
                        publishedDate = prior.publishedDate,
                        crossSourceMatches = prior.crossSourceMatches,
                        corroborationCount = prior.corroborationCount,
                        scoreReasons = prior.scoreReasons,
                    )
                } else {
                    // New article: stamp with current insertion time → sorts to top
                    fresh.copy(publishedDate = now)
                }
            }
            val enriched = enrich(pool.values.toList())
            if (enriched.isNotEmpty()) store.upsert(enriched)
        }
    }

    suspend fun rematchAll() = withContext(Dispatchers.IO) {
        val current = store.articles.value
        if (current.isEmpty()) return@withContext
        val enriched = enrich(current)
        store.upsert(enriched)
    }

    private fun enrich(pool: List<NewsArticle>): List<NewsArticle> =
        pool.map { article ->
            val matches = findMatches(article, pool)
            analyzer.applyCorroboration(article, matches)
        }

    private fun findMatches(
        target: NewsArticle,
        all: List<NewsArticle>,
        threshold: Float = 0.25f,
    ): List<CrossMatch> {
        val targetTokens = tokenize(target.title)
        if (targetTokens.isEmpty()) return emptyList()

        val scored = all.asSequence()
            .filter { it.url != target.url && it.source != target.source }
            .map { other -> other to jaccard(targetTokens, tokenize(other.title)) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .toList()

        val bestPerSource = LinkedHashMap<String, Pair<NewsArticle, Float>>()
        for ((other, sim) in scored) {
            val prev = bestPerSource[other.source]
            if (prev == null || sim > prev.second) {
                bestPerSource[other.source] = other to sim
            }
        }

        return bestPerSource.values
            .sortedByDescending { it.second }
            .take(5)
            .map { (other, sim) ->
                CrossMatch(
                    url = other.url,
                    title = other.title,
                    source = other.source,
                    similarity = sim,
                )
            }
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .replace(Regex("[\\p{Punct}״׳\"'”“‟‹›«»\\-–—]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
            .filterNot { it in HEBREW_STOPWORDS }
            .toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val inter = a.intersect(b).size
        val union = max(1, a.union(b).size)
        return inter.toFloat() / union.toFloat()
    }

    companion object {
        private val HEBREW_STOPWORDS = setOf(
            "של", "על", "את", "כל", "עם", "גם", "זה", "זו", "הוא", "היא",
            "הם", "הן", "אני", "אתה", "אתם", "יש", "אין", "לא", "כן",
            "אבל", "או", "כי", "אם", "רק", "עוד", "מה", "מי", "איך",
            "למה", "איפה", "כמה", "כאן", "שם", "אז", "עכשיו", "היום",
            "אחרי", "לפני", "בין", "נגד", "בעד", "תוך", "מול", "אצל",
            "the", "and", "for", "with", "from", "this", "that",
        )
    }
}