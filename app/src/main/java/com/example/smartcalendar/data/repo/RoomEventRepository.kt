package com.example.smartcalendar.data.repo


import android.content.Context
import androidx.room.Room
import com.example.smartcalendar.data.db.AppDatabase
import com.example.smartcalendar.data.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RoomEventRepository(context: Context) : EventRepository {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "smart_calendar.db"
    ).fallbackToDestructiveMigration().build()


    private val dao = db.eventDao()


    override suspend fun getEvents() = dao.getAll()
    override suspend fun save(event: Event): Long = dao.upsert(event)
    override suspend fun delete(event: Event) = dao.delete(event)
    override suspend fun getLastId(): Long = dao.lastId()
    override suspend fun search(query: String): List<Event> =
        withContext(Dispatchers.IO) { dao.searchByTitle("%$query%") }
}