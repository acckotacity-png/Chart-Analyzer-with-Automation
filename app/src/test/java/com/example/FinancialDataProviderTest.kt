package com.example

import com.example.data.api.AlphaVantageApi
import com.example.data.api.DailyQuote
import com.example.data.api.FinancialDataProviderClient
import com.example.data.api.GlobalQuoteData
import com.example.data.api.GlobalQuoteResponse
import com.example.data.api.TimeSeriesDailyResponse
import com.example.data.repository.FinancialMarketDataRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class FinancialDataProviderTest {

    private lateinit var mockApi: MockAlphaVantageApi
    private lateinit var repository: FinancialMarketDataRepository

    @Before
    fun setup() {
        mockApi = MockAlphaVantageApi()
        repository = FinancialMarketDataRepository(api = mockApi)
    }

    @Test
    fun testRetrofitClientConfiguration() {
        FinancialDataProviderClient.setApiKey("test_key_123")
        assertEquals("test_key_123", FinancialDataProviderClient.apiKey)
        assertNotNull(FinancialDataProviderClient.retrofit)
        assertNotNull(FinancialDataProviderClient.alphaVantageApi)
    }

    @Test
    fun testFetchRealTimeQuoteSuccess() = runBlocking {
        val result = repository.fetchRealTimeQuote("RELIANCE")
        assertTrue(result.isSuccess)
        val stock = result.getOrNull()
        assertNotNull(stock)
        assertEquals("RELIANCE", stock?.symbol)
        assertEquals(2850.50, stock?.price ?: 0.0, 0.01)
        assertEquals(35.20, stock?.change ?: 0.0, 0.01)
    }

    @Test
    fun testFetchDailyCandlesSuccess() = runBlocking {
        val result = repository.fetchDailyCandles("IBM")
        assertTrue(result.isSuccess)
        val candles = result.getOrNull()
        assertNotNull(candles)
        assertEquals(2, candles?.size)
        assertEquals(180.0f, candles?.first()?.open ?: 0f, 0.1f)
    }
}

class MockAlphaVantageApi : AlphaVantageApi {
    override suspend fun getGlobalQuote(
        function: String,
        symbol: String,
        apiKey: String
    ): Response<GlobalQuoteResponse> {
        val mockData = GlobalQuoteData(
            symbol = symbol,
            open = "2820.00",
            high = "2860.00",
            low = "2810.00",
            price = "2850.50",
            volume = "2400000",
            latestTradingDay = "2026-09-20",
            previousClose = "2815.30",
            change = "35.20",
            changePercent = "1.25%"
        )
        return Response.success(GlobalQuoteResponse(globalQuote = mockData))
    }

    override suspend fun getTimeSeriesDaily(
        function: String,
        symbol: String,
        outputSize: String,
        apiKey: String
    ): Response<TimeSeriesDailyResponse> {
        val series = mapOf(
            "2026-09-19" to DailyQuote(open = "180.0", high = "185.0", low = "179.0", close = "184.0", volume = "120000"),
            "2026-09-20" to DailyQuote(open = "184.0", high = "188.0", low = "183.0", close = "187.0", volume = "150000")
        )
        return Response.success(TimeSeriesDailyResponse(timeSeries = series))
    }
}
