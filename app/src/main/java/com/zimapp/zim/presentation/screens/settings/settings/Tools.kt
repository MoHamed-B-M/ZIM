package com.zimapp.zim.presentation.screens.settings.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zimapp.zim.R
import com.zimapp.zim.presentation.screens.settings.SettingsScaffold
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.widgets.ActionType
import com.zimapp.zim.presentation.screens.settings.widgets.SettingsBox


@Composable
fun ToolsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    settingsViewModel.noteUseCase.observe()
    SettingsScaffold(
        settingsViewModel = settingsViewModel,
        title = stringResource(id = R.string.tools),
        onBackNavClicked = { navController.navigateUp() }
    ) {
        LazyColumn {
            item {
                SettingsBox(
                    settingsViewModel = settingsViewModel,
                    title = stringResource(id = R.string.notes),
                    description = settingsViewModel.noteUseCase.notes.size.toString(),
                    icon = Icons.Rounded.Build,
                    actionType = ActionType.TEXT,
                    radius = shapeManager(isBoth = true, radius = settingsViewModel.settings.value.cornerRadius)
                )
            }
        }
    }
}