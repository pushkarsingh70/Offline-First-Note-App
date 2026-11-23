package com.offlinefirst.notes.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinefirst.notes.R
import com.offlinefirst.notes.model.Note
import com.offlinefirst.notes.ui.notes.components.DeleteConfirmationDialog
import com.offlinefirst.notes.ui.notes.components.EmptyNotesState
import com.offlinefirst.notes.ui.notes.components.NoSearchResultsState
import com.offlinefirst.notes.ui.notes.components.NoteCard
import com.offlinefirst.notes.ui.notes.components.SearchBarComponent
import com.offlinefirst.notes.ui.notes.components.SortFilterRow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onCreateNoteClick: () -> Unit,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.listUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var notePendingDelete by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            if (message == "Note deleted") {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    if (notePendingDelete != null) {
        DeleteConfirmationDialog(
            noteTitle = notePendingDelete?.displayTitle ?: "",
            onConfirm = {
                val note = notePendingDelete
                notePendingDelete = null
                if (note != null) {
                    viewModel.deleteNote(note)
                }
            },
            onDismiss = {
                notePendingDelete = null
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (!uiState.isLoading && uiState.totalNotesCount > 0) {
                            Text(
                                text = "${uiState.totalNotesCount} note${if (uiState.totalNotesCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNoteClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_note)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SearchBarComponent(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClearQuery = { viewModel.clearSearchQuery() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SortFilterRow(
                currentSort = uiState.sortOrder,
                onSortSelected = { viewModel.onSortOrderChanged(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isEmpty -> {
                    EmptyNotesState(
                        onCreateNoteClick = onCreateNoteClick
                    )
                }
                uiState.isNoSearchResults -> {
                    NoSearchResultsState(
                        searchQuery = uiState.searchQuery,
                        onClearSearchClick = { viewModel.clearSearchQuery() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.notes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                onDeleteClick = { notePendingDelete = note },
                                onTogglePinClick = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}
