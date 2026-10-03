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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.zimapp.zim.presentation.components.applyAppIcon
import com.zimapp.zim.presentation.components.currentAppIcon
import com.zimapp.zim.presentation.navigation.AppNavHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.zimapp.zim.presentation.navigation.NavRoutes
import com.zimapp.zim.presentation.navigation.resolveStartRoute
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
            // Resolved once: the activity needs a concrete start destination
            // before the graph is built, and re-deriving it on every
            // recomposition would reset the back stack.
            val startRoute = remember(settingsViewModel, noteId) {
                resolveStartRoute(settingsViewModel!!, noteId)
            }

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
                    AppNavHost(settingsViewModel!!, navController, noteId, startRoute)
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
