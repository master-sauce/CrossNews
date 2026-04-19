package com.newsbias.tracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.newsbias.tracker.data.NewsRepository
import com.newsbias.tracker.data.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: NewsRepository,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        repo.init()
        repo.refreshNews()
        Result.success()
    } catch (e: Exception) { Result.retry() }
}

object RefreshScheduler {
    private const val NAME = "news_refresh"

    suspend fun schedule(ctx: Context, settings: SettingsRepository) {
        val minutes = settings.refreshIntervalMin.first().toLong().coerceAtLeast(15L)
        val req = PeriodicWorkRequestBuilder<RefreshWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            NAME, ExistingPeriodicWorkPolicy.UPDATE, req,
        )
    }
}