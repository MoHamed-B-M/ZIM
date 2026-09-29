package com.zimapp.zim.ui.notes_list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.ui.note_edit.rich.HtmlCodec
import com.zimapp.zim.ui.theme.cardContainerFor

// Sub-millisecond FTS highlight: wraps raw query matches in primary-colored spans.
fun highlighted(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var i = text.lowercase().indexOf(query.lowercase().trim().take(32))
        var guard = 0
        while (i >= 0 && guard++ < 20) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), i, (i + query.trim().length).coerceAtMost(text.length))
            i = text.lowercase().indexOf(query.lowercase().trim(), i + 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(note: Note, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container = cardContainerFor(note.colorToken)
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = if (container != null)
            androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = container)
        else androidx.compose.material3.CardDefaults.elevatedCardColors(),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (note.isPinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned")
            Text(
                text = remember(note.title, query) { highlighted(note.title.ifBlank { "(untitled)" }, query) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = remember(note.content, note.format, query) {
                    val body = if (note.format == NoteFormat.RICH) HtmlCodec.htmlToPlain(note.content) else note.content
                    highlighted(body, query)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6, overflow = TextOverflow.Ellipsis,
            )
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    note.tags.take(3).forEach { FilterChip(selected = false, onClick = {}, label = { Text(it) }) }
                }
            }
        }
    }
}
