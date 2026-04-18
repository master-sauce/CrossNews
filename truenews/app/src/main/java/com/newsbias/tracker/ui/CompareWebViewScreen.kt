package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newsbias.tracker.ui.theme.*

private enum class CompareMode { VERTICAL, HORIZONTAL, FULLSCREEN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareWebViewScreen(
    leftUrl: String,
    rightUrl: String,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf(CompareMode.VERTICAL) }
    var fullscreenIndex by remember { mutableStateOf(0) }  // 0 = left, 1 = right
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (mode) {
                            CompareMode.FULLSCREEN -> "כתבה ${fullscreenIndex + 1}/2"
                            else -> "השוואה"
                        },
                        fontSize = 15.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "חזרה") }
                },
                actions = {
                    // Cycle layout mode
                    IconButton(onClick = {
                        mode = when (mode) {
                            CompareMode.VERTICAL   -> CompareMode.HORIZONTAL
                            CompareMode.HORIZONTAL -> CompareMode.FULLSCREEN
                            CompareMode.FULLSCREEN -> CompareMode.VERTICAL
                        }
                    }) {
                        Icon(
                            imageVector = when (mode) {
                                CompareMode.VERTICAL   -> Icons.Default.ViewAgenda
                                CompareMode.HORIZONTAL -> Icons.Default.ViewColumn
                                CompareMode.FULLSCREEN -> Icons.Default.Fullscreen
                            },
                            contentDescription = "שנה פריסה",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (mode == CompareMode.FULLSCREEN) {
                ExtendedFloatingActionButton(
                    onClick = { fullscreenIndex = 1 - fullscreenIndex },
                    icon = { Icon(Icons.Default.SwapHoriz, null) },
                    text = {
                        Text(if (fullscreenIndex == 0) "עבור לכתבה 2" else "עבור לכתבה 1")
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (mode) {
                CompareMode.VERTICAL -> Column(modifier = Modifier.fillMaxSize()) {
                    PaneHeader("כתבה 1", leftUrl, context, uriHandler)
                    ArticleWebView(leftUrl, Modifier.weight(1f).fillMaxWidth())
                    HorizontalDivider(thickness = 2.dp)
                    PaneHeader("כתבה 2", rightUrl, context, uriHandler)
                    ArticleWebView(rightUrl, Modifier.weight(1f).fillMaxWidth())
                }
                CompareMode.HORIZONTAL -> Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PaneHeader("כתבה 1", leftUrl, context, uriHandler)
                        ArticleWebView(leftUrl, Modifier.weight(1f).fillMaxWidth())
                    }
                    VerticalDivider(thickness = 2.dp)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PaneHeader("כתבה 2", rightUrl, context, uriHandler)
                        ArticleWebView(rightUrl, Modifier.weight(1f).fillMaxWidth())
                    }
                }
                CompareMode.FULLSCREEN -> {
                    val url = if (fullscreenIndex == 0) leftUrl else rightUrl
                    Column(modifier = Modifier.fillMaxSize()) {
                        PaneHeader("כתבה ${fullscreenIndex + 1}", url, context, uriHandler)
                        ArticleWebView(url, Modifier.weight(1f).fillMaxWidth())
                        Spacer(Modifier.height(72.dp))  // FAB space
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneHeader(
    label: String,
    url: String,
    context: Context,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface2)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface2,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("url", url))
            Toast.makeText(context, "הקישור הועתק", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, "העתק", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.FullscreenExit, "דפדפן", modifier = Modifier.size(16.dp))
        }
    }
}