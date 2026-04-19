package com.newsbias.tracker.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Feed       : Screen("feed")
    object Comparison : Screen("comparison")
    object Logs       : Screen("logs")
    object Bookmarks : Screen("bookmarks")
    object Settings  : Screen("settings")

    object Detail : Screen("detail/{encodedUrl}") {
        fun createRoute(url: String): String =
            "detail/${URLEncoder.encode(url, "UTF-8")}"
    }

    object WebView : Screen("web/{encodedUrl}") {
        fun createRoute(url: String): String =
            "web/${URLEncoder.encode(url, "UTF-8")}"
    }

    object Compare : Screen("compare/{encodedLeft}/{encodedRight}") {
        fun createRoute(left: String, right: String): String =
            "compare/${URLEncoder.encode(left, "UTF-8")}/${URLEncoder.encode(right, "UTF-8")}"
    }
}