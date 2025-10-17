package com.example.superdialer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Query("SELECT * FROM notes WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getNotesForNumber(phoneNumber: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    // ✅ NEW: Reactive projection — emits only note counts
    @Query("SELECT phoneNumber, COUNT(*) as count FROM notes GROUP BY phoneNumber")
    fun observeNoteCounts(): Flow<List<NoteCountProjection>>

    @Insert
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}

// ✅ Lightweight data holder for Flow projection
data class NoteCountProjection(
    val phoneNumber: String,
    val count: Int
)
