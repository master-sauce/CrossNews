package com.newsbias.tracker.data

import com.newsbias.tracker.analysis.CrossSourceMatcher
import com.newsbias.tracker.analysis.NewsAnalyzer
import com.newsbias.tracker.scraper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val ynetScraper: YnetScraper,
    private val n12Scraper: N12Scraper,
    private val kanScraper: KanScraper,
    private val c14Scraper: C14Scraper,
    private val tv13Scraper: Tv13Scraper,
    private val analyzer: NewsAnalyzer,
    private val crossSourceMatcher: CrossSourceMatcher,
) {
    val allArticles: Flow<List<NewsArticle>> = newsDao.getAllArticles()

    fun bySource(source: String): Flow<List<NewsArticle>> = newsDao.getBySource(source)

    suspend fun getArticle(url: String): NewsArticle? = newsDao.getByUrl(url)

    suspend fun refreshNews(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val raw = coroutineScope {
                listOf(
                    async { runCatching { ynetScraper.scrape() }.getOrDefault(emptyList()) },
                    async { runCatching { n12Scraper.scrape() }.getOrDefault(emptyList()) },
                    async { runCatching { kanScraper.scrape() }.getOrDefault(emptyList()) },
                    async { runCatching { c14Scraper.scrape() }.getOrDefault(emptyList()) },
                    async { runCatching { tv13Scraper.scrape() }.getOrDefault(emptyList()) },
                ).awaitAll().flatten()
            }

            val analyzed = raw.map { analyzer.analyze(it) }

            val withMatches = analyzed.map { article ->
                val matches = crossSourceMatcher.findMatches(article, analyzed)
                analyzer.applyCorroboration(article, matches)
            }

            newsDao.insertArticles(withMatches)

            val cutoff = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
            newsDao.deleteOldArticles(cutoff)

            Result.success(withMatches.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}