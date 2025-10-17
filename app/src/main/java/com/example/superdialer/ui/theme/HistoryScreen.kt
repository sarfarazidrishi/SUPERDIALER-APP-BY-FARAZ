package com.example.superdialer.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.superdialer.data.CallEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    number: String,
    callEntries: List<CallEntry>,
    onBack: () -> Unit
) {
    val tabs = listOf("All", "Incoming", "Outgoing", "Missed")
    var selectedTab by remember { mutableStateOf(0) }

    val filtered = remember(callEntries, selectedTab) {
        when (selectedTab) {
            1 -> callEntries.filter { it.type.equals("Incoming", true) }
            2 -> callEntries.filter { it.type.equals("Outgoing", true) }
            3 -> callEntries.filter { it.type.equals("Missed", true) }
            else -> callEntries
        }
    }.sortedByDescending { it.date }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = number) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No call history for this number.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    items(filtered) { entry ->
                        CallHistoryRow(entry)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryRow(entry: CallEntry) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val time = try { sdf.format(Date(entry.date)) } catch (_: Exception) { "" }

    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val (icon, tint) = when (entry.type.lowercase()) {
            "incoming" -> Icons.Default.CallReceived to androidx.compose.ui.graphics.Color(0xFF1976D2)
            "outgoing" -> Icons.Default.CallMade to androidx.compose.ui.graphics.Color(0xFF388E3C)
            "missed" -> Icons.Default.CallMissed to androidx.compose.ui.graphics.Color(0xFFD32F2F)
            else -> Icons.Default.CallMade to androidx.compose.ui.graphics.Color.Gray
        }

        Icon(icon, contentDescription = entry.type, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))

        Column {
            Text(text = entry.type, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(text = "$time • ${entry.duration ?: 0}s", style = MaterialTheme.typography.bodySmall)
        }
    }
}
