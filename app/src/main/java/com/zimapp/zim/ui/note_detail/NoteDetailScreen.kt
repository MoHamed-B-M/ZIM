package com.zimapp.zim.ui.note_detail

import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.ui.markdown.MarkdownText
import com.zimapp.zim.ui.note_edit.rich.FormatFloatingToolbar
import com.zimapp.zim.ui.note_edit.rich.HtmlCodec
import com.zimapp.zim.ui.note_edit.rich.RichEditorField
import com.zimapp.zim.ui.note_edit.rich.UrlDialog
import com.zimapp.zim.ui.note_edit.rich.insertImageBlock
import com.zimapp.zim.ui.note_edit.rich.insertLink
import com.zimapp.zim.ui.note_edit.rich.nextFont
import com.zimapp.zim.ui.note_edit.rich.setFontFamily
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import java.io.File
import java.util.UUID
import org.koin.androidx.compose.koinViewModel

// Editor with two modes: RICH (formatted text + floating format toolbar) and
// MARKDOWN (plain text + rendered preview, optional). New notes default RICH.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String?,
    isChecklist: Boolean = false,
    onBack: () -> Unit,
    vm: NoteDetailViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val appSettings by koinViewModel<AppSettingsViewModel>().settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(noteId) { vm.load(noteId) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }

    val mono = if (appSettings.monospaceFont) FontFamily.Monospace else null
    val rich = s.format == NoteFormat.RICH

    // Hoisted rich field: initialized from stored HTML once content loads.
    val initialDoc = remember(s.loaded, s.id) {
        HtmlCodec.fromHtml(s.content.ifBlank { if (isChecklist) "- [ ] " else "" })
    }
    var field by remember(initialDoc) { mutableStateOf(TextFieldValue(initialDoc)) }
    var toolsOpen by rememberSaveable { mutableStateOf(true) }
    var linkDialog by remember { mutableStateOf(false) }
    var fontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var fontLabel by remember { mutableStateOf("Sans") }

    fun saveRich() = vm.save(isChecklist, HtmlCodec.toHtml(field.annotatedString))

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val dir = File(ctx.filesDir, "images").apply { mkdirs() }
            val dest = File(dir, "${UUID.randomUUID()}.$ext")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            field = field.insertImageBlock(dest.absolutePath)
        }
    }

    if (linkDialog) {
        UrlDialog(
            onDismiss = { linkDialog = false },
            onConfirm = { url ->
                field = field.insertLink(url)
                linkDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(if (noteId == null) "New note" else "Edit note") },
                actions = {
                    TextButton(
                        onClick = {
                            if (rich) {
                                vm.edit(content = HtmlCodec.htmlToPlain(HtmlCodec.toHtml(field.annotatedString)))
                                vm.setFormat(NoteFormat.MARKDOWN)
                            } else {
                                vm.setFormat(NoteFormat.RICH)
                            }
                        },
                    ) {
                        Text(if (rich) "Markdown" else "Rich")
                    }
                    IconButton(onClick = { if (rich) saveRich() else vm.save(isChecklist) }) {
                        Icon(Icons.Filled.Check, "Save")
                    }
                },
            )
        },
    ) { p ->
        if (!rich) {
            Column(
                Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = s.title, onValueChange = { vm.edit(title = it) },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
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
                OutlinedTextField(
                    value = s.tagsCsv, onValueChange = { vm.edit(tags = it) },
                    label = { Text("Tags (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(p)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = s.title, onValueChange = { vm.edit(title = it) },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                RichEditorField(
                    value = field,
                    onValueChange = { field = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = s.tagsCsv, onValueChange = { vm.edit(tags = it) },
                    label = { Text("Tags (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(96.dp))
            }
            FormatFloatingToolbar(
                expanded = toolsOpen,
                onToggleExpand = { toolsOpen = !toolsOpen },
                field = field,
                onUpdate = { field = it },
                fontLabel = fontLabel,
                onFontCycle = {
                    val (next, label) = nextFont(fontFamily)
                    fontFamily = next
                    fontLabel = label
                    field = field.setFontFamily(next)
                },
                onLinkClick = { linkDialog = true },
                onImageClick = { imagePicker.launch(arrayOf("image/*")) },
                onSave = ::saveRich,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
            )
        }
    }
}
