package com.example.superdialer.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun TagFilterRow(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelect: (String?) -> Unit
) {
    val defaultTags = listOf("All", "Incoming", "Outgoing", "Missed")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(defaultTags + allTags.distinct()) { tag ->
            FilterChip(
                selected = selectedTag == tag || (tag == "All" && selectedTag == null),
                onClick = { onTagSelect(if (tag == "All") null else tag) },
                label = { Text(tag) }
            )
        }
    }
}
