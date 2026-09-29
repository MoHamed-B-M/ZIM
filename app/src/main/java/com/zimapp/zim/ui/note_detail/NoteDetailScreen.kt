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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.ui.markdown.MarkdownText
import com.zimapp.zim.ui.note_edit.rich.HtmlCodec
import com.zimapp.zim.ui.note_edit.rich.RichTextEditor
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel

// Editor with two modes: RICH (formatted text + tools) and MARKDOWN (plain
// text + rendered preview). Existing notes open in their stored format.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    noteId: String?,
    isChecklist: Boolean = false,
    onBack: () -> Unit,
    vm: NoteDetailViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val appSettings by koinViewModel<AppSettingsViewModel>().settings.collectAsStateWithLifecycle()
    var draftHtml by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(noteId) { vm.load(noteId) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    // Seed the rich draft once content loads (null = untouched).
    LaunchedEffect(s.loaded, s.id) {
        if (s.loaded) draftHtml = null
    }

    val mono = if (appSettings.monospaceFont) FontFamily.Monospace else null
    val rich = s.format == NoteFormat.RICH

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(if (noteId == null) "New note" else "Edit note") },
                actions = {
                    TextButton(
                        onClick = {
                            if (rich) {
                                // RICH → Markdown: keep readable text, drop styling.
                                val plain = draftHtml?.let { HtmlCodec.htmlToPlain(it) } ?: s.content
                                vm.edit(content = plain)
                                vm.setFormat(NoteFormat.MARKDOWN)
                            } else {
                                vm.setFormat(NoteFormat.RICH)
                                draftHtml = null
                            }
                        },
                    ) {
                        Text(if (rich) "Markdown" else "Rich")
                    }
                    IconButton(onClick = { vm.edit(color = (s.colorToken + 1) % 6) }) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Cycle color")
                    }
                    IconButton(onClick = { vm.save(isChecklist, draftHtml.takeIf { rich }) }) {
                        Icon(Icons.Filled.Check, "Save")
                    }
                },
            )
        },
    ) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.title, onValueChange = { vm.edit(title = it) },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            if (rich) {
                RichTextEditor(
                    initial = remember(s.loaded, s.id) {
                        HtmlCodec.fromHtml(s.content.ifBlank { if (isChecklist) "- [ ] " else "" })
                    },
                    onHtmlChange = { draftHtml = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = s.content, onValueChange = { vm.edit(content = it) },
                    label = { Text("Content (markdown)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    minLines = 8,
                )
                Text("Preview", style = MaterialTheme.typography.labelLarge)
                MarkdownText(
                    radius = appSettings.cornerRadius,
                    markdown = s.content.ifBlank { "_Nothing to preview_" },
                    isPreview = true,
                    isEnabled = appSettings.markdownEnabled,
                    fontSize = appSettings.fontSize.sp,
                    fontFamily = mono,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = s.tagsCsv, onValueChange = { vm.edit(tags = it) },
                label = { Text("Tags (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..5).forEach { token ->
                    FilterChip(
                        selected = s.colorToken == token, onClick = { vm.edit(color = token) },
                        label = { Text(if (token == 0) "Default" else "Tone $token") },
                    )
                }
            }
        }
    }
}
