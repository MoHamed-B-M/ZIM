package com.zimapp.zim.ui.note_edit.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

val RichTextColors = listOf(
    Color.Black, Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFF1976D2),
    Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF616161), Color.Unspecified,
)
val RichHighlightColors = listOf(
    Color(0xFFFFF59D), Color(0xFFC8E6C9), Color(0xFFF8BBD0), Color.Unspecified,
)

// Compact formatting toolbar. All actions delegate to RichDoc ops on the
// screen-owned TextFieldValue; active-state highlighting intentionally omitted.
@Composable
fun RichToolbar(
    fontLabel: String,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onSizeUp: () -> Unit,
    onSizeDown: () -> Unit,
    onFontCycle: () -> Unit,
    onAlign: (TextAlign) -> Unit,
    onBullet: () -> Unit,
    onChecklist: () -> Unit,
    onLink: () -> Unit,
    onImage: () -> Unit,
    onColor: (Color?) -> Unit,
    onHighlight: (Color?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Icons.Filled.FormatBold, "Bold", onBold)
            ToolButton(Icons.Filled.FormatItalic, "Italic", onItalic)
            ToolButton(Icons.Filled.FormatUnderlined, "Underline", onUnderline)
            ToolButton(Icons.Filled.FormatStrikethrough, "Strike", onStrike)
            ToolButton(Icons.Filled.TextDecrease, "Smaller", onSizeDown)
            ToolButton(Icons.Filled.TextIncrease, "Bigger", onSizeUp)
            IconButton(onClick = onFontCycle) {
                Text(fontLabel, style = MaterialTheme.typography.labelMedium)
            }
            ToolButton(Icons.Filled.FormatAlignLeft, "Left") { onAlign(TextAlign.Left) }
            ToolButton(Icons.Filled.FormatAlignCenter, "Center") { onAlign(TextAlign.Center) }
            ToolButton(Icons.Filled.FormatAlignRight, "Right") { onAlign(TextAlign.Right) }
            ToolButton(Icons.Filled.FormatListBulleted, "Bullets", onBullet)
            ToolButton(Icons.Filled.CheckBox, "Checklist", onChecklist)
            ToolButton(Icons.Filled.InsertLink, "Link", onLink)
            ToolButton(Icons.Filled.Image, "Image", onImage)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
            RichTextColors.forEach { c ->
                ColorDot(color = c, onPick = { onColor(if (c == Color.Unspecified) null else c) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
            RichHighlightColors.forEach { c ->
                ColorDot(color = c, onPick = { onHighlight(if (c == Color.Unspecified) null else c) })
            }
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = desc)
    }
}

@Composable
private fun ColorDot(color: Color, onPick: () -> Unit) {
    val isClear = color == Color.Unspecified
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(if (isClear) MaterialTheme.colorScheme.surfaceVariant else color)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (isClear) Text("×", style = MaterialTheme.typography.labelSmall)
    }
}

fun nextFont(current: FontFamily?): Pair<FontFamily?, String> = when (current) {
    FontFamily.Serif -> FontFamily.Monospace to "Mono"
    FontFamily.Monospace -> null to "Sans"
    else -> FontFamily.Serif to "Serif"
}
