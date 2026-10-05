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
import com.zimapp.zim.data.audio.AudioClipList
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

    // Voice note state. A note can hold several clips; the list is the source of
    // truth in memory and the note's single audio_path column only ever sees the
    // joined form, at the DB boundary. Survives a recomposition but not the
    // process, which is deliberate: a half-finished recording should not be
    // resurrected from a stale path.
    private val _audioClips = mutableStateOf<List<String>>(emptyList())
    val audioClips: State<List<String>> get() = _audioClips

    // The note's audio as stored. Null when there is no clip, which keeps the
    // column nullable and leaves every pre-multi-clip row readable.
    val audioPathForDb: String? get() =
        _audioClips.value.takeIf { it.isNotEmpty() }?.let(AudioClipList::encode)

    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> get() = _isRecording

    private val _isPlayingAudio = mutableStateOf(false)
    val isPlayingAudio: State<Boolean> get() = _isPlayingAudio

    // Index into [audioClips] of the clip loaded in the player, or null when
    // nothing is loaded. Together with [isPlayingAudio] this separates idle from
    // paused, which is what lets the transport offer resume rather than reload.
    private val _activeClip = mutableStateOf<Int?>(null)
    val activeClip: State<Int?> get() = _activeClip

    // True when the note was opened from the audio FAB, so the recorder shows
    // itself instead of waiting to be asked for.
    private val _audioRequested = mutableStateOf(false)
    val audioRequested: State<Boolean> get() = _audioRequested

    // Single source of truth for whether the editor shows the audio controls.
    // The description field reserves padding for the card using this same
    // predicate, so the two cannot drift out of step and leave text hidden.
    val showAudioControls: Boolean
        get() = _audioClips.value.isNotEmpty() || _isRecording.value || _audioRequested.value

    private val recorder = AudioRecorder(appContext)
    private val player = AudioPlayer().apply {
        onFinished = { _isPlayingAudio.value = false }
    }

    private val undoRedoState = UndoRedoState()

    private fun hasContent(): Boolean =
        noteName.value.text.isNotEmpty() ||
            noteDescription.value.text.isNotBlank() ||
            _audioClips.value.isNotEmpty()

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
                        audioPath = audioPathForDb,
                        createdAt = if (noteCreatedTime.value != 0L) noteCreatedTime.value else System.currentTimeMillis(),
                    )
                )

                fetchLastNoteAndUpdate()
            }
        }
    }

    fun deleteNote(id: Int) {
        // Remove the files too: nothing else references them, and leaving them
        // behind would grow the app's data directory without bound.
        _audioClips.value.forEach(::deleteAudioFile)
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
        _activeClip.value = null
        _audioClips.value = AudioClipList.decode(note.audioPath)
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
        // Appended, not substituted: a note can hold several takes, and a new
        // recording must never silently destroy an existing one.
        if (_activeClip.value != null) unloadClip()
        _audioClips.value = _audioClips.value + file.absolutePath
        // Select what was just recorded so the transport shows it immediately
        // rather than leaving the controls pointed at a stale clip.
        _activeClip.value = _audioClips.value.lastIndex
    }

    /**
     * Play, pause or resume [index] depending on what it is already doing.
     *
     * Three states, distinguished by (loaded, playing): nothing loaded means
     * load and start, loaded-and-playing means pause, loaded-and-paused means
     * resume. Collapsing pause into stop is what made a pause button impossible
     * before, since it restarted the clip from zero.
     */
    fun toggleClipPlayback(index: Int) {
        val path = _audioClips.value.getOrNull(index) ?: return
        val file = File(path)
        // The file can vanish under us (cleared storage, a failed migration
        // path); drop the reference rather than offer a dead play button.
        if (!file.isFile) {
            dropMissingClip(index)
            return
        }

        if (player.loadedPath == path) {
            if (player.isPlaying) {
                _isPlayingAudio.value = !player.pause()
            } else {
                _isPlayingAudio.value = player.resume()
            }
            return
        }

        // A different clip: stop first so two clips can never overlap, and
        // because MediaPlayer holds a single stream.
        player.stop()
        _isPlayingAudio.value = false
        val started = player.play(file)
        // Only claim the selection if something is genuinely loaded, otherwise
        // the transport would offer a resume that silently does nothing.
        _activeClip.value = index.takeIf { started || player.isLoaded }
    }

    /** Skips [deltaMs] within the loaded clip; a no-op when nothing is loaded. */
    fun seekAudio(deltaMs: Int): Int = player.seekBy(deltaMs)

    fun activeClipDurationMs(): Int = player.durationMs

    fun activeClipPositionMs(): Int = player.positionMs

    /** Removes one clip and its file, keeping the selection on the same clip. */
    fun deleteClip(index: Int) {
        val path = _audioClips.value.getOrNull(index) ?: return
        // Unload before the file goes, or the player holds a descriptor for
        // something that no longer exists.
        if (player.loadedPath == path) unloadClip()
        deleteAudioFile(path)
        val remaining = _audioClips.value.toMutableList().apply { removeAt(index) }
        _audioClips.value = remaining
        // Indices shift when an earlier clip goes, so follow the clip rather
        // than the slot.
        val active = _activeClip.value
        if (active != null) {
            _activeClip.value = when {
                index == active -> null
                index < active -> active - 1
                else -> active
            }?.takeIf { it in remaining.indices }
        }
    }

    fun deleteAudio() {
        player.stop()
        _isPlayingAudio.value = false
        _activeClip.value = null
        _audioClips.value.forEach(::deleteAudioFile)
        _audioClips.value = emptyList()
    }

    private fun unloadClip() {
        player.stop()
        _isPlayingAudio.value = false
        _activeClip.value = null
    }

    // Drops a clip whose file has gone missing. No file to delete in this case,
    // which is the only difference from deleteClip.
    private fun dropMissingClip(index: Int) {
        if (_activeClip.value == index) unloadClip()
        _audioClips.value = _audioClips.value.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
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