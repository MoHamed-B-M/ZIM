package com.zimapp.zim.presentation.screens.settings.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zimapp.zim.presentation.screens.settings.SettingsScaffold
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.update.AppUpdateSection

@Composable
fun UpdatesScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    SettingsScaffold(
        settingsViewModel = settingsViewModel,
        title = "App updates",
        onBackNavClicked = { navController.navigateUp() },
    ) {
        LazyColumn {
            item {
                Column(Modifier.padding(bottom = 18.dp)) {
                    AppUpdateSection()
                }
            }
        }
    }
}
