package com.offlinefirst.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.offlinefirst.notes.ui.navigation.NotesNavGraph
import com.offlinefirst.notes.ui.notes.NotesViewModel
import com.offlinefirst.notes.ui.notes.NotesViewModelFactory
import com.offlinefirst.notes.ui.theme.OfflineFirstNotesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels {
        val app = application as NotesApplication
        NotesViewModelFactory(app.container.noteRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflineFirstNotesTheme {
                NotesNavGraph(viewModel = viewModel)
            }
        }
    }
}
