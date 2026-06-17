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
        // Profile card image
        Image(
            painter = painterResource(id = cardDrawable),
            contentDescription = profile.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = Roll24Radius.Md, topEnd = Roll24Radius.Md))
        )
        
        // Profile name
        Text(
            text = profile.name,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            color = if (isSelected) Roll24Colors.WarmGold else Roll24Colors.Paper,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}
