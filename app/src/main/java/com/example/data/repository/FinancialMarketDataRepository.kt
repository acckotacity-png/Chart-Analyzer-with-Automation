package com.example.data.repository

import android.util.Log
import com.example.data.api.AlphaVantageApi
import com.example.data.api.FinancialDataProviderClient
import com.example.data.api.GlobalQuoteData
import com.example.data.model.Candle
import com.example.data.model.Stock
import com.example.data.model.TechnicalIndicators
import com.example.data.service.RawOhlc
import com.example.data.service.TechnicalAnalysisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repository for fetching real-time financial market data via Retrofit from Alpha Vantage,
 * and utilizing [TechnicalAnalysisService] to calculate moving averages (SMA/EMA), RSI,
 * MACD, and key support/resistance levels.
 */
class FinancialMarketDataRepository(
    private val api: AlphaVantageApi = FinancialDataProviderClient.alphaVantageApi,
    private val analysisService: TechnicalAnalysisService = TechnicalAnalysisService()
) {
    private val tag = "FinancialMarketDataRepo"

    /**
     * Fetch real-time stock quote by symbol using Retrofit.
     * Maps to domain [Stock] model.
     */
    suspend fun fetchRealTimeQuote(symbol: String): Result<Stock> = withContext(Dispatchers.IO) {
        try {
            val apiKey = FinancialDataProviderClient.apiKey
            val cleanSymbol = symbol.trim().uppercase()
            val response = api.getGlobalQuote(symbol = cleanSymbol, apiKey = apiKey)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }

            val body = response.body()
            if (body == null) {
                return@withContext Result.failure(Exception("Empty response body from financial data provider"))
            }

            if (body.errorMessage != null) {
                return@withContext Result.failure(Exception(body.errorMessage))
            }
            if (body.note != null) {
                Log.w(tag, "Alpha Vantage API rate limit note: ${body.note}")
            }

            val quote = body.globalQuote
            if (quote == null || quote.price.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No quote data returned for symbol $cleanSymbol"))
            }

            val parsedStock = mapQuoteToStock(cleanSymbol, quote)
            Result.success(parsedStock)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch real-time quote for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch daily historical candles for charting via Retrofit and processes them
     * through the [TechnicalAnalysisService] calculating SMA (20, 50).
     */
    suspend fun fetchDailyCandles(symbol: String): Result<List<Candle>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = FinancialDataProviderClient.apiKey
            val cleanSymbol = symbol.trim().uppercase()
            val response = api.getTimeSeriesDaily(symbol = cleanSymbol, apiKey = apiKey)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Empty body"))
            val timeSeries = body.timeSeries

            if (timeSeries.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No time series data returned for $cleanSymbol"))
            }

            // Convert raw API map to domain RawOhlc using TechnicalAnalysisService
            val ohlcList = analysisService.parseTimeSeriesDaily(timeSeries)
            // Compute SMA 20 & SMA 50 annotated candlesticks
            val candles = analysisService.buildCandlesWithMovingAverages(ohlcList)

            Result.success(candles)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch daily candles for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches raw OHLC from Retrofit and computes full technical indicators (RSI, EMA, SMA, MACD, Trend).
     */
    suspend fun fetchTechnicalAnalysis(symbol: String, currentPrice: Double): Result<TechnicalIndicators> = withContext(Dispatchers.IO) {
        try {
            val apiKey = FinancialDataProviderClient.apiKey
            val cleanSymbol = symbol.trim().uppercase()
            val response = api.getTimeSeriesDaily(symbol = cleanSymbol, apiKey = apiKey)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Empty body"))
            val timeSeries = body.timeSeries

            if (timeSeries.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No time series data for $cleanSymbol"))
            }

            val ohlcList = analysisService.parseTimeSeriesDaily(timeSeries)
            val indicators = analysisService.computeFullIndicators(ohlcList, currentPrice)
            Result.success(indicators)
        } catch (e: Exception) {
            Log.e(tag, "Failed to compute indicators for $symbol: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapQuoteToStock(symbol: String, quote: GlobalQuoteData): Stock {
        val price = quote.price?.toDoubleOrNull() ?: 0.0
        val change = quote.change?.toDoubleOrNull() ?: 0.0
        val changePercentStr = quote.changePercent?.replace("%", "")?.trim()
        val changePercent = changePercentStr?.toDoubleOrNull() ?: 0.0
        val high24h = quote.high?.toDoubleOrNull() ?: price
        val low24h = quote.low?.toDoubleOrNull() ?: price
        val volumeStr = formatVolume(quote.volume?.toLongOrNull() ?: 0L)

        return Stock(
            symbol = symbol,
            name = symbol,
            exchange = if (symbol.contains(".")) symbol.substringAfter(".") else "US",
            price = price,
            change = change,
            changePercent = changePercent,
            high24h = high24h,
            low24h = low24h,
            volume = volumeStr,
            marketCap = "--",
            peRatio = 0.0,
            week52High = high24h * 1.15,
            week52Low = low24h * 0.85,
            sector = "Equities"
        )
    }

    private fun formatVolume(vol: Long): String {
        return when {
            vol >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", vol / 1_000_000_000.0)
            vol >= 1_000_000 -> String.format(Locale.US, "%.1fM", vol / 1_000_000.0)
            vol >= 1_000 -> String.format(Locale.US, "%.1fK", vol / 1_000.0)
            vol > 0 -> vol.toString()
            else -> "--"
        }
    }
}
