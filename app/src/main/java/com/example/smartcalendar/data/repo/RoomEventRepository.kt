package com.example.smartcalendar.data.repo


import android.content.Context
import androidx.room.Room
import com.example.smartcalendar.data.db.AppDatabase
import com.example.smartcalendar.data.model.Event


class RoomEventRepository(context: Context) : EventRepository {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "smart_calendar.db"
    ).fallbackToDestructiveMigration().build()


    private val dao = db.eventDao()


    override suspend fun getEvents() = dao.getAll()
    override suspend fun save(event: Event) = dao.upsert(event)
    override suspend fun delete(event: Event) = dao.delete(event)
}