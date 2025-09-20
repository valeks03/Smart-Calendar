package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.repo.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarPresenter(
    private val repo: EventRepository,
    private val getDefaultReminderMinutes: suspend () -> Int,
    private val scheduleReminder: (id: Long, title: String, startMillis: Long, endMillis: Long, minutesBefore: Int) -> Unit,
    private val cancelReminder: (id: Long) -> Unit = { _ -> }
) : CalendarContract.Presenter {

    private var view: CalendarContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun attach(view: CalendarContract.View) { this.view = view }
    override fun detach() { this.view = null }

    override fun load() {
        scope.launch {
            runCatching { repo.getEvents() }
                .onSuccess { items ->
                    if (items.isEmpty()) view?.showEmptyState() else view?.showEvents(items)
                }
                .onFailure { e -> view?.showError(e.message ?: "Failed to load") }
        }
    }

    override fun createEvent(title: String, startMillis: Long, endMillis: Long, reminderMinutes: Int?) {
        scope.launch {
            runCatching { repo.save(Event(title = title, startMillis = startMillis, endMillis = endMillis)) }
                .onSuccess { newId ->
                    val minutes = reminderMinutes ?: runCatching { getDefaultReminderMinutes() }.getOrDefault(5)
                    try { scheduleReminder(newId, title, startMillis, endMillis, minutes) } catch (_: Throwable) {}
                    load()
                }
                .onFailure { e -> view?.showError(e.message ?: "Failed to save") }
        }
    }

    override fun updateEvent(id: Long, title: String, startMillis: Long, endMillis: Long, reminderMinutes: Int?) {
        scope.launch {
            runCatching { repo.save(Event(id = id, title = title, startMillis = startMillis, endMillis = endMillis)) }
                .onSuccess {
                    val minutes = reminderMinutes ?: runCatching { getDefaultReminderMinutes() }.getOrDefault(5)
                    try {
                        cancelReminder(id)
                        scheduleReminder(id, title, startMillis, endMillis, minutes)
                    } catch (_: Throwable) {}
                    load()
                }
                .onFailure { e -> view?.showError(e.message ?: "Failed to update") }
        }
    }

    override fun deleteEvent(id: Long) {
        scope.launch {
            runCatching {
                repo.delete(Event(id = id, title = "", startMillis = 0, endMillis = 0))
            }.onSuccess {
                cancelReminder(id)
                load()
            }.onFailure { e ->
                view?.showError(e.message ?: "Failed to delete")
            }
        }
    }
}