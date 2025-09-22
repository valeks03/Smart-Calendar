package com.example.smartcalendar.presentation.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.data.settings.SettingsRepository
import com.example.smartcalendar.presentation.search.SearchContract
import com.example.smartcalendar.presentation.search.SearchSheet
import com.example.smartcalendar.presentation.settings.SettingsSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.smartcalendar.BuildConfig
import com.example.smartcalendar.data.llm.LlmEventParser
import com.example.smartcalendar.data.llm.OpenAiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    presenter: CalendarContract.Presenter,
    searchPresenter: SearchContract.Presenter
) {
    // ---------- state ----------
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEmpty by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Event?>(null) }
    var pendingDelete by remember { mutableStateOf<Event?>(null) }

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // ---------- Quick Add (LLM) ----------
    val scope = rememberCoroutineScope()
    var nlText by remember { mutableStateOf("") }
    val llm = remember { LlmEventParser(OpenAiClient.api(BuildConfig.OPENAI_API_KEY)) }

    // ---------- settings (default reminder) ----------
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val defaultReminder by remember { settings.defaultReminderMinutesFlow() }
        .collectAsState(initial = 5)

    // ---------- MVP view adapter ----------
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

    // ---------- dialogs ----------
    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, start, end, minutes, type, interval, until, mask ->
                presenter.createEvent(
                    title = title,
                    startMillis = start,
                    endMillis = end,
                    reminderMinutes = minutes,
                    repeatType = type,
                    repeatInterval = interval,
                    repeatUntilMillis = until,
                    repeatDaysMask = mask
                )
                showAddDialog = false
            },
            initialReminderMinutes = defaultReminder
        )
    }

    showEditDialog?.let { e ->
        AddEventDialog(
            onDismiss = { showEditDialog = null },
            onConfirm = { title, start, end, minutes, type, interval, until, mask ->
                presenter.updateEvent(
                    id = e.id,
                    title = title,
                    startMillis = start,
                    endMillis = end,
                    reminderMinutes = minutes,
                    repeatType = type,
                    repeatInterval = interval,
                    repeatUntilMillis = until,
                    repeatDaysMask = mask
                )
                showEditDialog = null
            },
            initialTitle = e.title,
            initialStart = e.startMillis,
            initialEnd = e.endMillis,
            initialReminderMinutes = e.reminderMinutes,
            initialRepeatType = e.repeatType,
            initialRepeatInterval = e.repeatInterval,
            initialRepeatUntil = e.repeatUntilMillis,
            initialRepeatDaysMask = e.repeatDaysMask
        )
    }

    pendingDelete?.let { ev ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить событие?") },
            text = { Text(ev.title) },
            confirmButton = {
                TextButton(onClick = {
                    presenter.deleteEvent(ev.id)
                    pendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Отмена") } }
        )
    }

    // ---------- scaffold ----------
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Smart Calendar", maxLines = 1) },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = { showSettings = true }) {
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
            ) { Icon(Icons.Default.Add, contentDescription = "Добавить") }
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ---------- Quick Add: строка → LLM → создание события ----------
            OutlinedTextField(
                value = nlText,
                onValueChange = { nlText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Добавьте событие…") },
                trailingIcon = {
                    TextButton(
                        enabled = nlText.isNotBlank(),
                        onClick = {
                            val text = nlText.trim()
                            nlText = "" // очистим поле
                            scope.launch {
                                runCatching { llm.parse(text) }
                                    .onSuccess { p ->
                                        val rt = when (p.repeatType.uppercase()) {
                                            "DAILY" -> RepeatType.DAILY
                                            "WEEKLY" -> RepeatType.WEEKLY
                                            "MONTHLY" -> RepeatType.MONTHLY
                                            else -> RepeatType.NONE
                                        }
                                        presenter.createEvent(
                                            title = p.title,
                                            startMillis = p.startMillis,
                                            endMillis = p.endMillis,
                                            reminderMinutes = p.reminderMinutes,
                                            repeatType = rt,
                                            repeatInterval = p.repeatInterval ?: 1,
                                            repeatUntilMillis = p.repeatUntilMillis,
                                            repeatDaysMask = p.repeatDaysMask
                                        )
                                    }
                                    .onFailure { e ->
                                        error = "AI parse error: ${e.message ?: "Не удалось распознать"}"
                                    }
                            }
                        }
                    ) { Text("Создать") }
                }
            )

            // ---------- Список/состояния ----------
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
    }

    // ---------- search sheet ----------
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

    // ---------- settings sheet ----------
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

/* ----------------------- список/карточки ----------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventList(
    events: List<Event>,
    onClick: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    val sorted = events.sortedBy { it.startMillis }

    val zone = ZoneId.systemDefault()
    val groups: Map<LocalDate, List<Event>> = sorted.groupBy { e ->
        Instant.ofEpochMilli(e.startMillis).atZone(zone).toLocalDate()
    }
    val orderedDays = groups.keys.sorted()

    LazyColumn(Modifier.fillMaxSize()) {
        orderedDays.forEach { day ->
            stickyHeader { DayHeader(day) }
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

private fun formatDayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    // если это сегодня/завтра — оставляем как было
    if (day == today) return "Сегодня"
    if (day == today.plusDays(1)) return "Завтра"

    // если год отличается от текущего — добавляем год
    val pattern = if (day.year == today.year) "EEE, d MMMM" else "EEE, d MMMM yyyy"
    return DateTimeFormatter.ofPattern(pattern, Locale("ru"))
        .format(day)
        .replaceFirstChar { it.titlecase(Locale("ru")) }
}

@Composable
fun EventItem(
    event: Event,
    modifier: Modifier = Modifier
) {
    val range = formatTimeRange(event.startMillis, event.endMillis)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(
                range,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            if (event.repeatType != RepeatType.NONE) {
                SuggestionChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("повтор") },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (event.reminderMinutes > 0) {
                SuggestionChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("${event.reminderMinutes} м") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** HH:mm — HH:mm в локальной зоне */
fun formatTimeRange(startMillis: Long, endMillis: Long): String {
    val z = ZoneId.systemDefault()
    val f = DateTimeFormatter.ofPattern("HH:mm")
    val s = Instant.ofEpochMilli(startMillis).atZone(z).toLocalTime()
    val e = Instant.ofEpochMilli(endMillis).atZone(z).toLocalTime()
    return "${f.format(s)} — ${f.format(e)}"
}
