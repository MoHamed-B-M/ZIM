package com.zimapp.zim.ui.settings.screens

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Report
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import org.koin.androidx.compose.koinViewModel

private const val REPO_URL = "https://github.com/MoHamed-B-M/ZIM"

@Composable
fun AboutScreen(onBack: () -> Unit, vm: AppSettingsViewModel = koinViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val pkg = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
    val debuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    SettingsScaffold(title = "About", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "Version",
                    description = "${pkg?.versionName ?: "?"} (${pkg?.let {
                        if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toString() else "?"
                    }})",
                    icon = Icons.Rounded.Info,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.TEXT,
                    customText = pkg?.versionName ?: "?",
                )
            }
            item {
                SettingsBox(
                    title = "Build type",
                    description = if (debuggable) "Debug" else "Release",
                    icon = Icons.Rounded.Build,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    actionType = ActionType.TEXT,
                    customText = if (debuggable) "Debug" else "Release",
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    title = "Latest release",
                    description = "ZIM on GitHub",
                    icon = Icons.Rounded.NewReleases,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.LINK,
                    linkClicked = { uriHandler.openUri("$REPO_URL/releases") },
                )
            }
            item {
                SettingsBox(
                    title = "Source code",
                    description = "GPL-3.0 licensed",
                    icon = Icons.Rounded.Code,
                    radius = shapeManager(radius = s.cornerRadius),
                    actionType = ActionType.LINK,
                    linkClicked = { uriHandler.openUri(REPO_URL) },
                )
            }
            item {
                SettingsBox(
                    title = "Report an issue",
                    description = "Feature requests welcome",
                    icon = Icons.Rounded.Report,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    actionType = ActionType.LINK,
                    linkClicked = { uriHandler.openUri("$REPO_URL/issues") },
                )
            }
        }
    }
}
