package com.example.smartcalendar.data.llm

import retrofit2.HttpException
import android.util.Log
import com.example.smartcalendar.domain.recurrence.WeekMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class LlmEventParser(
    private val api: OpenAiRaw,
    private val model: String = "gpt-5-nano",           // компактно и дёшево
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    suspend fun parse(text: String): ParsedEvent = withContext(Dispatchers.IO) {
        // В идеале прицепить "сейчас" и локаль для корректной интерпретации "завтра", "в среду"
        val body = JSONObject(
            mapOf(
                "model" to model,
                "response_format" to mapOf("type" to "json_object"),
                "messages" to listOf(
                    mapOf("role" to "system", "content" to EVENT_SYSTEM_PROMPT),
                    mapOf("role" to "user", "content" to "Текущая тайм-зона: $zone. Текст: $text")
                )
            )
        ).toString()
        try {

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
        } catch (e: HttpException) {
            val err = e.response()?.errorBody()?.string()
            Log.e("LLM", "HTTP ${e.code()}: $err")
            throw e
        }


//
//        return@withContext ParsedEvent(
//            title = json.getString("title"),
//            startMillis = json.getLong("startMillis"),
//            endMillis = json.getLong("endMillis"),
//            reminderMinutes = json.optInt("reminderMinutes", -1).let { if (it >= 0) it else null },
//            repeatType = json.optString("repeatType", "NONE"),
//            repeatInterval = json.optInt("repeatInterval", -1).let { if (it >= 1) it else null },
//            repeatDaysMask = json.optInt("repeatDaysMask", -1).let { if (it >= 0) it else null },
//            repeatUntilMillis = json.optLong("repeatUntilMillis", -1L).let { if (it > 0) it else null }
//        )
    }
}

