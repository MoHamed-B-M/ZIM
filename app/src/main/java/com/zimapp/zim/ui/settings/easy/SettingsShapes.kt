package com.zimapp.zim.ui.settings.easy

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Ported from EasyNotes shapeManager; corner radius comes from ZIM AppSettings.
fun shapeManager(
    isBoth: Boolean = false,
    isLast: Boolean = false,
    isFirst: Boolean = false,
    radius: Int,
): RoundedCornerShape {
    val smallerRadius: Dp = (radius / 5).dp
    val defaultRadius: Dp = radius.dp
    return when {
        isBoth -> RoundedCornerShape(defaultRadius)
        isLast -> RoundedCornerShape(smallerRadius, smallerRadius, defaultRadius, defaultRadius)
        isFirst -> RoundedCornerShape(defaultRadius, defaultRadius, smallerRadius, smallerRadius)
        else -> RoundedCornerShape(smallerRadius)
    }
}
