package com.newsbias.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    onCompare: (String, String) -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ניתוח השוואה") },
                actions = {
                    TextButton(
                        onClick = { viewModel.rematch() },
                        enabled = !refreshing,
                    ) {
                        Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("חשב מחדש", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { viewModel.refreshAndRematch() },
                        enabled = !refreshing,
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("רענן מלא", fontSize = 12.sp)
                    }
                },
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (refreshing && groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (groups.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "אין כרגע קבוצות להשוואה",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "לחץ 'חשב מחדש' לניתוח מהיר של הכתבות הקיימות, או 'רענן מלא' כדי למשוך גם כתבות חדשות.",
                        fontSize = 12.sp,
                        color = OnSurface2,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshAndRematch() }) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("רענן ונתח עכשיו")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(groups) { group ->
                        GroupCard(group, onCompare)
                    }
                }
            }

            if (refreshing && groups.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun GroupCard(group: ComparisonGroup, onCompare: (String, String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "נושא דומה בכמה מקורות:",
                fontSize = 11.sp,
                color = OnSurface2,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))

            ArticleRow(group.primary, isPrimary = true)

            group.related.forEach { related ->
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface2)
                        .clickable { onCompare(group.primary.url, related.url) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SourceChip(related.source)
                        Spacer(Modifier.height(4.dp))
                        Text(related.title, fontSize = 13.sp, maxLines = 2)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("השווה ↔", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(article: NewsArticle, isPrimary: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) DarkSurface2 else DarkSurface)
            .padding(10.dp),
    ) {
        SourceChip(article.source)
        Spacer(Modifier.height(4.dp))
        Text(
            article.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
        )
    }
}