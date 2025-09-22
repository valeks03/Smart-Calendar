package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType


interface CalendarContract {
    interface View {
        fun showEvents(items: List<Event>)
        fun showEmptyState()
        fun showError(message: String)
    }
    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun load()
        fun createEvent(
            title: String,
            startMillis: Long,
            endMillis: Long,
            reminderMinutes: Int? = null,
            repeatType: RepeatType = RepeatType.NONE,
            repeatInterval: Int = 1,
            repeatUntilMillis: Long? = null,
            repeatDaysMask: Int? = null
        )
        fun updateEvent(
            id: Long,
            title: String,
            startMillis: Long,
            endMillis: Long,
            reminderMinutes: Int? = null,
            repeatType: RepeatType = RepeatType.NONE,
            repeatInterval: Int = 1,
            repeatUntilMillis: Long? = null,
            repeatDaysMask: Int? = null
        )

        fun deleteEvent(id: Long)
    }
}