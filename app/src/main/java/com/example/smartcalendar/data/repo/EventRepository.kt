package com.example.smartcalendar.data.repo


import com.example.smartcalendar.data.model.Event


interface EventRepository {
    suspend fun getEvents(): List<Event>
    suspend fun save(event: Event): Long
    suspend fun delete(event: Event)
    suspend fun getLastId(): Long
}