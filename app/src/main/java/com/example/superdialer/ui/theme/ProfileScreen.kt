package com.example.superdialer.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.superdialer.data.CallEntry
import com.example.superdialer.data.NoteEntity
import com.example.superdialer.data.NotesDatabase
import com.example.superdialer.data.getCallHistory
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    number: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NotesDatabase.getDatabase(context) }
    val dao = db.notesDao()
    var callHistory by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var notes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }
    var filter by remember { mutableStateOf("All") }
    var showNotes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        callHistory = getCallHistory(context).filter { it.number == number }
        notes = dao.getNotesForNumber(number)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile: $number") },
                navigationIcon = { IconButton(onClick = onBack) { Text("⬅️") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 🔹 Quick Actions
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { openWhatsApp(context, number) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text("💬")
                }
                IconButton(
                    onClick = { /* TODO: Implement call */ },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                }
                Button(onClick = { showNotes = !showNotes }) {
                    Text(if (showNotes) "Hide Notes" else "Show Notes")
                }
            }

            // 🔹 Filter Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("All", "Incoming", "Outgoing", "Missed").forEach { type ->
                    FilterChip(
                        selected = filter == type,
                        onClick = { filter = type },
                        label = { Text(type) }
                    )
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            if (showNotes) {
                // Accordion notes
                Text("🗒️ Notes (${notes.size})", fontWeight = FontWeight.Bold)
                LazyColumn {
                    items(notes) { note ->
                        var expanded by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clickable { expanded = !expanded }
                            ) {
                                Text(
                                    note.note.take(30),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (expanded) Text(note.note)
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        }
                    }
                }
            } else {
                // 🔹 Filtered Call History
                val displayed = when (filter) {
                    "Incoming" -> callHistory.filter { it.type.contains("Incoming", true) }
                    "Outgoing" -> callHistory.filter { it.type.contains("Outgoing", true) }
                    "Missed" -> callHistory.filter { it.type.contains("Missed", true) }
                    else -> callHistory
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(displayed) { call ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(call.type, fontWeight = FontWeight.Bold)
                                Text(call.duration)
                            }
                        }
                    }
                }
            }
        }
    }
}
