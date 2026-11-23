package com.offlinefirst.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinefirst.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesSortedByUpdatedDesc(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, LOWER(CASE WHEN TRIM(title) = '' THEN 'Untitled note' ELSE TRIM(title) END) ASC, updatedAt DESC")
    fun getAllNotesSortedByTitleAsc(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(content) LIKE '%' || LOWER(:query) || '%'
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotesSortedByUpdatedDesc(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(content) LIKE '%' || LOWER(:query) || '%'
        ORDER BY isPinned DESC, LOWER(CASE WHEN TRIM(title) = '' THEN 'Untitled note' ELSE TRIM(title) END) ASC, updatedAt DESC
    """)
    fun searchNotesSortedByTitleAsc(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNotesCount(): Int
}
