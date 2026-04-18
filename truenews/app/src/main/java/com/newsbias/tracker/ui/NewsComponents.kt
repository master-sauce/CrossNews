package com.newsbias.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun sourceColor(source: String) = when (source) {
    "Ynet"       -> YnetColor
    "N12"        -> N12Color
    "Kan"        -> KanColor
    "13TV"       -> Ch13Color
    "Channel 14" -> C14Color
    else         -> Color(0xFF607D8B)
}

@Composable
fun SourceChip(source: String) {
    val color = sourceColor(source)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(source, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CorroborationBadge(count: Int) {
    val (label, color) = when {
        count == 0  -> "לא נמצא במקורות אחרים" to FakeHigh
        count == 1  -> "דווח גם ב-1 מקור נוסף" to FakeMed
        else        -> "דווח ב-$count מקורות נוספים" to FakeLow
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color)
    }
}

@Composable
fun ArticleCard(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceChip(article.source)
                Text(
                    text = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                        .format(Date(article.publishedDate)),
                    fontSize = 11.sp,
                    color = OnSurface2,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            CorroborationBadge(article.corroborationCount)
        }
    }
}