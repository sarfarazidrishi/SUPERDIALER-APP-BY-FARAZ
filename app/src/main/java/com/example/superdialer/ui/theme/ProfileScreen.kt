package com.example.superdialer.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.superdialer.data.CallEntry
import com.example.superdialer.data.NoteEntity
import com.example.superdialer.data.NotesDatabase
import com.example.superdialer.data.getCallHistory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    number: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NotesDatabase.getDatabase(context) }
    val dao = db.notesDao()
    val scope = rememberCoroutineScope()

    // 🔢 State holders
    var callHistory by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var showNotes by remember { mutableStateOf(false) }

    // 🧠 Reactive Flow collector for notes
    LaunchedEffect(number) {
        // Load call history once
        callHistory = getCallHistory(context).filter { it.number == number }

        // Collect notes reactively from Flow (auto updates on DB change)
        dao.getNotesForNumber(number).collect { fetchedNotes ->
            notes = fetchedNotes
        }
    }

    // 🧱 UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile: $number") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📞 Total Calls: ${callHistory.size}")
            Text("🗒️ Total Notes: ${notes.size}")

            Divider()

            if (notes.isEmpty()) {
                Text("No notes yet for this number.")
            } else {
                notes.forEach { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(note.note)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "🕒 ${note.timestamp}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        dao.insert(
                            NoteEntity(
                                phoneNumber = number,
                                note = "Quick note added from Profile"
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Test Note")
            }
        }
    }
}
