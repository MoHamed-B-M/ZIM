package com.zimapp.zim.ui.note_detail

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.ui.markdown.MarkdownText
import com.zimapp.zim.ui.note_edit.rich.FormatFloatingToolbar
import com.zimapp.zim.ui.note_edit.rich.HtmlCodec
import com.zimapp.zim.ui.note_edit.rich.RichEditorField
import com.zimapp.zim.ui.note_edit.rich.UrlDialog
import com.zimapp.zim.ui.note_edit.rich.continueStyleOnEdit
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
    onOpenTheme: () -> Unit = {},
    vm: NoteDetailViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val appSettings by koinViewModel<AppSettingsViewModel>().settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(noteId) { vm.load(noteId) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    LaunchedEffect(s.deleted) { if (s.deleted) onBack() }

    val mono = if (appSettings.monospaceFont) FontFamily.Monospace else null
    val rich = s.format == NoteFormat.RICH

    // Hoisted rich field: initialized from stored HTML once content loads.
    // Keyed on format too, so toggling modes never shows stale content.
    val initialDoc = remember(s.loaded, s.id, s.format) {
        HtmlCodec.fromHtml(s.content.ifBlank { if (isChecklist) "- [ ] " else "" })
    }
    var field by remember(initialDoc) { mutableStateOf(TextFieldValue(initialDoc)) }

    fun richHtmlNow() = HtmlCodec.toHtml(field.annotatedString)
    fun saveRich() = vm.save(isChecklist, richHtmlNow())
    fun saveQuietly() = vm.saveQuietly(if (rich) richHtmlNow() else null)
    var toolsOpen by rememberSaveable { mutableStateOf(true) }
    var linkDialog by remember { mutableStateOf(false) }
    var tagsDialog by remember { mutableStateOf(false) }
    var findDialog by remember { mutableStateOf(false) }
    var fontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var fontLabel by remember { mutableStateOf("Sans") }

    // Undo/redo history (char-granular, capped).
    val past = remember { mutableStateListOf<AnnotatedString>() }
    val future = remember { mutableStateListOf<AnnotatedString>() }
    fun pushHistory(prev: AnnotatedString) {
        past.add(prev)
        if (past.size > 100) past.removeAt(0)
        future.clear()
    }

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
            pushHistory(field.annotatedString)
            field = field.insertImageBlock(dest.absolutePath)
        }
    }

    if (linkDialog) {
        UrlDialog(
            onDismiss = { linkDialog = false },
            onConfirm = { url ->
                pushHistory(field.annotatedString)
                field = field.insertLink(url)
                linkDialog = false
            },
        )
    }

    if (tagsDialog) {
        TagsDialog(
            tagsCsv = s.tagsCsv,
            onTags = { vm.edit(tags = it) },
            onDismiss = { tagsDialog = false },
        )
    }

    if (findDialog) {
        FindDialog(
            text = if (rich) HtmlCodec.htmlToPlain(field.text) else s.content,
            onDismiss = { findDialog = false },
            onJump = { start, end -> if (rich) field = field.copy(selection = androidx.compose.ui.text.TextRange(start, end)) },
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
                                vm.edit(content = HtmlCodec.htmlToPlain(richHtmlNow()))
                                vm.setFormat(NoteFormat.MARKDOWN)
                            } else {
                                vm.setFormat(NoteFormat.RICH)
                            }
                        },
                    ) {
                        Text(if (rich) "Markdown" else "Rich")
                    }
                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                        },
                    ) {
                        clickableItem(
                            onClick = {
                                vm.toggleArchive()
                                saveQuietly()
                                onBack()
                            },
                            label = if (s.isArchived) "Unarchive" else "Archive",
                            icon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        )
                        clickableItem(
                            onClick = { if (rich) saveRich() else vm.save(isChecklist) },
                            label = "Save",
                            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                        )
                        clickableItem(
                            onClick = {
                                vm.togglePin()
                                saveQuietly()
                            },
                            label = if (s.isPinned) "Pinned" else "Pin",
                            icon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                        )
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
                    onValueChange = {
                        pushHistory(field.annotatedString)
                        field = it.continueStyleOnEdit(field)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(96.dp))
            }
            FormatFloatingToolbar(
                expanded = toolsOpen,
                onToggleExpand = { toolsOpen = !toolsOpen },
                field = field,
                onUpdate = {
                    pushHistory(field.annotatedString)
                    field = it
                },
                fontLabel = fontLabel,
                onFontCycle = {
                    val (next, label) = nextFont(fontFamily)
                    fontFamily = next
                    fontLabel = label
                    pushHistory(field.annotatedString)
                    field = field.setFontFamily(next)
                },
                onAttachClick = { imagePicker.launch(arrayOf("image/*")) },
                onPaletteClick = onOpenTheme,
                canUndo = past.isNotEmpty(),
                onUndo = {
                    if (past.isNotEmpty()) {
                        future.add(0, field.annotatedString)
                        field = field.copy(annotatedString = past.removeAt(past.lastIndex))
                    }
                },
                canRedo = future.isNotEmpty(),
                onRedo = {
                    if (future.isNotEmpty()) {
                        past.add(field.annotatedString)
                        field = field.copy(annotatedString = future.removeAt(0))
                    }
                },
                onFind = { findDialog = true },
                onCopy = { vm.duplicate(richHtmlNow()) },
                onSend = {
                    val text = "${s.title}\n${HtmlCodec.htmlToPlain(richHtmlNow())}"
                    ctx.startActivity(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, s.title)
                            .putExtra(Intent.EXTRA_TEXT, text)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                onLabels = { tagsDialog = true },
                onDelete = { vm.trashCurrent() },
                onLinkClick = { linkDialog = true },
                onImageClick = { imagePicker.launch(arrayOf("image/*")) },
                onSave = ::saveRich,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
            )
        }
    }
}

@Composable
private fun TagsDialog(tagsCsv: String, onTags: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val tags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    fun current(vararg extra: String) = (tags + extra.toList()).distinct().joinToString(", ")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text("Labels", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close, contentDescription = "Remove",
                                modifier = Modifier.clickable {
                                    onTags(tags.filter { it != tag }.joinToString(", "))
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("New label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { if (input.isNotBlank()) { onTags(current(input.trim())); input = "" } }) {
                    Text("Add")
                }
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun FindDialog(text: String, onDismiss: () -> Unit, onJump: (Int, Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var index by remember { mutableStateOf(0) }
    val matches = remember(text, query) {
        if (query.isBlank()) emptyList()
        else Regex.escape(query).toRegex(RegexOption.IGNORE_CASE).findAll(text).map { it.range.first to it.range.last + 1 }.toList()
    }
    // Keep the index valid as matches change; jump on move.
    androidx.compose.runtime.LaunchedEffect(matches, index) {
        if (matches.isNotEmpty()) {
            val i = index.coerceIn(matches.indices)
            onJump(matches[i].first, matches[i].second)
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text("Find in note", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; index = 0 },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (query.isBlank()) "" else if (matches.isEmpty()) "No matches" else "${index.coerceIn(matches.indices) + 1} of ${matches.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row {
                    IconButton(
                        onClick = { if (matches.isNotEmpty()) index = (index - 1 + matches.size) % matches.size },
                        enabled = matches.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous")
                    }
                    IconButton(
                        onClick = { if (matches.isNotEmpty()) index = (index + 1) % matches.size },
                        enabled = matches.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
