package com.ahmetbircan.akillisulama.api

import com.ahmetbircan.akillisulama.data.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo API Interface
 */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getHavaDurumu(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum",
        @Query("timezone") timezone: String = "Europe/Istanbul",
        @Query("forecast_days") days: Int = 7
    ): WeatherResponse
}

/**
 * Retrofit singleton
 */
object WeatherApiClient {
    
    private const val BASE_URL = "https://api.open-meteo.com/"
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val api: WeatherApi by lazy {
        retrofit.create(WeatherApi::class.java)
    }
}
