package com.example.smartcalendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.domain.recurrence.nextOccurrenceAfter
import com.example.smartcalendar.ui.MainActivity
import kotlin.math.max

object ReminderScheduler {

    private fun eventPendingIntent(context: Context, eventId: Long, title: String): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("title", title)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Интент для "показать при срабатывании" (для setAlarmClock)
    private fun showIntent(context: Context): PendingIntent {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("smartcalendar://open")
        }
        return PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    fun schedule(
        context: Context,
        eventId: Long,
        title: String,
        startMillis: Long,
        endMillis: Long,
        minutesBefore: Int = 5,
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        repeatUntilMillis: Long? = null,
        repeatDaysMask: Int? = null
    ) {
        if (minutesBefore < 0) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        // вычисляем старт ближайшего инстанса
        val nextStart: Long? = if (repeatType == RepeatType.NONE) {
            if (endMillis > now) startMillis else null
        } else {
            val dummy = Event(
                id = eventId,
                title = title,
                startMillis = startMillis,
                endMillis = endMillis,
                reminderMinutes = minutesBefore,
                repeatType = repeatType,
                repeatInterval = repeatInterval,
                repeatUntilMillis = repeatUntilMillis,
                repeatDaysMask = repeatDaysMask
            )
            dummy.nextOccurrenceAfter(now)?.first
        }

        if (nextStart == null) return // нет будущих инстансов — нечего ставить

        val triggerAt = max(nextStart - minutesBefore * 60_000L, now + 1_000L)

        // PendingIntent для ресивера
        val i = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("title", title)
            putExtra("startMillis", startMillis)
            putExtra("endMillis", endMillis)
            putExtra("minutesBefore", minutesBefore)
            // extras для перепланирования повторов в ресивере
            putExtra("repeatType", repeatType.name)
            putExtra("repeatInterval", repeatInterval)
            putExtra("repeatUntilMillis", repeatUntilMillis ?: -1L)
            putExtra("repeatDaysMask", repeatDaysMask ?: -1)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Fallback, не требует SCHEDULE_EXACT_ALARM
            val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
            am.setAlarmClock(info, pi)
        }
    }

    fun cancel(context: Context, eventId: Long, title: String = "") {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(eventPendingIntent(context, eventId, title))
        // setAlarmClock использует тот же PendingIntent — отдельной отмены не требуется
    }
}
