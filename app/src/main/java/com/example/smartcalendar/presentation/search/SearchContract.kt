package com.example.smartcalendar.presentation.search

import com.example.smartcalendar.data.model.Event

interface SearchContract {
    interface View {
        fun showResults(items: List<Event>)
        fun showEmpty()
        fun showError(msg: String)
    }
    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun search(query: String)
    }
}