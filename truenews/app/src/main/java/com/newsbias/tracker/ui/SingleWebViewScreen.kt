package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleWebViewScreen(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("כתבה") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "חזרה") }
                },
                actions = {
                    IconButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("url", url))
                        Toast.makeText(context, "הקישור הועתק", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, "העתק") }
                    IconButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(Icons.Default.Public, "דפדפן")
                    }
                },
            )
        }
    ) { padding ->
        ArticleWebView(url, Modifier.fillMaxSize().padding(padding))
    }
}