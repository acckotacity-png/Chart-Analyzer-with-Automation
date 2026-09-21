package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SupabaseConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chart_analyzer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON = "supabase_anon_key"
        private const val KEY_SUPABASE_CONNECTED = "supabase_connected"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_PIN_LOCK_ENABLED = "pin_lock_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_REALTIME_SPEED = "realtime_speed_sec"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language" // "hi" or "en"
    }

    fun getSupabaseConfig(): SupabaseConfig {
        return SupabaseConfig(
            projectUrl = prefs.getString(KEY_SUPABASE_URL, "") ?: "",
            anonKey = prefs.getString(KEY_SUPABASE_ANON, "") ?: "",
            isConnected = prefs.getBoolean(KEY_SUPABASE_CONNECTED, false),
            lastSyncTime = prefs.getString(KEY_LAST_SYNC, "Never synced") ?: "Never synced"
        )
    }

    fun saveSupabaseConfig(url: String, anonKey: String, isConnected: Boolean) {
        prefs.edit()
            .putString(KEY_SUPABASE_URL, url)
            .putString(KEY_SUPABASE_ANON, anonKey)
            .putBoolean(KEY_SUPABASE_CONNECTED, isConnected)
            .putString(KEY_LAST_SYNC, java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            .apply()
    }

    fun isPinLockEnabled(): Boolean = prefs.getBoolean(KEY_PIN_LOCK_ENABLED, false)

    fun setPinLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_LOCK_ENABLED, enabled).apply()
    }

    fun getPinCode(): String = prefs.getString(KEY_PIN_CODE, "1234") ?: "1234"

    fun setPinCode(pin: String) {
        prefs.edit().putString(KEY_PIN_CODE, pin).apply()
    }

    fun getPreferredLanguage(): String = prefs.getString(KEY_PREFERRED_LANGUAGE, "hi") ?: "hi"

    fun setPreferredLanguage(lang: String) {
        prefs.edit().putString(KEY_PREFERRED_LANGUAGE, lang).apply()
    }
}
