package com.example.smartcalendar.presentation.calendar


import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.repo.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CalendarPresenter(
    private val repo: EventRepository
) : CalendarContract.Presenter {

    private var view: CalendarContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun attach(view: CalendarContract.View) {
        this.view = view
        load()
    }
    override fun detach() { view = null; job?.cancel() }

    override fun load() {
        job?.cancel()
        job = scope.launch {
            runCatching { repo.getEvents() }
                .onSuccess { list ->
                    if (list.isEmpty()) view?.showEmptyState() else view?.showEvents(list)
                }
                .onFailure { e -> view?.showError(e.message ?: "Unknown error") }
        }
    }

    override fun createEvent(title: String, startMillis: Long, endMillis: Long) {
        scope.launch {
            runCatching {
                repo.save(Event(title = title, startMillis = startMillis, endMillis = endMillis))
            }.onSuccess {
                load()
            }.onFailure { e ->
                view?.showError(e.message ?: "Failed to save")
            }
        }
    }

    override fun updateEvent(id: Long, title: String, startMillis: Long, endMillis: Long) {
        scope.launch {
            runCatching { repo.save(Event(id = id, title = title, startMillis = startMillis, endMillis = endMillis)) }
                .onSuccess { load() }
                .onFailure { view?.showError(it.message ?: "Failed to update") }
        }
    }

    override fun deleteEvent(id: Long) {
        scope.launch {
            runCatching { repo.delete(Event(id = id, title = "", startMillis = 0, endMillis = 0)) }
                .onSuccess { load() }
                .onFailure { view?.showError(it.message ?: "Failed to delete") }
        }
    }
}