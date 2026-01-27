package com.example.mydreamtrip.data.remote.wiki

import retrofit2.http.GET
import retrofit2.http.Path

interface WikiApi {
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getSummary(@Path("title", encoded = true) title: String): WikiSummaryResponse
}
