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
fun nextOccurrence(e: Event, after: Long): Pair<Long, Long>? {
    val zone = java.time.ZoneId.systemDefault()
    var start = java.time.Instant.ofEpochMilli(e.startMillis).atZone(zone)
    var end   = java.time.Instant.ofEpochMilli(e.endMillis).atZone(zone)

    val limit = e.repeatUntilMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(zone) }

    fun ok(): Boolean {
        val sMs = start.toInstant().toEpochMilli()
        val eMs = end.toInstant().toEpochMilli()
        if (sMs <= after) return false
        if (limit != null && sMs > limit.toInstant().toEpochMilli()) return false
        return true
    }

    when (e.repeatType) {
        RepeatType.DAILY -> {
            val step = (e.repeatInterval).coerceAtLeast(1)
            while (start.toInstant().toEpochMilli() <= after) {
                start = start.plusDays(step.toLong())
                end   = end.plusDays(step.toLong())
                if (limit != null && start.isAfter(limit)) return null
            }
            return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
        }

        RepeatType.WEEKLY -> {
            // mask может быть null/0 -> тогда каждые N недель в тот же день
            val mask = e.repeatDaysMask ?: 0
            val stepWeeks = (e.repeatInterval).coerceAtLeast(1)

            // Если маска пуста — держим исходный день недели
            if (mask == 0) {
                while (start.toInstant().toEpochMilli() <= after) {
                    start = start.plusWeeks(stepWeeks.toLong())
                    end   = end.plusWeeks(stepWeeks.toLong())
                    if (limit != null && start.isAfter(limit)) return null
                }
                return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
            }

            // С маской: ищем ближайший выбранный день, соблюдая шаг недель
            // Возьмём курсор с дня после исходного конца
            var cursor = start
            // Сдвигаем минимум на сутки, чтобы не возвращать ту же самую дату
            cursor = cursor.plusDays(1)

            // Чтобы учитывать шаг недель, считаем номер недели относительно исходной
            val baseWeek = start.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR).toString() +
                    "-" + start.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)

            fun sameStepWeek(z: java.time.ZonedDateTime): Boolean {
                val w = z.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val y = z.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                val baseY = start.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                val baseW = start.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(
                    java.time.LocalDate.of(baseY, 1, 4)
                        .with(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR, baseW.toLong()),
                    java.time.LocalDate.of(y, 1, 4)
                        .with(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR, w.toLong())
                )
                return (weeksBetween % stepWeeks) == 0L
            }

            repeat(366 * 3) { // безопасный лимит поиска ~3 года
                val dow = cursor.dayOfWeek.value % 7 // пн=1..вс=7 -> 0..6
                val bit = 1 shl dow
                if ((mask and bit) != 0 && sameStepWeek(cursor)) {
                    val dur = java.time.Duration.between(start, end)
                    val ns = cursor.withHour(start.hour).withMinute(start.minute).withSecond(0).withNano(0)
                    val ne = ns.plus(dur)
                    if (ns.toInstant().toEpochMilli() > after) {
                        if (limit != null && ns.isAfter(limit)) return null
                        return ns.toInstant().toEpochMilli() to ne.toInstant().toEpochMilli()
                    }
                }
                cursor = cursor.plusDays(1)
                if (limit != null && cursor.isAfter(limit)) return null
            }
            return null
        }

        RepeatType.MONTHLY -> {
            val step = (e.repeatInterval).coerceAtLeast(1)
            while (start.toInstant().toEpochMilli() <= after) {
                start = start.plusMonths(step.toLong())
                end   = end.plusMonths(step.toLong())
                if (limit != null && start.isAfter(limit)) return null
            }
            return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
        }

        else -> return null
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

