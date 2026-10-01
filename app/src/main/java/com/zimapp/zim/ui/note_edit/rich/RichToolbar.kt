package com.zimapp.zim.ui.note_edit.rich

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

val RichTextColors = listOf(
    Color.Black, Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFF1976D2),
    Color(0xFFF57C00), Color(0xFF7B1FA2), Color(0xFF616161), Color.Unspecified,
)
val RichHighlightColors = listOf(
    Color(0xFFFFF59D), Color(0xFFC8E6C9), Color(0xFFF8BBD0), Color.Unspecified,
)

fun nextFont(current: FontFamily?): Pair<FontFamily?, String> = when (current) {
    FontFamily.Serif -> FontFamily.Monospace to "Mono"
    FontFamily.Monospace -> null to "Sans"
    else -> FontFamily.Serif to "Serif"
}

// Compact format bar hosted in the screen's HorizontalFloatingToolbar.
// Pen toggles the tools; check saves; everything else formats the selection.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatFloatingToolbar(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    field: TextFieldValue,
    onUpdate: (TextFieldValue) -> Unit,
    fontLabel: String,
    onFontCycle: () -> Unit,
    onLinkClick: () -> Unit,
    onImageClick: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier.zIndex(1f),
        leadingContent = {
            IconButton(onClick = onToggleExpand) {
                Icon(Icons.Filled.Edit, contentDescription = "Format tools")
            }
        },
        trailingContent = {
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
        },
        content = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ToolButton(Icons.Filled.FormatBold, "Bold") { onUpdate(field.toggleBold()) }
                ToolButton(Icons.Filled.FormatItalic, "Italic") { onUpdate(field.toggleItalic()) }
                ToolButton(Icons.Filled.FormatUnderlined, "Underline") { onUpdate(field.toggleUnderline()) }
                ToolButton(Icons.Filled.FormatStrikethrough, "Strike") { onUpdate(field.toggleStrike()) }
                ToolButton(Icons.Filled.TextDecrease, "Smaller") { onUpdate(field.adjustSize(-2)) }
                ToolButton(Icons.Filled.TextIncrease, "Bigger") { onUpdate(field.adjustSize(2)) }
                IconButton(onClick = onFontCycle, modifier = Modifier.size(40.dp)) {
                    Text(fontLabel, style = MaterialTheme.typography.labelMedium)
                }
                ToolButton(Icons.Filled.FormatAlignLeft, "Left") { onUpdate(field.setAlignment(TextAlign.Left)) }
                ToolButton(Icons.Filled.FormatAlignCenter, "Center") { onUpdate(field.setAlignment(TextAlign.Center)) }
                ToolButton(Icons.Filled.FormatAlignRight, "Right") { onUpdate(field.setAlignment(TextAlign.Right)) }
                ToolButton(Icons.Filled.FormatListBulleted, "Bullets") { onUpdate(field.toggleLinePrefix("• ")) }
                ToolButton(Icons.Filled.CheckBox, "Checklist") { onUpdate(field.toggleLinePrefix("- [ ] ")) }
                ToolButton(Icons.Filled.InsertLink, "Link", onLinkClick)
                ToolButton(Icons.Filled.Image, "Image", onImageClick)
                RichTextColors.forEach { c ->
                    ColorDot(color = c, onPick = { onUpdate(field.setTextColor(if (c == Color.Unspecified) null else c)) })
                }
                RichHighlightColors.forEach { c ->
                    ColorDot(color = c, hl = true, onPick = { onUpdate(field.setHighlight(if (c == Color.Unspecified) null else c)) })
                }
            }
        },
    )
}

@Composable
internal fun ToolButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = desc)
    }
}

@Composable
private fun ColorDot(color: Color, hl: Boolean = false, onPick: () -> Unit) {
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
