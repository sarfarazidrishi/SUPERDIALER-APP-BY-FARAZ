package com.example.superdialer.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.superdialer.data.NoteEntity

@Composable
fun ShowNotesDialog(
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    onUpdate: (NoteEntity, String) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("📂 Saved Notes (${notes.size})") },
        text = {
            if (notes.isEmpty()) {
                Text("No notes yet for this contact.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(notes) { noteEntity ->
                        var expanded by remember { mutableStateOf(false) }
                        var editMode by remember { mutableStateOf(false) }
                        var editText by remember { mutableStateOf(noteEntity.note) }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            onClick = { expanded = !expanded }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // ✅ Show first line of note (or fallback)
                                val preview = noteEntity.note
                                    .lineSequence()
                                    .firstOrNull()
                                    ?.take(30) ?: "Untitled note"

                                Text(
                                    preview,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (expanded) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (editMode) {
                                        OutlinedTextField(
                                            value = editText,
                                            onValueChange = { editText = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextButton(onClick = {
                                                editMode = false
                                                editText = noteEntity.note
                                            }) { Text("Cancel") }
                                            Button(onClick = {
                                                onUpdate(noteEntity, editText)
                                                editMode = false
                                            }) { Text("Save") }
                                        }
                                    } else {
                                        Text(noteEntity.note, style = MaterialTheme.typography.bodyMedium)
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(onClick = { editMode = true }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = { onDelete(noteEntity) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
