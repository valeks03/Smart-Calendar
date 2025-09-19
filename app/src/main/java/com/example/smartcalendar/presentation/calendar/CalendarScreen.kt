package com.example.smartcalendar.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartcalendar.data.model.Event
import kotlinx.coroutines.launch
import java.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CalendarScreen(presenter: CalendarContract.Presenter) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEmpty by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Event?>(null) }

    val view = object : CalendarContract.View {
        override fun showEvents(items: List<Event>) {
            events = items; showEmpty = items.isEmpty(); error = null
        }
        override fun showEmptyState() { events = emptyList(); showEmpty = true }
        override fun showError(message: String) { error = message }
    }

    LaunchedEffect(Unit) { presenter.attach(view); presenter.load() }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, start, end ->
                presenter.createEvent(title, start, end)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                error != null -> Text("Ошибка: $error")
                showEmpty -> Text("Пока нет событий")
                else -> EventList(
                    events = events,
                    onClick = { e -> showEditDialog = e },
                    onDelete = { e -> presenter.deleteEvent(e.id) }
                )
            }
        }
    }
}
@Composable
fun EventList(events: List<Event>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events) { e ->
            EventItem(e)
        }
    }
}

@Composable
fun EventItem(event: Event) {
    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = event.title, style = MaterialTheme.typography.titleMedium)
        Text(text = "${df.format(event.startMillis)} — ${df.format(event.endMillis)}")
    }
}


@Composable
fun EventList(
    events: List<Event>,
    onClick: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(events, key = { it.id }) { e ->
            RevealSwipeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                onDelete = { onDelete(e) }
            ) {
                EventItem(
                    event = e,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(e) }
                )
            }
        }
    }
}


@Composable
fun EventItem(event: Event, modifier: Modifier = Modifier) {
    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    Column(modifier = modifier) {
        Text(event.title, style = MaterialTheme.typography.titleMedium)
        Text("${df.format(event.startMillis)} — ${df.format(event.endMillis)}")
    }
}
