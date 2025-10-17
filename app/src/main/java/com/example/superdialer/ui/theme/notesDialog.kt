package com.example.superdialer.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superdialer.data.NoteEntity
import com.example.superdialer.data.NotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesDialog(
    number: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = NotesDatabase.getDatabase(context)
    val dao = db.notesDao()
    val scope = rememberCoroutineScope()

    var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 🔁 Collect notes reactively (Flow)
    LaunchedEffect(number) {
        dao.getNotesForNumber(number).collect { fetchedNotes ->
            notes = fetchedNotes
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Notes for $number",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (notes.isEmpty()) {
                    Text("No notes yet. Tap + to add one.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notes) { note ->
                            ExpandableNoteItem(
                                note = note,
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.delete(note)
                                    }
                                },
                                onEdit = { updated ->
                                    scope.launch(Dispatchers.IO) {
                                        dao.update(note.copy(note = updated))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        icon = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    )

    // ➕ Add Note Dialog (integrated, improved version)
    if (showAddDialog) {
        AddNoteDialog(
            number = number,
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                scope.launch(Dispatchers.IO) {
                    dao.insert(NoteEntity(phoneNumber = number, note = text))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpandableNoteItem(
    note: NoteEntity,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(note.note) }

    val title = remember(note.note) {
        val words = note.note.split(" ")
        if (words.size > 20) words.take(20).joinToString(" ") + "..."
        else note.note
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    if (isEditing) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                onEdit(text)
                                isEditing = false
                            }) { Text("Save") }
                            TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                        }
                    } else {
                        Text(
                            text = note.note,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


