package com.newsbias.tracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsbias.tracker.data.ArticleFileStore
import com.newsbias.tracker.data.SettingsRepository
import com.newsbias.tracker.worker.RefreshScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val refreshIntervalMin: Int = 30,
    val retentionHours: Int = 24,
    val storageTreeUri: String = "",
    val folderDisplayName: String = "ברירת מחדל (פנימי)",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val store: ArticleFileStore,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        settings.refreshIntervalMin,
        settings.retentionHours,
        settings.storageTreeUri,
    ) { r, h, uri ->
        SettingsState(
            refreshIntervalMin = r,
            retentionHours = h,
            storageTreeUri = uri,
            folderDisplayName = if (uri.isBlank()) "ברירת מחדל (פנימי)"
            else prettyName(uri),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setRefreshInterval(min: Int) = viewModelScope.launch {
        settings.setRefreshInterval(min)
        RefreshScheduler.schedule(ctx, settings)
    }

    fun setRetentionHours(h: Int) = viewModelScope.launch {
        settings.setRetentionHours(h)
        store.load()
    }

    /** Persist URI + take persistable permission, then migrate store. */
    fun onFolderPicked(uri: Uri) = viewModelScope.launch {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) { e.printStackTrace() }
        settings.setStorageTreeUri(uri.toString())
        store.load()
    }

    fun resetFolder() = viewModelScope.launch {
        val cur = settings.storageTreeUri.first()
        if (cur.isNotBlank()) {
            try {
                ctx.contentResolver.releasePersistableUriPermission(
                    Uri.parse(cur),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: Exception) {}
        }
        settings.setStorageTreeUri("")
        store.load()
    }

    fun clearStorage() = viewModelScope.launch {
        store.clear()
        store.load()  // force re-read (will be empty)
    }
    private fun prettyName(uri: String): String = try {
        Uri.parse(uri).lastPathSegment
            ?.substringAfterLast(':')
            ?.ifBlank { "תיקייה חיצונית" }
            ?: "תיקייה חיצונית"
    } catch (_: Exception) { "תיקייה חיצונית" }
}