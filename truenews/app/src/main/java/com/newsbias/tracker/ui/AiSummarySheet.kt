package com.newsbias.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newsbias.tracker.ui.theme.*

@Composable
fun AiSummaryContent(state: ArticleAiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("סיכום AI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("מנתח את הכתבה... (15-30 שניות)", fontSize = 13.sp)
                }
            }
            state.error != null -> {
                Text("⚠️ ${state.error}", color = FakeHigh, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("נסה שוב") }
            }
            state.summary != null -> {
                SummaryBox("סיכום", state.summary, highlight = true)
                Spacer(Modifier.height(10.dp))
                SummaryBox("עובדות מרכזיות", state.keyPoints ?: "")
                Spacer(Modifier.height(12.dp))
                Text(
                    "ניתוח מבוסס AI חיצוני — עשוי להכיל שגיאות",
                    fontSize = 10.sp,
                    color = OnSurface2,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryBox(title: String, content: String, highlight: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else DarkSurface2
            )
            .padding(12.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface2)
        Spacer(Modifier.height(6.dp))
        Text(content, fontSize = 13.sp, lineHeight = 20.sp)
    }
}