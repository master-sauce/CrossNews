package com.newsbias.tracker.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.newsbias.tracker.analysis.CrossSourceMatcher
import com.newsbias.tracker.analysis.NewsAnalyzer
import com.newsbias.tracker.data.AppDatabase
import com.newsbias.tracker.data.NewsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "news_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNewsDao(db: AppDatabase): NewsDao = db.newsDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides @Singleton fun provideNewsAnalyzer() = NewsAnalyzer()
    @Provides @Singleton fun provideCrossSourceMatcher() = CrossSourceMatcher()
}