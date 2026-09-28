package com.zimapp.zim.ui.settings.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.repository.NoteRepository
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    vm: AppSettingsViewModel = koinViewModel(),
    repo: NoteRepository = koinInject(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val notes by repo.observeNotes(includeArchived = true).collectAsStateWithLifecycle(initialValue = emptyList())
    val trash by repo.observeTrash().collectAsStateWithLifecycle(initialValue = emptyList())

    SettingsScaffold(title = "Tools", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "Notes",
                    description = "${notes.size} total • ${trash.size} in trash",
                    icon = Icons.Rounded.Build,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.TEXT,
                    customText = notes.size.toString(),
                )
            }
            item {
                SettingsBox(
                    title = "Empty trash now",
                    description = "Permanently deletes ${trash.size} trashed notes",
                    icon = Icons.Rounded.DeleteSweep,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    customAction = {
                        CoroutineScope(Dispatchers.IO).launch {
                            repo.purgeTrashOlderThan(System.currentTimeMillis() + 1)
                        }
                    },
                )
            }
        }
    }
}
