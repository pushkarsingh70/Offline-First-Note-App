package com.offlinefirst.notes.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinefirst.notes.data.local.dao.NoteDao
import com.offlinefirst.notes.data.local.database.NotesDatabase
import com.offlinefirst.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetNoteById() = runBlocking {
        val entity = NoteEntity(
            id = 1L,
            title = "Room Persistence",
            content = "Testing offline room storage",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        noteDao.insertNote(entity)

        val retrieved = noteDao.getNoteByIdOnce(1L)
        assertNotNull(retrieved)
        assertEquals("Room Persistence", retrieved!!.title)
        assertEquals("Testing offline room storage", retrieved.content)
    }

    @Test
    fun updateNote_modifiesDataAndTimestamp() = runBlocking {
        val initial = NoteEntity(
            id = 2L,
            title = "Initial Title",
            content = "Initial Content",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        noteDao.insertNote(initial)

        val updated = initial.copy(
            title = "Modified Title",
            content = "Modified Content",
            updatedAt = 2000L
        )
        noteDao.updateNote(updated)

        val retrieved = noteDao.getNoteByIdOnce(2L)
        assertNotNull(retrieved)
        assertEquals("Modified Title", retrieved!!.title)
        assertEquals("Modified Content", retrieved.content)
        assertEquals(2000L, retrieved.updatedAt)
    }

    @Test
    fun deleteNote_removesFromDatabase() = runBlocking {
        val entity = NoteEntity(
            id = 3L,
            title = "To be deleted",
            content = "Will vanish",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        noteDao.insertNote(entity)
        assertEquals(1, noteDao.getNotesCount())

        noteDao.deleteNote(entity)
        assertEquals(0, noteDao.getNotesCount())
        assertNull(noteDao.getNoteByIdOnce(3L))
    }

    @Test
    fun searchNotes_isCaseInsensitive() = runBlocking {
        val note1 = NoteEntity(id = 10L, title = "ANDROID Jetpack", content = "Modern toolkit", createdAt = 1, updatedAt = 1)
        val note2 = NoteEntity(id = 20L, title = "Shopping", content = "Buy Android phone", createdAt = 2, updatedAt = 2)
        val note3 = NoteEntity(id = 30L, title = "Cooking", content = "Pasta recipe", createdAt = 3, updatedAt = 3)
        noteDao.insertNote(note1)
        noteDao.insertNote(note2)
        noteDao.insertNote(note3)

        val resultsLower = noteDao.searchNotesSortedByUpdatedDesc("android").first()
        assertEquals(2, resultsLower.size)

        val resultsUpper = noteDao.searchNotesSortedByUpdatedDesc("ANDROID").first()
        assertEquals(2, resultsUpper.size)
    }

    @Test
    fun sortNotes_updatedDesc_ordersNewestFirst() = runBlocking {
        noteDao.insertNote(NoteEntity(id = 1L, title = "Old", content = "", createdAt = 100, updatedAt = 100))
        noteDao.insertNote(NoteEntity(id = 2L, title = "Newest", content = "", createdAt = 300, updatedAt = 300))
        noteDao.insertNote(NoteEntity(id = 3L, title = "Middle", content = "", createdAt = 200, updatedAt = 200))

        val sorted = noteDao.getAllNotesSortedByUpdatedDesc().first()
        assertEquals("Newest", sorted[0].title)
        assertEquals("Middle", sorted[1].title)
        assertEquals("Old", sorted[2].title)
    }

    @Test
    fun sortNotes_titleAsc_ordersAlphabetically() = runBlocking {
        noteDao.insertNote(NoteEntity(id = 1L, title = "Banana", content = "", createdAt = 1, updatedAt = 1))
        noteDao.insertNote(NoteEntity(id = 2L, title = "Apple", content = "", createdAt = 1, updatedAt = 1))
        noteDao.insertNote(NoteEntity(id = 3L, title = "Cherry", content = "", createdAt = 1, updatedAt = 1))

        val sorted = noteDao.getAllNotesSortedByTitleAsc().first()
        assertEquals("Apple", sorted[0].title)
        assertEquals("Banana", sorted[1].title)
        assertEquals("Cherry", sorted[2].title)
    }

    /**
     * Acceptance Test: Data Persistence across App Restart / Process Death
     * Verifies that after closing the database completely and creating a fresh instance
     * from the SQLite database file, previously written notes remain intact.
     */
    @Test
    fun persistenceAcrossDatabaseReopen() {
        runBlocking {
            val dbFile = File(context.cacheDir, "restart_test_notes.db")
            if (dbFile.exists()) dbFile.delete()

            // 1. First app session: open DB, insert notes, close DB
            var persistentDb = Room.databaseBuilder(context, NotesDatabase::class.java, dbFile.absolutePath)
                .allowMainThreadQueries()
                .build()
            persistentDb.noteDao().insertNote(
                NoteEntity(
                    id = 99L,
                    title = "Meeting Notes Before Process Death",
                    content = "Offline data must survive app shutdown.",
                    createdAt = 5000L,
                    updatedAt = 5000L
                )
            )
            assertEquals(1, persistentDb.noteDao().getNotesCount())
            persistentDb.close()

            // 2. Simulated app restart: open brand new database instance pointing to disk
            persistentDb = Room.databaseBuilder(context, NotesDatabase::class.java, dbFile.absolutePath)
                .allowMainThreadQueries()
                .build()

            val reloadedNote = persistentDb.noteDao().getNoteByIdOnce(99L)
            assertNotNull("Note must persist across database closure/restart", reloadedNote)
            assertEquals("Meeting Notes Before Process Death", reloadedNote!!.title)
            assertEquals("Offline data must survive app shutdown.", reloadedNote.content)
            assertEquals(5000L, reloadedNote.updatedAt)

            persistentDb.close()
            dbFile.delete()
        }
    }
}
