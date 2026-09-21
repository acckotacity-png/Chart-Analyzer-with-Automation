package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SignalType(val label: String, val badgeColor: Long) {
    STRONG_BUY("STRONG BUY", 0xFF059669),
    BUY("BUY", 0xFF10B981),
    HOLD("HOLD", 0xFFF59E0B),
    SELL("SELL", 0xFFEF4444),
    STRONG_SELL("STRONG SELL", 0xFFDC2626)
}

data class Stock(
    val symbol: String,
    val name: String,
    val exchange: String, // "NSE", "BSE", "NASDAQ", "CRYPTO"
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume: String,
    val marketCap: String,
    val peRatio: Double,
    val week52High: Double,
    val week52Low: Double,
    val sector: String,
    val isWatchlisted: Boolean = false
)

data class Candle(
    val label: String,
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float,
    val ma20: Float = 0f,
    val ma50: Float = 0f
)

data class StockRecommendation(
    val signal: SignalType,
    val confidence: Int,
    val targetPrice: Double,
    val stopLoss: Double,
    val entryRange: String,
    val riskLevel: String, // "Low Risk", "Moderate", "High Risk"
    val riskRewardRatio: String,
    val summaryEnglish: String,
    val fresherExplanationHindi: String,
    val keyReasons: List<String>
)

data class TechnicalIndicators(
    val rsi: Double,
    val rsiStatus: String,
    val macd: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val macdCross: String,
    val ema20: Double,
    val sma50: Double,
    val trend: String,
    val supportPrice: Double,
    val resistancePrice: Double
)

data class MarketIndex(
    val name: String,
    val value: Double,
    val change: Double,
    val changePercent: Double
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val signalTag: SignalType? = null
)

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val addedPrice: Double,
    val alertHigh: Double? = null,
    val alertLow: Double? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_analyses")
data class SavedAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val stockName: String,
    val priceAtAnalysis: Double,
    val signal: String,
    val confidence: Int,
    val targetPrice: Double,
    val stopLoss: Double,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SupabaseConfig(
    val projectUrl: String = "",
    val anonKey: String = "",
    val isConnected: Boolean = false,
    val lastSyncTime: String = "Not synced yet"
)
