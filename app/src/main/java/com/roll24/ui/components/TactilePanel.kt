package com.roll24.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.roll24.ui.theme.Roll24Colors

@Composable
fun Roll24TactilePanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF1C1B18), Color(0xFF11110F))), RoundedCornerShape(22.dp)).border(1.dp, Roll24Colors.Stroke, RoundedCornerShape(22.dp)).padding(16.dp), content = content)
}
