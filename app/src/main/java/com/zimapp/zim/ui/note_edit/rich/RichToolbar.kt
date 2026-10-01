package com.zimapp.zim.ui.note_edit.rich

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

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

// Keep-style floating toolbar. Collapsed: attach, palette (theme page),
// A (format tools), undo, redo, overflow menu. Expanded: full format row.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatFloatingToolbar(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    field: TextFieldValue,
    onUpdate: (TextFieldValue) -> Unit,
    fontLabel: String,
    onFontCycle: () -> Unit,
    onAttachClick: () -> Unit,
    onPaletteClick: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    onCopy: () -> Unit,
    onSend: () -> Unit,
    onLabels: () -> Unit,
    onDelete: () -> Unit,
    onLinkClick: () -> Unit,
    onImageClick: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        leadingContent = {
            IconButton(onClick = onToggleExpand) {
                Text(
                    "A",
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    color = if (expanded) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
        },
        content = {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn(spring()) + scaleIn(spring())) togetherWith
                        (fadeOut(spring()) + scaleOut(spring())) using
                        SizeTransform(clip = false)
                },
                label = "toolbar-swap",
            ) { showTools ->
                if (showTools) {
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
                            ColorDot(color = c, onPick = { onUpdate(field.setHighlight(if (c == Color.Unspecified) null else c)) })
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ToolButton(Icons.Filled.Add, "Attach image", onAttachClick)
                        ToolButton(Icons.Filled.Palette, "Theme", onPaletteClick)
                        ToolButton(Icons.Filled.CheckBox, "Checklist") { onUpdate(field.toggleLinePrefix("- [ ] ")) }
                        IconButton(onClick = onUndo, enabled = canUndo) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = onRedo, enabled = canRedo) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Find in note") },
                                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                                    onClick = { menuOpen = false; onFind() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Make a copy") },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                    onClick = { menuOpen = false; onCopy() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Send") },
                                    leadingIcon = { Icon(Icons.Filled.Send, null) },
                                    onClick = { menuOpen = false; onSend() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Labels") },
                                    leadingIcon = { Icon(Icons.Filled.Label, null) },
                                    onClick = { menuOpen = false; onLabels() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                    onClick = { menuOpen = false; onDelete() },
                                )
                            }
                        }
                    }
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
