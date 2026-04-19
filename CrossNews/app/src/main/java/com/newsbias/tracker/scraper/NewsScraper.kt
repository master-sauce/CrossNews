package com.newsbias.tracker.scraper

import com.newsbias.tracker.data.NewsArticle

interface NewsScraper {
    suspend fun scrape(): List<NewsArticle>
}