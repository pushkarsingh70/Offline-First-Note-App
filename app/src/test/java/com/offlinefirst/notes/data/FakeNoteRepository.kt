package com.offlinefirst.notes.data

import com.offlinefirst.notes.data.repository.NoteRepository
import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteRepository : NoteRepository {

    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun getNotes(query: String, sortOrder: SortOrder): Flow<List<Note>> {
        return notesFlow.map { list ->
            val filtered = if (query.trim().isEmpty()) {
                list
            } else {
                val q = query.trim().lowercase()
                list.filter {
                    it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
                }
            }

            when (sortOrder) {
                SortOrder.UPDATED_DESC -> {
                    filtered.sortedWith(
                        compareByDescending<Note> { it.isPinned }
                            .thenByDescending { it.updatedAt }
                    )
                }
                SortOrder.TITLE_ASC -> {
                    filtered.sortedWith(
                        compareByDescending<Note> { it.isPinned }
                            .thenBy { it.displayTitle.lowercase() }
                            .thenByDescending { it.updatedAt }
                    )
                }
            }
        }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return notesFlow.map { list -> list.firstOrNull { it.id == id } }
    }

    override suspend fun getNoteByIdOnce(id: Long): Note? {
        return notesFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun insertNote(note: Note): Long {
        val assignedId = if (note.id > 0) note.id else nextId++
        val newNote = note.copy(id = assignedId)
        notesFlow.update { current ->
            current.filterNot { it.id == assignedId } + newNote
        }
        return assignedId
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.update { current ->
            current.map { if (it.id == note.id) note else it }
        }
    }

    override suspend fun deleteNote(note: Note) {
        notesFlow.update { current ->
            current.filterNot { it.id == note.id }
        }
    }

    override suspend fun deleteNoteById(id: Long) {
        notesFlow.update { current ->
            current.filterNot { it.id == id }
        }
    }

    fun clear() {
        notesFlow.value = emptyList()
        nextId = 1L
    }
}
