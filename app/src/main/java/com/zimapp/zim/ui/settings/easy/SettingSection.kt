/*
 * Copyright (C) 2024 Vexzure
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.zimapp.zim.ui.settings.easy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zimapp.zim.ui.settings.easy.MaterialText

// Hub row: title + bullet-joined features on the left, circular icon on the right.
// Ported from EasyNotes SettingSection; strings passed in (no string resources).
data class SettingSection(
    val title: String,
    val features: List<String>,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun SettingSection(section: SettingSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { section.onClick() }
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialText(
            titleSize = 16.sp,
            title = section.title,
            description = section.features.joinToString(separator = " • "),
        )
        Spacer(modifier = Modifier.weight(1f))
        CircleWrapper(color = MaterialTheme.colorScheme.primary) {
            Icon(section.icon, null, tint = MaterialTheme.colorScheme.surfaceContainerLow)
        }
    }
}

@Composable
fun SectionBlock(
    sections: List<SettingSection>,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.clip(shape).fillMaxSize()) {
            sections.forEach { settingSection ->
                SettingSection(section = settingSection)
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
