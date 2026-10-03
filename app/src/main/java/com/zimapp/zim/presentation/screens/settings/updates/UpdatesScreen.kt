package com.zimapp.zim.presentation.screens.settings.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zimapp.zim.R
import com.zimapp.zim.presentation.navigation.NavRoutes
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
            // Re-entry into the walkthrough for anyone who skipped it on first
            // run. Installing from here already prompts for the permission, so
            // this is about explaining it rather than requesting it.
            item {
                OutlinedButton(
                    onClick = { navController.navigate(NavRoutes.Onboarding.route) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.onboarding_revisit))
                }
            }
        }
    }
}
