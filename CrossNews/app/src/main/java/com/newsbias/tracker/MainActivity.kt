package com.newsbias.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.newsbias.tracker.data.BookmarkStore
import com.newsbias.tracker.data.NewsRepository
import com.newsbias.tracker.data.SettingsRepository
import com.newsbias.tracker.ui.navigation.NavGraph
import com.newsbias.tracker.ui.navigation.Screen
import com.newsbias.tracker.ui.theme.NewsBiasTrackerTheme
import com.newsbias.tracker.worker.RefreshScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: NewsRepository
    @Inject lateinit var bookmarks: BookmarkStore
    @Inject lateinit var settings: SettingsRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repository.init()
            bookmarks.load()
            RefreshScheduler.schedule(this@MainActivity, settings)
        }

        setContent {
            NewsBiasTrackerTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                val hideBottomBar = currentRoute == Screen.Detail.route ||
                        currentRoute == Screen.WebView.route ||
                        currentRoute == Screen.Compare.route

                Scaffold(
                    bottomBar = {
                        if (!hideBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Feed.route,
                                    onClick = {
                                        navController.navigate(Screen.Feed.route) { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.Feed, null) },
                                    label = { Text("עדכונים") },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Comparison.route,
                                    onClick = {
                                        navController.navigate(Screen.Comparison.route) { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.CompareArrows, null) },
                                    label = { Text("השוואה") },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Bookmarks.route,
                                    onClick = {
                                        navController.navigate(Screen.Bookmarks.route) { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { Text("סימניות") },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Logs.route,
                                    onClick = {
                                        navController.navigate(Screen.Logs.route) { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.Article, null) },
                                    label = { Text("לוג") },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = {
                                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.Settings, null) },
                                    label = { Text("הגדרות") },
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(navController = navController, outerPadding = innerPadding)
                }
            }
        }
    }
}