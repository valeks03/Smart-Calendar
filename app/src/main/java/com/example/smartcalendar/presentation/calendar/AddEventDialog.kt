package com.example.smartcalendar.presentation.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Calendar

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startMillis: Long, endMillis: Long) -> Unit,
    initialTitle: String = "",
    initialStart: Long = System.currentTimeMillis(),
    initialEnd: Long = System.currentTimeMillis() + 60 * 60 * 1000
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var endMillis by remember { mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1000) }
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

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
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Заголовок") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = df.format(startMillis), onValueChange = {}, label = { Text("Начало") }, readOnly = true,
                    trailingIcon = { TextButton(onClick = { pickDateTime(startMillis) { startMillis = it } }) { Text("Выбрать") } },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = df.format(endMillis), onValueChange = {}, label = { Text("Конец") }, readOnly = true,
                    trailingIcon = { TextButton(onClick = { pickDateTime(endMillis) { endMillis = it } }) { Text("Выбрать") } },
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() || endMillis < startMillis) return@TextButton
                onConfirm(title.trim(), startMillis, endMillis)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
