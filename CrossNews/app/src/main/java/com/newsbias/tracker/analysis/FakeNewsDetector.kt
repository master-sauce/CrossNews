package com.newsbias.tracker.analysis

import com.newsbias.tracker.data.CrossMatch
import com.newsbias.tracker.data.NewsArticle
import javax.inject.Inject
import javax.inject.Singleton

enum class Verdict { LIKELY_REAL, SUSPICIOUS, LIKELY_FAKE }

data class FakeAnalysis(
    val score: Float,
    val verdict: Verdict,
    val corroborated: Boolean,
    val corroborationCount: Int,
    val sensationalTitle: Boolean,
)

@Singleton
class FakeNewsDetector @Inject constructor() {

    fun analyze(article: NewsArticle, matches: List<CrossMatch>): FakeAnalysis {
        var score = article.fakeNewsProbability ?: 0.1f

        val corroborated = matches.isNotEmpty()
        if (!corroborated) score += 0.40f
        else score -= (matches.size * 0.08f).coerceAtMost(0.30f)

        // Neutral public broadcaster corroboration = strong real signal
        if (matches.any { it.source == "Kan" }) score -= 0.20f

        // Short body = clickbait
        val bodyWords = article.content.split("\\s+".toRegex()).size
        if (bodyWords < 50) score += 0.15f

        val final = score.coerceIn(0f, 1f)
        return FakeAnalysis(
            score = final,
            verdict = when {
                final > 0.60f -> Verdict.LIKELY_FAKE
                final > 0.35f -> Verdict.SUSPICIOUS
                else          -> Verdict.LIKELY_REAL
            },
            corroborated = corroborated,
            corroborationCount = matches.size,
            sensationalTitle = article.sensationalTitle,
        )
    }
}