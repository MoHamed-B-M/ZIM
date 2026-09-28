package com.zimapp.zim.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.ui.settings.easy.SectionBlock
import com.zimapp.zim.ui.settings.easy.SettingSection
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import org.koin.androidx.compose.koinViewModel

// Hub rebuilt on EasyNotes MainSettings (Support banner skipped per request).
// ZIM extras (icon picker, updater) live in their feature screens / below.
@Composable
fun SettingsScreen(
    currentTab: String = "settings",
    onTab: (String) -> Unit = {},
    onBack: () -> Unit,
    onColors: () -> Unit,
    onBackup: () -> Unit,
    onTools: () -> Unit,
    onAbout: () -> Unit,
    onUpdates: () -> Unit,
    onPrivacy: () -> Unit,
    onLanguage: () -> Unit,
    vm: AppSettingsViewModel = koinViewModel(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()

    SettingsScaffold(title = "Settings", onBack = onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = "Colors & Styles",
                            features = listOf(
                                when (s.themeMode.name) {
                                    "LIGHT" -> "Light"
                                    "DARK" -> "Dark"
                                    else -> "System"
                                } + " • Accent • Font",
                            ),
                            icon = Icons.Rounded.Palette,
                            onClick = onColors,
                        ),
                    ),
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = "Backup & Sync",
                            features = listOf("Cloud • Encrypted • JSON"),
                            icon = Icons.Rounded.Cloud,
                            onClick = onBackup,
                        ),
                    ),
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = "App updates",
                            features = listOf("Stable • Beta"),
                            icon = Icons.Rounded.SystemUpdate,
                            onClick = onUpdates,
                        ),
                    ),
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = "Privacy & Lock",
                            features = listOf("Screen • App lock"),
                            icon = Icons.Rounded.Security,
                            onClick = onPrivacy,
                        ),
                        SettingSection(
                            title = "Language",
                            features = listOf("App language • Font"),
                            icon = Icons.Rounded.Language,
                            onClick = onLanguage,
                        ),
                    ),
                )
            }
            item {
                SectionBlock(
                    listOf(
                        SettingSection(
                            title = "Tools",
                            features = listOf("Counts • Trash"),
                            icon = Icons.Rounded.Build,
                            onClick = onTools,
                        ),
                        SettingSection(
                            title = "About",
                            features = listOf("Version • Source • License"),
                            icon = Icons.Rounded.Info,
                            onClick = onAbout,
                        ),
                    ),
                )
            }
        }
    }
}
