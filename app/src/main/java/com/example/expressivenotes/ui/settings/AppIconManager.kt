package com.example.expressivenotes.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.expressivenotes.R
import com.example.expressivenotes.data.work.syncPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val KEY_APP_ICON = stringPreferencesKey("app_icon")

enum class AppIcon(
    val key: String,
    val label: String,
    val drawableRes: Int,
) {
    STACK("stack", "Stacked notes", R.drawable.ic_app_stack),
    ALARM("alarm", "Reminder", R.drawable.ic_app_alarm),
    CIRCLE("circle", "Ring note", R.drawable.ic_app_circle),
    ;

    companion object {
        fun byKey(key: String?): AppIcon = entries.firstOrNull { it.key == key } ?: STACK
    }
}

fun appIconFlow(context: Context): Flow<AppIcon> =
    context.syncPrefs.data.map { AppIcon.byKey(it[KEY_APP_ICON]) }

suspend fun setAppIcon(context: Context, icon: AppIcon) {
    context.syncPrefs.edit { it[KEY_APP_ICON] = icon.key }
    applyAppIcon(context, icon)
}

// Enables the chosen LAUNCHER component, disables the other two.
// Uses DONT_KILL_APP so the switch is instant with no process restart.
fun applyAppIcon(context: Context, icon: AppIcon) {
    val pm = context.packageManager
    val pkg = context.packageName
    val all = listOf(
        ComponentName(pkg, "$pkg.MainActivity"),
        ComponentName(pkg, "$pkg.MainActivityAlarm"),
        ComponentName(pkg, "$pkg.MainActivityCircle"),
    )
    val target = when (icon) {
        AppIcon.STACK -> ComponentName(pkg, "$pkg.MainActivity")
        AppIcon.ALARM -> ComponentName(pkg, "$pkg.MainActivityAlarm")
        AppIcon.CIRCLE -> ComponentName(pkg, "$pkg.MainActivityCircle")
    }
    all.forEach { cmp ->
        pm.setComponentEnabledSetting(
            cmp,
            if (cmp == target) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
