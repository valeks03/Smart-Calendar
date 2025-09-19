package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event


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
        fun createEvent(title: String, startMillis: Long, endMillis: Long)
        fun updateEvent(id: Long, title: String, startMillis: Long, endMillis: Long)
        fun deleteEvent(id: Long)
    }
}