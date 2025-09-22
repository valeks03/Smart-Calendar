package com.example.smartcalendar.data.db


import androidx.room.*
import com.example.smartcalendar.data.model.Event


@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startMillis ASC")
    suspend fun getAll(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: Event): Long

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Event?

    @Delete
    suspend fun delete(event: Event)

    @Query("SELECT * FROM events WHERE title LIKE :q ORDER BY startMillis ASC")
    suspend fun searchByTitle(q: String): List<Event>

    @Query("SELECT last_insert_rowid()")
    suspend fun lastId(): Long
}