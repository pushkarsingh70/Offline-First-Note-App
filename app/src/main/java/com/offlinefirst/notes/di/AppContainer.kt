package com.offlinefirst.notes.di

import android.content.Context
import com.offlinefirst.notes.data.local.database.NotesDatabase
import com.offlinefirst.notes.data.repository.NoteRepository
import com.offlinefirst.notes.data.repository.NoteRepositoryImpl

interface AppContainer {
    val noteRepository: NoteRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val noteRepository: NoteRepository by lazy {
        val database = NotesDatabase.getDatabase(context)
        NoteRepositoryImpl(database.noteDao())
    }
}
