package com.zimapp.zim.ui.settings.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.ListDialog
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import org.koin.androidx.compose.koinViewModel

// Subset of EasyNotes locales (its locales_config has 22; these 12 cover the
// vast majority). System default clears the override.
private val APP_LOCALES = listOf(
    "System default" to "",
    "English" to "en",
    "Arabic" to "ar",
    "German" to "de",
    "Spanish" to "es",
    "French" to "fr",
    "Hindi" to "hi",
    "Italian" to "it",
    "Japanese" to "ja",
    "Dutch" to "nl",
    "Portuguese" to "pt",
    "Russian" to "ru",
    "Chinese" to "zh",
)

@Composable
fun LanguageScreen(onBack: () -> Unit, vm: AppSettingsViewModel = koinViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    val currentTag = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: ""

    if (showPicker) {
        ListDialog(
            text = "Language",
            list = APP_LOCALES,
            initialItem = APP_LOCALES.first(),
            onExit = { showPicker = false },
            extractDisplayData = { it },
            setting = { (name, tag) ->
                SettingsBox(
                    title = name,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.RADIOBUTTON,
                    variable = if (tag.isNotBlank()) currentTag == tag else currentTag.isBlank(),
                    switchEnabled = {
                        if (tag.isNotBlank()) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        else AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        showPicker = false
                    },
                )
            },
        )
    }

    SettingsScaffold(title = "Language", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "App language",
                    description = APP_LOCALES.firstOrNull { it.second == currentTag }?.first ?: "System default",
                    icon = Icons.Rounded.Translate,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { showPicker = true },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    title = "Monospace font",
                    description = "Use monospace in the editor",
                    icon = Icons.Rounded.FontDownload,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.SWITCH,
                    variable = s.monospaceFont,
                    switchEnabled = { vm.update { it.copy(monospaceFont = it) } },
                )
            }
        }
    }
}
