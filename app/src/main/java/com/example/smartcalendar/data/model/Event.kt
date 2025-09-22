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
    val reminderMinutes: Int = 5,
    val tag: String? = null,

    val repeatType: RepeatType = RepeatType.NONE,
    val repeatInterval: Int = 1,              // каждый N дней/недель/месяцев
    val repeatUntilMillis: Long? = null,      // дата окончания серии (включительно), null — бесконечно
    val repeatDaysMask: Int? = null
)

enum class RepeatType { NONE, DAILY, WEEKLY, MONTHLY }