package com.newsbias.tracker.analysis

import com.newsbias.tracker.data.CrossMatch
import com.newsbias.tracker.data.NewsArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrossSourceMatcher @Inject constructor() {

    private val HEBREW_STOPWORDS = setOf(
        "של", "על", "עם", "את", "זה", "זו", "הוא", "היא", "הם", "הן",
        "לא", "כן", "גם", "רק", "אבל", "או", "כי", "אם", "כך", "כמו",
        "מה", "מי", "איך", "איפה", "מתי", "למה", "יש", "אין", "היה",
        "היתה", "יהיה", "להיות", "אני", "אתה", "אנחנו", "אתם",
        "ב", "ל", "מ", "ה", "ו", "ש", "כ",
    )

    fun findMatches(target: NewsArticle, all: List<NewsArticle>): List<CrossMatch> {
        val targetTokens = tokenize(target.title)
        if (targetTokens.size < 2) return emptyList()

        return all.asSequence()
            .filter { it.url != target.url && it.source != target.source }
            .mapNotNull { other ->
                val otherTokens = tokenize(other.title)
                if (otherTokens.size < 2) return@mapNotNull null
                val sim = jaccard(targetTokens, otherTokens)
                if (sim >= 0.20f) CrossMatch(other.source, other.title, other.url, sim) else null
            }
            .sortedByDescending { it.similarity }
            .take(6)
            .toList()
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .replace(Regex("[^\\u0590-\\u05FFa-zA-Z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in HEBREW_STOPWORDS }
            .toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.intersect(b).size.toFloat()
        val union = a.union(b).size.toFloat()
        return intersection / union
    }
}