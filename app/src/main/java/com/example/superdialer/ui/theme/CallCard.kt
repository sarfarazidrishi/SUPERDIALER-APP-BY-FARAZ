package com.example.superdialer.ui.theme

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.superdialer.data.CallEntry
import com.example.superdialer.utils.openWhatsApp

@Composable
fun CallCard(
    name: String,
    number: String,
    latest: CallEntry,
    tag: String?,
    noteCount: Int,
    onProfileClick: (String) -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAddTagClick: (String) -> Unit,
    onOpenNotes: (String) -> Unit,
    onNoteAdded: () -> Unit // 🔥 added refresh callback
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 📞 Call button
            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color(0xFF1E88EF)
                )
            }

            // 🧍 Contact details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onProfileClick(number) }
                            .weight(1f)
                    )

                    // ➕ Add dropdown
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.PersonAddAlt1,
                                contentDescription = "Add",
                                tint = Color(0xFF757575)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Add Contact") },
                                onClick = {
                                    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                        type = ContactsContract.RawContacts.CONTENT_TYPE
                                        putExtra(ContactsContract.Intents.Insert.PHONE, number)
                                    }
                                    context.startActivity(intent)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Tag") },
                                onClick = {
                                    showMenu = false
                                    onAddTagClick("Important")
                                }
                            )
                        }
                    }
                }

                // 📞 Type label
                val typeColor = when (latest.type.lowercase()) {
                    "incoming" -> Color(0xFF1976D2)
                    "outgoing" -> Color(0xFF388E3C)
                    "missed" -> Color(0xFFD32F2F)
                    else -> Color.Gray
                }

                Text(
                    text = latest.type,
                    fontSize = 12.sp,
                    color = typeColor
                )
            }

            // 💬 WhatsApp + 📝 Notes (with badge)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(90.dp)
            ) {
                // 💬 WhatsApp
                IconButton(onClick = onWhatsAppClick, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Whatsapp,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366)
                    )
                }

                // 📝 Notes with Badge
                BadgedBox(
                    badge = {
                        if (noteCount > 0) {
                            Badge(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ) {
                                Text(noteCount.toString(), fontSize = 9.sp)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { onOpenNotes(number) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription = "Notes",
                            tint = Color(0xFF1E88E5)
                        )
                    }
                }
            }
        }
    }
}
