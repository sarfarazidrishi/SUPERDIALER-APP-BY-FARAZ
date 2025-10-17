package com.example.superdialer.ui.theme

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.superdialer.data.*
import com.example.superdialer.utils.openSmsApp
import com.example.superdialer.utils.openVideoCall
import com.example.superdialer.utils.openWhatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.TextFieldDefaults


@SuppressLint("DefaultLocale")
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CallCard(
    name: String,
    number: String,
    latest: CallEntry,
    tag: String?,
    allTags: List<String>,
    noteCount: Int,
    expandedNumber: String?,
    onExpandChange: (String?) -> Unit,
    onProfileClick: (String) -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAddTagClick: (String) -> Unit,
    onOpenNotes: (String) -> Unit,
    onNoteAdded: () -> Unit,
    navToHistory: (String) -> Unit
) {
    val context = LocalContext.current
    val db = NotesDatabase.getDatabase(context)
    val tagDao = db.tagsDao()
    val scope = rememberCoroutineScope()

    var showTagDialog by remember { mutableStateOf(false) }
    //var allTags by remember { mutableStateOf<List<String>>(emptyList()) }
//    val allTags by viewModel.allTags.collectAsState()

    val expanded = expandedNumber == number

    val formattedTime = remember(latest.date) {
        try { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(latest.date)) } catch (_: Exception) { "" }
    }

    // ✅ Properly parse durations (seconds or "mm:ss" or empty)
    val formattedDuration = remember(latest.duration) {
        try {
            val raw = latest.duration.trim()
            when {
                raw.contains(":") -> raw // already formatted like 02:35
                raw.all { it.isDigit() } -> {
                    var totalSeconds = raw.toLong()
                    if (totalSeconds > 3600) totalSeconds /= 1000 // ✅ convert ms to sec if needed
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    String.format("%02d:%02d", minutes, seconds)
                }
                else -> "--:--"
            }
        } catch (_: Exception) { "--:--" }
    }


    // Load all available tags
//    LaunchedEffect(showTagDialog) {
//        try {
//            allTags = tagDao.getAllTags()
//        } catch (_: Exception) { allTags = emptyList() }
//    }

    // Call type icons and colors
    val (iconType, typeColor) = when (latest.type.lowercase(Locale.getDefault())) {
        "incoming" -> Icons.Default.CallReceived to Color(0xFF4CAF50)
        "outgoing" -> Icons.Default.CallMade to Color(0xFF2196F3)
        "missed" -> Icons.Default.CallMissed to Color(0xFFF44336)
        else -> Icons.Default.Call to Color.Gray
    }

    // Pastel tag color generator
    fun tagColor(tag: String): Color {
        val colors = listOf(
            Color(0xFFE3F2FD), // light blue
            Color(0xFFF3E5F5), // light purple
            Color(0xFFE8F5E9), // mint
            Color(0xFFFFF8E1), // soft yellow
            Color(0xFFFFEBEE), // blush pink
            Color(0xFFE0F7FA)  // cyan
        )
        return colors[tag.hashCode().absoluteValue % colors.size]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clickable { onExpandChange(if (expanded) null else number) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FF)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {

            // 🔹 Header Row
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1C4E9))
                        .clickable { onProfileClick(number) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name.isNotBlank()) name.first().uppercase() else "#",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF4A148C)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Name + Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (name.isNotBlank()) name else number,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = iconType,
                            contentDescription = latest.type,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Duration + Tag row
                    // Duration + Tag row
                    if (name == number) { // ✅ Unsaved number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formattedDuration,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            if (!tag.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .background(tagColor(tag), RoundedCornerShape(8.dp))
                                        .clickable { showTagDialog = true } // 👈 opens bottom sheet
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        color = Color(0xFF333333)
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = { showTagDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+ Add Tag", color = Color(0xFF6A1B9A), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // ✅ Saved contact — show duration only
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formattedDuration,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(onClick = onCallClick) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF3A86FF))
                }

                IconButton(onClick = onWhatsAppClick) {
                    Icon(Icons.Default.Whatsapp, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                }
            }

            // Expanded Section
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(160)) + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Divider(color = Color(0xFFE0E0E0))
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { navToHistory(number) }) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF6A1B9A))
                        }

                        BadgedBox(badge = {
                            if (noteCount > 0) {
                                Badge(containerColor = Color(0xFFD32F2F), contentColor = Color.White) {
                                    Text(noteCount.toString(), fontSize = 9.sp)
                                }
                            }
                        }) {
                            IconButton(onClick = { onOpenNotes(number) }) {
                                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Notes", tint = Color(0xFF1E88E5))
                            }
                        }

                        IconButton(onClick = { openSmsApp(context, number) }) {
                            Icon(Icons.Default.Message, contentDescription = "Message", tint = Color(0xFF009688))
                        }

                        IconButton(onClick = { openVideoCall(context, number) }) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = Color(0xFFE91E63))
                        }
                    }
                }
            }
        }
    }

    // 🔹 Add Tag Dialog
    // 🧩 Bottom Sheet Tag Editor
    if (showTagDialog) {
        var tagText by remember { mutableStateOf(tag ?: "") }
        val isEditing = !tag.isNullOrBlank()
        val context = LocalContext.current
        val viewModel: DialerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showTagDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFFF9F9FF),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Tag" else "Add Tag",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF4A148C)
                )

                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    placeholder = { Text("Enter or select tag") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF151313),
                        unfocusedTextColor = Color(0xFF151313),
                        cursorColor = Color(0xFF151313),
                        focusedBorderColor = Color(0xFF3F3650),
                        unfocusedBorderColor = Color(0xFFBBBBBB),
                        focusedPlaceholderColor = Color(0xFF777777),
                        unfocusedPlaceholderColor = Color(0xFF777777)
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    items(allTags) { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tagText = suggestion
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            color = Color(0xFF151313),
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 🗑 Delete
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteTag(context, number)
                                }
                                showTagDialog = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Tag",
                                tint = Color.Red
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = Color.Red)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ❌ Cancel
                    TextButton(onClick = { showTagDialog = false }) {
                        Text("Cancel", color = Color.Black)
                    }

                    // 💾 Save / Update
                    Button(
                        onClick = {
                            if (tagText.isNotBlank()) {
                                scope.launch {
                                    if (isEditing) viewModel.updateTag(context, number, tagText)
                                    else viewModel.addTag(context, number, tagText)
                                }
                                showTagDialog = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEditing) "Update" else "Save")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
