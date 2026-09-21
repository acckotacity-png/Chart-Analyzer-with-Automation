package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Alpha Vantage Financial Market Data.
 * Documentation: https://www.alphavantage.co/documentation/
 */
interface AlphaVantageApi {

    /**
     * Real-time quote for a given equity symbol.
     * e.g., function=GLOBAL_QUOTE&symbol=IBM&apikey=demo
     */
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<GlobalQuoteResponse>

    /**
     * Intraday / Daily series for chart candlesticks.
     */
    @GET("query")
    suspend fun getTimeSeriesDaily(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String
    ): Response<TimeSeriesDailyResponse>
}

@JsonClass(generateAdapter = true)
data class GlobalQuoteResponse(
    @Json(name = "Global Quote")
    val globalQuote: GlobalQuoteData? = null,
    @Json(name = "Information")
    val information: String? = null,
    @Json(name = "Note")
    val note: String? = null,
    @Json(name = "Error Message")
    val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class GlobalQuoteData(
    @Json(name = "01. symbol")
    val symbol: String? = null,
    @Json(name = "02. open")
    val open: String? = null,
    @Json(name = "03. high")
    val high: String? = null,
    @Json(name = "04. low")
    val low: String? = null,
    @Json(name = "05. price")
    val price: String? = null,
    @Json(name = "06. volume")
    val volume: String? = null,
    @Json(name = "07. latest trading day")
    val latestTradingDay: String? = null,
    @Json(name = "08. previous close")
    val previousClose: String? = null,
    @Json(name = "09. change")
    val change: String? = null,
    @Json(name = "10. change percent")
    val changePercent: String? = null
)

@JsonClass(generateAdapter = true)
data class TimeSeriesDailyResponse(
    @Json(name = "Meta Data")
    val metaData: Map<String, String>? = null,
    @Json(name = "Time Series (Daily)")
    val timeSeries: Map<String, DailyQuote>? = null,
    @Json(name = "Information")
    val information: String? = null,
    @Json(name = "Note")
    val note: String? = null,
    @Json(name = "Error Message")
    val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyQuote(
    @Json(name = "1. open")
    val open: String? = null,
    @Json(name = "2. high")
    val high: String? = null,
    @Json(name = "3. low")
    val low: String? = null,
    @Json(name = "4. close")
    val close: String? = null,
    @Json(name = "5. volume")
    val volume: String? = null
)
