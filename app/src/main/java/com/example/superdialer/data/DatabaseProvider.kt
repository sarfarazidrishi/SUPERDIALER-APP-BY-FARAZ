package com.example.superdialer.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var db: NotesDatabase? = null

    fun getDatabase(context: Context): NotesDatabase {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java,
                "notes_db"
            ).build()
        }
        return db!!
    }
}
