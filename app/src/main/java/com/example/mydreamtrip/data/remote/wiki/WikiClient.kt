package com.example.mydreamtrip.data.remote.wiki

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WikiClient {

    private const val BASE_URL = "https://en.wikipedia.org/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val headersInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", "MyDreamTrip/1.0 (Android; student project)")
            .header("Accept", "application/json")
            .header("Accept-Language", "en")
            .build()
        chain.proceed(req)
    }

    private val http = OkHttpClient.Builder()
        .addInterceptor(headersInterceptor)
        .addInterceptor(logging)
        .build()

    val api: WikiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikiApi::class.java)
    }
}
