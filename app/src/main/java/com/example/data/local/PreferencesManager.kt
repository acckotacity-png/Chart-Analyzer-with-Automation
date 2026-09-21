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
        private const val KEY_LOGGED_USER_EMAIL = "logged_user_email"
        private const val KEY_LOGGED_USER_NAME = "logged_user_name"
        private const val KEY_LOGGED_USER_MOBILE = "logged_user_mobile"
        private const val KEY_LOGGED_USER_ROLE = "logged_user_role"
        private const val KEY_LOGGED_USER_STATUS = "logged_user_status"
    }

    fun saveLoggedUser(user: com.example.data.model.AppUser?) {
        if (user == null) {
            prefs.edit()
                .remove(KEY_LOGGED_USER_EMAIL)
                .remove(KEY_LOGGED_USER_NAME)
                .remove(KEY_LOGGED_USER_MOBILE)
                .remove(KEY_LOGGED_USER_ROLE)
                .remove(KEY_LOGGED_USER_STATUS)
                .apply()
        } else {
            prefs.edit()
                .putString(KEY_LOGGED_USER_EMAIL, user.email)
                .putString(KEY_LOGGED_USER_NAME, user.full_name)
                .putString(KEY_LOGGED_USER_MOBILE, user.mobile_number)
                .putString(KEY_LOGGED_USER_ROLE, user.role)
                .putString(KEY_LOGGED_USER_STATUS, user.status)
                .apply()
        }
    }

    fun getLoggedUser(): com.example.data.model.AppUser? {
        val email = prefs.getString(KEY_LOGGED_USER_EMAIL, null) ?: return null
        return com.example.data.model.AppUser(
            email = email,
            full_name = prefs.getString(KEY_LOGGED_USER_NAME, "") ?: "",
            mobile_number = prefs.getString(KEY_LOGGED_USER_MOBILE, "") ?: "",
            role = prefs.getString(KEY_LOGGED_USER_ROLE, "user") ?: "user",
            status = prefs.getString(KEY_LOGGED_USER_STATUS, "pending") ?: "pending"
        )
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
