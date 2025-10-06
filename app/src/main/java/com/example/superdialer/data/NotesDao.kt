package com.example.superdialer.data

import androidx.room.*

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    suspend fun getNotesForNumber(phoneNumber: String): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}
