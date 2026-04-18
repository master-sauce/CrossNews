package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newsbias.tracker.ui.theme.*

private enum class CompareMode { VERTICAL, HORIZONTAL, FULLSCREEN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareWebViewScreen(
    leftUrl: String,
    rightUrl: String,
    onBack: () -> Unit,
    aiViewModel: CompareAiViewModel = hiltViewModel(),
) {
    var mode by remember { mutableStateOf(CompareMode.VERTICAL) }
    var fullscreenIndex by remember { mutableStateOf(0) }
    var showAiSheet by remember { mutableStateOf(false) }
    val aiState by aiViewModel.state.collectAsStateWithLifecycle()

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
                    IconButton(onClick = {
                        showAiSheet = true
                        if (aiState.leftSummary == null && !aiState.loading) {
                            aiViewModel.analyze(leftUrl, rightUrl)
                        }
                    }) {
                        Icon(Icons.Default.AutoAwesome, "נתח עם AI")
                    }
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
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }

        if (showAiSheet) {
            ModalBottomSheet(onDismissRequest = { showAiSheet = false }) {
                AiAnalysisContent(aiState) {
                    aiViewModel.analyze(leftUrl, rightUrl)
                }
            }
        }
    }
}

@Composable
private fun AiAnalysisContent(state: CompareAiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("ניתוח AI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("מנתח את שתי הכתבות... (30-60 שניות)", fontSize = 13.sp)
                }
            }
            state.error != null -> {
                Text("⚠️ ${state.error}", color = FakeHigh, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("נסה שוב") }
            }
            state.comparison != null -> {
                SummaryBlock("סיכום כתבה 1", state.leftSummary ?: "")
                Spacer(Modifier.height(10.dp))
                SummaryBlock("סיכום כתבה 2", state.rightSummary ?: "")
                Spacer(Modifier.height(10.dp))
                SummaryBlock("השוואה בין השתיים", state.comparison, highlight = true)
                Spacer(Modifier.height(12.dp))
                Text(
                    "ניתוח מבוסס AI חיצוני — עשוי להכיל שגיאות",
                    fontSize = 10.sp,
                    color = OnSurface2,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryBlock(title: String, content: String, highlight: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else DarkSurface2
            )
            .padding(12.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface2)
        Spacer(Modifier.height(6.dp))
        Text(content, fontSize = 13.sp, lineHeight = 20.sp)
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
            Icon(Icons.Default.Public, "דפדפן", modifier = Modifier.size(16.dp))
        }
    }
}