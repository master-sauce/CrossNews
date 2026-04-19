package com.newsbias.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.Serif   // newspaper headline feel
private val Body    = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Black,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Black,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp,   // ALL CAPS section labels
    ),
    labelMedium = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Body, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 12.sp,
    ),
)