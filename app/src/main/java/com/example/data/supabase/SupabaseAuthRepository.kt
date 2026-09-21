package com.example.data.supabase

import android.util.Log
import com.example.data.model.AppUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SupabaseAuthRepository {

    private val supabase = SupabaseClient

    suspend fun registerUser(fullName: String, mobileNumber: String, email: String): Result<AppUser> = withContext(Dispatchers.IO) {
        try {
            // First check if user already exists
            val existing = checkUserStatus(email.trim())
            if (existing.isSuccess && existing.getOrNull() != null) {
                return@withContext Result.success(existing.getOrNull()!!)
            }

            val jsonBody = JSONObject().apply {
                put("full_name", fullName.trim())
                put("mobile_number", mobileNumber.trim())
                put("email", email.trim().lowercase())
                put("status", "pending")
                put("role", "user")
            }.toString()

            val insertResult = supabase.insertRecord("app_users", jsonBody)
            if (insertResult.isSuccess) {
                val jsonArray = JSONArray(insertResult.getOrNull() ?: "[]")
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val user = parseUser(obj)
                    Result.success(user)
                } else {
                    // Fallback to fetch
                    val fetched = checkUserStatus(email.trim()).getOrNull()
                    if (fetched != null) {
                        Result.success(fetched)
                    } else {
                        Result.failure(Exception("Registered, but user retrieval pending"))
                    }
                }
            } else {
                Result.failure(insertResult.exceptionOrNull() ?: Exception("Failed to register"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Register error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkUserStatus(emailOrMobile: String): Result<AppUser?> = withContext(Dispatchers.IO) {
        try {
            val queryParam = if (emailOrMobile.contains("@")) {
                "email=eq.${emailOrMobile.trim().lowercase()}"
            } else {
                "mobile_number=eq.${emailOrMobile.trim()}"
            }
            
            val res = supabase.queryRecords("app_users", "*&$queryParam")
            if (res.isSuccess) {
                val jsonArray = JSONArray(res.getOrNull() ?: "[]")
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    Result.success(parseUser(obj))
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<AppUser>> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryRecords("app_users", "*&order=created_at.desc")
            if (res.isSuccess) {
                val jsonArray = JSONArray(res.getOrNull() ?: "[]")
                val list = mutableListOf<AppUser>()
                for (i in 0 until jsonArray.length()) {
                    list.add(parseUser(jsonArray.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Failed to fetch users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserStatus(userId: String, newStatus: String, approvedBy: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("status", newStatus)
                put("approved_by", approvedBy)
                put("approved_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
            }.toString()

            val res = supabase.updateRecord("app_users", "id=eq.$userId", jsonBody)
            if (res.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUser(obj: JSONObject): AppUser {
        return AppUser(
            id = obj.optString("id", ""),
            created_at = obj.optString("created_at", ""),
            full_name = obj.optString("full_name", ""),
            mobile_number = obj.optString("mobile_number", ""),
            email = obj.optString("email", ""),
            status = obj.optString("status", "pending"),
            role = obj.optString("role", "user"),
            approved_by = obj.optString("approved_by", null),
            approved_at = obj.optString("approved_at", null)
        )
    }
}
