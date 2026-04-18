package com.newsbias.tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filtered = remember(articles, searchQuery) {
        if (searchQuery.isBlank()) articles
        else articles.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.source.contains(searchQuery, ignoreCase = true) ||
                    it.scoreReasons.any { r -> r.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("חפש בלוג...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        )
                    } else {
                        Text("לוג ניתוח")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchActive) { searchQuery = ""; searchActive = false }
                        else searchActive = true
                    }) {
                        Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, null)
                    }
                },
            )
        }
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isBlank()) "אין נתונים" else "לא נמצאו תוצאות", color = OnSurface2)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.url }) { article ->
                LogCard(article)
            }
        }
    }
}

@Composable
private fun LogCard(article: NewsArticle) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceChip(article.source)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorroborationBadge(article.corroborationCount)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = OnSurface2,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                article.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = if (expanded) 10 else 2,
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Text("פירוט הניתוח:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface2)
                    Spacer(Modifier.height(6.dp))

                    if (article.scoreReasons.isEmpty()) {
                        Text("אין נתוני ניתוח", fontSize = 12.sp, color = OnSurface2)
                    } else {
                        article.scoreReasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurface2)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(reason, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}