package com.offlinefirst.notes.ui.notes

import com.offlinefirst.notes.model.NoteColor

data class NoteEditorUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isPinned: Boolean = false,
    val colorLong: Long = NoteColor.DEFAULT.colorLong,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isNewNote: Boolean = true
) {
    val isBlank: Boolean
        get() = title.trim().isEmpty() && content.trim().isEmpty()
}
