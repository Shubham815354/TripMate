package com.example.tripmate.myRetrofit

import com.example.tripmate.myModel.Details
import retrofit2.http.GET
import retrofit2.http.Path

interface API {
    @GET("summary/{query}")
    suspend fun get_data(@Path("query")query:String): Details
}