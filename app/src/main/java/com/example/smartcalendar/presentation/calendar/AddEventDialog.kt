package com.example.smartcalendar.presentation.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.max

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startMillis: Long, endMillis: Long, reminderMinutes: Int) -> Unit,
    initialTitle: String = "",
    initialStart: Long = System.currentTimeMillis(),
    initialEnd: Long = System.currentTimeMillis() + 60 * 60 * 1000,
    initialReminderMinutes: Int = 5
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var startMillis by remember { mutableStateOf(initialStart) }
    var endMillis by remember { mutableStateOf(initialEnd) }
    var selectedReminder by remember(initialReminderMinutes) {
        mutableStateOf(initialReminderMinutes)
    }

    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val options = listOf(0, 5, 15, 30, 60, 90)

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Новое событие" else "Редактировать событие") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Заголовок") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = df.format(startMillis), onValueChange = {}, readOnly = true,
                    label = { Text("Начало") },
                    trailingIcon = {
                        TextButton(onClick = {
                            pickDateTime(startMillis) { newStart ->
                                startMillis = newStart
                                // главное правило: конец не раньше начала
                                if (endMillis < newStart) endMillis = newStart
                            }
                        }) { Text("Выбрать") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = df.format(endMillis), onValueChange = {}, readOnly = true,
                    label = { Text("Конец") },
                    trailingIcon = {
                        TextButton(onClick = {
                            pickDateTime(endMillis) { newEnd ->
                                // можно дополнительно не давать выбрать раньше начала:
                                endMillis = max(newEnd, startMillis)
                            }
                        }) { Text("Выбрать") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- ЧИПЫ НАПОМИНАНИЯ ---
                Text("Напомнить за…", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { m ->
                        FilterChip(
                            selected = selectedReminder == m,
                            onClick = { selectedReminder = m },
                            label = { Text(if (m == 0) "0" else "$m") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,           // фон по умолчанию
                                labelColor = MaterialTheme.colorScheme.onSurface,             // текст
                                selectedContainerColor = MaterialTheme.colorScheme.onSurfaceVariant, // фон при выборе
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Text(
                    text = if (selectedReminder == 0) "В момент начала"
                    else "За $selectedReminder мин. до начала",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() || endMillis < startMillis) return@TextButton
                onConfirm(title.trim(), startMillis, endMillis, selectedReminder)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}