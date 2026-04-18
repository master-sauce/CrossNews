package com.newsbias.tracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        topBar = {
            TopAppBar(
                title = { Text(article?.source ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "חזרה") }
                },
                actions = {
                    article?.let { a ->
                        IconButton(onClick = {
                            showAiSheet = true
                            aiViewModel.analyze(a.url)
                        }) {
                            Icon(Icons.Default.AutoAwesome, "סכם עם AI")
                        }
                    }
                },
            )
        }
    ) { padding ->
        article?.let { a ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        a.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item { CorroborationBadge(a.corroborationCount) }

                item {
                    Button(
                        onClick = { onOpenWebView(a.url) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
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
                        Text("דיווחים דומים ממקורות אחרים", fontWeight = FontWeight.SemiBold)
                        Text(
                            "לחץ על ידיעה כדי להשוות ולבדוק בעצמך",
                            fontSize = 11.sp,
                            color = OnSurface2,
                        )
                    }
                    items(a.crossSourceMatches) { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface2)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    match.source,
                                    fontSize = 12.sp,
                                    color = sourceColor(match.source),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(match.title, fontSize = 13.sp, maxLines = 3)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${(match.similarity * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = OnSurface2,
                                )
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    onClick = { onOpenWebView(match.url) },
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
            CircularProgressIndicator()
        }
    }
}