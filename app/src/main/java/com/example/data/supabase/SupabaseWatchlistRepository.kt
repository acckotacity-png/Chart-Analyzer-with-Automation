package com.example.data.supabase

import com.example.data.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository dedicated to Watchlist CRUD operations against Supabase PostgreSQL cloud backend.
 * Provides a clean separation of concerns from local Room caching and UI state managers.
 */
class SupabaseWatchlistRepository(
    private val client: SupabaseClient = SupabaseClient,
    private val tableName: String = "watchlist"
) {

    /**
     * Fetch all watchlist items for the current user/tenant from Supabase.
     * Table columns expected: symbol (text), name (text), exchange (text), added_price (numeric/float), alert_high, alert_low, added_at
     */
    suspend fun getAllWatchlistItems(): Result<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        if (!client.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val result = client.queryRecords(tableName, "*")
        result.mapCatching { jsonString ->
            parseWatchlistJson(jsonString)
        }
    }

    /**
     * Get a single watchlist item by stock symbol.
     */
    suspend fun getWatchlistItemBySymbol(symbol: String): Result<WatchlistItem?> = withContext(Dispatchers.IO) {
        if (!client.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val query = "*&symbol=eq.$symbol"
        val result = client.queryRecords(tableName, query)
        result.mapCatching { jsonString ->
            val list = parseWatchlistJson(jsonString)
            list.firstOrNull()
        }
    }

    /**
     * Create / Add a new stock to the Supabase watchlist table.
     */
    suspend fun addToWatchlist(item: WatchlistItem): Result<WatchlistItem> = withContext(Dispatchers.IO) {
        if (!client.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val jsonObject = JSONObject().apply {
            put("symbol", item.symbol)
            put("name", item.name)
            put("exchange", item.exchange)
            put("added_price", item.addedPrice)
            if (item.alertHigh != null) put("alert_high", item.alertHigh)
            if (item.alertLow != null) put("alert_low", item.alertLow)
            put("added_at", item.addedAt)
        }

        val result = client.insertRecord(tableName, jsonObject.toString())
        result.mapCatching { responseJson ->
            val parsedList = parseWatchlistJson(responseJson)
            parsedList.firstOrNull() ?: item
        }
    }

    /**
     * Update an existing watchlist item (e.g., target price alerts or updated purchase basis).
     */
    suspend fun updateWatchlistItem(item: WatchlistItem): Result<WatchlistItem> = withContext(Dispatchers.IO) {
        if (!client.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val jsonObject = JSONObject().apply {
            put("name", item.name)
            put("exchange", item.exchange)
            put("added_price", item.addedPrice)
            if (item.alertHigh != null) put("alert_high", item.alertHigh) else put("alert_high", JSONObject.NULL)
            if (item.alertLow != null) put("alert_low", item.alertLow) else put("alert_low", JSONObject.NULL)
        }

        val queryParams = "symbol=eq.${item.symbol}"
        val result = client.updateRecord(tableName, queryParams, jsonObject.toString())
        result.mapCatching { responseJson ->
            val parsedList = parseWatchlistJson(responseJson)
            parsedList.firstOrNull() ?: item
        }
    }

    /**
     * Delete a watchlist item by stock symbol.
     */
    suspend fun deleteWatchlistItem(symbol: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!client.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val queryParams = "symbol=eq.$symbol"
        val result = client.deleteRecord(tableName, queryParams)
        result.map { true }
    }

    /**
     * Parse Supabase PostgREST JSON array or object response.
     */
    private fun parseWatchlistJson(jsonString: String): List<WatchlistItem> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()

        val items = mutableListOf<WatchlistItem>()

        if (trimmed.startsWith("[")) {
            val jsonArray = JSONArray(trimmed)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(parseSingleItem(obj))
            }
        } else if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            items.add(parseSingleItem(obj))
        }

        return items
    }

    private fun parseSingleItem(obj: JSONObject): WatchlistItem {
        val symbol = obj.optString("symbol", "")
        val name = obj.optString("name", symbol)
        val exchange = obj.optString("exchange", "NSE")
        val addedPrice = if (obj.has("added_price")) obj.optDouble("added_price", 0.0) else obj.optDouble("addedPrice", 0.0)
        val alertHigh = if (obj.has("alert_high") && !obj.isNull("alert_high")) obj.optDouble("alert_high") else null
        val alertLow = if (obj.has("alert_low") && !obj.isNull("alert_low")) obj.optDouble("alert_low") else null
        val addedAt = obj.optLong("added_at", System.currentTimeMillis())

        return WatchlistItem(
            symbol = symbol,
            name = name,
            exchange = exchange,
            addedPrice = addedPrice,
            alertHigh = alertHigh,
            alertLow = alertLow,
            addedAt = addedAt
        )
    }
}
