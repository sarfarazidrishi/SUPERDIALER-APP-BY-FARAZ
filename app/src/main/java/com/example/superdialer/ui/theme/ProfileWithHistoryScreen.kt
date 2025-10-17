package com.example.superdialer.ui.theme

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superdialer.data.*
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWithHistoryScreen(
    number: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NotesDatabase.getDatabase(context) }
    val dao = db.notesDao()
    val scope = rememberCoroutineScope()

    var callHistory by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var selectedTag by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val allTags = listOf("All", "Incoming", "Outgoing", "Missed")

    LaunchedEffect(number) {
        callHistory = getCallHistory(context).filter { it.number == number }

        dao.getNotesForNumber(number).collect { fetchedNotes ->
            notes = fetchedNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & History") },
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
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 👤 Profile Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(number, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 📞 Call
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = android.net.Uri.parse("tel:$number")
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF1E88E5))
                        }

                        // 💬 WhatsApp
                        IconButton(onClick = { openWhatsApp(context, number) }) {
                            Icon(Icons.Default.Whatsapp, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                        }

                        // 📝 Notes
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Notes", tint = Color(0xFFD32F2F))
                        }

                        // 👤 Save Contact
                        IconButton(onClick = {
                            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.PHONE, number)
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.PersonAddAlt1, contentDescription = "Save Contact", tint = Color.Gray)
                        }
                    }
                }
            }

            // 🗒️ Notes Section
            Text("Notes (${notes.size})", style = MaterialTheme.typography.titleMedium)
            if (notes.isEmpty()) {
                Text("No notes yet for this contact.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth()
                ) {
                    items(notes) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth()
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(note.note)
                                Text(
                                    text = "🕒 ${note.timestamp}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // 🕓 Call History Section
            Text("Call History (${callHistory.size})", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) }
                    )
                }
            }

            val filteredHistory = remember(callHistory, selectedTag) {
                when (selectedTag) {
                    "Incoming" -> callHistory.filter { it.type.equals("Incoming", true) }
                    "Outgoing" -> callHistory.filter { it.type.equals("Outgoing", true) }
                    "Missed" -> callHistory.filter { it.type.equals("Missed", true) }
                    else -> callHistory
                }
            }

            if (filteredHistory.isEmpty()) {
                Text("No calls found.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredHistory) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.type)
                                Text("${entry.duration}s")
                                Text(entry.date)
                            }
                        }
                    }
                }
            }
        }
    }

    // 📝 Add Note Dialog
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
