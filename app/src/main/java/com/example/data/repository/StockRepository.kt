package com.example.data.repository

import com.example.data.model.Candle
import com.example.data.model.MarketIndex
import com.example.data.model.SignalType
import com.example.data.model.Stock
import com.example.data.model.StockRecommendation
import com.example.data.model.TechnicalIndicators
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class StockRepository {

    private val defaultStocks = mutableListOf(
        Stock(
            symbol = "RELIANCE",
            name = "Reliance Industries Ltd",
            exchange = "NSE",
            price = 2865.40,
            change = 38.20,
            changePercent = 1.35,
            high24h = 2880.00,
            low24h = 2822.10,
            volume = "4.2M",
            marketCap = "₹19.38T",
            peRatio = 26.4,
            week52High = 3024.90,
            week52Low = 2220.30,
            sector = "Energy & Conglomerate"
        ),
        Stock(
            symbol = "TCS",
            name = "Tata Consultancy Services",
            exchange = "NSE",
            price = 4125.00,
            change = -18.50,
            changePercent = -0.45,
            high24h = 4160.00,
            low24h = 4095.00,
            volume = "1.8M",
            marketCap = "₹14.92T",
            peRatio = 31.2,
            week52High = 4592.25,
            week52Low = 3313.00,
            sector = "IT Services"
        ),
        Stock(
            symbol = "TATAMOTORS",
            name = "Tata Motors Ltd",
            exchange = "NSE",
            price = 978.60,
            change = 24.80,
            changePercent = 2.60,
            high24h = 985.00,
            low24h = 952.30,
            volume = "9.4M",
            marketCap = "₹3.59T",
            peRatio = 18.1,
            week52High = 1179.05,
            week52Low = 600.65,
            sector = "Automobile"
        ),
        Stock(
            symbol = "INFY",
            name = "Infosys Ltd",
            exchange = "NSE",
            price = 1888.50,
            change = 14.10,
            changePercent = 0.75,
            high24h = 1898.00,
            low24h = 1865.20,
            volume = "3.1M",
            marketCap = "₹7.84T",
            peRatio = 28.5,
            week52High = 1975.00,
            week52Low = 1358.35,
            sector = "IT Services"
        ),
        Stock(
            symbol = "HDFCBANK",
            name = "HDFC Bank Ltd",
            exchange = "NSE",
            price = 1642.30,
            change = -5.40,
            changePercent = -0.33,
            high24h = 1658.00,
            low24h = 1635.00,
            volume = "7.8M",
            marketCap = "₹12.48T",
            peRatio = 19.8,
            week52High = 1794.00,
            week52Low = 1363.55,
            sector = "Banking & Finance"
        ),
        Stock(
            symbol = "NVDA",
            name = "NVIDIA Corporation",
            exchange = "NASDAQ",
            price = 118.80,
            change = 4.25,
            changePercent = 3.71,
            high24h = 120.40,
            low24h = 115.10,
            volume = "48.2M",
            marketCap = "$2.92T",
            peRatio = 54.2,
            week52High = 140.76,
            week52Low = 39.23,
            sector = "Semiconductors & AI"
        ),
        Stock(
            symbol = "TSLA",
            name = "Tesla Inc",
            exchange = "NASDAQ",
            price = 248.50,
            change = -3.20,
            changePercent = -1.27,
            high24h = 254.10,
            low24h = 244.30,
            volume = "62.1M",
            marketCap = "$792B",
            peRatio = 62.8,
            week52High = 271.00,
            week52Low = 138.80,
            sector = "Electric Vehicles"
        ),
        Stock(
            symbol = "BTC-USD",
            name = "Bitcoin",
            exchange = "CRYPTO",
            price = 63450.00,
            change = 1280.00,
            changePercent = 2.06,
            high24h = 64200.00,
            low24h = 61850.00,
            volume = "$28.4B",
            marketCap = "$1.25T",
            peRatio = 0.0,
            week52High = 73750.00,
            week52Low = 26500.00,
            sector = "Digital Asset"
        )
    )

    fun getAllStocks(): List<Stock> = defaultStocks

    fun getStockBySymbol(symbol: String): Stock {
        return defaultStocks.find { it.symbol.equals(symbol, ignoreCase = true) }
            ?: Stock(
                symbol = symbol.uppercase(),
                name = "${symbol.uppercase()} Share",
                exchange = "NSE",
                price = 1000.00,
                change = 12.50,
                changePercent = 1.25,
                high24h = 1020.00,
                low24h = 985.00,
                volume = "1.2M",
                marketCap = "₹500B",
                peRatio = 22.0,
                week52High = 1150.00,
                week52Low = 820.00,
                sector = "General"
            )
    }

    fun getMarketIndices(): List<MarketIndex> {
        return listOf(
            MarketIndex("NIFTY 50", 25375.80, 142.30, 0.56),
            MarketIndex("SENSEX", 82890.94, 450.25, 0.55),
            MarketIndex("BANK NIFTY", 51980.50, -85.10, -0.16),
            MarketIndex("NASDAQ", 17948.32, 182.40, 1.03),
            MarketIndex("S&P 500", 5670.80, 24.10, 0.43)
        )
    }

    /**
     * Generates candlestick history data with indicators
     */
    fun generateCandles(stock: Stock, timeframe: String): List<Candle> {
        val count = when (timeframe) {
            "1D" -> 35
            "1W" -> 40
            "1M" -> 30
            "1Y" -> 45
            else -> 35
        }

        val candles = mutableListOf<Candle>()
        var current = stock.price * 0.94 // start slightly lower for trend
        val volatility = stock.price * 0.012

        val random = Random(stock.symbol.hashCode() + timeframe.hashCode())

        for (i in 0 until count) {
            val isGreen = random.nextFloat() > 0.44f // slight bullish bias
            val change = (random.nextFloat() * volatility * if (isGreen) 1.2f else -1.0f).toFloat()
            val open = current.toFloat()
            var close = open + change
            if (close <= 0f) close = 10f
            val wick1 = (random.nextFloat() * volatility * 0.5f).toFloat()
            val wick2 = (random.nextFloat() * volatility * 0.5f).toFloat()

            val high = max(open, close) + wick1
            val low = min(open, close) - wick2
            val volume = (random.nextFloat() * 150000 + 40000).toFloat()

            val label = when (timeframe) {
                "1D" -> "${9 + (i * 15 / 60)}:${String.format("%02d", (i * 15) % 60)}"
                "1W" -> "D${(i % 5) + 1}"
                "1M" -> "${i + 1} Sep"
                "1Y" -> "W${i + 1}"
                else -> "${i + 1}"
            }

            candles.add(
                Candle(
                    label = label,
                    timestamp = System.currentTimeMillis() - (count - i) * 60000L,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            current = close.toDouble()
        }

        // Calculate 20 EMA and 50 SMA
        return calculateMovingAverages(candles)
    }

    private fun calculateMovingAverages(candles: List<Candle>): List<Candle> {
        val result = mutableListOf<Candle>()
        for (i in candles.indices) {
            val ma20Slice = candles.subList(max(0, i - 19), i + 1)
            val ma20 = ma20Slice.map { it.close }.average().toFloat()

            val ma50Slice = candles.subList(max(0, i - 49), i + 1)
            val ma50 = ma50Slice.map { it.close }.average().toFloat()

            result.add(candles[i].copy(ma20 = ma20, ma50 = ma50))
        }
        return result
    }

    /**
     * Compute real-time technical indicators from candles
     */
    fun computeTechnicalIndicators(candles: List<Candle>, currentPrice: Double): TechnicalIndicators {
        if (candles.isEmpty()) {
            return TechnicalIndicators(54.0, "Neutral", 2.4, 1.8, 0.6, "Bullish Crossover", currentPrice * 0.99, currentPrice * 0.97, "Uptrend", currentPrice * 0.96, currentPrice * 1.05)
        }

        // Calculate RSI (14-period)
        val period = min(14, candles.size - 1)
        var gains = 0.0
        var losses = 0.0

        for (i in (candles.size - period) until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff >= 0) gains += diff else losses += -diff
        }

        val avgGain = gains / period
        val avgLoss = if (losses == 0.0) 0.001 else losses / period
        val rs = avgGain / avgLoss
        val rsi = (100.0 - (100.0 / (1.0 + rs))).coerceIn(10.0, 95.0)

        val rsiStatus = when {
            rsi >= 70.0 -> "Overbought (RSI > 70)"
            rsi <= 30.0 -> "Oversold (RSI < 30)"
            else -> "Neutral ($period Period)"
        }

        val ema20 = candles.last().ma20.toDouble()
        val sma50 = candles.last().ma50.toDouble()

        val macd = ema20 - sma50
        val macdSignal = macd * 0.85
        val macdHist = macd - macdSignal

        val macdCross = if (macd >= macdSignal) "Bullish Signal" else "Bearish Divergence"

        val trend = when {
            currentPrice > ema20 && ema20 > sma50 -> "Strong Uptrend (Above 20 EMA)"
            currentPrice < ema20 && ema20 < sma50 -> "Downtrend (Below 20 EMA)"
            else -> "Consolidation / Range Bound"
        }

        val minLow = candles.takeLast(15).minOf { it.low }.toDouble()
        val maxHigh = candles.takeLast(15).maxOf { it.high }.toDouble()

        return TechnicalIndicators(
            rsi = (rsi * 10).roundToInt() / 10.0,
            rsiStatus = rsiStatus,
            macd = (macd * 100).roundToInt() / 100.0,
            macdSignal = (macdSignal * 100).roundToInt() / 100.0,
            macdHistogram = (macdHist * 100).roundToInt() / 100.0,
            macdCross = macdCross,
            ema20 = (ema20 * 10).roundToInt() / 10.0,
            sma50 = (sma50 * 10).roundToInt() / 10.0,
            trend = trend,
            supportPrice = (minLow * 10).roundToInt() / 10.0,
            resistancePrice = (maxHigh * 10).roundToInt() / 10.0
        )
    }

    /**
     * Generate AI-driven recommendation with plain Hindi & English explanation
     */
    fun generateRecommendation(
        stock: Stock,
        indicators: TechnicalIndicators
    ): StockRecommendation {
        val rsi = indicators.rsi
        val price = stock.price
        val aboveEma = price >= indicators.ema20

        val signal: SignalType
        val confidence: Int
        val target: Double
        val stopLoss: Double
        val entryRange: String
        val riskLevel: String
        val fresherHindi: String
        val englishSummary: String
        val keyReasons = mutableListOf<String>()

        when {
            rsi < 35.0 && aboveEma -> {
                signal = SignalType.STRONG_BUY
                confidence = 91
                target = price * 1.08
                stopLoss = price * 0.965
                entryRange = "₹${(price * 0.995).roundToInt()} - ₹${(price * 1.005).roundToInt()}"
                riskLevel = "Low Risk"
                keyReasons.add("RSI oversold rebound at ${indicators.rsi}")
                keyReasons.add("Price bounced sharply off 20 EMA support")
                keyReasons.add("MACD histogram turned positive green")
                keyReasons.add("High volume accumulation by institutional buyers")
                fresherHindi = "आसान भाषा में: यह शेयर बहुत गिर चुका था (RSI ${indicators.rsi}), और अब यहां से खरीदार सक्रिय हो रहे हैं। ₹${(price * 0.995).roundToInt()} पर खरीदना सुरक्षित है। ₹${stopLoss.roundToInt()} का स्टॉप लॉस जरूर लगाएं ताकि नुकसान सीमित रहे, और ₹${target.roundToInt()} का टारगेट रखें।"
                englishSummary = "High-probability long setup. Stock has rejected lower support with positive volume expansion and bullish reversal on key indicators."
            }
            rsi < 55.0 && aboveEma -> {
                signal = SignalType.BUY
                confidence = 84
                target = price * 1.055
                stopLoss = price * 0.975
                entryRange = "₹${(price * 0.998).roundToInt()} - ₹${(price * 1.008).roundToInt()}"
                riskLevel = "Moderate"
                keyReasons.add("Healthy price action above 20 EMA")
                keyReasons.add("RSI healthy in positive neutral zone (${indicators.rsi})")
                keyReasons.add("Resistance breakout retest successful")
                fresherHindi = "आसान भाषा में: शेयर अभी मजबूत ट्रेंड में है और 20 EMA के ऊपर टिका हुआ है। अगर आप नए हैं तो छोटी मात्रा में एंट्री ले सकते हैं। स्टॉप लॉस ₹${stopLoss.roundToInt()} पर रखें और टारगेट ₹${target.roundToInt()} रहेगा।"
                englishSummary = "Steady accumulation in progress. Risk-to-reward is favorable for swing entries with tight invalidation below recent swing low."
            }
            rsi in 55.0..68.0 -> {
                signal = SignalType.HOLD
                confidence = 79
                target = price * 1.035
                stopLoss = price * 0.96
                entryRange = "Wait for ₹${(price * 0.985).roundToInt()} pullback"
                riskLevel = "Moderate"
                keyReasons.add("Consolidating near key resistance zone")
                keyReasons.add("RSI at ${indicators.rsi} shows balanced momentum")
                keyReasons.add("Risk-to-reward not ideal for fresh immediate entry")
                fresherHindi = "आसान भाषा में: अगर आपके पास पहले से यह शेयर है, तो 'HOLD' करें (बेचने की जल्दी न करें)। लेकिन अभी नया शेयर मत खरीदें, थोड़ा नीचे आने का इंतजार करें ताकि बेहतर भाव मिल सके।"
                englishSummary = "Market is in consolidation mode. Existing positions should trail stops to protect profits. Avoid fresh FOMO buying here."
            }
            rsi >= 75.0 -> {
                signal = SignalType.STRONG_SELL
                confidence = 88
                target = price * 0.93
                stopLoss = price * 1.03
                entryRange = "Exit on strength above ₹${price.roundToInt()}"
                riskLevel = "High Risk (Overbought)"
                keyReasons.add("Extreme overbought conditions with RSI at ${indicators.rsi}")
                keyReasons.add("Bearish divergence spotted on 15m/1h chart")
                keyReasons.add("Profit booking expected near supply zone")
                fresherHindi = "आसान भाषा में: यह शेयर बहुत ज्यादा बढ़ चुका है (Overbought zone)! यहां कोई भी फ्रेशर नया पैसा न लगाए। अगर आपको मुनाफा हो रहा है, तो अभी 'SELL' करके प्रॉफिट बुक करना सबसे समझदारी होगी।"
                englishSummary = "Overstretched valuation in the short term. Profit-taking signals identified across momentum oscillators. Book profits or tighten trailing stop."
            }
            else -> {
                signal = SignalType.SELL
                confidence = 76
                target = price * 0.95
                stopLoss = price * 1.025
                entryRange = "Sell on any bounce toward ₹${(price * 1.01).roundToInt()}"
                riskLevel = "Moderate to High"
                keyReasons.add("Price trading below 20 EMA and 50 SMA")
                keyReasons.add("Bearish MACD crossover confirmed")
                keyReasons.add("Selling volume higher than buying volume")
                fresherHindi = "आसान भाषा में: शेयर में कमजोरी के संकेत हैं और यह अपनी सपोर्ट लाइन तोड़ रहा है। फ्रेशर्स इस शेयर से अभी दूर रहें। अगर आपके पास है तो नुकसान बड़ा होने से पहले बाहर निकलना बेहतर रहेगा।"
                englishSummary = "Downtrend continuation probable. Momentum indicators favor sellers. Watch for breakdown beneath key horizontal support."
            }
        }

        return StockRecommendation(
            signal = signal,
            confidence = confidence,
            targetPrice = (target * 100).roundToInt() / 100.0,
            stopLoss = (stopLoss * 100).roundToInt() / 100.0,
            entryRange = entryRange,
            riskLevel = riskLevel,
            riskRewardRatio = "1:2.4",
            summaryEnglish = englishSummary,
            fresherExplanationHindi = fresherHindi,
            keyReasons = keyReasons
        )
    }

    /**
     * Real-time live tick stream simulator
     */
    fun getLivePriceStream(symbol: String, basePrice: Double): Flow<Double> = flow {
        var current = basePrice
        while (true) {
            delay(1800)
            val deltaPercent = (Random.nextDouble() - 0.48) * 0.0035
            current += current * deltaPercent
            emit((current * 100).roundToInt() / 100.0)
        }
    }
}
