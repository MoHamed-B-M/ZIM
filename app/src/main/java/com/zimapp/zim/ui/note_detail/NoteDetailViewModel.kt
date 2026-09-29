package com.zimapp.zim.ui.note_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.domain.model.SyncStatus
import com.zimapp.zim.domain.repository.NoteRepository
import com.zimapp.zim.ui.note_edit.rich.HtmlCodec
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val tagsCsv: String = "",
    val colorToken: Int = 0,
    val isPinned: Boolean = false,
    val format: NoteFormat = NoteFormat.RICH,
    val loaded: Boolean = false,
    val saved: Boolean = false,
)

class NoteDetailViewModel(private val repo: NoteRepository) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun load(id: String?) {
        if (id == null) { _state.update { it.copy(loaded = true) }; return }
        viewModelScope.launch {
            repo.getById(id)?.let { n ->
                _state.update {
                    it.copy(id = n.id, title = n.title, content = n.content,
                        tagsCsv = n.tags.joinToString(", "), colorToken = n.colorToken,
                        isPinned = n.isPinned, format = n.format, loaded = true)
                }
            } ?: _state.update { it.copy(loaded = true) }
        }
    }

    fun edit(title: String? = null, content: String? = null, tags: String? = null, color: Int? = null) {
        _state.update {
            it.copy(
                title = title ?: it.title, content = content ?: it.content,
                tagsCsv = tags ?: it.tagsCsv, colorToken = color ?: it.colorToken, saved = false,
            )
        }
    }

    fun setFormat(format: NoteFormat) {
        _state.update { it.copy(format = format, saved = false) }
    }

    fun save(isChecklist: Boolean = false, richHtml: String? = null) = viewModelScope.launch {
        val s = _state.value
        val now = System.currentTimeMillis()
        val rawBody = richHtml ?: s.content
        val plainCheck = richHtml?.let { HtmlCodec.htmlToPlain(it) } ?: s.content
        val body = if (isChecklist && plainCheck.isBlank()) "- [ ] " else rawBody
        val existing = repo.getById(s.id)
        repo.upsert(
            Note(id = s.id, title = s.title, content = body,
                createdAt = existing?.createdAt ?: now, updatedAt = now,
                isPinned = s.isPinned, isArchived = existing?.isArchived == true,
                syncStatus = SyncStatus.PENDING,
                tags = s.tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                colorToken = s.colorToken, format = s.format)
        )
        _state.update { it.copy(saved = true) }
    }
}
