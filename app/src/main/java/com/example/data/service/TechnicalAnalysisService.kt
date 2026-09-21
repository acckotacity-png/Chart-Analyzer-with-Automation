package com.example.data.service

import com.example.data.api.DailyQuote
import com.example.data.model.Candle
import com.example.data.model.TechnicalIndicators
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Clean data model representing standard OHLC data point.
 */
data class RawOhlc(
    val dateOrLabel: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

/**
 * Service layer responsible for processing raw OHLC responses from the Retrofit API
 * and calculating key quantitative technical indicators:
 * - Simple Moving Averages (SMA)
 * - Exponential Moving Averages (EMA)
 * - Relative Strength Index (RSI - standard Wilder's smoothing or SMA based)
 * - MACD (Moving Average Convergence Divergence)
 * - Dynamic Support & Resistance levels
 */
class TechnicalAnalysisService {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Parses and converts a raw Retrofit Alpha Vantage time series map into chronologically sorted RawOhlc records.
     */
    fun parseTimeSeriesDaily(rawSeries: Map<String, DailyQuote>?): List<RawOhlc> {
        if (rawSeries.isNullOrEmpty()) return emptyList()

        return rawSeries.entries
            .sortedBy { it.key } // Sort oldest to newest
            .mapNotNull { (dateStr, quote) ->
                val open = quote.open?.toDoubleOrNull() ?: return@mapNotNull null
                val high = quote.high?.toDoubleOrNull() ?: return@mapNotNull null
                val low = quote.low?.toDoubleOrNull() ?: return@mapNotNull null
                val close = quote.close?.toDoubleOrNull() ?: return@mapNotNull null
                val volume = quote.volume?.toDoubleOrNull() ?: 0.0

                val timestamp = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }

                RawOhlc(
                    dateOrLabel = if (dateStr.length >= 5) dateStr.substring(5) else dateStr,
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            }
    }

    /**
     * Calculates Simple Moving Average (SMA) for each point in series given a window size.
     */
    fun calculateSma(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty() || period <= 0) return emptyList()
        val result = mutableListOf<Double>()
        for (i in prices.indices) {
            val start = max(0, i - period + 1)
            val sublist = prices.subList(start, i + 1)
            result.add(sublist.average())
        }
        return result
    }

    /**
     * Calculates Exponential Moving Average (EMA) for each point in series given a period.
     * Formula: Multiplier = 2 / (period + 1)
     * EMA = (Close - PreviousEMA) * Multiplier + PreviousEMA
     */
    fun calculateEma(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty() || period <= 0) return emptyList()
        val result = ArrayList<Double>(prices.size)
        val multiplier = 2.0 / (period + 1.0)

        // Seed with first price
        var prevEma = prices.first()
        result.add(prevEma)

        for (i in 1 until prices.size) {
            val currentPrice = prices[i]
            val currentEma = (currentPrice - prevEma) * multiplier + prevEma
            result.add(currentEma)
            prevEma = currentEma
        }
        return result
    }

    /**
     * Calculates 14-period Relative Strength Index (RSI).
     * Range: 0.0 to 100.0
     */
    fun calculateRsi(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < 2) return 50.0

        val effectivePeriod = min(period, prices.size - 1)
        var totalGains = 0.0
        var totalLosses = 0.0

        val startIndex = prices.size - effectivePeriod
        for (i in startIndex until prices.size) {
            val change = prices[i] - prices[i - 1]
            if (change >= 0) {
                totalGains += change
            } else {
                totalLosses += -change
            }
        }

        val avgGain = totalGains / effectivePeriod
        val avgLoss = if (totalLosses == 0.0) 0.0001 else totalLosses / effectivePeriod

        val rs = avgGain / avgLoss
        val rsi = 100.0 - (100.0 / (1.0 + rs))
        return ((rsi * 100.0).roundToInt() / 100.0).coerceIn(0.0, 100.0)
    }

    /**
     * Converts OHLC records to chartable Candlestick models annotated with SMA 20 & SMA 50.
     */
    fun buildCandlesWithMovingAverages(ohlcList: List<RawOhlc>): List<Candle> {
        if (ohlcList.isEmpty()) return emptyList()

        val closePrices = ohlcList.map { it.close }
        val sma20 = calculateSma(closePrices, 20)
        val sma50 = calculateSma(closePrices, 50)

        return ohlcList.mapIndexed { index, ohlc ->
            Candle(
                label = ohlc.dateOrLabel,
                timestamp = ohlc.timestamp,
                open = ohlc.open.toFloat(),
                high = ohlc.high.toFloat(),
                low = ohlc.low.toFloat(),
                close = ohlc.close.toFloat(),
                volume = ohlc.volume.toFloat(),
                ma20 = sma20[index].toFloat(),
                ma50 = sma50[index].toFloat()
            )
        }
    }

    /**
     * Produces comprehensive TechnicalIndicators model from raw OHLC data and current price.
     */
    fun computeFullIndicators(ohlcList: List<RawOhlc>, currentPrice: Double): TechnicalIndicators {
        if (ohlcList.isEmpty()) {
            return TechnicalIndicators(
                rsi = 50.0,
                rsiStatus = "Neutral",
                macd = 0.0,
                macdSignal = 0.0,
                macdHistogram = 0.0,
                macdCross = "Neutral",
                ema20 = currentPrice,
                sma50 = currentPrice,
                trend = "Consolidation",
                supportPrice = currentPrice * 0.96,
                resistancePrice = currentPrice * 1.04
            )
        }

        val closePrices = ohlcList.map { it.close }
        val rsi = calculateRsi(closePrices, 14)

        val rsiStatus = when {
            rsi >= 70.0 -> "Overbought (RSI > 70)"
            rsi <= 30.0 -> "Oversold (RSI < 30)"
            rsi >= 55.0 -> "Bullish Momentum"
            rsi <= 45.0 -> "Bearish Momentum"
            else -> "Neutral (RSI ~$rsi)"
        }

        // EMA 20 & SMA 50
        val ema20List = calculateEma(closePrices, 20)
        val sma50List = calculateSma(closePrices, 50)
        val latestEma20 = ema20List.lastOrNull() ?: currentPrice
        val latestSma50 = sma50List.lastOrNull() ?: currentPrice

        // MACD (12 EMA - 26 EMA, with 9 EMA signal line)
        val ema12List = calculateEma(closePrices, 12)
        val ema26List = calculateEma(closePrices, 26)
        val macdLine = if (ema12List.isNotEmpty() && ema26List.isNotEmpty()) {
            ema12List.last() - ema26List.last()
        } else {
            latestEma20 - latestSma50
        }

        val macdSeries = ema12List.indices.map { i ->
            if (i < ema26List.size) ema12List[i] - ema26List[i] else 0.0
        }
        val macdSignalList = calculateEma(macdSeries, 9)
        val macdSignal = macdSignalList.lastOrNull() ?: (macdLine * 0.8)
        val macdHistogram = macdLine - macdSignal

        val macdCross = when {
            macdHistogram > 0.5 -> "Strong Bullish Crossover"
            macdHistogram > 0.0 -> "Bullish Divergence"
            macdHistogram < -0.5 -> "Strong Bearish Breakdown"
            else -> "Bearish Crossover"
        }

        // Support & Resistance from local highs/lows in last 20 periods
        val window = ohlcList.takeLast(20)
        val support = window.minOfOrNull { it.low } ?: (currentPrice * 0.96)
        val resistance = window.maxOfOrNull { it.high } ?: (currentPrice * 1.04)

        val trend = when {
            currentPrice > latestEma20 && latestEma20 > latestSma50 -> "Strong Uptrend"
            currentPrice > latestEma20 -> "Mild Bullish Trend"
            currentPrice < latestEma20 && latestEma20 < latestSma50 -> "Strong Downtrend"
            currentPrice < latestEma20 -> "Mild Bearish Trend"
            else -> "Consolidation Range"
        }

        return TechnicalIndicators(
            rsi = rsi,
            rsiStatus = rsiStatus,
            macd = ((macdLine * 100).roundToInt()) / 100.0,
            macdSignal = ((macdSignal * 100).roundToInt()) / 100.0,
            macdHistogram = ((macdHistogram * 100).roundToInt()) / 100.0,
            macdCross = macdCross,
            ema20 = ((latestEma20 * 100).roundToInt()) / 100.0,
            sma50 = ((latestSma50 * 100).roundToInt()) / 100.0,
            trend = trend,
            supportPrice = ((support * 100).roundToInt()) / 100.0,
            resistancePrice = ((resistance * 100).roundToInt()) / 100.0
        )
    }
}
