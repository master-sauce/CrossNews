package com.newsbias.tracker.analysis

import com.newsbias.tracker.data.CrossMatch
import com.newsbias.tracker.data.NewsArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsAnalyzer @Inject constructor() {

    /** Initial pass — neutral. Real scoring happens in applyCorroboration. */
    fun analyze(article: NewsArticle): NewsArticle {
        return article.copy(
            fakeNewsProbability = null,
            biasScore = null,
            scoreReasons = listOf("ממתין לבדיקה צולבת מול מקורות אחרים"),
            sensationalTitle = false,
        )
    }

    fun applyCorroboration(article: NewsArticle, matches: List<CrossMatch>): NewsArticle {
        val reasons = mutableListOf<String>()

        val sources = matches.map { it.source }.distinct()
        val avgSimilarity = if (matches.isNotEmpty())
            matches.map { it.similarity }.average().toFloat() else 0f

        reasons.add("מקור הכתבה: ${article.source}")
        reasons.add("נבדקו ${sources.size} מקורות חיצוניים")

        if (matches.isEmpty()) {
            reasons.add("לא נמצא דיווח דומה באף מקור אחר")
            reasons.add("מסקנה: הידיעה מופיעה רק במקור אחד — לא ניתן לאמת")
        } else {
            matches.forEach { m ->
                reasons.add("• ${m.source}: דומה ב ${(m.similarity * 100).toInt()}% — \"${m.title.take(60)}...\"")
            }
            reasons.add("ממוצע בדמיון: ${(avgSimilarity * 100).toInt()}%")
            reasons.add("מסקנה: ${matches.size} מקורות חיצוניים מדווחים על ידיעה דומה")
        }

        return article.copy(
            fakeNewsProbability = null,    // no score — user decides
            crossSourceMatches = matches,
            corroborationCount = matches.size,
            scoreReasons = reasons,
        )
    }

    // kept for backwards compat if called elsewhere — no-op
    fun applyContentScore(article: NewsArticle): NewsArticle = article
}