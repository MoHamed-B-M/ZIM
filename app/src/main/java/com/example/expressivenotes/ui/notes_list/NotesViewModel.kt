package com.example.expressivenotes.ui.notes_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expressivenotes.domain.model.Note
import com.example.expressivenotes.domain.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val query: String = "",
    val notes: List<Note> = emptyList(),
    val showArchived: Boolean = false,
    val isGrid: Boolean = true,
    val isLoading: Boolean = true,
)

sealed interface NotesEvent { data class UndoTrash(val id: String, val title: String) : NotesEvent }

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repo: NoteRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)
    private val isGrid = MutableStateFlow(true)
    private val _events = MutableSharedFlow<NotesEvent>()
    val events = _events.asSharedFlow()

    val state: StateFlow<NotesUiState> = combine(
        query.debounce(150).distinctUntilChanged(),
        showArchived, isGrid,
    ) { q, arch, grid -> Triple(q, arch, grid) }
        .flatMapLatest { (q, arch, _) ->
            val flow = if (q.isBlank()) repo.observeNotes(arch) else repo.searchNotes(q)
            combine(flow, query, showArchived, isGrid) { notes, qq, a, g ->
                val filtered = if (qq.isBlank() && !a) notes.filter { !it.isArchived } else notes
                NotesUiState(query = qq, notes = filtered, showArchived = a, isGrid = g, isLoading = false)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onQueryChange(q: String) { query.value = q }
    fun toggleArchivedFilter() { showArchived.value = !showArchived.value }
    fun toggleLayout() { isGrid.value = !isGrid.value }
    fun togglePin(id: String) = viewModelScope.launch { repo.togglePin(id) }
    fun archive(id: String, archived: Boolean) = viewModelScope.launch { repo.setArchived(id, archived) }
    fun trash(id: String) = viewModelScope.launch {
        repo.moveToTrash(id)
        _events.emit(NotesEvent.UndoTrash(id, ""))
    }
    fun restore(id: String) = viewModelScope.launch { repo.restore(id) }
}
