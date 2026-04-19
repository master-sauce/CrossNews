//package com.newsbias.tracker.data
//
//import androidx.room.*
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface NewsDao {
//
//    @Query("SELECT * FROM news_articles ORDER BY publishedDate DESC")
//    fun getAllArticles(): Flow<List<NewsArticle>>
//
//    @Query("SELECT * FROM news_articles WHERE source = :source ORDER BY publishedDate DESC")
//    fun getBySource(source: String): Flow<List<NewsArticle>>
//
//    @Query("""
//        SELECT * FROM news_articles
//        WHERE fakeNewsProbability >= :threshold
//        ORDER BY fakeNewsProbability DESC
//    """)
//    fun getSuspicious(threshold: Float = 0.4f): Flow<List<NewsArticle>>
//
//    @Query("SELECT * FROM news_articles ORDER BY publishedDate DESC LIMIT :limit")
//    suspend fun getRecent(limit: Int = 500): List<NewsArticle>
//
//    @Query("SELECT * FROM news_articles WHERE url = :url LIMIT 1")
//    suspend fun getByUrl(url: String): NewsArticle?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertArticles(articles: List<NewsArticle>)
//
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insertIgnore(article: NewsArticle): Long
//
//    @Update
//    suspend fun update(article: NewsArticle)
//
//    @Query("DELETE FROM news_articles WHERE publishedDate < :timestamp")
//    suspend fun deleteOldArticles(timestamp: Long)
//}