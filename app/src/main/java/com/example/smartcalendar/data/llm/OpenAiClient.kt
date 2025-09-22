package com.example.smartcalendar.data.llm

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.converter.scalars.ScalarsConverterFactory

// Простая обёртка над Responses/Chat Completions в "raw" виде (строка<>строка)
interface OpenAiRaw {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(@Body body: String): String
}

class BearerInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(req)
    }
}

object OpenAiClient {
    fun api(key: String): OpenAiRaw {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val http = OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(key))
            .addInterceptor(log)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(http)
            .build()
            .create(OpenAiRaw::class.java)
    }
}