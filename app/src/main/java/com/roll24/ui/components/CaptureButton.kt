package com.roll24.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.roll24.haptics.Roll24Haptics
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.ui.theme.Roll24Colors

@Composable
fun Roll24CaptureButton(
    modifier: Modifier = Modifier, 
    onCapture: () -> Unit,
    haptics: Roll24Haptics = rememberRoll24Haptics()
) {
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, label = "capturePress")
    
    Canvas(modifier = modifier.size(86.dp).pointerInput(Unit) { 
        detectTapGestures(
            onPress = { 
                pressed = true
                haptics.shutterHalfPress()
                tryAwaitRelease()
                pressed = false
                haptics.shutterRelease()
                onCapture() 
            }
        ) 
    }) {
        val r = size.minDimension / 2f * press; val c = center
        drawCircle(Color(0xFF34312B), r, c); drawCircle(Color(0xFF1B1A17), r*.90f, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3630), Color(0xFF0B0B0A)), Offset(c.x-r*.25f,c.y-r*.35f), r), r*.72f, c)
        drawCircle(Roll24Colors.WarmGold.copy(alpha=.82f), r*.74f, c, style=androidx.compose.ui.graphics.drawscope.Stroke(width=2.2f))
    }
}

// Legacy alias
@Composable
fun CaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Roll24CaptureButton(
        modifier = modifier,
        onCapture = {
            if (enabled) onClick()
        }
    )
}
