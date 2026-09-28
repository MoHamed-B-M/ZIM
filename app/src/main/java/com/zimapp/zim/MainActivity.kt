package com.zimapp.zim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.hasRoute
import androidx.navigation.toRoute
import com.zimapp.zim.ui.navigation.BottomDockWithFab
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
// The tab is DERIVED from the back stack (single source of truth — selection can
// never desync or stack), and one dock instance is overlaid for tab destinations.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                val destTab = when {
                    entry?.destination?.hasRoute(Route.Todo::class) == true -> TAB_TODO
                    entry?.destination?.hasRoute(Route.Settings::class) == true -> TAB_SETTINGS
                    entry?.destination?.hasRoute(Route.List::class) == true -> TAB_NOTES
                    else -> null // Detail / CloudConfig keep the last tab highlighted
                }
                var lastTab by rememberSaveable { mutableStateOf(TAB_NOTES) }
                LaunchedEffect(destTab) { if (destTab != null) lastTab = destTab }
                val tab = destTab ?: lastTab

                var notesMenuOpen by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(tab) { if (tab != TAB_NOTES) notesMenuOpen = false }

                fun goTab(route: String) {
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

                Box(Modifier.fillMaxSize()) {
                    NavHost(navController = nav, startDestination = Route.List) {
                        composable<Route.List> {
                            NotesListScreen(
                                menuOpen = notesMenuOpen,
                                onMenuOpenChange = { notesMenuOpen = it },
                                onOpenNote = { id -> nav.navigate(Route.Detail(id)) },
                                onNewNote = { checklist ->
                                    nav.navigate(Route.Detail(null, checklist))
                                },
                            )
                        }
                        composable<Route.Todo> {
                            TodoScreen(
                                onOpenNote = { id -> nav.navigate(Route.Detail(id)) },
                                onNewChecklist = { nav.navigate(Route.Detail(null, true)) },
                            )
                        }
                        composable<Route.Detail> { backStackEntry ->
                            val args = backStackEntry.toRoute<Route.Detail>()
                            NoteDetailScreen(
                                noteId = args.id,
                                isChecklist = args.isChecklist,
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable<Route.Settings> {
                            SettingsScreen(
                                onBack = { nav.popBackStack() },
                                onCloudConfig = { nav.navigate(Route.CloudConfig) },
                            )
                        }
                        composable<Route.CloudConfig> {
                            CloudSyncConfigScreen(onBack = { nav.popBackStack() })
                        }
                    }

                    AnimatedVisibility(
                        visible = destTab != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        BottomDockWithFab(
                            currentRoute = tab,
                            onTab = ::goTab,
                            onFabClick = when (tab) {
                                TAB_NOTES -> ({ notesMenuOpen = !notesMenuOpen })
                                TAB_TODO -> ({ nav.navigate(Route.Detail(null, true)) })
                                else -> null
                            },
                            fabExpanded = notesMenuOpen,
                        )
                    }
                }
            }
        }
    }
}
