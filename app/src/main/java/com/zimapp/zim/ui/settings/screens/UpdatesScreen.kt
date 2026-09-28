package com.zimapp.zim.ui.settings.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zimapp.zim.ui.settings.AppUpdateSection
import com.zimapp.zim.ui.settings.easy.SettingsScaffold

@Composable
fun UpdatesScreen(onBack: () -> Unit) {
    SettingsScaffold(title = "App updates", onBack = onBack) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                androidx.compose.foundation.layout.Column(Modifier.padding(4.dp)) {
                    AppUpdateSection()
                }
            }
        }
    }
}
