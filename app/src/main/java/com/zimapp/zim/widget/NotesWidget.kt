package com.zimapp.zim.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zimapp.zim.MainActivity
import com.zimapp.zim.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

// Shows the most recently updated note; tap opens the app.
// (Simplified from EasyNotes' per-widget note picker — no config activity.)
class NotesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo: NoteRepository = GlobalContext.get().get()
        val note = repo.observeNotes(includeArchived = false).first().firstOrNull()
        provideContent {
            GlanceTheme {
                val ctx = LocalContext.current
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp)
                        .clickable {
                            ctx.startActivity(
                                Intent(ctx, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                },
                            )
                        },
                ) {
                    if (note == null) {
                        Text(
                            text = "No notes yet — open ZIM",
                            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.primary),
                        )
                    } else {
                        if (note.title.isNotBlank()) {
                            Text(
                                text = note.title,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.primary,
                                ),
                                maxLines = 2,
                            )
                        }
                        if (note.content.isNotBlank()) {
                            Text(
                                text = note.content.take(300),
                                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onBackground),
                                maxLines = 8,
                                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}
