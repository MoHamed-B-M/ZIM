package com.zimapp.zim.presentation.screens.settings.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zimapp.zim.R
import com.zimapp.zim.core.constant.ConnectionConst
import com.zimapp.zim.presentation.screens.settings.SettingsScaffold
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.update.AppUpdateSection
import com.zimapp.zim.presentation.screens.settings.widgets.ActionType
import com.zimapp.zim.presentation.screens.settings.widgets.SettingsBox


@Composable
fun AboutScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val uriHandler = LocalUriHandler.current
    SettingsScaffold(
        settingsViewModel = settingsViewModel,
        title = stringResource(id = R.string.about),
        onBackNavClicked = { navController.navigateUp() }
    ) {
        LazyColumn {
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(bottom = 18.dp),
                ) {
                    AppUpdateSection()
                }
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.build_type),
                    description = settingsViewModel.build,
                    icon = Icons.Rounded.Build,
                    actionType = ActionType.TEXT,
                    radius = shapeManager(isFirst = true, radius = settingsViewModel.settings.value.cornerRadius)
                )
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.version),
                    description = settingsViewModel.version,
                    icon = Icons.Rounded.Info,
                    actionType = ActionType.TEXT,
                    radius = shapeManager(isLast = true, radius = settingsViewModel.settings.value.cornerRadius),
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.latest_release),
                    icon = Icons.Rounded.Verified,
                    actionType = ActionType.LINK,
                    radius = shapeManager(isFirst = true, radius = settingsViewModel.settings.value.cornerRadius),
                    linkClicked = { uriHandler.openUri("https://github.com/Kin69/EasyNotes/releases") }
                )
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.source_code),
                    icon = Icons.Rounded.Download,
                    actionType = ActionType.LINK,
                    radius = shapeManager(isLast = true, radius = settingsViewModel.settings.value.cornerRadius),
                    linkClicked = { uriHandler.openUri("https://github.com/Kin69/EasyNotes/releases") }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.email),
                    icon = Icons.Rounded.Email,
                    clipboardText = ConnectionConst.SUPPORT_MAIL,
                    actionType = ActionType.CLIPBOARD,
                    radius = shapeManager(isFirst = true, radius = settingsViewModel.settings.value.cornerRadius),
                )
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    size = 8.dp,
                    title = stringResource(id = R.string.discord),
                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                    actionType = ActionType.LINK,
                    linkClicked = { uriHandler.openUri(ConnectionConst.SUPPORT_DISCORD) },
                    radius = shapeManager(radius = settingsViewModel.settings.value.cornerRadius),
                )
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    size = 8.dp,
                    title = stringResource(id = R.string.instagram_fastest_support),
                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                    actionType = ActionType.LINK,
                    linkClicked = { uriHandler.openUri(ConnectionConst.SUPPORT_INSTAGRAM) },
                    radius = shapeManager(radius = settingsViewModel.settings.value.cornerRadius),
                )
            }
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    size = 8.dp,
                    title = stringResource(id = R.string.feature),
                    icon = Icons.Rounded.BugReport,
                    linkClicked = { uriHandler.openUri(ConnectionConst.GITHUB_FEATURE_REQUEST) },
                    actionType = ActionType.LINK,
                    radius = shapeManager(isLast = true, radius = settingsViewModel.settings.value.cornerRadius),
                )
            }
        }
    }

}


