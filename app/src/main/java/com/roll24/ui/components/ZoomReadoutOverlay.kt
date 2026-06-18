package com.roll24.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roll24.camera.zoom.S24UltraZoomModel
import com.roll24.camera.zoom.ZoomUiState
import com.roll24.ui.theme.Roll24Colors

/**
 * Zoom readout overlay - positioned in a corner, out of the way.
 *
 * Displays mm + zoom ratio + lens label.
 * Fade in on gesture start, fade out 1.2s after gesture end.
 * No scale animations, no bounce - just clean fade.
 * mm counter animates smoothly between values (no flicker).
 */
@Composable
fun ZoomReadoutOverlay(
    state: ZoomUiState,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val accentAlpha by animateFloatAsState(
        targetValue = if (state.isNearOpticalAnchor) 1f else 0.7f,
        animationSpec = tween(durationMillis = 250),
        label = "accentAlpha"
    )

    // Smooth mm counter - no jumping/flickering between values
    val animatedMm by animateIntAsState(
        targetValue = state.equivalentMm,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mmCounter"
    )

    val textColor = if (state.isNearOpticalAnchor) {
        Roll24Colors.WarmGold
    } else {
        Roll24Colors.Paper
    }

    val chipColor = if (state.isNearOpticalAnchor) {
        Roll24Colors.WarmGold.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }

    val chipTextColor = if (state.isNearOpticalAnchor) {
        Roll24Colors.WarmGold
    } else {
        Roll24Colors.MutedText
    }

    val formattedRatio = remember(state.zoomRatio) {
        S24UltraZoomModel.formatZoomRatio(state.zoomRatio)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(350)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Roll24Colors.InkBlack.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Primary: focal length mm (animated, no flicker)
                Text(
                    text = "${animatedMm}mm",
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.alpha(accentAlpha)
                )

                // Secondary: zoom ratio
                Text(
                    text = formattedRatio,
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                // Lens chip
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(chipColor)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = state.lensRegionLabel,
                        color = chipTextColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
