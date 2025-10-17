package com.example.superdialer.ui.theme

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.navigation.NavController
import com.example.superdialer.data.NotesDatabase
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DialerScreen(
    navController: NavController,
    onProfileClick: (String) -> Unit,
    viewModel: DialerViewModel = viewModel()
) {
    var showNotesDialog by remember { mutableStateOf(false) }
    var selectedNumberForNotes by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val db = remember { NotesDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val callHistory by viewModel.callHistory.collectAsState()
    val noteCounts by viewModel.noteCounts.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val contactNames by viewModel.contactNames.collectAsState()

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showDialPad by remember { mutableStateOf(false) }

    var expandedNumber by remember { mutableStateOf<String?>(null) }

    // Load data once on start
    LaunchedEffect(Unit) {
        viewModel.loadData(context, db)
        viewModel.observeTags(db)
        viewModel.observeNotes(db)
    }

    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )
    // 🚀 Ask for all required permissions
    val permissions = arrayOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val denied = permissionsMap.filterValues { !it }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(context, "Please grant all permissions for full functionality", Toast.LENGTH_LONG).show()
        } else {
            // Re-load data once permissions are granted
            viewModel.loadData(context, db)
        }
    }

// ask permissions when screen starts
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
    }


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialPad = !showDialPad }, containerColor = MaterialTheme.colorScheme.primary) {
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
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = { Text("Search or dial number") },
                shape = RoundedCornerShape(50.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TagFilterRow(allTags = allTags, selectedTag = selectedTag, onTagSelect = { selectedTag = it })

            Spacer(modifier = Modifier.height(8.dp))

            val filteredHistory = remember(callHistory, selectedTag) {
                when (selectedTag) {
                    null, "All" -> callHistory
                    "Incoming" -> callHistory.filter { it.type.equals("Incoming", true) }
                    "Outgoing" -> callHistory.filter { it.type.equals("Outgoing", true) }
                    "Missed" -> callHistory.filter { it.type.equals("Missed", true) }
                    else -> callHistory.filter { tags[it.number] == selectedTag }
                }
            }

            // Group by date & merge consecutive same-number entries (so repeated in row show once)
            val groupedByDate = remember(filteredHistory) {
                filteredHistory
                    .groupBy { call ->
                        val callDate = Date(call.date)
                        val today = Calendar.getInstance()
                        val callCal = Calendar.getInstance().apply { time = callDate }

                        when {
                            isSameDay(callCal, today) -> "Today"
                            isYesterday(callCal, today) -> "Yesterday"
                            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(callDate)
                        }
                    }
                    .mapValues { (_, calls) ->
                        buildList {
                            var lastNumber: String? = null
                            for (call in calls) {
                                if (call.number != lastNumber) {
                                    add(call)
                                }
                                lastNumber = call.number
                            }
                        }
                    }
                    .toSortedMap(compareByDescending { parseDateKey(it) })
            }

            if (groupedByDate.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No calls found.\nGrant call log permission or check contacts.", textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groupedByDate.forEach { (dateHeader, calls) ->
                        item {
                            Text(text = dateHeader, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Color(0xFF6A1B9A), modifier = Modifier.padding(vertical = 6.dp))
                        }

                        itemsIndexed(calls) { _, call ->
                            val name = contactNames[call.number] ?: call.number
                            val noteCount = noteCounts[call.number] ?: 0
                            val tag = tags[call.number]

                            CallCard(
                                name = name,
                                number = call.number,
                                latest = call,
                                tag = tag,
                                allTags = allTags,
                                noteCount = noteCount,
                                expandedNumber = expandedNumber,
                                onExpandChange = { expandedNumber = it },
                                onProfileClick = onProfileClick,
                                onCallClick = {
                                    callLauncher.launch(Manifest.permission.CALL_PHONE)
                                    val intent = Intent(Intent.ACTION_CALL, "tel:${call.number}".toUri())
                                    context.startActivity(intent)
                                },
                                onWhatsAppClick = {
                                    try { openWhatsApp(context, call.number) } catch (e: Exception) { Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show() }
                                },
                                onAddTagClick = { tagText ->
                                    scope.launch {
                                        if (tagText.isNotBlank()) {
                                            viewModel.addTag(context, call.number, tagText)
                                        } else {
                                            viewModel.deleteTag(context, call.number)
                                        }
                                    }
                                },
                                onOpenNotes = { clickedNumber ->
                                    selectedNumberForNotes = clickedNumber
                                    showNotesDialog = true
                                },
                                onNoteAdded = { viewModel.addNote(context, call.number, "New Note") },
                                navToHistory = { clickedNumber -> navController.navigate("profileWithHistory/$clickedNumber") }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotesDialog && selectedNumberForNotes != null) {
        NotesDialog(number = selectedNumberForNotes!!, onDismiss = {
            showNotesDialog = false
            selectedNumberForNotes = null
            scope.launch { viewModel.loadData(context, db) }
        })
    }

    AnimatedVisibility(visible = showDialPad) {
        DialPadOverlay(phoneNumber = phoneNumber, onNumberChange = { phoneNumber = it }, onClose = { showDialPad = false }, onCall = {
            callLauncher.launch(Manifest.permission.CALL_PHONE)
            if (phoneNumber.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                context.startActivity(intent)
            }
        })
    }
}

// helper fns unchanged...
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

private fun isYesterday(cal1: Calendar, today: Calendar): Boolean {
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal1, yesterday)
}

private fun parseDateKey(key: String): Long {
    return try {
        when (key) {
            "Today" -> System.currentTimeMillis()
            "Yesterday" -> System.currentTimeMillis() - 24 * 60 * 60 * 1000
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(key)?.time ?: 0L
        }
    } catch (e: Exception) {
        0L
    }
}
