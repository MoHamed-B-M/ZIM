package com.zimapp.zim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zimapp.zim.ui.navigation.Route
import com.zimapp.zim.ui.note_detail.NoteDetailScreen
import com.zimapp.zim.ui.notes_list.NotesListScreen
import com.zimapp.zim.ui.settings.CloudSyncConfigScreen
import com.zimapp.zim.ui.settings.SettingsScreen
import com.zimapp.zim.ui.theme.ExpressiveTheme

// Single-Activity + type-safe Navigation-Compose (kotlinx.serialization routes).
// Migration path to Navigation3: Route.* already @Serializable NavKeys — swap NavHost
// for NavDisplay(backStack) with zero ViewModel changes.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = Route.List) {
                    composable<Route.List> {
                        NotesListScreen(
                            onOpenNote = { id -> nav.navigate(Route.Detail(id)) },
                            onNewNote = { nav.navigate(Route.Detail(null)) },
                            onOpenSettings = { nav.navigate(Route.Settings) },
                        )
                    }
                    composable<Route.Detail> { entry ->
                        val args = entry.toRoute<Route.Detail>()
                        NoteDetailScreen(noteId = args.id, onBack = { nav.popBackStack() })
                    }
                    composable<Route.Settings> {
                        SettingsScreen(onBack = { nav.popBackStack() }, onCloudConfig = { nav.navigate(Route.CloudConfig) })
                    }
                    composable<Route.CloudConfig> {
                        CloudSyncConfigScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
