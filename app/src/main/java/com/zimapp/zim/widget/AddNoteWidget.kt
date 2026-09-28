package com.zimapp.zim.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import android.content.Intent
import com.zimapp.zim.MainActivity

// 1x1 button widget: tap opens a blank note.
class AddNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val ctx = LocalContext.current
                val intent = Intent(ctx, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_NEW_NOTE, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                Row(
                    modifier = GlanceModifier.fillMaxSize().height(50.dp).width(100.dp)
                        .background(GlanceTheme.colors.primary)
                        .clickable { ctx.startActivity(intent) }
                        .cornerRadius(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "+ Note",
                        style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.background),
                    )
                }
            }
        }
    }
}
