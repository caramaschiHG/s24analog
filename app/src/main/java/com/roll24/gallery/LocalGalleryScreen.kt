package com.roll24.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.roll24.R
import com.roll24.ui.theme.Roll24Colors
import com.roll24.ui.theme.Roll24Radius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GalleryTab { DEVELOPED, FAILED }

@Composable
fun LocalGalleryScreen(
    captures: List<CaptureRecord>,
    onClose: () -> Unit,
    onRemoveLocal: (CaptureRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.DEVELOPED) }
    val filtered = remember(captures, selectedTab) {
        captures.filter { record ->
            when (selectedTab) {
                GalleryTab.DEVELOPED -> record.galleryUri != null && record.status == CaptureStatus.SAVED
                GalleryTab.FAILED -> record.status == CaptureStatus.FAILED
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Roll24Colors.InkBlack)
            .systemBarsPadding()
    ) {
        val useGrid = maxWidth >= 600.dp
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryHeader(
                count = captures.size,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onClose = onClose
            )
            if (filtered.isEmpty()) {
                EmptyGallery(selectedTab = selectedTab, modifier = Modifier.weight(1f))
            } else if (useGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(filtered, key = { it.id }) { record ->
                        CaptureRecordCard(record, onRemoveLocal, useGrid = true)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { record ->
                        CaptureRecordCard(record, onRemoveLocal, useGrid = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryHeader(
    count: Int,
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Roll24Colors.Charcoal)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gallery_title),
                    color = Roll24Colors.Paper,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = pluralStringResource(R.plurals.local_captures, count, count),
                    color = Roll24Colors.MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Roll24Colors.Paper)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GalleryTab.values().forEach { tab ->
                val selected = selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    label = {
                        Text(stringResource(if (tab == GalleryTab.DEVELOPED) R.string.developed else R.string.failures))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Roll24Colors.Raised,
                        labelColor = Roll24Colors.Paper,
                        selectedContainerColor = Roll24Colors.WarmGold,
                        selectedLabelColor = Roll24Colors.InkBlack
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun EmptyGallery(selectedTab: GalleryTab, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = Roll24Colors.WarmGold,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.gallery_empty_title),
            color = Roll24Colors.Paper,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (selectedTab == GalleryTab.DEVELOPED) R.string.gallery_empty_body
                else R.string.gallery_failed_empty_body
            ),
            color = Roll24Colors.MutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CaptureRecordCard(record: CaptureRecord, onRemoveLocal: (CaptureRecord) -> Unit, useGrid: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Panel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (useGrid) {
            GalleryThumbnail(
                uri = record.galleryUri ?: record.thumbnailUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
            RecordDetails(record)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GalleryThumbnail(
                    uri = record.galleryUri ?: record.thumbnailUri,
                    modifier = Modifier.size(76.dp)
                )
                RecordDetails(record, modifier = Modifier.padding(start = 12.dp))
            }
        }

        record.error?.let {
            Text(it, color = Color(0xFFFFC7C7), style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (record.usedFallback) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.fallback)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Roll24Colors.Raised,
                        labelColor = Roll24Colors.MutedText
                    ),
                    border = null
                )
            }
            AssistChip(
                onClick = { onRemoveLocal(record) },
                label = { Text(stringResource(R.string.remove_local)) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Roll24Colors.Raised,
                    labelColor = Roll24Colors.Paper,
                    leadingIconContentColor = Roll24Colors.Danger
                ),
                border = null
            )
        }
    }
}

@Composable
private fun RecordDetails(record: CaptureRecord, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = record.filmName,
            color = Roll24Colors.Paper,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatDate(record.createdAt),
            color = Roll24Colors.MutedText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${record.lensLabel ?: stringResource(R.string.lens_unknown)} · ${record.aspect}",
            color = Roll24Colors.MutedText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GalleryThumbnail(uri: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Roll24Radius.Sm)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Roll24Colors.Raised),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            SubcomposeAsyncImage(
                model = uri,
                contentDescription = stringResource(R.string.thumbnail),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Roll24Colors.WarmGold, strokeWidth = 2.dp)
                },
                error = {
                    Icon(Icons.Rounded.Image, contentDescription = null, tint = Roll24Colors.MutedText)
                }
            )
        } else {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Roll24Colors.MutedText)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))
}
