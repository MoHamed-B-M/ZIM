package com.zimapp.zim.presentation.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.util.Logger
import com.zimapp.zim.presentation.popUpToTop
import com.zimapp.zim.presentation.screens.settings.settings.lock.LockScreen
import com.zimapp.zim.presentation.screens.edit.EditNoteView
import com.zimapp.zim.presentation.screens.home.HomeView
import com.zimapp.zim.presentation.screens.onboarding.OnboardingScreen
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.terms.TermsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Composable
fun AppNavHost(settingsModel: SettingsViewModel,navController: NavHostController = rememberNavController(), noteId: Int, defaultRoute: String) {

    val activity = (LocalContext.current as? Activity)
    val startRoute = if (defaultRoute != NavRoutes.LockScreen.route && noteId != -1) NavRoutes.Edit.route else defaultRoute

    NavHost(navController, startDestination = startRoute) {
        animatedComposable(NavRoutes.Home.route) {
            HomeView(
                onSettingsClicked = { navController.navigate(NavRoutes.Settings.route) },
                onNoteClicked = { id, encrypted, kind ->
                    navController.navigate(
                        NavRoutes.Edit.createRoute(
                            id,
                            encrypted,
                            kind
                        )
                    )
                },
                settingsModel = settingsModel
            )
        }

        animatedComposable(NavRoutes.Terms.route) {
            TermsScreen(
                settingsViewModel = settingsModel,
                onAgreed = {
                    // Terms used to leave the user stranded on this screen, since
                    // accepting only flipped a flag nothing was watching.
                    if (settingsModel.settings.value.onboardingComplete) {
                        navController.navigate(NavRoutes.Home.route) { popUpToTop(navController) }
                    } else {
                        navController.navigate(NavRoutes.Onboarding.route) { popUpToTop(navController) }
                    }
                }
            )
        }

        animatedComposable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                settingsViewModel = settingsModel,
                onFinished = {
                    // Re-opened from a settings screen: dismiss and keep the
                    // stack the user came from, rather than dumping the user
                    // back at the notes list.
                    val previousRoute =
                        navController.previousBackStackEntry?.destination?.route
                    when {
                        // Launched from a widget, which expects to open a note.
                        noteId != -1 -> activity?.finish()
                        previousRoute != null && settingScreens.containsKey(previousRoute) ->
                            navController.navigateUp()
                        else -> navController.navigate(NavRoutes.Home.route) { popUpToTop(navController) }
                    }
                }
            )
        }

        animatedComposable(NavRoutes.LockScreen.route) { backStackEntry ->
            val actionString = backStackEntry.arguments?.getString("type") ?: null
            val action = if (actionString == "null" || actionString == null) {
                null
            } else {
                ActionType.valueOf(actionString)
            }
            println(actionString)
            println(action)
            LockScreen(
                settingsViewModel = settingsModel,
                navController = navController,
                action = action,
            )
        }

        animatedComposable(NavRoutes.Edit.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val encrypted = backStackEntry.arguments?.getString("encrypted").toBoolean()
            val kind = backStackEntry.arguments?.getString("kind").orEmpty()
            EditNoteView(
                settingsViewModel = settingsModel,
                id = if (noteId == -1) id else noteId,
                encrypted = encrypted,
                kind = if (noteId == -1) kind else "",
                isWidget = noteId != -1
            ) {
                if (noteId == -1) {
                    navController.navigateUp()
                } else {
                    activity?.finish()
                }
            }
        }

        settingScreens.forEach { (route, screen) ->
            if (route == NavRoutes.Settings.route) {
                slideInComposable(route) {
                    screen(settingsModel, navController)
                }
            } else {
                animatedComposable(route) {
                    screen(settingsModel, navController)
                }
            }
        }
    }
}

suspend fun getDefaultRoute(
    settingsModel: SettingsViewModel,
    noteId: Int
): String {
    val routeFlow = MutableStateFlow<String?>(null)
    runBlocking {
            val route = when {
                settingsModel.settings.value.passcode != null -> NavRoutes.LockScreen.route
                !settingsModel.settings.value.termsOfService -> NavRoutes.Terms.route
                !settingsModel.settings.value.onboardingComplete -> NavRoutes.Onboarding.route
                noteId == -1 -> NavRoutes.Home.route
                else -> NavRoutes.Edit.createRoute(noteId, false)
            }
            routeFlow.value = route
        }

    return routeFlow.filterNotNull().first()
}

// Synchronous start-destination resolution for the activity. Settings are
// loaded in the SettingsViewModel constructor, so this is safe to call
// straight from onCreate.
//
// Only first-run ordering is decided here. A device with an app lock keeps its
// existing behaviour (the view model's own default route, which onResume
// re-applies) rather than being re-routed here.
fun resolveStartRoute(settingsModel: SettingsViewModel, noteId: Int): String {
    val settings = settingsModel.settings.value
    val locked = settings.passcode != null || settings.fingerprint || settings.pattern != null
    if (locked) return settingsModel.defaultRoute ?: NavRoutes.Home.route
    return when {
        !settings.termsOfService -> NavRoutes.Terms.route
        !settings.onboardingComplete -> NavRoutes.Onboarding.route
        noteId == -1 -> NavRoutes.Home.route
        else -> NavRoutes.Edit.createRoute(noteId, false)
    }
}
