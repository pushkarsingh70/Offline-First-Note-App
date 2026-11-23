package com.offlinefirst.notes.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorLong: Long = NoteColor.DEFAULT.colorLong
) {
    /**
     * Returns true if both title and content are blank.
     */
    val isBlank: Boolean
        get() = title.trim().isEmpty() && content.trim().isEmpty()

    /**
     * Consistent display title handling:
     * If title is blank but body has text, display "Untitled note".
     */
    val displayTitle: String
        get() = if (title.trim().isEmpty()) "Untitled note" else title.trim()

    /**
     * A clean content preview with newlines collapsed.
     */
    val contentPreview: String
        get() = content.trim().replace(Regex("\\s+"), " ")
}
