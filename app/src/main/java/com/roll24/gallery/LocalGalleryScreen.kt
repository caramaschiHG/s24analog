package com.roll24.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.roll24.ui.theme.Roll24Colors
import com.roll24.ui.theme.Roll24Radius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GalleryTab(val label: String) {
    DEVELOPED("Reveladas"),
    FAILED("Falhas")
}

@Composable
fun LocalGalleryScreen(
    captures: List<CaptureRecord>,
    onClose: () -> Unit,
    onRemoveLocal: (CaptureRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GalleryTab.DEVELOPED) }
    val filtered = captures.filter { record ->
        when (selectedTab) {
            GalleryTab.DEVELOPED -> record.galleryUri != null && record.status == CaptureStatus.SAVED
            GalleryTab.FAILED -> record.status == CaptureStatus.FAILED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Roll24Colors.InkBlack)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Roll24", color = Roll24Colors.WarmGold, fontWeight = FontWeight.Bold)
                    Text("${captures.size} capturas locais", color = Roll24Colors.MutedText)
                }
                GalleryPill("Fechar", false, onClose)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GalleryTab.values().forEach { tab ->
                    GalleryPill(
                        text = tab.label,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Roll24Radius.Md))
                        .background(Roll24Colors.Panel.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nada nesta aba ainda", color = Roll24Colors.MutedText)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { record ->
                        CaptureRecordRow(record = record, onRemoveLocal = onRemoveLocal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureRecordRow(
    record: CaptureRecord,
    onRemoveLocal: (CaptureRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Panel.copy(alpha = 0.88f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GalleryThumbnail(uri = record.galleryUri ?: record.thumbnailUri)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(record.filmName, color = Roll24Colors.Paper, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatDate(record.createdAt)}  ${record.lensLabel ?: "lente ?"}  ${record.aspect}",
                    color = Roll24Colors.MutedText
                )
            }
            Text(record.status.name, color = Roll24Colors.WarmGold)
        }

        record.error?.let {
            Text(it, color = Color(0xFFFFC7C7))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (record.usedFallback) GalleryPill("Fallback", true, {})
            GalleryPill("Remover local", false) { onRemoveLocal(record) }
        }
    }
}

@Composable
private fun GalleryThumbnail(uri: String?) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(Roll24Colors.Raised),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            SubcomposeAsyncImage(
                model = uri,
                contentDescription = "Miniatura",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Roll24Colors.WarmGold,
                        strokeWidth = 2.dp
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF642020))
                    )
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF642020))
            )
        }
    }
}

@Composable
private fun GalleryPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Roll24Colors.WarmGold else Roll24Colors.Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
