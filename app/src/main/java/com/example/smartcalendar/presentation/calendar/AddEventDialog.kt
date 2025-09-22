package com.example.smartcalendar.presentation.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.domain.recurrence.WeekMask
import java.text.DateFormat
import java.time.DayOfWeek
import java.util.Calendar
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        startMillis: Long,
        endMillis: Long,
        reminderMinutes: Int,
        repeatType: RepeatType,
        repeatInterval: Int,
        repeatUntilMillis: Long?,
        repeatDaysMask: Int?
    ) -> Unit,
    initialTitle: String = "",
    initialStart: Long = System.currentTimeMillis(),
    initialEnd: Long = System.currentTimeMillis() + 60 * 60 * 1000,
    initialReminderMinutes: Int = 5,
    // повторы — инициализация для режима редактирования
    initialRepeatType: RepeatType = RepeatType.NONE,
    initialRepeatInterval: Int = 1,
    initialRepeatUntil: Long? = null,
    initialRepeatDaysMask: Int? = null
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var startMillis by rememberSaveable { mutableLongStateOf(initialStart) }
    var endMillis by rememberSaveable { mutableLongStateOf(initialEnd) }
    var reminderMinutes by rememberSaveable { mutableIntStateOf(initialReminderMinutes) }

    var repeatType by rememberSaveable { mutableStateOf(initialRepeatType) }
    var repeatInterval by rememberSaveable { mutableIntStateOf(initialRepeatInterval) }
    var repeatUntil by rememberSaveable { mutableStateOf<Long?>(initialRepeatUntil) }
    var weekMask by rememberSaveable { mutableStateOf(initialRepeatDaysMask ?: 0) }

    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val dOnly = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val reminderOptions = listOf(0, 5, 15, 30, 60, 90)

    fun pickDateTime(initial: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    context,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickDate(initial: Long?, onPicked: (Long?) -> Unit) {
        val base = initial ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(y, m, d, 23, 59, 59)
                onPicked(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Новое событие" else "Редактировать событие") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                // начало
                OutlinedTextField(
                    value = df.format(startMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Начало") },
                    trailingIcon = {
                        TextButton(onClick = {
                            pickDateTime(startMillis) { newStart ->
                                startMillis = newStart
                                if (endMillis < newStart) endMillis = newStart
                            }
                        }) { Text("Выбрать") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // конец
                OutlinedTextField(
                    value = df.format(endMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Конец") },
                    trailingIcon = {
                        TextButton(onClick = {
                            pickDateTime(endMillis) { newEnd ->
                                endMillis = max(newEnd, startMillis)
                            }
                        }) { Text("Выбрать") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // напоминание
                Text("Напомнить за…", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderOptions.forEach { m ->
                        FilterChip(
                            selected = reminderMinutes == m,
                            onClick = { reminderMinutes = m },
                            label = { Text(if (m == 0) "0" else "$m") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Text(
                    text = if (reminderMinutes == 0) "В момент начала"
                    else "За $reminderMinutes мин. до начала",
                    style = MaterialTheme.typography.bodySmall
                )

                // повтор
                Divider()
                Text("Повтор", style = MaterialTheme.typography.titleMedium)

                val repeatOptions = listOf(
                    RepeatType.NONE to "Нет",
                    RepeatType.DAILY to "Каждый день",
                    RepeatType.WEEKLY to "Каждую неделю",
                    RepeatType.MONTHLY to "Каждый месяц"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeatOptions.forEach { (t, label) ->
                        FilterChip(
                            selected = repeatType == t,
                            onClick = { repeatType = t },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                if (repeatType == RepeatType.WEEKLY) {
                    Spacer(Modifier.height(4.dp))
                    val days = listOf(
                        DayOfWeek.MONDAY to "Пн",
                        DayOfWeek.TUESDAY to "Вт",
                        DayOfWeek.WEDNESDAY to "Ср",
                        DayOfWeek.THURSDAY to "Чт",
                        DayOfWeek.FRIDAY to "Пт",
                        DayOfWeek.SATURDAY to "Сб",
                        DayOfWeek.SUNDAY to "Вс"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        days.forEach { (d, label) ->
                            val selected = WeekMask.has(weekMask, d)
                            FilterChip(
                                selected = selected,
                                onClick = { weekMask = WeekMask.toggle(weekMask, d) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                if (repeatType != RepeatType.NONE) {
                    Spacer(Modifier.height(8.dp))
                    Text("Интервал:", style = MaterialTheme.typography.titleSmall)

                    // ваши чипы 1/2/3/4 (или какие у вас) — компонент остался прежним
                    RepeatIntervalChips(
                        repeatInterval = repeatInterval,
                        onSelect = { repeatInterval = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (repeatUntil == null) "Без даты окончания"
                            else "До: ${dOnly.format(repeatUntil!!)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = {
                            pickDate(repeatUntil) { picked ->
                                repeatUntil = picked
                            }
                        }) {
                            Text(if (repeatUntil == null) "Указать дату…" else "Изменить")
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() || endMillis < startMillis) return@TextButton
                onConfirm(
                    title.trim(),
                    startMillis,
                    endMillis,
                    reminderMinutes,
                    repeatType,
                    repeatInterval,
                    repeatUntil,
                    weekMask.takeIf { it != 0 }
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatIntervalChips(
    repeatInterval: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(1, 2, 3, 4)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { n ->
            FilterChip(
                selected = repeatInterval == n,
                onClick = { onSelect(n) },
                label = { Text("$n") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
