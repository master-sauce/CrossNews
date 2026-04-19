package com.newsbias.tracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val REFRESH_INTERVAL_MIN = intPreferencesKey("refresh_interval_min")
        val STORAGE_TREE_URI     = stringPreferencesKey("storage_tree_uri")
        val RETENTION_HOURS      = intPreferencesKey("retention_hours")
    }

    val refreshIntervalMin: Flow<Int> = context.dataStore.data.map {
        it[REFRESH_INTERVAL_MIN] ?: 30
    }
    val storageTreeUri: Flow<String> = context.dataStore.data.map {
        it[STORAGE_TREE_URI] ?: ""
    }
    val retentionHours: Flow<Int> = context.dataStore.data.map {
        it[RETENTION_HOURS] ?: 24
    }

    suspend fun setRefreshInterval(min: Int) =
        context.dataStore.edit { it[REFRESH_INTERVAL_MIN] = min.coerceIn(5, 1440) }

    suspend fun setStorageTreeUri(uri: String) =
        context.dataStore.edit { it[STORAGE_TREE_URI] = uri }

    suspend fun setRetentionHours(h: Int) =
        context.dataStore.edit { it[RETENTION_HOURS] = h.coerceIn(1, 168) }
}