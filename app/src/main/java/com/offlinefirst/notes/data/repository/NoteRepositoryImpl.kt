package com.offlinefirst.notes.data.repository

import com.offlinefirst.notes.data.local.dao.NoteDao
import com.offlinefirst.notes.data.local.entity.NoteEntity
import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getNotes(query: String, sortOrder: SortOrder): Flow<List<Note>> {
        val trimmedQuery = query.trim()
        val flow = if (trimmedQuery.isEmpty()) {
            when (sortOrder) {
                SortOrder.UPDATED_DESC -> noteDao.getAllNotesSortedByUpdatedDesc()
                SortOrder.TITLE_ASC -> noteDao.getAllNotesSortedByTitleAsc()
            }
        } else {
            when (sortOrder) {
                SortOrder.UPDATED_DESC -> noteDao.searchNotesSortedByUpdatedDesc(trimmedQuery)
                SortOrder.TITLE_ASC -> noteDao.searchNotesSortedByTitleAsc(trimmedQuery)
            }
        }

        return flow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id).map { it?.toDomain() }
    }

    override suspend fun getNoteByIdOnce(id: Long): Note? {
        return noteDao.getNoteByIdOnce(id)?.toDomain()
    }

    override suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(NoteEntity.fromDomain(note))
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(NoteEntity.fromDomain(note))
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(NoteEntity.fromDomain(note))
    }

    override suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }
}
