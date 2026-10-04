package com.zimapp.zim.presentation.screens.edit.model

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.data.audio.AudioPlayer
import com.zimapp.zim.data.audio.AudioRecorder
import com.zimapp.zim.data.audio.deleteAudioFile
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.usecase.NoteUseCase
import com.zimapp.zim.presentation.components.DecryptionResult
import com.zimapp.zim.presentation.components.EncryptionHelper
import com.zimapp.zim.presentation.screens.edit.components.UndoRedoState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// Nav argument values that seed a new note's body.
const val TEMPLATE_TODO = "todo"
// Opens the editor with the recorder already showing, for a note whose point
// is the recording rather than the text.
const val TEMPLATE_AUDIO = "audio"
// Markdown checkboxes. Must start with "[ ] " — the checkbox processor
// anchors at the line start, and ListItemProcessor would swallow a "- "
// prefix before it ever ran.
private const val TODO_TEMPLATE = "[ ] \n[ ] \n[ ] "

@HiltViewModel
class EditViewModel @Inject constructor(
    private val noteUseCase: NoteUseCase,
    private val encryption: EncryptionHelper,
    // Application context only: the recorder outlives no screen, and holding
    // an Activity here would leak it across configuration changes.
    @ApplicationContext private val appContext: Context
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

    // Voice note state. audioPath is null until a recording finishes, and
    // survives a recomposition but not the process, which is deliberate: a
    // half-finished recording should not be resurrected from a stale path.
    private val _audioPath = mutableStateOf<String?>(null)
    val audioPath: State<String?> get() = _audioPath

    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> get() = _isRecording

    private val _isPlayingAudio = mutableStateOf(false)
    val isPlayingAudio: State<Boolean> get() = _isPlayingAudio

    // True when the note was opened from the audio FAB, so the recorder shows
    // itself instead of waiting to be asked for.
    private val _audioRequested = mutableStateOf(false)
    val audioRequested: State<Boolean> get() = _audioRequested

    private val recorder = AudioRecorder(appContext)
    private val player = AudioPlayer().apply {
        onFinished = { _isPlayingAudio.value = false }
    }

    private val undoRedoState = UndoRedoState()

    private fun hasContent(): Boolean =
        noteName.value.text.isNotEmpty() ||
            noteDescription.value.text.isNotBlank() ||
            audioPath.value != null

    // An audio-only note has no text at all, so the emptiness check has to
    // count the recording or "new audio note" would save nothing.
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
                        audioPath = audioPath.value,
                        createdAt = if (noteCreatedTime.value != 0L) noteCreatedTime.value else System.currentTimeMillis(),
                    )
                )

                fetchLastNoteAndUpdate()
            }
        }
    }

    fun deleteNote(id: Int) {
        // Remove the file too: nothing else references it, and leaving it
        // behind would grow the app's data directory without bound.
        deleteAudioFile(audioPath.value)
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
        // Stop anything in flight first, so reopening a note never leaves the
        // previous clip playing over the new one.
        player.stop()
        _isPlayingAudio.value = false
        _audioPath.value = note.audioPath
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
            // No text to seed: the template's job is to reveal the recorder.
            TEMPLATE_AUDIO -> _audioRequested.value = true
        }
    }

    // --- Voice notes -------------------------------------------------------
    // Recording is opt-in and never starts on its own: the microphone opens
    // only when the user taps the mic.

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        val ok = recorder.start()
        _isRecording.value = ok
        return ok
    }

    fun stopRecording() {
        val file = recorder.stop()
        _isRecording.value = false
        if (file == null) return
        // Replacing a clip: the old one is ours to delete, otherwise every
        // re-record would leave an orphan behind.
        deleteAudioFile(audioPath.value)
        _audioPath.value = file.absolutePath
    }

    fun toggleAudioPlayback(): Boolean {
        val path = audioPath.value ?: return false
        val file = File(path)
        // The file can vanish under us (cleared storage, a failed migration
        // path); drop the reference rather than offer a dead play button.
        if (!file.isFile) {
            _audioPath.value = null
            return false
        }
        val playing = player.toggle(file)
        _isPlayingAudio.value = playing
        return playing
    }

    fun deleteAudio() {
        player.stop()
        _isPlayingAudio.value = false
        deleteAudioFile(audioPath.value)
        _audioPath.value = null
    }

    // Leaving the editor mid-take: stop and throw the partial file away, so a
    // half-sentence never becomes the note's audio.
    fun cancelRecording() {
        if (!_isRecording.value) return
        recorder.cancel()
        _isRecording.value = false
    }

    override fun onCleared() {
        cancelRecording()
        player.stop()
        super.onCleared()
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
    }
}