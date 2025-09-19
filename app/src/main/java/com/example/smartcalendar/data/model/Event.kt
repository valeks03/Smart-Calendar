package com.example.smartcalendar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startMillis: Long,   // UTC millis
    val endMillis: Long,     // UTC millis
    val tag: String? = null
)