package com.smish.wheresmymoney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val PixelFontFamily = FontFamily.Monospace

val PixelTypography = Typography(
    headlineLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = PixelFontFamily, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = PixelFontFamily, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = PixelFontFamily, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = PixelFontFamily, fontSize = 9.sp, lineHeight = 12.sp),
)
