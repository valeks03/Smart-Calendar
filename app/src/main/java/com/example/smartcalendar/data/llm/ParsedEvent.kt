package com.example.smartcalendar.data.llm

data class ParsedEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val reminderMinutes: Int?,   // null, если не указано
    val repeatType: String,      // "NONE"|"DAILY"|"WEEKLY"|"MONTHLY"
    val repeatInterval: Int?,    // >=1 или null
    val repeatDaysMask: Int?,    // битовая маска пн..вс или null
    val repeatUntilMillis: Long? // null, если без даты окончания
)
