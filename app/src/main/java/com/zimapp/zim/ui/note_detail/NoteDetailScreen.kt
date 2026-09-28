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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.ui.markdown.MarkdownText
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel

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
    LaunchedEffect(noteId) { vm.load(noteId) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    val mono = if (appSettings.monospaceFont) FontFamily.Monospace else null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(if (noteId == null) "New note" else "Edit note") },
                actions = {
                    IconButton(onClick = { vm.edit(color = (s.colorToken + 1) % 6) }) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Cycle color")
                    }
                    IconButton(onClick = { vm.save(isChecklist) }) { Icon(Icons.Filled.Check, "Save") }
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
            MarkdownText(
                radius = appSettings.cornerRadius,
                markdown = s.content.ifBlank { "_Nothing to preview_" },
                isPreview = true,
                isEnabled = appSettings.markdownEnabled,
                fontSize = appSettings.fontSize.sp,
                fontFamily = mono,
                modifier = Modifier.fillMaxWidth(),
            )
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
