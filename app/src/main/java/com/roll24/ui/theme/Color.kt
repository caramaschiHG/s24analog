package com.roll24.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Roll24Colors {
    val InkBlack = Color(0xFF080807)
    val Charcoal = Color(0xFF11110F)
    val Panel = Color(0xFF151412)
    val PanelSoft = Color(0xFF1B1A17)
    val Raised = Color(0xFF24221E)
    val Stroke = Color(0xFF34312B)
    val WarmGold = Color(0xFFDCA94A)
    val WarmGoldDeep = Color(0xFFA87622)
    val AmberLight = Color(0xFFF2C76B)
    val Paper = Color(0xFFD9CCB7)
    val MutedText = Color(0xFFA99F8D)
    val Olive = Color(0xFF3E482F)
    val CyanGreen = Color(0xFF143B3E)
    val TungstenBlue = Color(0xFF1A2E3A)
    val Danger = Color(0xFFBC553F)
    val Success = Color(0xFFAFB875)
}

object Roll24Radius {
    val Sm = 8.dp
    val Md = 14.dp
    val Lg = 22.dp
    val Xl = 32.dp
}

object Roll24Spacing {
    val Xs = 8.dp
    val Sm = 12.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
}

// Legacy aliases for backward compatibility
val Black = Roll24Colors.InkBlack
val DarkGray = Roll24Colors.Charcoal
val MediumGray = Roll24Colors.Panel
val LightGray = Roll24Colors.MutedText
val White = Roll24Colors.Paper
val AccentGold = Roll24Colors.WarmGold
val AccentWarm = Roll24Colors.AmberLight
val FilmWarm = Roll24Colors.AmberLight
val FilmCool = Roll24Colors.TungstenBlue
val FilmGreen = Roll24Colors.CyanGreen
val FilmMono = Roll24Colors.Paper
