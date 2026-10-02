package com.zimapp.zim.presentation

import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zimapp.zim.presentation.components.navigation.ExpressiveFloatingBottomBar
import com.zimapp.zim.presentation.components.navigation.TAB_NOTES
import com.zimapp.zim.presentation.components.navigation.TAB_SETTINGS
import com.zimapp.zim.presentation.navigation.AppNavHost
import com.zimapp.zim.presentation.components.applyAppIcon
import com.zimapp.zim.presentation.components.currentAppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.zimapp.zim.presentation.navigation.NavRoutes
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.theme.EasyNotesTheme
import dagger.hilt.android.AndroidEntryPoint

fun NavOptionsBuilder.popUpToTop(navController: NavController) {
    popUpTo(navController.currentBackStackEntry?.destination?.route ?: return) {
        inclusive =  true
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavHostController
    private var settingsViewModel: SettingsViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            settingsViewModel = hiltViewModel<SettingsViewModel>()
            val noteId = intent?.getIntExtra("noteId", -1) ?: -1

            // Re-apply the selected launcher icon (aliases don't survive reinstalls).
            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    applyAppIcon(this@MainActivity, currentAppIcon(this@MainActivity))
                }
            }

            if (settingsViewModel!!.settings.value.gallerySync) {
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    settingsViewModel!!.galleryObserver
                )
            }

            EasyNotesTheme(settingsViewModel!!) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                ) {
                    navController = rememberNavController()
                    val entry by navController.currentBackStackEntryAsState()
                    val route = entry?.destination?.route
                    // Single-destination tabs: home + settings hub. Everything
                    // else (edit, lock, sub-screens) is full-screen, no bar.
                    val tab = when {
                        route == NavRoutes.Home.route -> TAB_NOTES
                        route == NavRoutes.Settings.route ||
                            (route?.startsWith("settings/") == true) -> TAB_SETTINGS
                        else -> null
                    }
                    Box(Modifier.fillMaxSize()) {
                        AppNavHost(settingsViewModel!!, navController, noteId, settingsViewModel!!.defaultRoute!!)
                        if (tab != null) {
                            ExpressiveFloatingBottomBar(
                                currentRoute = tab,
                                onNavigate = { selected ->
                                    val dest = if (selected == TAB_NOTES) {
                                        NavRoutes.Home.route
                                    } else {
                                        NavRoutes.Settings.route
                                    }
                                    navController.navigate(dest) {
                                        popUpTo(NavRoutes.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel?.let {
            it.loadDefaultRoute()
            if (it.defaultRoute != NavRoutes.Home.route) {
                if (it.settings.value.passcode != null || it.settings.value.fingerprint || it.settings.value.pattern != null) {
                    if (it.settings.value.lockImmediately) {
                        navController.navigate(it.defaultRoute!!) { popUpToTop(navController) }
                    }
                }
            }
        }
    }
}
