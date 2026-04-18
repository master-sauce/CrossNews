package com.newsbias.tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    onArticleClick: (String) -> Unit,
    onCompare: (String, String) -> Unit,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("חפש כותרת...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        )
                    } else {
                        Text(if (state.selectionMode) "בחר 2 כתבות" else "חדשות")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchActive) { viewModel.setSearchQuery(""); searchActive = false }
                        else searchActive = true
                    }) {
                        Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, "חיפוש")
                    }
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            if (state.selectionMode) Icons.Default.Close else Icons.Default.CompareArrows,
                            "מצב בחירה",
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "רענון")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.selectionMode && state.selected.size == 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onCompare(state.selected[0], state.selected[1])
                        viewModel.toggleSelectionMode()
                    },
                    icon = { Icon(Icons.Default.CompareArrows, null) },
                    text = { Text("השווה") },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            FilterBar(state.filter) { viewModel.setFilter(it) }

            SourceFilterBar(
                allSources = state.allSources,
                selected = state.sourceFilter,
                onToggle = { viewModel.toggleSource(it) },
                onClear = { viewModel.clearSources() },
            )

            if (state.selectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface2)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("נבחרו ${state.selected.size}/2", fontSize = 12.sp, color = OnSurface2)
                }
            }

            if (state.articles.isEmpty() && state.isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.articles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין כתבות בסינון הנוכחי", color = OnSurface2)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.articles, key = { it.url }) { article ->
                        if (state.selectionMode) {
                            SelectableArticleCard(
                                article = article,
                                isSelected = article.url in state.selected,
                                onClick = { viewModel.toggleSelected(article.url) },
                            )
                        } else {
                            ArticleCard(article) { onArticleClick(article.url) }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(current: CorroborationFilter, onChange: (CorroborationFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill("הכל", current == CorroborationFilter.ALL) { onChange(CorroborationFilter.ALL) }
        FilterPill("נמצא במקורות", current == CorroborationFilter.FOUND) { onChange(CorroborationFilter.FOUND) }
        FilterPill("לא נמצא במקורות", current == CorroborationFilter.NOT_FOUND) { onChange(CorroborationFilter.NOT_FOUND) }
    }
}

@Composable
private fun SourceFilterBar(
    allSources: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (allSources.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterPill("כל המקורות", selected.isEmpty()) { onClear() }
        allSources.forEach { src ->
            SourceFilterPill(src, src in selected) { onToggle(src) }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else DarkSurface2
    val fg = if (selected) MaterialTheme.colorScheme.primary else OnSurface2
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SourceFilterPill(source: String, selected: Boolean, onClick: () -> Unit) {
    val color = sourceColor(source)
    val bg = if (selected) color.copy(alpha = 0.25f) else DarkSurface2
    val fg = if (selected) color else OnSurface2
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(source, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectableArticleCard(
    article: NewsArticle,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 0.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else OnSurface2,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SourceChip(article.source)
                Spacer(Modifier.height(6.dp))
                Text(article.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 3)
                Spacer(Modifier.height(6.dp))
                CorroborationBadge(article.corroborationCount)
            }
        }
    }
}