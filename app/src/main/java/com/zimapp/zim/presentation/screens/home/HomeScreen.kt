package com.zimapp.zim.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zimapp.zim.R
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.presentation.components.CloseButton
import com.zimapp.zim.presentation.components.DeleteButton
import com.zimapp.zim.presentation.components.material.MaterialScaffold
import com.zimapp.zim.presentation.components.material.MaterialButton
import com.zimapp.zim.presentation.components.PinButton
import com.zimapp.zim.presentation.components.SelectAllButton
import com.zimapp.zim.presentation.components.SettingsButton
import com.zimapp.zim.presentation.components.TitleText
import com.zimapp.zim.presentation.components.VaultButton
import com.zimapp.zim.presentation.components.defaultScreenEnterAnimation
import com.zimapp.zim.presentation.components.defaultScreenExitAnimation
import com.zimapp.zim.presentation.screens.edit.model.TEMPLATE_TODO
import com.zimapp.zim.presentation.screens.home.viewmodel.HomeViewModel
import com.zimapp.zim.presentation.screens.home.widgets.NoteFilter
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.settings.PasswordPrompt
import com.zimapp.zim.presentation.screens.settings.settings.shapeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeView (
    viewModel: HomeViewModel = hiltViewModel(),
    settingsModel: SettingsViewModel,
    onSettingsClicked: () -> Unit,
    onNoteClicked: (Int, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    if (viewModel.isPasswordPromptVisible.value) {
        PasswordPrompt(
            context = context,
            text = stringResource(id = R.string.password_continue),
            settingsViewModel = settingsModel,
            onExit = { password ->
                if (password != null) {
                    if (password.text.isNotBlank()) {
                        viewModel.encryptionHelper.setPassword(password.text)
                        viewModel.noteUseCase.observe()
                    }
                }
                viewModel.toggleIsPasswordPromptVisible(false)
            }
        )
    }

    if (settingsModel.databaseUpdate.value) viewModel.noteUseCase.observe()
    val containerColor = getContainerColor(settingsModel)
    MaterialScaffold(
        floatingActionButton = {
            NewNoteMenu(
                onNewNote = { onNoteClicked(0, viewModel.isVaultMode.value, "") },
                onNewToDo = { onNoteClicked(0, viewModel.isVaultMode.value, TEMPLATE_TODO) },
            )
        },
        topBar = {
            AnimatedVisibility(
                visible = viewModel.selectedNotes.isNotEmpty(),
                enter = defaultScreenEnterAnimation(),
                exit = defaultScreenExitAnimation()
            ) {
                SelectedNotesTopAppBar(
                    selectedNotes = viewModel.selectedNotes,
                    allNotes = viewModel.getAllNotes(),
                    settingsModel = settingsModel,
                    onPinClick = { viewModel.pinOrUnpinNotes() },
                    onDeleteClick = { viewModel.toggleIsDeleteMode(true) },
                    onSelectAllClick = { selectAllNotes(viewModel, viewModel.getAllNotes()) },
                    onCloseClick = { viewModel.selectedNotes.clear() }
                )
            }
            AnimatedVisibility(
                viewModel.selectedNotes.isEmpty(),
                enter = defaultScreenEnterAnimation(),
                exit = defaultScreenExitAnimation()
            ) {
                NotesSearchBar(
                    settingsModel = settingsModel,
                    query = viewModel.searchQuery.value,
                    onQueryChange = { viewModel.changeSearchQuery(it) },
                    onSettingsClick = onSettingsClicked,
                    onClearClick = { viewModel.changeSearchQuery("") },
                    viewModel = viewModel,
                    onVaultClicked = {
                        if (!viewModel.isVaultMode.value) {
                            viewModel.toggleIsPasswordPromptVisible(true)
                        } else {
                            viewModel.toggleIsVaultMode(false)
                            viewModel.encryptionHelper.removePassword()
                        }
                    }
                )
            }
        },
        content = {
            var refreshing by remember { mutableStateOf(false) }
            val refreshScope = rememberCoroutineScope()
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    refreshScope.launch {
                        viewModel.noteUseCase.observe()
                        delay(800)
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
            NoteFilter(
                settingsViewModel = settingsModel,
                containerColor = containerColor,
                shape = shapeManager(
                    radius = settingsModel.settings.value.cornerRadius / 2,
                    isBoth = true
                ),
                onNoteClicked = { onNoteClicked(it, viewModel.isVaultMode.value, "")  },
                notes = viewModel.getAllNotes().sortedWith(sorter(settingsModel.settings.value.sortDescending)),
                selectedNotes = viewModel.selectedNotes,
                viewMode = settingsModel.settings.value.viewMode,
                searchText = viewModel.searchQuery.value.ifBlank { null },
                isDeleteMode = viewModel.isDeleteMode.value,
                onNoteUpdate = { note -> CoroutineScope(Dispatchers.IO).launch {viewModel.noteUseCase.addNote(note) } },
                onDeleteNote = {
                    viewModel.toggleIsDeleteMode(false)
                    viewModel.noteUseCase.deleteNoteById(it)
                },
            )
            }
        }
    )
}

@Composable
fun getContainerColor(settingsModel: SettingsViewModel): Color {
    return if (settingsModel.settings.value.extremeAmoledMode) Color.Black else MaterialTheme.colorScheme.surfaceContainer
}

// M3 FAB menu: a collapsed FAB that expands into labelled actions. Kept as
// an AnimatedVisibility pair rather than a modal menu so the labels stay
// readable and the whole thing composes without extra dependencies.
@Composable
private fun NewNoteMenu(
    onNewNote: () -> Unit,
    onNewToDo: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.imePadding(),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            NewNoteMenuItem(
                icon = Icons.Rounded.CheckBox,
                label = stringResource(R.string.new_todo),
                onClick = onNewToDo,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            NewNoteMenuItem(
                icon = Icons.Rounded.Edit,
                label = stringResource(R.string.new_note),
                onClick = onNewNote,
            )
        }
        ExtendedFloatingActionButton(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(if (expanded) Icons.Rounded.Close else Icons.Rounded.Add, null) },
            text = {
                Text(
                    text = stringResource(if (expanded) R.string.cancel else R.string.new_note)
                )
            },
        )
    }
}

@Composable
private fun NewNoteMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        // Flat: these sit stacked over the note grid, where the default token
        // elevation draws a shadow under every item and the cluster reads as
        // muddy. Press feedback comes from the container colour change instead.
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        icon = { Icon(icon, null) },
        text = { Text(text = label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedNotesTopAppBar(
    selectedNotes: List<Note>,
    allNotes: List<Note>,
    settingsModel: SettingsViewModel,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    var deletelaert by remember {
        mutableStateOf(false)
    }
    AnimatedVisibility(visible = deletelaert) {
        AlertDialog(onDismissRequest = { deletelaert = false }, title = {
            Text(
                text = stringResource(id = R.string.alert_text)
            )
        }, confirmButton = {
            TextButton(onClick = { deletelaert=false
                onDeleteClick()
            }) {
                Text(text = stringResource(id = R.string.yes), color = MaterialTheme.colorScheme.error )
            }
        },
            dismissButton = {
                TextButton(onClick = { deletelaert = false }) {
                    Text(text =stringResource(id = R.string.cancel))
                }
            })

    }
    TopAppBar(
        modifier = Modifier.padding(bottom = 36.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        title = { TitleText(titleText = selectedNotes.size.toString()) },
        navigationIcon = { CloseButton(onCloseClicked = onCloseClick) },
        actions = {
            Row {
                PinButton(isPinned = selectedNotes.all { it.pinned }, onClick = onPinClick)
                DeleteButton(onClick =  {deletelaert = true})
                SelectAllButton(
                    enabled = selectedNotes.size != allNotes.size,
                    onClick = onSelectAllClick
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSearchBar(
    settingsModel: SettingsViewModel,
    viewModel: HomeViewModel,
    query: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onVaultClicked: () -> Unit,
    onClearClick: () -> Unit
) {
    val isTyping = query.isNotBlank()
    val searchBarScale by animateFloatAsState(
        targetValue = if (isTyping) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "searchBarScale"
    )
    val horizontalPadding = if (settingsModel.settings.value.makeSearchBarLonger) 20.dp else 32.dp

    SearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontalPadding, 16.dp, horizontalPadding, 18.dp)
            .scale(searchBarScale),
        query = query,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
        trailingIcon = {
            Row(
                modifier = Modifier.padding(end = 6.dp)
            ) {
                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        ),
                        initialOffsetX = { it / 2 }
                    ) + fadeIn(
                        animationSpec = tween(250)
                    ) + scaleIn(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        ),
                        initialScale = 0.7f
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(200),
                        targetOffsetX = { it / 2 }
                    ) + fadeOut(
                        animationSpec = tween(200)
                    ) + scaleOut(
                        animationSpec = tween(200),
                        targetScale = 0.7f
                    )
                ) {
                    MaterialButton(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear"
                    ) {
                        onClearClick()
                    }
                }

                AnimatedVisibility(
                    visible = query.isBlank(),
                    enter = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        ),
                        initialOffsetX = { -it / 2 }
                    ) + fadeIn(
                        animationSpec = tween(250)
                    ) + scaleIn(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        ),
                        initialScale = 0.7f
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(200),
                        targetOffsetX = { -it / 2 }
                    ) + fadeOut(
                        animationSpec = tween(200)
                    ) + scaleOut(
                        animationSpec = tween(200),
                        targetScale = 0.7f
                    )
                ) {
                    Row {
                        if (settingsModel.settings.value.vaultSettingEnabled) {
                            VaultButton(viewModel.isVaultMode.value) { onVaultClicked() }
                        }
                        SettingsButton(onSettingsClicked = onSettingsClick)
                    }
                }
            }
        },
        onQueryChange = onQueryChange,
        onSearch = onQueryChange,
        onActiveChange = {},
        active = false,
        colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {}
}

private fun selectAllNotes(viewModel: HomeViewModel, allNotes: List<Note>) {
    allNotes.forEach {
        if (!viewModel.selectedNotes.contains(it)) {
            viewModel.selectedNotes.add(it)
        }
    }
}

fun sorter(descending: Boolean): Comparator<Note> {
    return if (descending) {
        compareByDescending { it.createdAt }
    } else {
        compareBy { it.createdAt }
    }
}