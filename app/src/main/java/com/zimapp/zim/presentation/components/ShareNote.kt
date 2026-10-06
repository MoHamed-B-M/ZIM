package com.zimapp.zim.presentation.components

import android.content.Context
import android.content.Intent

// Text goes out as EXTRA_TEXT on a plain ACTION_SEND.
fun shareNote(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
    }
    // The chooser needs a NEW_TASK when the caller is not an Activity; the
    // activity context used here does not, and adding it is harmless either way.
    context.startActivity(
        Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
