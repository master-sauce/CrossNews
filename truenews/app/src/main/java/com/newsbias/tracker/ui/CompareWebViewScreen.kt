package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "חזרה")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (mode) {
                                CompareMode.FULLSCREEN -> "כתבה ${fullscreenIndex + 1}/2"
                                else -> "השוואה"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(32.dp)
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    IconButton(onClick = {
                        showAiSheet = true
                        if (aiState.leftSummary == null && !aiState.loading) {
                            aiViewModel.analyze(leftUrl, rightUrl)
                        }
                    }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            "נתח עם AI",
                            tint = MaterialTheme.colorScheme.primary,
                        )
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
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        floatingActionButton = {
            if (mode == CompareMode.FULLSCREEN) {
                ExtendedFloatingActionButton(
                    onClick = { fullscreenIndex = 1 - fullscreenIndex },
                    icon = { Icon(Icons.Default.SwapHoriz, null) },
                    text = {
                        Text(if (fullscreenIndex == 0) "עבור לכתבה 2" else "עבור לכתבה 1")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (mode) {
                CompareMode.VERTICAL -> Column(modifier = Modifier.fillMaxSize()) {
                    PaneHeader("כתבה 1", leftUrl, context, uriHandler)
                    ArticleWebView(leftUrl, Modifier.weight(1f).fillMaxWidth())
                    HorizontalDivider(
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PaneHeader("כתבה 2", rightUrl, context, uriHandler)
                    ArticleWebView(rightUrl, Modifier.weight(1f).fillMaxWidth())
                }
                CompareMode.HORIZONTAL -> Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PaneHeader("כתבה 1", leftUrl, context, uriHandler)
                        ArticleWebView(leftUrl, Modifier.weight(1f).fillMaxWidth())
                    }
                    VerticalDivider(
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                    aiViewModel.retry(leftUrl, rightUrl)
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("url", url))
                    Toast.makeText(context, "הקישור הועתק", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ContentCopy, "העתק", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { uriHandler.openUri(url) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.Public, "דפדפן", modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun AiAnalysisContent(
    state: CompareAiState,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "ניתוח AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(42.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.height(14.dp))

        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    state.stage.ifBlank { "מתחיל..." },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.error != null) {
            Text("⚠️ ${state.error}", color = FakeHigh, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("נסה שוב") }
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        state.leftSummary?.let {
            SummaryBlock("סיכום כתבה 1", it)
            Spacer(Modifier.height(10.dp))
        }
        state.rightSummary?.let {
            SummaryBlock("סיכום כתבה 2", it)
            Spacer(Modifier.height(10.dp))
        }
        state.comparison?.let {
            SummaryBlock("השוואה בין השתיים", it, highlight = true)
            Spacer(Modifier.height(12.dp))
            Text(
                "ניתוח מבוסס AI חיצוני — עשוי להכיל שגיאות",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val anyFailed = !state.loading && (
                state.leftSummary?.startsWith("(שגיאה") == true ||
                        state.rightSummary?.startsWith("(שגיאה") == true ||
                        state.comparison?.startsWith("(שגיאה") == true
                )
        if (anyFailed) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) { Text("נסה שוב") }
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
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp)
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}