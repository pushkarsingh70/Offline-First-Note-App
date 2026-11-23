package com.offlinefirst.notes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.offlinefirst.notes.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val colorLong: Long = 0xFFFFFFFF
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned,
            colorLong = colorLong
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                title = note.title,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                isPinned = note.isPinned,
                colorLong = note.colorLong
            )
        }
    }
}
