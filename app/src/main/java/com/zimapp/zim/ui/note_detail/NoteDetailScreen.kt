package com.zimapp.zim.ui.note_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Minimal markdown → AnnotatedString (**bold**, *italic*, `code`, "- [ ]" checklist).
fun markdownPreview(src: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    append(src)
    // Bold
    Regex("\\*\\*(.+?)\\*\\*").findAll(src).forEach { m ->
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
    }
    Regex("`(.+?)`").findAll(src).forEach { m ->
        addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color(0x22000000)), m.range.first, m.range.last + 1)
    }
    Regex("(?m)^- \\[ \\] ").findAll(src).forEach { m ->
        addStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.Gray), m.range.first, m.range.last + 1)
    }
    Regex("\\*(.+?)\\*").findAll(src).forEach { m ->
        if (!src.substring(maxOf(0, m.range.first - 1), m.range.first).contains("*"))
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(noteId: String?, onBack: () -> Unit, vm: NoteDetailViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(noteId) { vm.load(noteId) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(if (noteId == null) "New note" else "Edit note") },
                actions = {
                    IconButton(onClick = { vm.edit(color = (s.colorToken + 1) % 6) }) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Cycle color")
                    }
                    IconButton(onClick = { vm.save() }) { Icon(Icons.Filled.Check, "Save") }
                },
            )
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = s.title, onValueChange = { vm.edit(title = it) },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = s.content, onValueChange = { vm.edit(content = it) },
                label = { Text("Content (markdown)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                minLines = 8)
            Text("Preview", style = MaterialTheme.typography.labelLarge)
            Text(markdownPreview(s.content.ifBlank { "_Nothing to preview_" }),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = s.tagsCsv, onValueChange = { vm.edit(tags = it) },
                label = { Text("Tags (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..5).forEach { token ->
                    FilterChip(selected = s.colorToken == token, onClick = { vm.edit(color = token) },
                        label = { Text(if (token == 0) "Default" else "Tone $token") })
                }
            }
        }
    }
}
