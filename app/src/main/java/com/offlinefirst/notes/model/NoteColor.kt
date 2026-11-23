package com.offlinefirst.notes.model

import androidx.compose.ui.graphics.Color

enum class NoteColor(val colorLong: Long, val displayName: String) {
    DEFAULT(0xFFFFFFFF, "Default"),
    SOFT_BLUE(0xFFE0F2FE, "Sky"),
    SOFT_GREEN(0xFFDCFCE7, "Mint"),
    SOFT_YELLOW(0xFFFEF9C3, "Lemon"),
    SOFT_PEACH(0xFFFFEDD5, "Peach"),
    SOFT_LAVENDER(0xFFF3E8FF, "Lavender"),
    SOFT_ROSE(0xFFFFE4E6, "Rose");

    fun toComposeColor(): Color = Color(colorLong)

    companion object {
        fun fromColorLong(colorLong: Long): NoteColor {
            return entries.firstOrNull { it.colorLong == colorLong } ?: DEFAULT
        }
    }
}
