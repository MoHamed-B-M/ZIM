package com.zimapp.zim.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.zimapp.zim.presentation.screens.settings.MainSettings
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.settings.AboutScreen
import com.zimapp.zim.presentation.screens.settings.settings.CloudScreen
import com.zimapp.zim.presentation.screens.settings.settings.ColorStylesScreen
import com.zimapp.zim.presentation.screens.settings.settings.LanguageScreen
import com.zimapp.zim.presentation.screens.settings.settings.MarkdownScreen
import com.zimapp.zim.presentation.screens.settings.settings.PrivacyScreen
import com.zimapp.zim.presentation.screens.settings.settings.ToolsScreen
import com.zimapp.zim.presentation.screens.settings.updates.UpdatesScreen
import com.zimapp.zim.presentation.screens.settings.updates.UpdatesScreen

enum class ActionType {
    PASSCODE,
    FINGERPRINT,
    PATTERN
}

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object Edit : NavRoutes("edit/{id}/{encrypted}") {
        fun createRoute(id: Int, encrypted : Boolean) = "edit/$id/$encrypted"
    }
    data object Terms : NavRoutes("terms")
    data object Settings : NavRoutes("settings")
    data object ColorStyles : NavRoutes("settings/color_styles")
    data object Language : NavRoutes("settings/language")
    data object Cloud : NavRoutes("settings/cloud")
    data object Privacy : NavRoutes("settings/privacy")
    data object Markdown : NavRoutes("settings/markdown")
    data object Tools : NavRoutes("settings/tools")
    data object History : NavRoutes("settings/history")
    data object Widgets : NavRoutes("settings/widgets")
    data object About : NavRoutes("settings/about")
    data object Updates : NavRoutes("settings/updates")
    data object LockScreen : NavRoutes("settings/lock/{type}") {
        fun createRoute(action: ActionType?) = "settings/lock/$action"
    }
}

val settingScreens = mapOf<String, @Composable (settingsViewModel: SettingsViewModel, navController : NavController) -> Unit>(
    NavRoutes.Settings.route to { settings, navController -> MainSettings(settings, navController) },
    NavRoutes.ColorStyles.route to { settings, navController -> ColorStylesScreen(navController,settings) },
    NavRoutes.Language.route to { settings, navController -> LanguageScreen(navController,settings) },
    NavRoutes.Cloud.route to { settings, navController -> CloudScreen(navController,settings) },
    NavRoutes.Privacy.route to { settings, navController -> PrivacyScreen(navController, settings) },
    NavRoutes.Markdown.route to { settings, navController ->  MarkdownScreen(navController,settings) },
    NavRoutes.Tools.route to { settings, navController -> ToolsScreen(navController,settings) },
    NavRoutes.About.route to { settings, navController -> AboutScreen(navController,settings) },
    NavRoutes.Updates.route to { settings, navController -> UpdatesScreen(navController,settings) },
)
