package com.offlinefirst.notes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinefirst.notes.data.repository.NoteRepository
import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.NoteColor
import com.offlinefirst.notes.model.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.UPDATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var lastDeletedNote: Note? = null

    val listUiState: StateFlow<NotesListUiState> = combine(
        _searchQuery,
        _sortOrder
    ) { query, sort ->
        Pair(query, sort)
    }.flatMapLatest { (query, sort) ->
        repository.getNotes(query, sort).map { notes ->
            NotesListUiState(
                isLoading = false,
                notes = notes,
                searchQuery = query,
                sortOrder = sort
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = NotesListUiState(isLoading = true)
    )

    private val _editorUiState = MutableStateFlow(NoteEditorUiState())
    val editorUiState: StateFlow<NoteEditorUiState> = _editorUiState.asStateFlow()

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun onSortOrderChanged(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            lastDeletedNote = note
            repository.deleteNote(note)
            _userMessage.emit("Note deleted")
        }
    }

    fun undoDelete() {
        val noteToRestore = lastDeletedNote ?: return
        viewModelScope.launch {
            repository.insertNote(noteToRestore)
            lastDeletedNote = null
            _userMessage.emit("Note restored")
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
        }
    }

    // -------------------------------------------------------------
    // Editor Actions
    // -------------------------------------------------------------

    fun prepareNewNote() {
        _editorUiState.value = NoteEditorUiState(
            id = 0,
            title = "",
            content = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isPinned = false,
            colorLong = NoteColor.DEFAULT.colorLong,
            isLoading = false,
            isSaved = false,
            errorMessage = null,
            isNewNote = true
        )
    }

    fun loadNoteForEditing(noteId: Long) {
        if (noteId <= 0) {
            prepareNewNote()
            return
        }

        _editorUiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val note = repository.getNoteByIdOnce(noteId)
            if (note != null) {
                _editorUiState.value = NoteEditorUiState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isPinned = note.isPinned,
                    colorLong = note.colorLong,
                    isLoading = false,
                    isSaved = false,
                    errorMessage = null,
                    isNewNote = false
                )
            } else {
                _editorUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Note not found"
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _editorUiState.update {
            it.copy(title = newTitle, errorMessage = null)
        }
    }

    fun onContentChanged(newContent: String) {
        _editorUiState.update {
            it.copy(content = newContent, errorMessage = null)
        }
    }

    fun onColorChanged(colorLong: Long) {
        _editorUiState.update {
            it.copy(colorLong = colorLong)
        }
    }

    fun onPinToggledInEditor() {
        _editorUiState.update {
            it.copy(isPinned = !it.isPinned)
        }
    }

    fun clearEditorError() {
        _editorUiState.update { it.copy(errorMessage = null) }
    }

    fun saveNote(onSaved: () -> Unit = {}): Boolean {
        val current = _editorUiState.value
        val trimmedTitle = current.title.trim()
        val trimmedContent = current.content.trim()

        if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
            _editorUiState.update {
                it.copy(errorMessage = "Cannot save an empty note. Please enter a title or content.")
            }
            return false
        }

        val now = System.currentTimeMillis()
        val noteToSave = Note(
            id = current.id,
            title = trimmedTitle,
            content = trimmedContent,
            createdAt = if (current.isNewNote) now else current.createdAt,
            updatedAt = now,
            isPinned = current.isPinned,
            colorLong = current.colorLong
        )

        viewModelScope.launch {
            if (current.isNewNote) {
                val newId = repository.insertNote(noteToSave)
                _editorUiState.update { it.copy(id = newId, isSaved = true) }
            } else {
                repository.updateNote(noteToSave)
                _editorUiState.update { it.copy(isSaved = true) }
            }
            _userMessage.emit("Note saved")
            onSaved()
        }
        return true
    }

    fun deleteCurrentNote(onDeleted: () -> Unit = {}) {
        val current = _editorUiState.value
        if (!current.isNewNote && current.id > 0) {
            val noteToDelete = Note(
                id = current.id,
                title = current.title,
                content = current.content,
                createdAt = current.createdAt,
                updatedAt = current.updatedAt,
                isPinned = current.isPinned,
                colorLong = current.colorLong
            )
            deleteNote(noteToDelete)
            onDeleted()
        }
    }
}
