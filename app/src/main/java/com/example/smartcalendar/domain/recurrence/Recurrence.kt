package com.example.smartcalendar.domain.recurrence

import com.example.smartcalendar.data.model.Event
import com.example.smartcalendar.data.model.RepeatType
import java.time.*
import kotlin.math.max

// Маски дней недели для WEEKLY (Пн=0 ... Вс=6)
object WeekMask {
    fun has(mask: Int?, dayOfWeek: DayOfWeek): Boolean =
        mask != null && (mask and (1 shl ((dayOfWeek.value + 6) % 7))) != 0

    fun toggle(mask: Int?, dayOfWeek: DayOfWeek): Int {
        val bit = 1 shl ((dayOfWeek.value + 6) % 7)
        val m = mask ?: 0
        return m xor bit
    }
}

/** Следующий инстанс после момента [after]. Возвращает Pair(start,end) или null. */
fun Event.nextOccurrenceAfter(after: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long>? {
    if (repeatType == RepeatType.NONE) {
        // одиночное
        return if (endMillis > after) startMillis to endMillis else null
    }

    val baseStart = Instant.ofEpochMilli(startMillis).atZone(zone)
    val baseEnd = Instant.ofEpochMilli(endMillis).atZone(zone)
    val duration = Duration.between(baseStart, baseEnd)

    val limit = repeatUntilMillis?.let { Instant.ofEpochMilli(it).atZone(zone) }

    fun build(s: ZonedDateTime): Pair<Long, Long>? {
        val e = s.plus(duration)
        if (limit != null && s.toInstant().toEpochMilli() > limit.toInstant().toEpochMilli()) return null
        return s.toInstant().toEpochMilli() to e.toInstant().toEpochMilli()
    }

    val afterZ = Instant.ofEpochMilli(max(after, 0L)).atZone(zone)

    return when (repeatType) {
        RepeatType.DAILY -> {
            val daysBetween = Duration.between(baseStart.toLocalDate().atStartOfDay(zone), afterZ).toDays()
            val step = (daysBetween / repeatInterval).toInt()
            var candidate = baseStart.plusDays((step * repeatInterval).toLong())
            while (!candidate.isAfter(afterZ)) candidate = candidate.plusDays(repeatInterval.toLong())
            build(candidate)
        }
        RepeatType.WEEKLY -> {
            // Ищем ближайший день недели из маски, шагая неделями с интервалом
            var weekStart = baseStart.toLocalDate()
            var candidate: ZonedDateTime? = null
            var weeksFromBase = Duration.between(baseStart.toLocalDate().atStartOfDay(zone), afterZ).toDays() / 7
            weeksFromBase = max(0, weeksFromBase)

            fun findInWeek(weekIndex: Long): ZonedDateTime? {
                val monday = weekStart.plusWeeks(weekIndex).with(DayOfWeek.MONDAY)
                // проверяем все 7 дней
                for (i in 0..6) {
                    val day = monday.plusDays(i.toLong())
                    val dt = day.atTime(baseStart.toLocalTime()).atZone(zone)
                    if (WeekMask.has(repeatDaysMask, dt.dayOfWeek) && dt.isAfter(afterZ)) return dt
                }
                return null
            }

            var k = (weeksFromBase / repeatInterval).toInt() * repeatInterval
            while (true) {
                candidate = findInWeek(k.toLong())
                if (candidate != null) break
                k += repeatInterval
                if (k > 5200) return null // защита от бесконечного цикла
            }
            build(candidate!!)
        }
        RepeatType.MONTHLY -> {
            // та же дата месяца (или ближайшая допустимая, если короче)
            val monthsBetween = Period.between(baseStart.toLocalDate().withDayOfMonth(1), afterZ.toLocalDate().withDayOfMonth(1)).toTotalMonths()
            val step = (monthsBetween / repeatInterval).toInt()
            var candidate = baseStart.plusMonths((step * repeatInterval).toLong())
            while (!candidate.isAfter(afterZ)) candidate = candidate.plusMonths(repeatInterval.toLong())
            val day = baseStart.dayOfMonth.coerceAtMost(candidate.toLocalDate().lengthOfMonth())
            candidate = candidate.withDayOfMonth(day)
            build(candidate)
        }
        else -> null
    }
}

/** Возвращает все инстансы события в диапазоне [from, to) (ограничено safetyLimiter). */
fun Event.occurrencesBetween(
    from: Long,
    to: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    safetyLimiter: Int = 500
): List<Pair<Long, Long>> {
    val out = mutableListOf<Pair<Long, Long>>()
    var next = nextOccurrenceAfter(from - 1, zone) ?: return out
    var guard = 0
    while (next.first < to && guard < safetyLimiter) {
        out += next
        guard++
        // ищем следующий после конца текущего
        next = nextOccurrenceAfter(next.first, zone) ?: break
        if (repeatUntilMillis != null && next.first > repeatUntilMillis!!) break
    }
    return out
}
