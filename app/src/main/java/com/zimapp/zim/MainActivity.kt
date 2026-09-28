package com.zimapp.zim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zimapp.zim.ui.navigation.Route
import com.zimapp.zim.ui.navigation.TAB_NOTES
import com.zimapp.zim.ui.navigation.TAB_SETTINGS
import com.zimapp.zim.ui.navigation.TAB_TODO
import com.zimapp.zim.ui.note_detail.NoteDetailScreen
import com.zimapp.zim.ui.notes_list.NotesListScreen
import com.zimapp.zim.ui.settings.CloudSyncConfigScreen
import com.zimapp.zim.ui.settings.SettingsScreen
import com.zimapp.zim.ui.theme.ExpressiveTheme
import com.zimapp.zim.ui.todo.TodoScreen

// Single-Activity + type-safe Navigation-Compose (kotlinx.serialization routes).
// Notes / To-Do / Settings are tab destinations with the floating bottom bar;
// Detail / CloudConfig are full-screen (no bar) and keep the last tab.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                val nav = rememberNavController()
                var tab by rememberSaveable { mutableStateOf(TAB_NOTES) }
                fun goTab(route: String) {
                    tab = route
                    val dest = when (route) {
                        TAB_TODO -> Route.Todo
                        TAB_SETTINGS -> Route.Settings
                        else -> Route.List
                    }
                    nav.navigate(dest) {
                        popUpTo(Route.List) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                NavHost(navController = nav, startDestination = Route.List) {
                    composable<Route.List> {
                        NotesListScreen(
                            currentTab = tab,
                            onTab = ::goTab,
                            onOpenNote = { id -> nav.navigate(Route.Detail(id)) },
                            onNewNote = { checklist ->
                                nav.navigate(Route.Detail(null, checklist))
                            },
                        )
                    }
                    composable<Route.Todo> {
                        TodoScreen(
                            currentTab = tab,
                            onTab = ::goTab,
                            onOpenNote = { id -> nav.navigate(Route.Detail(id)) },
                            onNewChecklist = { nav.navigate(Route.Detail(null, true)) },
                        )
                    }
                    composable<Route.Detail> { entry ->
                        val args = entry.toRoute<Route.Detail>()
                        NoteDetailScreen(
                            noteId = args.id,
                            isChecklist = args.isChecklist,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable<Route.Settings> {
                        SettingsScreen(
                            currentTab = tab,
                            onTab = ::goTab,
                            onBack = { nav.popBackStack() },
                            onCloudConfig = { nav.navigate(Route.CloudConfig) },
                        )
                    }
                    composable<Route.CloudConfig> {
                        CloudSyncConfigScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
