package com.example.smartcalendar.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartcalendar.data.model.Event
import androidx.compose.foundation.lazy.items
import com.example.smartcalendar.presentation.calendar.EventItem
import com.example.smartcalendar.presentation.calendar.RevealSwipeItem
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

@Composable
fun SearchSheet(
    presenter: SearchContract.Presenter,
    onClose: () -> Unit,
    onOpenEvent: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Event>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // MVP "View"
    DisposableEffect(Unit) {
        val view = object : SearchContract.View {
            override fun showResults(items: List<Event>) { results = items; error = null }
            override fun showEmpty() { results = emptyList(); error = null }
            override fun showError(msg: String) { error = msg }
        }
        presenter.attach(view)
        onDispose { presenter.detach() }
    }

    // дебаунс запроса
    LaunchedEffect(query) {
        snapshotFlow { query.trim() }
            .filter { true }
            .debounce(200)
            .collect { presenter.search(it) }
    }

    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Поиск события…") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Закрыть") }
        }

        Divider()

        when {
            error != null -> Text(
                error!!, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            results.isEmpty() && query.isNotBlank() ->
                Text("Ничего не найдено", modifier = Modifier.padding(16.dp))
            else -> LazyColumn(Modifier.fillMaxWidth()) {
                items(results, key = { it.id }) { e ->
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
                                .clickable { onOpenEvent(e) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}