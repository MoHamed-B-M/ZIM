package com.example.expressivenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expressivenotes.ui.navigation.Route
import com.example.expressivenotes.ui.note_detail.NoteDetailScreen
import com.example.expressivenotes.ui.notes_list.NotesListScreen
import com.example.expressivenotes.ui.settings.CloudSyncConfigScreen
import com.example.expressivenotes.ui.settings.SettingsScreen
import com.example.expressivenotes.ui.theme.ExpressiveTheme

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
