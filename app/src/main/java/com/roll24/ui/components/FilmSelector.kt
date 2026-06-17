package com.roll24.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.roll24.R
import com.roll24.film.FilmProfile
import com.roll24.film.FilmType
import com.roll24.haptics.Roll24Haptics
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.ui.theme.Roll24Colors
import com.roll24.ui.theme.Roll24Radius
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun FilmSelector(
    profiles: List<FilmProfile>,
    selectedProfile: FilmProfile,
    onProfileSelected: (FilmProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberRoll24Haptics()
    val listState = rememberLazyListState()
    
    // Detecta scroll e dispara filmTick
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { _ ->
                haptics.filmTick()
            }
    }
    
    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(profiles) { profile ->
            FilmCard(
                profile = profile,
                isSelected = profile.id == selectedProfile.id,
                onClick = { 
                    haptics.filmLoaded()
                    onProfileSelected(profile) 
                }
            )
        }
    }
}

@Composable
private fun FilmCard(
    profile: FilmProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Roll24Colors.WarmGold else Roll24Colors.Stroke
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    // Map profile ID to drawable resource
    val cardDrawable = when (profile.id) {
        "warm_gold_200" -> R.drawable.warm_gold_200_card
        "soft_portrait_400" -> R.drawable.soft_portrait_400_card
        "night_tungsten_800" -> R.drawable.night_tungsten_800_card
        "green_street_400" -> R.drawable.green_street_400_card
        "mono_press_400" -> R.drawable.mono_press_400_card
        "s24_1x_clean_negative" -> R.drawable.s24_1x_clean_negative_card
        "s24_1x_street_400" -> R.drawable.s24_1x_street_400_card
        "s24_3x_portrait_400" -> R.drawable.s24_3x_portrait_400_card
        "s24_5x_chrome_200" -> R.drawable.s24_5x_chrome_200_card
        "s24_night_800" -> R.drawable.s24_night_800_card
        "portra_400" -> R.drawable.portra_400_card
        "ektar_100" -> R.drawable.ektar_100_card
        "pro_400h" -> R.drawable.pro_400h_card
        "velvia_50" -> R.drawable.velvia_50_card
        "cinestill_800t" -> R.drawable.cinestill_800t_card
        "vision3_250d" -> R.drawable.vision3_250d_card
        "gold_200" -> R.drawable.gold_200_card
        "fujicolor_c200" -> R.drawable.fujicolor_c200_card
        "hp5_plus_400" -> R.drawable.hp5_plus_400_card
        "tri_x_400" -> R.drawable.tri_x_400_card
        else -> R.drawable.warm_gold_200_card
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Panel)
            .border(borderWidth, borderColor, RoundedCornerShape(Roll24Radius.Md))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            // Profile card image
            Image(
                painter = painterResource(id = cardDrawable),
                contentDescription = profile.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = Roll24Radius.Md, topEnd = Roll24Radius.Md))
            )

            // Film-type badge (C41 / E6 / BW / V3)
            FilmTypeBadge(
                filmType = profile.filmType,
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopStart)
            )
        }

        // Profile name
        Text(
            text = profile.name,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            color = if (isSelected) Roll24Colors.WarmGold else Roll24Colors.Paper,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FilmTypeBadge(
    filmType: FilmType,
    modifier: Modifier = Modifier
) {
    val label = when (filmType) {
        FilmType.C41 -> "C41"
        FilmType.E6 -> "E6"
        FilmType.BLACK_AND_WHITE -> "BW"
        FilmType.VISION3 -> "V3"
    }

    Box(
        modifier = modifier
            .background(
                color = Roll24Colors.Panel.copy(alpha = 0.84f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Roll24Colors.Paper,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
        )
    }
}
