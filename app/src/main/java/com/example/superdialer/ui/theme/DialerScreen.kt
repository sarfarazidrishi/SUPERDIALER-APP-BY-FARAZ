package com.example.superdialer.ui.theme

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.superdialer.data.NotesDatabase
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.launch

@Composable
fun DialerScreen(
    onProfileClick: (String) -> Unit,
    viewModel: DialerViewModel = viewModel()
) {
    var showNotesDialog by remember { mutableStateOf(false) }
    var selectedNumberForNotes by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val db = remember { NotesDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // 🧠 State flows
    val callHistory by viewModel.callHistory.collectAsState()
    val noteCounts by viewModel.noteCounts.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val contactNames by viewModel.contactNames.collectAsState()

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showDialPad by remember { mutableStateOf(false) }

    // 🚀 Load data when screen starts
    LaunchedEffect(Unit) {
        viewModel.loadData(context, db)
    }

    // ☎️ Permission launcher
    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // 🧱 UI Layout
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialPad = !showDialPad },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = "Dial Pad")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 🔍 Search bar
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = { Text("Search or dial number") },
                shape = RoundedCornerShape(50.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Tag filters
            TagFilterRow(
                allTags = allTags,
                selectedTag = selectedTag,
                onTagSelect = { selectedTag = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🧮 Filtered call history
            val filteredHistory = remember(callHistory, selectedTag) {
                when (selectedTag) {
                    null, "All" -> callHistory
                    "Incoming" -> callHistory.filter { it.type.equals("Incoming", true) }
                    "Outgoing" -> callHistory.filter { it.type.equals("Outgoing", true) }
                    "Missed" -> callHistory.filter { it.type.equals("Missed", true) }
                    else -> callHistory.filter { tags[it.number] == selectedTag }
                }
            }

            // Group by unique numbers
            val grouped = remember(filteredHistory) {
                filteredHistory.groupBy { it.number }
                    .mapValues { it.value.first() }
                    .toList()
            }

            if (grouped.isEmpty()) {
                // 👻 Empty-state view
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No calls found.\nGrant call log permission or check contacts.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            } else {
                // 📞 List of calls
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(grouped.size) { index ->
                        val (number, latest) = grouped[index]
                        val name = contactNames[number] ?: number
                        val noteCount = noteCounts[number] ?: 0
                        val tag = tags[number]

                        CallCard(
                            name = name,
                            number = number,
                            latest = latest,
                            tag = tag,
                            noteCount = noteCount,
                            onProfileClick = onProfileClick,
                            onCallClick = {
                                callLauncher.launch(Manifest.permission.CALL_PHONE)
                                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                                context.startActivity(intent)
                            },
                            onWhatsAppClick = {
                                try {
                                    openWhatsApp(context, number)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "WhatsApp not installed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onAddTagClick = { tagText ->
                                scope.launch {
                                    viewModel.addTag(context, number, tagText)
                                }
                            },
                            onOpenNotes = { clickedNumber ->
                                selectedNumberForNotes = clickedNumber
                                showNotesDialog = true
                            },
                            onNoteAdded = {
                                scope.launch { viewModel.loadData(context, db) }
                            }
                        )
                    }
                }
            }
        }
    }

    // 🗒️ Notes Dialog
    if (showNotesDialog && selectedNumberForNotes != null) {
        NotesDialog(
            number = selectedNumberForNotes!!,
            onDismiss = {
                showNotesDialog = false
                selectedNumberForNotes = null
                scope.launch { viewModel.loadData(context, db) } // refresh after close
            }
        )
    }

    // 🔢 Dial pad overlay
    AnimatedVisibility(visible = showDialPad) {
        DialPadOverlay(
            phoneNumber = phoneNumber,
            onNumberChange = { phoneNumber = it },
            onClose = { showDialPad = false },
            onCall = {
                callLauncher.launch(Manifest.permission.CALL_PHONE)
                if (phoneNumber.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                    context.startActivity(intent)
                }
            }
        )
    }
}
