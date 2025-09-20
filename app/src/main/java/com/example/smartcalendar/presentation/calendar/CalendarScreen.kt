package com.example.smartcalendar.presentation.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcalendar.data.settings.SettingsRepository
import com.example.smartcalendar.presentation.search.SearchContract
import com.example.smartcalendar.presentation.search.SearchPresenter
import com.example.smartcalendar.presentation.search.SearchSheet
import com.example.smartcalendar.presentation.settings.SettingsSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    presenter: CalendarContract.Presenter,
    searchPresenter: SearchContract.Presenter
) {
    // --- состояния экрана ---
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEmpty by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Event?>(null) }
    var pendingDelete by remember { mutableStateOf<Event?>(null) }

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }           // ← добавили

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // --- настройки (дефолтное напоминание) ---
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val defaultReminder by remember { settings.defaultReminderMinutesFlow() }
        .collectAsState(initial = 5)

    // --- MVP view-прослойка ---
    LaunchedEffect(Unit) {
        presenter.attach(object : CalendarContract.View {
            override fun showEvents(items: List<Event>) {
                events = items; showEmpty = items.isEmpty(); error = null
            }
            override fun showEmptyState() { events = emptyList(); showEmpty = true }
            override fun showError(message: String) { error = message }
        })
        presenter.load()
    }

    // --- диалоги ---
    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, start, end, minutes ->
                presenter.createEvent(title, start, end, minutes)
                showAddDialog = false
            },
            initialReminderMinutes = defaultReminder
        )
    }

    showEditDialog?.let { e ->
        AddEventDialog(
            onDismiss = { showEditDialog = null },
            onConfirm = { title, start, end, minutes ->
                presenter.updateEvent(e.id, title, start, end, minutes)
                showEditDialog = null
            },
            initialTitle = e.title,
            initialStart = e.startMillis,
            initialEnd = e.endMillis,
            initialReminderMinutes = e.reminderMinutes
        )
    }

    pendingDelete?.let { ev ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить событие?") },
            text = { Text(ev.title, color = Color.Black) },
            confirmButton = {
                TextButton(onClick = {
                    presenter.deleteEvent(ev.id)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            }
        )
    }

    // --- Scaffold с нашим TopAppBar и FAB ---
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Smart Calendar", maxLines = 1) },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = { showSettings = true }) {     // ← кнопка настроек
                        Icon(Icons.Default.MoreVert, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets.statusBars,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                error != null -> Text("Ошибка: $error")
                showEmpty     -> Text("Пока нет событий")
                else          -> EventList(
                    events = events,
                    onClick = { e -> showEditDialog = e },
                    onDelete = { e -> pendingDelete = e }
                )
            }
        }
    }

    // --- BottomSheet: поиск ---
    if (showSearch) {
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SearchSheet(
                presenter = searchPresenter,
                onClose = { showSearch = false },
                onOpenEvent = { e -> showEditDialog = e; showSearch = false },
                onDelete = { e -> pendingDelete = e }
            )
        }
    }

    // --- BottomSheet: настройки (выбор дефолтного напоминания) ---
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SettingsSheet(
                settings = settings,
                onClose = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventList(
    events: List<Event>,
    onClick: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    // 1) сортируем
    val sorted = events.sortedBy { it.startMillis }

    // 2) группируем по дню (локальная зона)
    val zone = ZoneId.systemDefault()
    val groups: Map<LocalDate, List<Event>> = sorted.groupBy { millis ->
        Instant.ofEpochMilli(millis.startMillis).atZone(zone).toLocalDate()
    }

    // 3) упорядочим дни
    val orderedDays = groups.keys.sorted()



    LazyColumn(Modifier.fillMaxSize()) {
        orderedDays.forEach { day ->
            stickyHeader {
                DayHeader(day)
            }
            items(groups.getValue(day), key = { it.id }) { e ->
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
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
private fun formatDayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Сегодня"
        today.plusDays(1) -> "Завтра"
        else -> DateTimeFormatter.ofPattern("EEE, d MMMM", Locale("ru"))
            .format(day)
            .replaceFirstChar { it.titlecase(Locale("ru")) }
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = formatDayLabel(day),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}


@Composable
fun EventItem(event: Event, modifier: Modifier = Modifier) {
    val range = formatTimeRange(event.startMillis, event.endMillis)
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(range, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (event.reminderMinutes > 0) {
            SuggestionChip(
                onClick = {},
                label = { Text("${event.reminderMinutes} м") },
                enabled = false,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
private fun formatTimeRange(startMillis: Long, endMillis: Long): String {
    val z = ZoneId.systemDefault()
    val f = DateTimeFormatter.ofPattern("HH:mm")
    val s = Instant.ofEpochMilli(startMillis).atZone(z).toLocalTime()
    val e = Instant.ofEpochMilli(endMillis).atZone(z).toLocalTime()
    return "${f.format(s)} — ${f.format(e)}"
}