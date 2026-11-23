package com.offlinefirst.notes.data.repository

import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(query: String, sortOrder: SortOrder): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun getNoteByIdOnce(id: Long): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteNoteById(id: Long)
}
