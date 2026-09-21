package com.splitmate.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class FrankfurterLatestResponse(
    val amount: Double = 1.0,
    val base: String = "EUR",
    val date: String = "",
    val rates: Map<String, Double> = emptyMap()
)

/**
 * Retrofit Service fetching live currency conversion rates at runtime from
 * the Frankfurter API (`https://api.frankfurter.dev/v1/latest`).
 */
interface FrankfurterApiService {

    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") baseCurrency: String = "USD"
    ): FrankfurterLatestResponse

    @GET("currencies")
    suspend fun getSupportedCurrencies(): Map<String, String>
}

object FrankfurterNetwork {
    private const val BASE_URL = "https://api.frankfurter.dev/v1/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    val api: FrankfurterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApiService::class.java)
    }
}
