package com.example

import com.example.data.ai.GeminiStockService
import com.example.data.model.SignalType
import com.example.data.model.Stock
import com.example.data.model.StockRecommendation
import com.example.data.model.TechnicalIndicators
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeminiStockServiceTest {

    private lateinit var geminiService: GeminiStockService
    private lateinit var mockStock: Stock
    private lateinit var mockIndicators: TechnicalIndicators
    private lateinit var mockRec: StockRecommendation

    @Before
    fun setup() {
        geminiService = GeminiStockService()
        mockStock = Stock(
            symbol = "TCS",
            name = "Tata Consultancy Services",
            exchange = "NSE",
            price = 3950.0,
            change = 45.0,
            changePercent = 1.15,
            high24h = 3980.0,
            low24h = 3920.0,
            volume = "1.2M",
            marketCap = "14.5T",
            peRatio = 29.5,
            week52High = 4200.0,
            week52Low = 3300.0,
            sector = "Technology"
        )
        mockIndicators = TechnicalIndicators(
            rsi = 62.5,
            rsiStatus = "Bullish Momentum",
            macd = 5.2,
            macdSignal = 3.8,
            macdHistogram = 1.4,
            macdCross = "Strong Bullish Crossover",
            ema20 = 3910.0,
            sma50 = 3850.0,
            trend = "Strong Uptrend",
            supportPrice = 3880.0,
            resistancePrice = 4050.0
        )
        mockRec = StockRecommendation(
            signal = SignalType.BUY,
            confidence = 85,
            targetPrice = 4120.0,
            stopLoss = 3850.0,
            entryRange = "₹3920 - ₹3960",
            riskLevel = "Moderate",
            riskRewardRatio = "1:2.3",
            summaryEnglish = "Bullish continuation supported by 20 EMA and expanding MACD.",
            fresherExplanationHindi = "शेयर अपने 20 EMA के ऊपर मजबूत है, स्टॉप लॉस ₹3850 रखें।",
            keyReasons = listOf("Above 20 EMA", "RSI 62.5 bullish")
        )
    }

    @Test
    fun testGenerateIndicatorRecommendationReturnsNaturalLanguageAdvice() = runBlocking {
        val (adviceText, signal) = geminiService.generateIndicatorRecommendation(
            currentStock = mockStock,
            indicators = mockIndicators,
            baseRecommendation = mockRec
        )

        assertNotNull(adviceText)
        assertTrue(adviceText.isNotBlank())
        assertTrue("Advice should contain symbol or stock name", adviceText.contains("TCS") || adviceText.contains("BUY") || adviceText.contains("शेयर"))
        assertNotNull(signal)
    }

    @Test
    fun testAskAssistantHandlesQuestions() = runBlocking {
        val (response, signal) = geminiService.askAssistant(
            userPrompt = "Hold karun ya sell?",
            currentStock = mockStock,
            recommendation = mockRec,
            indicators = mockIndicators
        )

        assertNotNull(response)
        assertTrue(response.contains("BUY") || response.contains("सलाह") || response.contains("TCS"))
    }
}
