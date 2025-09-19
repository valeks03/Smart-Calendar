package com.example.smartcalendar.data.db


import androidx.room.*
import com.example.smartcalendar.data.model.Event


@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startMillis ASC")
    suspend fun getAll(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: Event): Long

    @Delete
    suspend fun delete(event: Event)
}