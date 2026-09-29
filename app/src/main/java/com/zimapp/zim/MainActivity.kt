package com.zimapp.zim

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zimapp.zim.domain.model.AppSettings
import com.zimapp.zim.domain.model.LockType
import com.zimapp.zim.domain.model.ThemeMode
import com.zimapp.zim.domain.repository.AppSettingsRepository
import com.zimapp.zim.ui.navigation.BottomDockWithFab
import com.zimapp.zim.ui.navigation.Route
import com.zimapp.zim.ui.navigation.TAB_NOTES
import com.zimapp.zim.ui.navigation.TAB_SETTINGS
import com.zimapp.zim.ui.navigation.TAB_TODO
import com.zimapp.zim.ui.note_detail.NoteDetailScreen
import com.zimapp.zim.ui.notes_list.NotesListScreen
import com.zimapp.zim.ui.settings.CloudSyncConfigScreen
import com.zimapp.zim.ui.settings.SettingsScreen
import com.zimapp.zim.ui.settings.lock.LockSetupScreen
import com.zimapp.zim.ui.settings.lock.LockSetupScreen
import com.zimapp.zim.ui.settings.screens.AboutScreen
import com.zimapp.zim.ui.settings.screens.BackupScreen
import com.zimapp.zim.ui.settings.screens.ColorsScreen
import com.zimapp.zim.ui.settings.screens.LanguageScreen
import com.zimapp.zim.ui.settings.screens.PrivacyScreen
import com.zimapp.zim.ui.settings.screens.ToolsScreen
import com.zimapp.zim.ui.settings.screens.UpdatesScreen
import com.zimapp.zim.ui.theme.ExpressiveTheme
import com.zimapp.zim.ui.todo.TodoScreen
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// Single-Activity + type-safe Navigation-Compose (kotlinx.serialization routes).
// The tab is DERIVED from the back stack (single source of truth — selection can
// never desync or stack), and one dock instance is overlaid for tab destinations.
// AppSettings drives theme/seed, FLAG_SECURE, and the app-lock gate.
class MainActivity : AppCompatActivity() {
    private val settingsRepo: AppSettingsRepository by inject()
    private val widgetNewNote = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetNewNote.value = intent?.getBooleanExtra(EXTRA_NEW_NOTE, false) == true
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(settings.screenProtection) {
                if (settings.screenProtection) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            var unlocked by rememberSaveable { mutableStateOf(false) }
            val locked = settings.lockType != LockType.NONE && !unlocked

            ExpressiveTheme(
                darkTheme = dark,
                dynamicColor = settings.dynamicColor,
                seedColor = settings.seedColor,
                fontSize = settings.fontSize,
                cornerRadius = settings.cornerRadius,
            ) {
                if (locked) {
                    LockSetupScreen(
                        type = settings.lockType,
                        expected = settings.lockSecret,
                        hasLock = true,
                        onPinEntered = {},
                        onPatternEntered = {},
                        onUnlock = { unlocked = true },
                        onCancel = { finish() },
                    )
                    return@ExpressiveTheme
                }

                val nav = rememberNavController()
                LaunchedEffect(widgetNewNote.value) {
                    if (widgetNewNote.value) {
                        widgetNewNote.value = false
                        nav.navigate(Route.Detail(null, false))
                    }
                }
                val entry by nav.currentBackStackEntryAsState()
                // Route strings for type-safe destinations are the qualified
                // class names; match on the simple name (null-safe on all variants).
                val destTab = when (entry?.destination?.route?.substringAfterLast('.')) {
                    "Todo" -> TAB_TODO
                    "Settings" -> TAB_SETTINGS
                    "List" -> TAB_NOTES
                    else -> null // Detail / settings subscreens keep the last tab highlighted
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

                fun saveLock(type: LockType, secret: String?) {
                    lifecycleScope.launch {
                        settingsRepo.update { it.copy(lockType = type, lockSecret = secret) }
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
                                onColors = { nav.navigate(Route.Colors) },
                                onBackup = { nav.navigate(Route.Backup) },
                                onTools = { nav.navigate(Route.Tools) },
                                onAbout = { nav.navigate(Route.About) },
                                onUpdates = { nav.navigate(Route.Updates) },
                                onPrivacy = { nav.navigate(Route.Privacy) },
                                onLanguage = { nav.navigate(Route.Language) },
                            )
                        }
                        composable<Route.Colors> {
                            ColorsScreen(onBack = { nav.popBackStack() })
                        }
                        composable<Route.Backup> {
                            BackupScreen(
                                onBack = { nav.popBackStack() },
                                onCloudConfig = { nav.navigate(Route.CloudConfig) },
                            )
                        }
                        composable<Route.Privacy> {
                            PrivacyScreen(
                                onBack = { nav.popBackStack() },
                                onLockSetup = { t -> nav.navigate(Route.LockSetup(t.name)) },
                            )
                        }
                        composable<Route.Language> {
                            LanguageScreen(onBack = { nav.popBackStack() })
                        }
                        composable<Route.LockSetup> { backStackEntry ->
                            val t = runCatching {
                                LockType.valueOf(backStackEntry.toRoute<Route.LockSetup>().type)
                            }.getOrDefault(LockType.NONE)
                            LockSetupScreen(
                                type = t,
                                expected = null,
                                hasLock = false,
                                onPinEntered = { pin ->
                                    saveLock(LockType.PASSCODE, pin)
                                    nav.popBackStack()
                                },
                                onPatternEntered = { pattern ->
                                    saveLock(LockType.PATTERN, pattern)
                                    nav.popBackStack()
                                },
                                onUnlock = {
                                    saveLock(LockType.FINGERPRINT, null)
                                    nav.popBackStack()
                                },
                                onCancel = { nav.popBackStack() },
                            )
                        }
                        composable<Route.Tools> {
                            ToolsScreen(onBack = { nav.popBackStack() })
                        }
                        composable<Route.About> {
                            AboutScreen(onBack = { nav.popBackStack() })
                        }
                        composable<Route.Updates> {
                            UpdatesScreen(onBack = { nav.popBackStack() })
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
                            onPickNote = {
                                notesMenuOpen = false
                                nav.navigate(Route.Detail(null, false))
                            },
                            onPickChecklist = {
                                notesMenuOpen = false
                                nav.navigate(Route.Detail(null, true))
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetNewNote.value = intent.getBooleanExtra(EXTRA_NEW_NOTE, false)
    }

    companion object {
        const val EXTRA_NEW_NOTE = "zim_new_note"
    }
}
