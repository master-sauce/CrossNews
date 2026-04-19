package com.newsbias.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onArticleClick: (String) -> Unit,
    onCompare: (String, String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.selectionMode) "בחר 2 סימניות" else "סימניות",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(42.dp)
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            if (state.selectionMode) Icons.Default.Close else Icons.Default.CompareArrows,
                            "מצב בחירה",
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    ) { padding ->
        if (state.bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "אין כתבות שמורות",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(state.bookmarks, key = { it.url }) { a ->
                if (state.selectionMode) {
                    SelectableArticleCard(
                        article = a,
                        isSelected = a.url in state.selected,
                        onClick = { viewModel.toggleSelected(a.url) },
                    )
                } else {
                    ArticleCard(a) { onArticleClick(a.url) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}