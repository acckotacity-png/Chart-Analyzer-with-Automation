package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SupabaseClient provides a lightweight, secure helper object to connect to Supabase
 * REST and Realtime endpoints using SUPABASE_URL and SUPABASE_ANON_KEY.
 *
 * It reads from BuildConfig (injected securely via Secrets Gradle Plugin / .env)
 * with a fallback to user-configured dynamic values stored in Preferences.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Dynamic runtime overrides (if user configures via UI or Settings)
    @Volatile
    private var dynamicUrl: String? = null

    @Volatile
    private var dynamicAnonKey: String? = null

    /**
     * Get active SUPABASE_URL from environment BuildConfig or runtime override.
     */
    val supabaseUrl: String
        get() {
            val runtime = dynamicUrl?.trim()
            if (!runtime.isNullOrBlank()) return runtime

            // BuildConfig value injected via Secrets plugin / .env
            val buildConfigUrl = runCatching {
                val field = BuildConfig::class.java.getField("SUPABASE_URL")
                field.get(null) as? String
            }.getOrNull()

            return buildConfigUrl?.trim().takeUnless { it.isNullOrBlank() || it.contains("your-project") }
                ?: ""
        }

    /**
     * Get active SUPABASE_ANON_KEY from environment BuildConfig or runtime override.
     */
    val supabaseAnonKey: String
        get() {
            val runtime = dynamicAnonKey?.trim()
            if (!runtime.isNullOrBlank()) return runtime

            // BuildConfig value injected via Secrets plugin / .env
            val buildConfigKey = runCatching {
                val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
                field.get(null) as? String
            }.getOrNull()

            return buildConfigKey?.trim().takeUnless { it.isNullOrBlank() || it.contains("your-supabase") }
                ?: ""
        }

    /**
     * Check if valid Supabase credentials are configured and secure.
     */
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
                supabaseUrl.startsWith("https://") &&
                supabaseAnonKey.isNotBlank()

    /**
     * Configure runtime credentials if overridden dynamically by user.
     */
    fun configure(url: String, anonKey: String) {
        dynamicUrl = url.trim()
        dynamicAnonKey = anonKey.trim()
        Log.i(TAG, "SupabaseClient configured. isConfigured=$isConfigured")
    }

    /**
     * Test connection to Supabase instance (GET /rest/v1/).
     * Returns true if connection succeeded with 200 or valid Supabase schema response.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured or incomplete"))
        }

        val baseUrl = supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Supabase HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Insert a JSON record into a Supabase PostgreSQL table via PostgREST.
     */
    suspend fun insertRecord(table: String, jsonBody: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are missing"))
        }

        val baseUrl = supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$table"

        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(respString)
                } else {
                    Result.failure(Exception("Supabase insert error [${response.code}]: $respString"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase insert failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Query records from a Supabase PostgreSQL table via PostgREST.
     */
    suspend fun queryRecords(table: String, selectQuery: String = "*"): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are missing"))
        }

        val baseUrl = supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$table?select=$selectQuery"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(respString)
                } else {
                    Result.failure(Exception("Supabase query error [${response.code}]: $respString"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase query failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update records in a Supabase PostgreSQL table via PostgREST.
     * e.g. queryParams: "symbol=eq.RELIANCE"
     */
    suspend fun updateRecord(table: String, queryParams: String, jsonBody: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are missing"))
        }

        val baseUrl = supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$table?$queryParams"

        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(respString)
                } else {
                    Result.failure(Exception("Supabase update error [${response.code}]: $respString"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete records from a Supabase PostgreSQL table via PostgREST.
     * e.g. queryParams: "symbol=eq.RELIANCE"
     */
    suspend fun deleteRecord(table: String, queryParams: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are missing"))
        }

        val baseUrl = supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$table?$queryParams"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .delete()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(respString)
                } else {
                    Result.failure(Exception("Supabase delete error [${response.code}]: $respString"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase delete failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
