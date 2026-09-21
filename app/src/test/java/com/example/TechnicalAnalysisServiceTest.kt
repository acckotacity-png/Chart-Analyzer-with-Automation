package com.example

import com.example.data.api.DailyQuote
import com.example.data.service.RawOhlc
import com.example.data.service.TechnicalAnalysisService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TechnicalAnalysisServiceTest {

    private lateinit var service: TechnicalAnalysisService

    @Before
    fun setup() {
        service = TechnicalAnalysisService()
    }

    @Test
    fun testParseTimeSeriesDaily() {
        val rawMap = mapOf(
            "2026-09-18" to DailyQuote(open = "100.0", high = "105.0", low = "98.0", close = "103.0", volume = "5000"),
            "2026-09-19" to DailyQuote(open = "103.0", high = "108.0", low = "102.0", close = "107.0", volume = "7000")
        )

        val parsed = service.parseTimeSeriesDaily(rawMap)
        assertEquals(2, parsed.size)
        // Verify sorted order
        assertEquals(100.0, parsed[0].open, 0.01)
        assertEquals(107.0, parsed[1].close, 0.01)
    }

    @Test
    fun testCalculateSma() {
        val prices = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val sma3 = service.calculateSma(prices, period = 3)

        assertEquals(5, sma3.size)
        // 3rd element: average of 10, 20, 30 = 20.0
        assertEquals(20.0, sma3[2], 0.001)
        // 5th element: average of 30, 40, 50 = 40.0
        assertEquals(40.0, sma3[4], 0.001)
    }

    @Test
    fun testCalculateEma() {
        val prices = listOf(10.0, 12.0, 14.0, 16.0, 18.0)
        val ema = service.calculateEma(prices, period = 3)

        assertEquals(5, ema.size)
        assertTrue(ema.last() > 10.0)
        assertTrue(ema.last() <= 18.0)
    }

    @Test
    fun testCalculateRsiBullish() {
        // Continuous upward trend -> RSI should be high (> 70)
        val bullishPrices = listOf(100.0, 102.0, 105.0, 107.0, 110.0, 112.0, 115.0, 119.0, 122.0, 125.0, 130.0, 135.0, 140.0, 145.0, 150.0)
        val rsi = service.calculateRsi(bullishPrices, period = 14)

        assertTrue("RSI should be high on strong bullish series, was $rsi", rsi >= 70.0)
    }

    @Test
    fun testCalculateRsiBearish() {
        // Continuous downward trend -> RSI should be low (< 30)
        val bearishPrices = listOf(150.0, 145.0, 140.0, 135.0, 130.0, 125.0, 120.0, 115.0, 110.0, 105.0, 100.0, 95.0, 90.0, 85.0, 80.0)
        val rsi = service.calculateRsi(bearishPrices, period = 14)

        assertTrue("RSI should be low on strong bearish series, was $rsi", rsi <= 30.0)
    }

    @Test
    fun testComputeFullIndicators() {
        val ohlcList = (1..30).map { i ->
            val price = 100.0 + i * 1.5
            RawOhlc(
                dateOrLabel = "09-$i",
                timestamp = System.currentTimeMillis() + i * 86400000L,
                open = price - 1.0,
                high = price + 2.0,
                low = price - 2.0,
                close = price,
                volume = 50000.0
            )
        }

        val indicators = service.computeFullIndicators(ohlcList, currentPrice = 150.0)
        assertNotNull(indicators)
        assertTrue(indicators.rsi in 0.0..100.0)
        assertTrue(indicators.ema20 > 0.0)
        assertTrue(indicators.sma50 > 0.0)
        assertNotNull(indicators.trend)
        assertNotNull(indicators.macdCross)
    }
}
