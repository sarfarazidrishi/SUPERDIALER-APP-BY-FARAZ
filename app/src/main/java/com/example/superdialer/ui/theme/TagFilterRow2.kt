package com.example.superdialer.ui.theme

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TagFilterRow(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelect: (String?) -> Unit
) {
    val tags = listOf("All", "Incoming", "Outgoing", "Missed") + allTags

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = tag == selectedTag || (selectedTag == null && tag == "All")
            FilterChip(
                selected = isSelected,
                onClick = { onTagSelect(if (tag == "All") null else tag) },
                label = { Text(tag) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6A1B9A),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF1F1F1),
                    labelColor = Color.Black
                )
            )
        }
    }
}
