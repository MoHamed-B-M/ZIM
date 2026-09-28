package com.example.expressivenotes.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback expressive seeds (used when dynamic color unavailable / < API 31).
val ExpressiveSeed = Color(0xFF6750A4)
val LightExpressive = lightColorScheme(
    primary = Color(0xFF4F378B), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiaryContainer = Color(0xFFFFD8E4), surfaceContainer = Color(0xFFF3EDF7),
)
val DarkExpressive = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458), surfaceContainer = Color(0xFF211F26),
)

// Card tonal accents by colorToken (0 = default, 1..5 expressive pops).
fun cardContainerFor(token: Int): Color? = when (token) {
    1 -> Color(0xFFEADDFF); 2 -> Color(0xFFC2E7FF); 3 -> Color(0xFFFFD8E4)
    4 -> Color(0xFFC4EFAC); 5 -> Color(0xFFFFE1B3); else -> null
}
