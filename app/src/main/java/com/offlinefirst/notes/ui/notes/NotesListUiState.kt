package com.offlinefirst.notes.ui.notes

import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.model.SortOrder

data class NotesListUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val userMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && notes.isEmpty() && searchQuery.trim().isEmpty()

    val isNoSearchResults: Boolean
        get() = !isLoading && notes.isEmpty() && searchQuery.trim().isNotEmpty()

    val totalNotesCount: Int
        get() = notes.size
}
