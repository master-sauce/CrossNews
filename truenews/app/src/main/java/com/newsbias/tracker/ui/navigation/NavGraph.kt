package com.newsbias.tracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.newsbias.tracker.ui.*
import java.net.URLDecoder

@Composable
fun NavGraph(
    navController: NavHostController,
    outerPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route,
        modifier = Modifier.padding(outerPadding),
    ) {

        composable(Screen.Feed.route) {
            NewsListScreen(
                onArticleClick = { url ->
                    navController.navigate(Screen.Detail.createRoute(url))
                },
                onCompare = { left, right ->
                    navController.navigate(Screen.Compare.createRoute(left, right))
                },
            )
        }

        composable(Screen.Comparison.route) {
            ComparisonScreen(
                onCompare = { left, right ->
                    navController.navigate(Screen.Compare.createRoute(left, right))
                }
            )
        }

        composable(Screen.Logs.route) { LogsScreen() }

        composable(Screen.Detail.route) { backStack ->
            val encodedUrl = backStack.arguments?.getString("encodedUrl") ?: return@composable
            ArticleDetailScreen(
                encodedUrl = encodedUrl,
                onBack = { navController.popBackStack() },
                onOpenWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                },
                onOpenArticle = { url ->
                    navController.navigate(Screen.Detail.createRoute(url))
                },
            )
        }

        composable(Screen.WebView.route) { backStack ->
            val encoded = backStack.arguments?.getString("encodedUrl") ?: return@composable
            val url = URLDecoder.decode(encoded, "UTF-8")
            SingleWebViewScreen(url = url, onBack = { navController.popBackStack() })
        }

        composable(Screen.Compare.route) { backStack ->
            val left = backStack.arguments?.getString("encodedLeft")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: return@composable
            val right = backStack.arguments?.getString("encodedRight")?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: return@composable
            CompareWebViewScreen(
                leftUrl = left,
                rightUrl = right,
                onBack = { navController.popBackStack() },
            )
        }
    }
}