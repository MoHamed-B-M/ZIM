package com.zimapp.zim.presentation.screens.edit.model

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.usecase.NoteUseCase
import com.zimapp.zim.presentation.components.DecryptionResult
import com.zimapp.zim.presentation.components.EncryptionHelper
import com.zimapp.zim.presentation.screens.edit.components.UndoRedoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// Nav argument values that seed a new note's body.
const val TEMPLATE_TODO = "todo"
// Markdown checkboxes. Must start with "[ ] " — the checkbox processor
// anchors at the line start, and ListItemProcessor would swallow a "- "
// prefix before it ever ran.
private const val TODO_TEMPLATE = "[ ] \n[ ] \n[ ] "

@HiltViewModel
class EditViewModel @Inject constructor(
    private val noteUseCase: NoteUseCase,
    private val encryption: EncryptionHelper,
) : ViewModel() {
    private val _noteName = mutableStateOf(TextFieldValue())
    val noteName: State<TextFieldValue> get() = _noteName

    private val _isDescriptionInFocus = mutableStateOf(false)
    val isDescriptionInFocus: State<Boolean> get() = _isDescriptionInFocus

    private val _isEncrypted = mutableStateOf(false)
    val isEncrypted: State<Boolean> get() = _isEncrypted

    private val _noteDescription = mutableStateOf(TextFieldValue())
    val noteDescription: State<TextFieldValue> get() = _noteDescription

    private val _noteId = mutableIntStateOf(0)
    val noteId: State<Int> get() = _noteId

    private val _noteCreatedTime = mutableLongStateOf(System.currentTimeMillis())
    val noteCreatedTime: State<Long> get() = _noteCreatedTime

    private val _isNoteInfoVisible = mutableStateOf(false)
    val isNoteInfoVisible: State<Boolean> get() = _isNoteInfoVisible

    private val _isEditMenuVisible = mutableStateOf(false)
    val isEditMenuVisible: State<Boolean> get() = _isEditMenuVisible

    private val _isPinned = mutableStateOf(false)
    val isPinned: State<Boolean> get() = _isPinned

    private val undoRedoState = UndoRedoState()

    private fun hasContent(): Boolean =
        noteName.value.text.isNotEmpty() ||
            noteDescription.value.text.isNotBlank()

    fun saveNote(id: Int) {
        if (hasContent()) {
            viewModelScope.launch {
                noteUseCase.addNote(
                    Note(
                        id = id,
                        name = noteName.value.text,
                        description = noteDescription.value.text,
                        pinned = isPinned.value,
                        encrypted = isEncrypted.value,
                        createdAt = if (noteCreatedTime.value != 0L) noteCreatedTime.value else System.currentTimeMillis(),
                    )
                )

                fetchLastNoteAndUpdate()
            }
        }
    }

    fun deleteNote(id: Int) {
        noteUseCase.deleteNoteById(id = id)
    }

    private fun syncNote(note: Note) {
        if (note.encrypted) {
            val (name, nameStatus) = encryption.decrypt(note.name)
            val (description, descriptionStatus) = encryption.decrypt(note.description)
            if (nameStatus == DecryptionResult.SUCCESS && descriptionStatus == DecryptionResult.SUCCESS) {
                updateNoteName(TextFieldValue(name!!, selection = TextRange(note.name.length)))
                updateNoteDescription(TextFieldValue(description!!, selection = TextRange(note.description.length)))
            }
        } else {
            updateNoteName(TextFieldValue(note.name, selection = TextRange(note.name.length)))
            updateNoteDescription(TextFieldValue(note.description, selection = TextRange(note.description.length)))
        }
        updateNoteCreatedTime(note.createdAt)
        updateNoteId(note.id)
        updateNotePin(note.pinned)
        updateIsEncrypted(note.encrypted)
    }

    fun setupNoteData(id : Int = noteId.value) {
        if (id != 0) {
            viewModelScope.launch {
                noteUseCase.getNoteById(id).collectLatest { note ->
                    if (note != null) {
                        syncNote(note)
                    }
                }
            }
        }
    }

    // Seeds a brand new (unsaved) note with a starting point. Guarded so it
    // never overwrites typed content and never fires while editing an existing
    // note. Called from composition, so it must stay idempotent: the `templated`
    // latch is what stops recomposition from re-seeding an already-open note.
    fun applyTemplate(kind: String) {
        if (noteId.value != 0 || templated) return
        templated = true
        when (kind) {
            TEMPLATE_TODO -> if (noteDescription.value.text.isBlank()) {
                _noteDescription.value = TextFieldValue(TODO_TEMPLATE, TextRange(TODO_TEMPLATE.length))
            }
        }
    }

    // Set once a template has been applied, so recomposition cannot re-seed.
    private var templated = false

    private fun fetchLastNoteAndUpdate() {
        if (hasContent()) {
            if (noteId.value == 0) {
                viewModelScope.launch {
                    noteUseCase.getLastNoteId { lastId ->
                        viewModelScope.launch {
                            setupNoteData(lastId?.toInt() ?: 1)
                        }
                    }
                }
            }
        }
    }


    fun toggleEditMenuVisibility(value: Boolean) {
        _isEditMenuVisible.value = value
    }

    fun toggleNoteInfoVisibility(value: Boolean) {
        _isNoteInfoVisible.value = value
    }

    fun toggleIsDescriptionInFocus(value: Boolean) {
        _isDescriptionInFocus.value = value
    }

    fun toggleNotePin(value: Boolean) {
        _isPinned.value = value
    }

    fun updateNoteName(newName: TextFieldValue) {
        _noteName.value = newName
        undoRedoState.onInput(newName)
    }

    fun updateIsEncrypted(value: Boolean) {
        _isEncrypted.value = value
    }

    fun updateNoteDescription(newDescription: TextFieldValue) {
        _noteDescription.value = newDescription
        undoRedoState.onInput(newDescription)
    }

    private fun updateNoteCreatedTime(newTime: Long) {
        _noteCreatedTime.longValue = newTime
    }

    private fun updateNotePin(pinned: Boolean) {
        _isPinned.value = pinned
    }

    fun updateNoteId(newId: Int) {
        _noteId.intValue = newId
    }

    fun undo() {
        undoRedoState.undo()
        _noteDescription.value = undoRedoState.input
    }

    fun redo() {
        undoRedoState.redo()
        _noteDescription.value = undoRedoState.input
    }

    private fun isSelectorAtStartOfNonEmptyLine(): Boolean {
        val text = _noteDescription.value.text
        val selectionStart = _noteDescription.value.selection.start

        if (selectionStart == 0) {
            return true
        }
        return text[selectionStart - 1] == '\n'
    }


    private fun getIntRangeForCurrentLine(): IntRange {
        val text = _noteDescription.value.text
        val selectionStart = _noteDescription.value.selection.start
        val selectionEnd = _noteDescription.value.selection.end
        var lineStart = selectionStart
        var lineEnd = selectionEnd

        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        while (lineEnd < text.length && text[lineEnd] != '\n') {
            lineEnd++
        }
        return IntRange(lineStart, lineEnd - 1);
    }

    fun insertText(insertText: String, offset: Int = 1, newLine: Boolean = true) {
        val currentText = _noteDescription.value.text
        val resultSelectionIndex: Int
        val rangeOfCurrentLine = getIntRangeForCurrentLine()
        val updatedText = if (!rangeOfCurrentLine.isEmpty()) {
            val currentLineContents = currentText.substring(rangeOfCurrentLine)
            val newLine = if (isSelectorAtStartOfNonEmptyLine()) {
                insertText + currentLineContents
            } else {
                if (newLine) {
                    currentLineContents + "\n" + insertText
                } else {
                    currentLineContents + insertText
                }
            }
            resultSelectionIndex = rangeOfCurrentLine.first + newLine.length - 1
            currentText.replaceRange(rangeOfCurrentLine, newLine)
        } else {
            resultSelectionIndex = (currentText + insertText).length
            currentText + insertText
        }

        _noteDescription.value = TextFieldValue(
            text = updatedText,
            selection = TextRange(resultSelectionIndex + offset)
        )
        // Recorded so the line-level actions are undoable too. They used to
        // write straight to the state, which made them the one toolbar action
        // that Ctrl+Z could not take back.
        undoRedoState.pushHistory(_noteDescription.value)
    }

    /**
     * Applies [mark] to the selection, or removes it when it is already applied.
     *
     * Cases, in priority order:
     *  1. Collapsed caret: insert an empty delimiter pair and park the caret
     *     between them, ready to type inside.
     *  2. The delimiters are inside the selection: strip them and keep the
     *     inner text selected, so a second tap unwraps rather than re-wraps.
     *  3. The delimiters surround the selection (the selection is an already
     *     marked run): strip the surrounding pair.
     *  4. Otherwise wrap, keeping the selection over the same original text.
     */
    fun toggleMark(mark: MarkdownMark) {
        val value = _noteDescription.value
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        val open = mark.open
        val close = mark.close

        val next = when {
            start == end -> TextFieldValue(
                text = text.substring(0, start) + open + close + text.substring(end),
                selection = TextRange(start + open.length),
            )

            end - start >= open.length + close.length &&
                text.startsWith(open, start) && text.startsWith(close, end - close.length) ->
                TextFieldValue(
                    text = text.substring(0, start) +
                        text.substring(start + open.length, end - close.length) +
                        text.substring(end),
                    selection = TextRange(start, end - close.length - open.length),
                )

            start - open.length >= 0 && end + close.length <= text.length &&
                text.startsWith(open, start - open.length) && text.startsWith(close, end) &&
                isStandaloneDelimiter(text, start - open.length, open) &&
                isStandaloneDelimiter(text, end, close) ->
                TextFieldValue(
                    text = text.substring(0, start - open.length) +
                        text.substring(start, end) + text.substring(end + close.length),
                    selection = TextRange(start - open.length, end - open.length),
                )

            else -> {
                val selected = text.substring(start, end)
                TextFieldValue(
                    text = text.substring(0, start) + open + selected + close + text.substring(end),
                    selection = TextRange(start + open.length, end + open.length),
                )
            }
        }

        _noteDescription.value = next
        undoRedoState.pushHistory(next)
    }

    /**
     * Whether [mark] currently covers the selection, i.e. whether tapping it
     * would unwrap. Mirrors the unwrap branches of [toggleMark] exactly, so the
     * toolbar's highlight can never disagree with what a tap will do.
     */
    fun isMarkActive(mark: MarkdownMark): Boolean {
        val value = _noteDescription.value
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        // A bare caret marks nothing, so nothing is highlighted.
        if (start == end) return false
        val open = mark.open
        val close = mark.close

        if (end - start >= open.length + close.length &&
            text.startsWith(open, start) && text.startsWith(close, end - close.length)
        ) return true

        return start - open.length >= 0 && end + close.length <= text.length &&
            text.startsWith(open, start - open.length) && text.startsWith(close, end) &&
            isStandaloneDelimiter(text, start - open.length, open) &&
            isStandaloneDelimiter(text, end, close)
    }

    /**
     * Whether the delimiter beginning at [index] is its own run rather than one
     * character of a longer run.
     *
     * Without this, italic (`*`) latches onto the stars of a bold (`**`) run:
     * asking for italic with the caret inside `**bold**` would strip a star and
     * mangle the bold.
     */
    private fun isStandaloneDelimiter(text: String, index: Int, delimiter: String): Boolean {
        val char = delimiter.first()
        return text.getOrNull(index - 1) != char && text.getOrNull(index + delimiter.length) != char
    }
}