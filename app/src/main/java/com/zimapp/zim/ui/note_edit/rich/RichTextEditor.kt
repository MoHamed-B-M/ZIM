package com.zimapp.zim.ui.note_edit.rich

import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.util.UUID

// Rich editor: toolbar + BasicTextField over AnnotatedString. Images are
// picked via SAF, copied into app files, and inserted as ![](abs-path) lines
// (rendered by MarkdownText/AsyncImage, parsed by TodoScreen as plain text).
@Composable
fun RichTextEditor(
    initial: AnnotatedString,
    onHtmlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var field by remember(initial) { mutableStateOf(TextFieldValue(initial)) }
    var linkDialog by remember { mutableStateOf(false) }
    var fontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var fontLabel by remember { mutableStateOf("Sans") }
    val ctx = LocalContext.current

    fun report() = onHtmlChange(HtmlCodec.toHtml(field.annotatedString))

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
            report()
        }
    }

    if (linkDialog) {
        UrlDialog(
            onDismiss = { linkDialog = false },
            onConfirm = { url ->
                field = field.insertLink(url)
                report()
                linkDialog = false
            },
        )
    }

    Column(modifier) {
        RichToolbar(
            fontLabel = fontLabel,
            onBold = { field = field.toggleBold(); report() },
            onItalic = { field = field.toggleItalic(); report() },
            onUnderline = { field = field.toggleUnderline(); report() },
            onStrike = { field = field.toggleStrike(); report() },
            onSizeUp = { field = field.adjustSize(+2); report() },
            onSizeDown = { field = field.adjustSize(-2); report() },
            onFontCycle = {
                val (next, label) = nextFont(fontFamily)
                fontFamily = next
                fontLabel = label
                field = field.setFontFamily(next)
                report()
            },
            onAlign = { field = field.setAlignment(it); report() },
            onBullet = { field = field.toggleLinePrefix("• "); report() },
            onChecklist = { field = field.toggleLinePrefix("- [ ] "); report() },
            onLink = { linkDialog = true },
            onImage = { imagePicker.launch(arrayOf("image/*")) },
            onColor = { field = field.setTextColor(it); report() },
            onHighlight = { field = field.setHighlight(it); report() },
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                report()
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
        )
    }
}

@Composable
private fun UrlDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text("Insert link", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { onConfirm(url.trim()) },
                modifier = Modifier.align(Alignment.End),
                enabled = url.isNotBlank(),
            ) {
                Text("Insert")
            }
        }
    }
}
