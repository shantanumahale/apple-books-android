package com.applebooks.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.applebooks.android.domain.model.BookFormat
import java.io.File

private val PdfPlaceholderTop = Color(0xFFE8404A)
private val PdfPlaceholderBottom = Color(0xFFCC2D37)
private val EpubPlaceholderTop = Color(0xFF4A90D9)
private val EpubPlaceholderBottom = Color(0xFF2E6BB5)

@Composable
fun CoverImage(
    coverCachePath: String?,
    title: String,
    format: BookFormat,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .aspectRatio(2f / 3f),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (coverCachePath != null) {
            AsyncImage(
                model = File(coverCachePath),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        } else {
            PlaceholderCover(
                title = title,
                format = format,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PlaceholderCover(
    title: String,
    format: BookFormat,
    modifier: Modifier = Modifier
) {
    val gradientBrush = when (format) {
        BookFormat.PDF -> Brush.verticalGradient(
            colors = listOf(PdfPlaceholderTop, PdfPlaceholderBottom)
        )
        BookFormat.EPUB -> Brush.verticalGradient(
            colors = listOf(EpubPlaceholderTop, EpubPlaceholderBottom)
        )
    }

    Box(
        modifier = modifier.background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        Text(
            text = format.name,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        )
    }
}
