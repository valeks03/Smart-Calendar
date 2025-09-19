package com.example.smartcalendar.data.db


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smartcalendar.data.model.Event


@Database(
    entities = [Event::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}