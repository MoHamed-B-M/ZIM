package com.zimapp.zim.presentation.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// Text always goes out as EXTRA_TEXT. When the note carries a voice recording
// the clip is attached too, and the mime type follows it, because a chooser
// filtering on "text/plain" will not show apps that only accept audio.
fun shareNote(context: Context, text: String, audioPath: String? = null) {
    val audioFile = audioPath?.let(::File)?.takeIf { it.isFile }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = if (audioFile != null) "audio/*" else "text/plain"
        if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
        if (audioFile != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile,
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            // Some receivers read the read-grant from clipData rather than from
            // the data URI — the same reason the updater sets both.
            clipData = ClipData.newRawUri("zim-audio", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    // The chooser needs a NEW_TASK when the caller is not an Activity; the
    // activity context used here does not, and adding it is harmless either way.
    context.startActivity(
        Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}