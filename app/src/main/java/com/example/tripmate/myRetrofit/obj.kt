package com.example.tripmate.myRetrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object obj {
    val api : API by lazy{
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/api/rest_v1/page/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(API :: class.java)
    }
}