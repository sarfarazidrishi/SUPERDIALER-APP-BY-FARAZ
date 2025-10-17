package com.example.superdialer.data
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Dao
interface TagsDao {
    @Query("SELECT DISTINCT tag FROM tags")
    fun observeAllTags(): Flow<List<String>>

    // 🌀 Reactive stream for all tags (per contact)
    @Query("SELECT * FROM tags")
    fun observeAllTagEntities(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE phoneNumber = :phoneNumber")
    suspend fun getTagsForNumber(phoneNumber: String): List<TagEntity>

    @Insert
    suspend fun insert(tag: TagEntity)

    @Query("UPDATE tags SET tag = :newTag WHERE phoneNumber = :phoneNumber")
    suspend fun updateTag(phoneNumber: String, newTag: String)

    @Query("DELETE FROM tags WHERE phoneNumber = :phoneNumber")
    suspend fun clearTagsForNumber(phoneNumber: String)

    @Query("SELECT DISTINCT tag FROM tags")
    suspend fun getAllTags(): List<String>



}
