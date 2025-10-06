package com.example.superdialer.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superdialer.data.CallEntry
import com.example.superdialer.data.NoteEntity
import com.example.superdialer.data.NotesDatabase
import com.example.superdialer.data.getCallHistory
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DialerScreen(onCallClick: (String) -> Unit) {
    val context = LocalContext.current

    // 📞 Dialer state
    var phoneNumber by remember { mutableStateOf("") }
    var callHistory by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    // 📝 Notes state (Room DB)
    val db = remember { NotesDatabase.getDatabase(context) }
    val notesDao = db.notesDao()
    var showNotesDialog by remember { mutableStateOf(false) }
    var showSavedNotesDialog by remember { mutableStateOf(false) }
    var currentPhoneNumber by remember { mutableStateOf<String?>(null) }
    var notesForCurrent by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var noteCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) } // 👈 stores note count for each number

    // Load call history & preload note counts
    LaunchedEffect(Unit) {
        callHistory = getCallHistory(context)
        // Preload counts
        CoroutineScope(Dispatchers.IO).launch {
            val counts = callHistory.associate { entry ->
                entry.number to notesDao.getNotesForNumber(entry.number).size
            }
            withContext(Dispatchers.Main) { noteCounts = counts }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 🔹 Tag Filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Zepto", "Amazon", "Spam").forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag || (tag == "All" && selectedTag == null),
                    onClick = { selectedTag = if (tag == "All") null else tag },
                    label = { Text(tag) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Call History
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Recent Calls", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val filteredHistory = if (selectedTag == null) callHistory
            else callHistory.filter { it.tag == selectedTag }

            if (filteredHistory.isEmpty()) {
                Text("No calls yet", fontSize = 14.sp)
            } else {
                filteredHistory.take(5).forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📞 ${entry.number}", fontSize = 16.sp)
                            Text("${entry.type} • ${entry.duration}", fontSize = 12.sp)
                        }

                        // 💬 WhatsApp button
                        IconButton(onClick = { openWhatsApp(context, entry.number) }) {
                            Text("💬")
                        }

                        // 📝 Add note button
                        IconButton(onClick = {
                            currentPhoneNumber = entry.number
                            showNotesDialog = true
                        }) {
                            Text("📝")
                        }

                        // 📋 Show notes button with badge
                        BadgedBox(
                            badge = {
                                val count = noteCounts[entry.number] ?: 0
                                if (count > 0) {
                                    Badge { Text(count.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = {
                                currentPhoneNumber = entry.number
                                CoroutineScope(Dispatchers.IO).launch {
                                    val notes = notesDao.getNotesForNumber(entry.number)
                                    withContext(Dispatchers.Main) {
                                        if (notes.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No notes yet",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            notesForCurrent = notes
                                            showSavedNotesDialog = true
                                        }
                                        noteCounts = noteCounts.toMutableMap().apply {
                                            this[entry.number] = notes.size
                                        }
                                    }
                                }
                            }) {
                                Text("📋")
                            }
                        }
                    }
                }
            }
        }

        // ☎️ Dialer Section
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Number Display + Backspace
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = phoneNumber,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                IconButton(onClick = {
                    if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1)
                }) {
                    Text("⌫", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dial Pad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    row.forEach { digit ->
                        Button(
                            onClick = { phoneNumber += digit },
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(digit, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Call Button
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        onCallClick(phoneNumber)
                        callHistory = listOf(
                            CallEntry(
                                phoneNumber,
                                "OUTGOING",
                                "Now",
                                "0s",
                                tag = selectedTag
                            )
                        ) + callHistory
                        phoneNumber = ""
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(90.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📞", fontSize = 28.sp)
            }
        }
    }

    // 📝 Add Notes Dialog
    if (showNotesDialog && currentPhoneNumber != null) {
        NotesDialog(
            initialText = "",
            onDismiss = { showNotesDialog = false },
            onSave = { noteText ->
                CoroutineScope(Dispatchers.IO).launch {
                    notesDao.insert(
                        NoteEntity(
                            phoneNumber = currentPhoneNumber!!,
                            note = noteText
                        )
                    )
                    val notes = notesDao.getNotesForNumber(currentPhoneNumber!!)
                    withContext(Dispatchers.Main) {
                        notesForCurrent = notes
                        noteCounts = noteCounts.toMutableMap().apply {
                            this[currentPhoneNumber!!] = notes.size
                        }
                    }
                }
                showNotesDialog = false
            }
        )
    }

    // 📋 Show Saved Notes Dialog
    if (showSavedNotesDialog && currentPhoneNumber != null) {
        ShowNotesDialog(
            notes = notesForCurrent,
            onDismiss = { showSavedNotesDialog = false },
            onUpdate = { noteEntity, updatedText ->
                CoroutineScope(Dispatchers.IO).launch {
                    val updated = noteEntity.copy(note = updatedText)
                    notesDao.update(updated)
                    val notes = notesDao.getNotesForNumber(currentPhoneNumber!!)
                    withContext(Dispatchers.Main) {
                        notesForCurrent = notes
                        noteCounts = noteCounts.toMutableMap().apply {
                            this[currentPhoneNumber!!] = notes.size
                        }
                    }
                }
            },
            onDelete = { noteEntity ->
                CoroutineScope(Dispatchers.IO).launch {
                    notesDao.delete(noteEntity)
                    val notes = notesDao.getNotesForNumber(currentPhoneNumber!!)
                    withContext(Dispatchers.Main) {
                        notesForCurrent = notes
                        noteCounts = noteCounts.toMutableMap().apply {
                            this[currentPhoneNumber!!] = notes.size
                        }
                    }
                }
            }
        )
    }
}
