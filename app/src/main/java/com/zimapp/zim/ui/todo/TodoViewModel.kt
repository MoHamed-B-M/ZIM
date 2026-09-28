package com.zimapp.zim.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val TASK_LINE = Regex("""^-\s*\[( |x|X)\]\s?(.*)$""")

data class TaskLine(val lineIndex: Int, val text: String, val done: Boolean)
data class TodoNote(val note: Note, val tasks: List<TaskLine>)

class TodoViewModel(private val repo: NoteRepository) : ViewModel() {
    val state: StateFlow<List<TodoNote>> = repo.observeNotes(includeArchived = false)
        .map { notes ->
            notes.mapNotNull { n ->
                val tasks = n.content.lines().mapIndexedNotNull { i, line ->
                    TASK_LINE.matchEntire(line.trim())?.let { m ->
                        TaskLine(i, m.groupValues[2], m.groupValues[1].lowercase() == "x")
                    }
                }
                if (tasks.isEmpty()) null else TodoNote(n, tasks)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(noteId: String, lineIndex: Int) = viewModelScope.launch {
        val note = repo.getById(noteId) ?: return@launch
        val lines = note.content.lines().toMutableList()
        if (lineIndex !in lines.indices) return@launch
        val m = TASK_LINE.matchEntire(lines[lineIndex].trim()) ?: return@launch
        val flipped = if (m.groupValues[1].lowercase() == "x") " " else "x"
        val indent = lines[lineIndex].takeWhile { it == ' ' || it == '\t' }
        lines[lineIndex] = "$indent- [$flipped] ${m.groupValues[2]}"
        repo.upsert(note.copy(content = lines.joinToString("\n"), updatedAt = System.currentTimeMillis()))
    }
}
