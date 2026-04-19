package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    encodedUrl: String,
    onBack: () -> Unit,
    onOpenWebView: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
    aiViewModel: ArticleAiViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val aiState by aiViewModel.state.collectAsStateWithLifecycle()
    val article = state.article
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var showAiSheet by remember { mutableStateOf(false) }

    LaunchedEffect(encodedUrl) { viewModel.load(encodedUrl) }

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
                            article?.source ?: "כתבה",
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
                    article?.let { a ->
                        IconButton(onClick = {
                            showAiSheet = true
                            aiViewModel.analyze(a.url)
                        }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                "סכם עם AI",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    ) { padding ->
        article?.let { a ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(sourceColor(a.source))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            a.source.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = sourceColor(a.source),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            timeAgoHebrew(a.publishedDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Text(
                        a.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                item { CorroborationBadge(a.corroborationCount) }

                item {
                    Button(
                        onClick = { onOpenWebView(a.url) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Default.OpenInBrowser, null)
                        Spacer(Modifier.width(8.dp))
                        Text("פתח את הכתבה", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            showAiSheet = true
                            aiViewModel.analyze(a.url)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("סכם עם AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("url", a.url))
                                Toast.makeText(context, "הקישור הועתק", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("העתק קישור", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { uriHandler.openUri(a.url) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.Public, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("פתח בדפדפן", fontSize = 13.sp)
                        }
                    }
                }

                if (a.crossSourceMatches.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "דיווחים דומים ממקורות אחרים",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "לחץ על ידיעה כדי לפתוח את הכרטיס שלה",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(a.crossSourceMatches) { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenArticle(match.url) }
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(sourceColor(match.source))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        match.source.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = sourceColor(match.source),
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    match.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${(match.similarity * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    onClick = { onOpenArticle(match.url) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) { Text("פתח", fontSize = 11.sp) }
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(FakeHigh.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "⚠️ הידיעה לא נמצאה במקורות אחרים — פתח ובדוק בעצמך",
                                color = FakeHigh,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            if (showAiSheet) {
                ModalBottomSheet(onDismissRequest = { showAiSheet = false }) {
                    AiSummaryContent(aiState) { aiViewModel.retry(a.url) }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}