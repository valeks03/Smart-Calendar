package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.data.repo.EventRepository
import com.example.smartcalendar.domain.recurrence.nextOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.compareTo

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
            runCatching { rollRepeatsForward() }
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

    private suspend fun rollRepeatsForward(now: Long = System.currentTimeMillis()) {
        // Берём все события и прокручиваем только те, что с повтором и уже прошли
        val items = runCatching { repo.getEvents() }.getOrElse { return }
        for (e in items) {
            if (e.repeatType != RepeatType.NONE && e.endMillis < now) {
                val next = nextOccurrence(e, after = now) ?: continue
                // переносим событие вперёд "на месте"
                val moved = e.copy(
                    startMillis = next.first,
                    endMillis   = next.second
                )
                runCatching { repo.save(moved) }
                runCatching {
                    cancelReminder(e.id) // старый таймер уже не актуален
                    scheduleReminder(
                        moved.id,
                        moved.title,
                        moved.startMillis,
                        moved.endMillis,
                        moved.reminderMinutes,
                        moved.repeatType,
                        moved.repeatInterval,
                        moved.repeatUntilMillis,
                        moved.repeatDaysMask)
                }
            }
        }
    }
    override fun snoozeEvent(id: Long, minutes: Int) {
        scope.launch {
            val event = runCatching { repo.getById(id) }.getOrNull() ?: return@launch
            val moved = event.copy(
                startMillis = event.startMillis + minutes * 60_000,
                endMillis   = event.endMillis + minutes * 60_000
            )
            runCatching { repo.save(moved) }
                .onSuccess {
                    try {
                        cancelReminder(event.id)
                        scheduleReminder(
                            moved.id,
                            moved.title,
                            moved.startMillis,
                            moved.endMillis,
                            moved.reminderMinutes,
                            moved.repeatType,
                            moved.repeatInterval,
                            moved.repeatUntilMillis,
                            moved.repeatDaysMask
                        )
                    } catch (_: Throwable) {}
                    load()
                }
        }
    }
}
