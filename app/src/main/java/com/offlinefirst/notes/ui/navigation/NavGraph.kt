package com.offlinefirst.notes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offlinefirst.notes.ui.notes.NoteEditorScreen
import com.offlinefirst.notes.ui.notes.NotesListScreen
import com.offlinefirst.notes.ui.notes.NotesViewModel

@Composable
fun NotesNavGraph(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NotesList.route,
        modifier = modifier
    ) {
        composable(route = Screen.NotesList.route) {
            NotesListScreen(
                viewModel = viewModel,
                onCreateNoteClick = {
                    navController.navigate(Screen.NoteEditor.createRoute(-1L))
                },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteEditorScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
