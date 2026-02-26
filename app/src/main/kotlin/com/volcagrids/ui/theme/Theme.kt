package com.volcagrids.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Raster-Noton / Ikeda Data Palette
val RasterBlack = Color(0xFF020202) // True Void Black
val RasterDark = Color(0xFF0A0A0A)  // Subtle Panel Background
val RasterGrid = Color(0xFF161616)  // Extremely subtle grid/border lines
val RasterWhite = Color(0xFFF0F0F0) // Stark white data text

// Signal Colors: Cold, Clinical, Abstract
val RasterSignalA = Color(0xFFEBEBEB) // Primary Data Flow (High Contrast)
val RasterSignalB = Color(0xFF757575) // Secondary Data Flow (Muted Grey)
val RasterActive = Color(0xFFFFFFFF)  // Maximum Intensity White

val RasterColorScheme = darkColorScheme(
    primary = RasterSignalA,
    secondary = RasterSignalB,
    background = RasterBlack,
    surface = RasterDark,
    onBackground = RasterWhite,
    onSurface = RasterWhite
)

val RasterTypography = Typography(
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        letterSpacing = 1.0.sp,
        color = Color.Gray
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        letterSpacing = (-0.5).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace, // Enforce Monospace everywhere
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)
