package com.example.superdialer.data
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Dao

@Dao
interface TagsDao {

    @Query("SELECT * FROM tags WHERE phoneNumber = :phoneNumber")
    suspend fun getTagsForNumber(phoneNumber: String): List<TagEntity>

    @Insert
    suspend fun insert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE phoneNumber = :phoneNumber")
    suspend fun clearTagsForNumber(phoneNumber: String)

    @Query("SELECT DISTINCT tag FROM tags")
    suspend fun getAllTags(): List<String>
}