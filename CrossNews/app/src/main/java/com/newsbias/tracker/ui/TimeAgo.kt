package com.newsbias.tracker.ui

fun timeAgoHebrew(timestampMs: Long): String {
    val diffSec = (System.currentTimeMillis() - timestampMs) / 1000
    return when {
        diffSec < 60          -> "עכשיו"
        diffSec < 3600        -> "לפני ${diffSec / 60} דק'"
        diffSec < 86_400      -> "לפני ${diffSec / 3600} שע'"
        diffSec < 604_800     -> "לפני ${diffSec / 86_400} ימים"
        else                  -> "לפני ${diffSec / 604_800} שבועות"
    }
}