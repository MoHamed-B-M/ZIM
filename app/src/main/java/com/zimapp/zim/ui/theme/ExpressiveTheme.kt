package com.zimapp.zim.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ZIM theme: system dynamic → custom seed (m3color Expressive) → fallback.
// fontSize (sp baseline 16) scales key text styles; cornerRadius (dp)
// reshapes the card-scale corners. Driven by AppSettings in MainActivity.
@Composable
fun ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Int? = null,
    fontSize: Int = 16,
    cornerRadius: Int = 28,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        seedColor != null ->
            generatePalette(Color(seedColor), darkTheme, PaletteStyle.Expressive)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkExpressive
        else -> LightExpressive
    }
    val scale = (fontSize / 16f).coerceIn(0.75f, 1.5f)
    val base = Typography()
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * scale),
        headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * scale),
        titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * scale),
        titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * scale),
        bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * scale),
        bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * scale),
        labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * scale),
    )
    val r = cornerRadius.dp
    val shapes = ExpressiveShapes.copy(
        medium = RoundedCornerShape(r),
        large = RoundedCornerShape(r + 4.dp),
        largeIncreased = RoundedCornerShape(r + 6.dp),
        extraLarge = RoundedCornerShape(r + 12.dp),
        extraLargeIncreased = RoundedCornerShape(r + 16.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
