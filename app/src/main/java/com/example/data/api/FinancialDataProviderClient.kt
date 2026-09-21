package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Service factory that initializes the Retrofit instance for financial market data providers.
 */
object FinancialDataProviderClient {

    private const val TAG = "FinancialDataClient"
    private const val BASE_URL = "https://www.alphavantage.co/"

    @Volatile
    private var customApiKey: String? = null

    /**
     * Get API key from runtime override or injected BuildConfig.
     */
    val apiKey: String
        get() {
            val custom = customApiKey?.trim()
            if (!custom.isNullOrBlank()) return custom

            val buildConfigKey = runCatching {
                val field = BuildConfig::class.java.getField("ALPHA_VANTAGE_API_KEY")
                field.get(null) as? String
            }.getOrNull()

            return buildConfigKey?.trim().takeUnless { it.isNullOrBlank() } ?: "demo"
        }

    fun setApiKey(key: String) {
        customApiKey = key.trim()
        Log.i(TAG, "Financial API key updated. Has key: ${customApiKey?.isNotBlank()}")
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val alphaVantageApi: AlphaVantageApi by lazy {
        retrofit.create(AlphaVantageApi::class.java)
    }
}
