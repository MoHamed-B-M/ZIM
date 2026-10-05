package com.zimapp.zim.presentation.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// Text always goes out as EXTRA_TEXT. When the note carries voice recordings the
// clips are attached too, and the mime type follows them, because a chooser
// filtering on "text/plain" will not show apps that only accept audio.
//
// One clip goes out as ACTION_SEND. Several need ACTION_SEND_MULTIPLE with an
// ArrayList of uris — a single ACTION_SEND carrying a list is not reliably
// honoured across receivers, so the intent actually changes with the count.
fun shareNote(context: Context, text: String, audioPaths: List<String> = emptyList()) {
    // A clip can have gone missing between being recorded and being shared;
    // filter rather than let FileProvider throw on a dead path.
    val uris = audioPaths.map(::File)
        .filter { it.isFile }
        .map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }

    val send = when (uris.size) {
        0 -> Intent(Intent.ACTION_SEND).apply { type = "text/plain" }

        1 -> Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }

        else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }.apply {
        if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
        if (uris.isNotEmpty()) {
            // Some receivers read the read-grant from clipData rather than from
            // the data URI — the same reason the updater sets both. Every uri
            // has to be listed, or the ones past the first get no grant.
            clipData = ClipData.newRawUri("zim-audio", uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    // The chooser needs a NEW_TASK when the caller is not an Activity; the
    // activity context used here does not, and adding it is harmless either way.
    context.startActivity(
        Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}