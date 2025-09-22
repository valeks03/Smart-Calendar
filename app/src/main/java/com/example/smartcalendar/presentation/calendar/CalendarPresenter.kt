package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.data.repo.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarPresenter(
    private val repo: EventRepository,
    private val getDefaultReminderMinutes: suspend () -> Int,
    private val scheduleReminder: (
        id: Long,
        title: String,
        startMillis: Long,
        endMillis: Long,
        minutesBefore: Int,
        repeatType: RepeatType,
        repeatInterval: Int,
        repeatUntilMillis: Long?,
        repeatDaysMask: Int?
    ) -> Unit,
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

    override fun createEvent(
        title: String,
        startMillis: Long,
        endMillis: Long,
        reminderMinutes: Int?,
        repeatType: RepeatType,
        repeatInterval: Int,
        repeatUntilMillis: Long?,
        repeatDaysMask: Int?
    ) {
        scope.launch {
            val minutes = reminderMinutes ?: runCatching { getDefaultReminderMinutes() }.getOrDefault(5)
            val event = Event(
                title = title,
                startMillis = startMillis,
                endMillis = endMillis,
                reminderMinutes = minutes,
                repeatType = repeatType,
                repeatInterval = repeatInterval,
                repeatUntilMillis = repeatUntilMillis,
                repeatDaysMask = repeatDaysMask
            )
            runCatching { repo.save(event) } // ожидаем, что вернёт id созданной записи
                .onSuccess { newId ->
                    try {
                        scheduleReminder(
                            newId, title, startMillis, endMillis, minutes,
                            repeatType, repeatInterval, repeatUntilMillis, repeatDaysMask
                        )
                    } catch (_: Throwable) {}
                    load()
                }
                .onFailure { e -> view?.showError(e.message ?: "Failed to save") }
        }
    }

    override fun updateEvent(
        id: Long,
        title: String,
        startMillis: Long,
        endMillis: Long,
        reminderMinutes: Int?,
        repeatType: RepeatType,
        repeatInterval: Int,
        repeatUntilMillis: Long?,
        repeatDaysMask: Int?
    ) {
        scope.launch {
            val minutes = reminderMinutes ?: runCatching { getDefaultReminderMinutes() }.getOrDefault(5)
            val event = Event(
                id = id,
                title = title,
                startMillis = startMillis,
                endMillis = endMillis,
                reminderMinutes = minutes,
                repeatType = repeatType,
                repeatInterval = repeatInterval,
                repeatUntilMillis = repeatUntilMillis,
                repeatDaysMask = repeatDaysMask
            )
            runCatching { repo.save(event) }
                .onSuccess {
                    try {
                        cancelReminder(id)
                        scheduleReminder(
                            id, title, startMillis, endMillis, minutes,
                            repeatType, repeatInterval, repeatUntilMillis, repeatDaysMask
                        )
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