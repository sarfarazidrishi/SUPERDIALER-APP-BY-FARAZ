package com.example.superdialer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val note: String,  // ✅ Keep only this one
    val timestamp: Long = System.currentTimeMillis()
)