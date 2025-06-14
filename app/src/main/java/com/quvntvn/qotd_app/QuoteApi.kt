package com.quvntvn.qotd_app

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 4. QuoteApi.kt (Interface Retrofit)
interface QuoteApi {
    @GET("daily_quote")
    suspend fun getDailyQuote(): Response<Quote>

    @GET("random_quote")
    suspend fun getRandomQuote(): Response<Quote>

    companion object {
        const val BASE_URL = "https://qotd-api-ne8l.onrender.com/api/"

        fun create(): QuoteApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QuoteApi::class.java)
        }
    }
}