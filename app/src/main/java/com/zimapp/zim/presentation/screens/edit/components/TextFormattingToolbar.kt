package com.zimapp.zim.presentation.screens.edit.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.HMobiledata
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.StrikethroughS
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zimapp.zim.R
import com.zimapp.zim.presentation.components.getExternalStorageDir
import com.zimapp.zim.presentation.components.getImageName
import com.zimapp.zim.presentation.screens.edit.model.EditViewModel
import com.zimapp.zim.presentation.screens.edit.model.MarkdownMark
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** A line-level insertion: acts on the current line rather than a selection. */
private data class LineAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClickAction: () -> Unit,
)

private data class MarkAction(
    val mark: MarkdownMark,
    val icon: ImageVector,
    val contentDescription: String,
)

/**
 * Markdown toolbar for the note body.
 *
 * Built on M3's `HorizontalFloatingToolbar`, the expressive floating toolbar
 * from material3 1.5.x. Two notes on the shape of this API, since the
 * documented preset-based form (`ExpressiveHorizontalToolbar` +
 * `ExpressiveToolbarDefaults.RichTextPreset`) does not exist in this version:
 * the toolbar takes an arbitrary `RowScope` content lambda, so the item list and
 * its ordering live here rather than in a library preset, and inline marks are
 * driven by `isMarkActive` instead of a locally held set of selected ids — the
 * note text is the source of truth for what is formatted.
 *
 * One scrollable row replaces the old four pages of arrow-navigated items: a
 * floating toolbar is a single row, and paging through it fought the gesture.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextFormattingToolbar(viewModel: EditViewModel, expanded: Boolean) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val savedUri = saveImageToAppStorage(context, it)
            viewModel.insertText("!($savedUri)")
        }
    }

    val markActions = remember {
        listOf(
            MarkAction(MarkdownMark.BOLD, Icons.Rounded.FormatBold, "Bold"),
            MarkAction(MarkdownMark.ITALIC, Icons.Rounded.FormatItalic, "Italic"),
            MarkAction(MarkdownMark.UNDERLINE, Icons.Rounded.FormatUnderlined, "Underline"),
            MarkAction(MarkdownMark.STRIKETHROUGH, Icons.Rounded.StrikethroughS, "Strikethrough"),
            MarkAction(MarkdownMark.HIGHLIGHT, Icons.Rounded.Highlight, "Highlight"),
            MarkAction(MarkdownMark.CODE, Icons.Rounded.Code, "Inline code"),
        )
    }

    val lineActions = remember {
        listOf(
            LineAction(Icons.Rounded.HMobiledata, "Header") { viewModel.insertText("# ") },
            LineAction(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bullet list") { viewModel.insertText("- ") },
            LineAction(Icons.Rounded.CheckBox, "Checkbox") { viewModel.insertText("[ ] ") },
            LineAction(Icons.Rounded.FormatQuote, "Quote") { viewModel.insertText("> ", newLine = true) },
            // DataObject, not Code: the inline-code mark already uses Code, and
            // two identical icons either side of the divider are unreadable.
            LineAction(Icons.Rounded.DataObject, "Code block") { viewModel.insertText("```\n\n```", offset = -4) },
            LineAction(Icons.Rounded.Image, "Insert image") { launcher.launch("image/*") },
        )
    }

    HorizontalFloatingToolbar(
        expanded = expanded,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            markActions.forEach { action ->
                val checked = viewModel.isMarkActive(action.mark)
                IconToggleButton(
                    checked = checked,
                    onCheckedChange = { viewModel.toggleMark(action.mark) },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        // Only the *checked* pair is themed: that is the state
                        // that means "this mark covers the selection". Leaving
                        // the unchecked pair on the tonal default keeps the
                        // inactive buttons quiet against the vibrant container.
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = stringResource(
                            when (action.mark) {
                                MarkdownMark.BOLD -> R.string.format_bold
                                MarkdownMark.ITALIC -> R.string.format_italic
                                MarkdownMark.UNDERLINE -> R.string.format_underline
                                MarkdownMark.STRIKETHROUGH -> R.string.format_strikethrough
                                MarkdownMark.HIGHLIGHT -> R.string.format_highlight
                                MarkdownMark.CODE -> R.string.format_code
                            }
                        ),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Separates inline marks from line actions. Only the height is set
            // here; the 1dp thickness comes from the component's own token.
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            lineActions.forEach { action ->
                IconButton(onClick = action.onClickAction) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun saveImageToAppStorage(context: Context, uri: Uri): String {
    val appStorageDir = getExternalStorageDir(context)
    if (!appStorageDir.exists()) {
        appStorageDir.mkdirs()
    }
    val imageFile = File(appStorageDir, getImageName(uri))

    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    inputStream?.use { input ->
        FileOutputStream(imageFile).use { output ->
            input.copyTo(output)
        }
    }

    inputStream?.close()
    return imageFile.path.toString()
}
