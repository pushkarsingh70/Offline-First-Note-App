package com.offlinefirst.notes.ui.notes

import app.cash.turbine.test
import com.offlinefirst.notes.data.FakeNoteRepository
import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private lateinit var repository: FakeNoteRepository
    private lateinit var viewModel: NotesViewModel
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNoteRepository()
        viewModel = NotesViewModel(repository, SharingStarted.Eagerly)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankInput_cannotBeSaved_validationErrorSet() = runTest {
        viewModel.prepareNewNote()
        viewModel.onTitleChanged("   ")
        viewModel.onContentChanged("   ")

        val result = viewModel.saveNote()

        assertFalse("Blank note should not be saved", result)
        val editorState = viewModel.editorUiState.value
        assertNotNull(editorState.errorMessage)
        assertTrue(editorState.errorMessage!!.contains("Cannot save an empty note"))
    }

    @Test
    fun createNote_validTitleAndContent_savesSuccessfully() = runTest {
        viewModel.prepareNewNote()
        viewModel.onTitleChanged("Shopping List")
        viewModel.onContentChanged("Milk, Eggs, Bread")

        val result = viewModel.saveNote()
        assertTrue("Valid note should be saved", result)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            val state = awaitItem()
            assertEquals(1, state.notes.size)
            assertEquals("Shopping List", state.notes[0].title)
            assertEquals("Milk, Eggs, Bread", state.notes[0].content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createNote_blankTitleWithContent_handledConsistently() = runTest {
        viewModel.prepareNewNote()
        viewModel.onTitleChanged("")
        viewModel.onContentChanged("Meeting notes for tomorrow morning")

        val result = viewModel.saveNote()
        assertTrue(result)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            val state = awaitItem()
            assertEquals(1, state.notes.size)
            assertEquals("Untitled note", state.notes[0].displayTitle)
            assertEquals("Meeting notes for tomorrow morning", state.notes[0].content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editNote_updatesContentAndModifiedTimestamp() = runTest {
        val note = Note(
            id = 1L,
            title = "Original Title",
            content = "Original Content",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        repository.insertNote(note)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadNoteForEditing(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onContentChanged("Updated Content Body")
        val result = viewModel.saveNote()
        assertTrue(result)

        testDispatcher.scheduler.advanceUntilIdle()

        val updatedNote = repository.getNoteByIdOnce(1L)
        assertNotNull(updatedNote)
        assertEquals("Updated Content Body", updatedNote!!.content)
        assertTrue("updatedAt must change on edit", updatedNote.updatedAt > 1000L)
    }

    @Test
    fun deleteNote_removesFromList_andUndoRestoresIt() = runTest {
        val note = Note(id = 1L, title = "To Delete", content = "Content")
        repository.insertNote(note)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            val stateBefore = awaitItem()
            assertEquals(1, stateBefore.notes.size)

            viewModel.deleteNote(note)
            testDispatcher.scheduler.advanceUntilIdle()

            val stateAfterDelete = awaitItem()
            assertTrue("Note should be removed from list", stateAfterDelete.notes.isEmpty())

            viewModel.undoDelete()
            testDispatcher.scheduler.advanceUntilIdle()

            val stateAfterUndo = awaitItem()
            assertEquals(1, stateAfterUndo.notes.size)
            assertEquals("To Delete", stateAfterUndo.notes[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchNotes_byTitle_isCaseInsensitive() = runTest {
        repository.insertNote(Note(id = 1L, title = "Kotlin Coroutines", content = "Threading"))
        repository.insertNote(Note(id = 2L, title = "Jetpack Compose", content = "UI Framework"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            awaitItem() // initial 2 items

            viewModel.onSearchQueryChanged("kotlin")
            testDispatcher.scheduler.advanceUntilIdle()
            val stateLower = awaitItem()
            assertEquals(1, stateLower.notes.size)
            assertEquals("Kotlin Coroutines", stateLower.notes[0].title)

            viewModel.onSearchQueryChanged("KOTLIN")
            testDispatcher.scheduler.advanceUntilIdle()
            val stateUpper = awaitItem()
            assertEquals(1, stateUpper.notes.size)
            assertEquals("Kotlin Coroutines", stateUpper.notes[0].title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchNotes_byBodyContent_isCaseInsensitive() = runTest {
        repository.insertNote(Note(id = 1L, title = "Tech Stack", content = "We use Room Database offline"))
        repository.insertNote(Note(id = 2L, title = "Shopping", content = "Apples and Oranges"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            awaitItem() // initial items

            viewModel.onSearchQueryChanged("room")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(1, state.notes.size)
            assertEquals("Tech Stack", state.notes[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchNotes_noMatches_setsNoResultsState() = runTest {
        repository.insertNote(Note(id = 1L, title = "Hello", content = "World"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            awaitItem() // initial

            viewModel.onSearchQueryChanged("NonExistentTerm123")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isNoSearchResults)
            assertEquals(0, state.notes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sortNotes_byUpdated_showsNewestFirst() = runTest {
        repository.insertNote(Note(id = 1L, title = "Older Note", content = "A", updatedAt = 1000L))
        repository.insertNote(Note(id = 2L, title = "Newer Note", content = "B", updatedAt = 5000L))
        repository.insertNote(Note(id = 3L, title = "Middle Note", content = "C", updatedAt = 3000L))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            val state = awaitItem()
            assertEquals(3, state.notes.size)
            assertEquals("Newer Note", state.notes[0].title)
            assertEquals("Middle Note", state.notes[1].title)
            assertEquals("Older Note", state.notes[2].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sortNotes_byTitle_ordersAlphabetically() = runTest {
        repository.insertNote(Note(id = 1L, title = "Zebra", content = "A"))
        repository.insertNote(Note(id = 2L, title = "Apple", content = "B"))
        repository.insertNote(Note(id = 3L, title = "Mango", content = "C"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            awaitItem() // initial

            viewModel.onSortOrderChanged(SortOrder.TITLE_ASC)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(3, state.notes.size)
            assertEquals("Apple", state.notes[0].title)
            assertEquals("Mango", state.notes[1].title)
            assertEquals("Zebra", state.notes[2].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pinnedNotes_appearAtTopRegardlessOfSort() = runTest {
        repository.insertNote(Note(id = 1L, title = "Apple", content = "A", isPinned = false))
        repository.insertNote(Note(id = 2L, title = "Zebra", content = "Z", isPinned = true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.listUiState.filter { !it.isLoading }.test {
            awaitItem() // initial

            viewModel.onSortOrderChanged(SortOrder.TITLE_ASC)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Zebra", state.notes[0].title)
            assertTrue(state.notes[0].isPinned)
            assertEquals("Apple", state.notes[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
