package com.offlinefirst.notes.ui.navigation

sealed class Screen(val route: String) {
    data object NotesList : Screen("notes_list")
    data object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long): String = "note_editor/$noteId"
    }
}
