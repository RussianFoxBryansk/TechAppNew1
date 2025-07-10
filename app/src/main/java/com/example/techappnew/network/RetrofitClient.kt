package com.example.techappnew.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api-v-1.onrender.com/"

    // Создаем единственный экземпляр Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Создаем экземпляры API-интерфейсов
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }
    val cumpusApi: CumpusApi by lazy {
        retrofit.create(CumpusApi::class.java)
    }
    val roomsApi: RoomsApi by lazy {
        retrofit.create(RoomsApi::class.java)
    }
    val placesApi:PlacesApi by lazy {
        retrofit.create(PlacesApi::class.java)
    }
    val devicesApi:DevicesApi by lazy {
        retrofit.create(DevicesApi::class.java)
    }
    val problemsApi:ProblemsApi by lazy{
        retrofit.create(ProblemsApi::class.java)
    }

}