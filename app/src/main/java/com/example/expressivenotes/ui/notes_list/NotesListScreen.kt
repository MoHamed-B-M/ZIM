package com.example.expressivenotes.ui.notes_list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expressivenotes.domain.model.Note
import com.example.expressivenotes.ui.notes_list.components.ExpressiveSearchInput
import com.example.expressivenotes.ui.notes_list.components.NoteCard
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// M3 Expressive showcase (grounded in Compose-Material-3-Expressive-Catalog):
// - AppBarWithSearch + contained search (SearchBarSamples.kt)
// - Staggered grid of ElevatedCards (CardSamples.kt)
// - SwipeToDismissBox archive/delete (SwipeToDismissSamples.kt)
// - FloatingActionButtonMenu quick-create (FloatingActionButtonMenuSamples.kt)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onOpenNote: (String) -> Unit,
    onNewNote: (isChecklist: Boolean) -> Unit = {},
    onOpenSettings: () -> Unit,
    vm: NotesViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val searchState = rememberContainedSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val appBarColors = SearchBarDefaults.appBarWithSearchColors()
    val inputField = @Composable {
        ExpressiveSearchInput(query = state.query, onQuery = vm::onQueryChange)
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            if (e is NotesEvent.UndoTrash) {
                val r = snackbar.showSnackbar("Note moved to trash", actionLabel = "Undo")
                if (r == SnackbarResult.ActionPerformed) vm.restore(e.id)
            }
        }
    }

    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabExpanded) { fabExpanded = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppBarWithSearch(
                scrollBehavior = scrollBehavior,
                state = searchState,
                colors = appBarColors,
                inputField = inputField,
                navigationIcon = {},
                actions = {
                    IconButton(onClick = vm::toggleLayout) {
                        Icon(if (state.isGrid) Icons.Filled.ViewList else Icons.Filled.GridView, contentDescription = "Toggle layout")
                    }
                    IconButton(onClick = vm::toggleArchivedFilter) {
                        Icon(Icons.Filled.Archive, contentDescription = "Archived",
                            tint = if (state.showArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
            ExpandedFullScreenContainedSearchBar(state = searchState, inputField = inputField) { }
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier = Modifier.animateFloatingActionButton(
                            visible = true, alignment = Alignment.BottomEnd,
                        ),
                        checked = fabExpanded,
                        onCheckedChange = { fabExpanded = !fabExpanded },
                    ) {
                        val vec by remember {
                            derivedStateOf { if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add }
                        }
                        Icon(rememberVectorPainter(vec), contentDescription = null, Modifier.animateIcon({ checkedProgress }))
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; onNewNote(false) },
                    icon = { Icon(Icons.Filled.Edit, null) }, text = { Text("New note") },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; onNewNote(true) },
                    icon = { Icon(Icons.Filled.Checklist, null) }, text = { Text("Checklist") },
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(0.5f))
            }
        } else if (state.notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (state.query.isBlank()) "No notes yet — tap + to create one"
                    else "No matches for \"${state.query}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = if (state.isGrid) StaggeredGridCells.Adaptive(160.dp) else StaggeredGridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, start = 12.dp, end = 12.dp, bottom = 96.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.notes, key = { it.id }) { note ->
                    DismissibleNote(note = note, query = state.query,
                        onClick = { onOpenNote(note.id) },
                        onPin = { vm.togglePin(note.id) },
                        onArchive = { vm.archive(note.id, !note.isArchived) },
                        onTrash = { vm.trash(note.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleNote(
    note: Note, query: String,
    onClick: () -> Unit, onPin: () -> Unit, onArchive: () -> Unit, onTrash: () -> Unit,
) {
    // Swipe: start→end = pin, end→start = trash (catalog: SwipeToDismissSamples.kt).
    // Uses default expressive motion; reset() returns with spring, no custom threshold.
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }, label = "swipe-bg",
            )
            val icon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.PushPin
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                else -> Icons.Filled.Archive
            }
            Box(Modifier.fillMaxSize().background(color, MaterialTheme.shapes.medium).padding(16.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart) {
                Icon(icon, contentDescription = null)
            }
        },
        onDismiss = { dir ->
            when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> { onPin(); scope.launch { dismissState.reset() } }
                SwipeToDismissBoxValue.EndToStart -> onTrash()
                else -> {}
            }
        },
    ) {
        NoteCard(note = note, query = query, onClick = onClick)
    }
}
