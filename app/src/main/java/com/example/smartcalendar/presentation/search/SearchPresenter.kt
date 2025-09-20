package com.example.smartcalendar.presentation.search

import com.example.smartcalendar.data.repo.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SearchPresenter(
    private val repo: EventRepository
) : SearchContract.Presenter {
    private var view: SearchContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun attach(view: SearchContract.View) { this.view = view }
    override fun detach() { this.view = null; scope.cancel() }

    override fun search(query: String) {
        scope.launch {
            if (query.isBlank()) { view?.showEmpty(); return@launch }
            runCatching { repo.search(query) }
                .onSuccess { if (it.isEmpty()) view?.showEmpty() else view?.showResults(it) }
                .onFailure { e -> view?.showError(e.message ?: "Ошибка поиска") }
        }
    }
}