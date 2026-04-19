package com.newsbias.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.newsbias.tracker.data.NewsArticle
import com.newsbias.tracker.ui.theme.*

@Composable
fun ArticleCard(article: NewsArticle, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(sourceColor(article.source))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        article.source.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = sourceColor(article.source),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                )
            }

            article.imageUrl?.takeIf { it.isNotBlank() }?.let { img ->
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CorroborationBadge(article.corroborationCount)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun CorroborationBadge(count: Int) {
    val (icon, color, text) = when {
        count >= 2 -> Triple(Icons.Default.CheckCircle, Verified, "מאומת ב-$count מקורות")
        count == 1 -> Triple(Icons.Default.CheckCircle, WarnYellow, "נמצא במקור נוסף")
        else       -> Triple(Icons.Default.ErrorOutline, FakeHigh, "לא אומת במקורות אחרים")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SourceChip(source: String) {
    val color = sourceColor(source)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            source.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

fun sourceColor(source: String): Color = when (source) {
    "Ynet"       -> SourceYnet
    "N12"        -> SourceN12
    "Kan"        -> SourceKan
    "Channel 14" -> SourceC14
    "13TV"       -> Source13
    else         -> Color.Gray
}