package com.zimapp.zim.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zimapp.zim.R
import com.zimapp.zim.data.work.syncPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val KEY_APP_ICON = stringPreferencesKey("app_icon")

// The three launcher sets: default adaptive mipmap + the two sets from
// appicon2 / appicon3 (copied into res/ as ic_launcher_2_* / ic_launcher_3_*).
enum class AppIcon(
    val key: String,
    val label: String,
    val drawableRes: Int,
) {
    DEFAULT("default", "Midnight", R.mipmap.ic_launcher),
    ICON2("icon2", "Paper ring", R.mipmap.ic_launcher_2),
    ICON3("icon3", "Stack", R.mipmap.ic_launcher_3),
    ;

    companion object {
        fun byKey(key: String?): AppIcon = entries.firstOrNull { it.key == key } ?: DEFAULT
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
        ComponentName(pkg, "$pkg.MainActivityIcon2"),
        ComponentName(pkg, "$pkg.MainActivityIcon3"),
    )
    val target = when (icon) {
        AppIcon.DEFAULT -> ComponentName(pkg, "$pkg.MainActivity")
        AppIcon.ICON2 -> ComponentName(pkg, "$pkg.MainActivityIcon2")
        AppIcon.ICON3 -> ComponentName(pkg, "$pkg.MainActivityIcon3")
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
