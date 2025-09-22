package com.example.smartcalendar.data.llm

import com.example.smartcalendar.domain.recurrence.WeekMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class LlmEventParser(
    private val api: OpenAiRaw,
    private val model: String = "gpt-4o-mini",           // компактно и дёшево
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    suspend fun parse(text: String): ParsedEvent = withContext(Dispatchers.IO) {
        // В идеале прицепить "сейчас" и локаль для корректной интерпретации "завтра", "в среду"
        val body = JSONObject(
            mapOf(
                "model" to model,
                "temperature" to 0,
                "response_format" to mapOf("type" to "json_object"),
                "messages" to listOf(
                    mapOf("role" to "system", "content" to EVENT_SYSTEM_PROMPT),
                    mapOf("role" to "user", "content" to "Текущая тайм-зона: $zone. Текст: $text")
                )
            )
        ).toString()

        val resp = api.chat(body)
        // Chat Completions возвращает choices[0].message.content
        val content = JSONObject(resp)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val json = JSONObject(content)

        return@withContext ParsedEvent(
            title = json.getString("title"),
            startMillis = json.getLong("startMillis"),
            endMillis = json.getLong("endMillis"),
            reminderMinutes = json.optInt("reminderMinutes", -1).let { if (it >= 0) it else null },
            repeatType = json.optString("repeatType", "NONE"),
            repeatInterval = json.optInt("repeatInterval", -1).let { if (it >= 1) it else null },
            repeatDaysMask = json.optInt("repeatDaysMask", -1).let { if (it >= 0) it else null },
            repeatUntilMillis = json.optLong("repeatUntilMillis", -1L).let { if (it > 0) it else null }
        )
    }
}

