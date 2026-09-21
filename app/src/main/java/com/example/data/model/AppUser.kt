package com.example.data.model

data class AppUser(
    val id: String = "",
    val created_at: String = "",
    val full_name: String = "",
    val mobile_number: String = "",
    val email: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val role: String = "user",       // "user", "admin"
    val approved_by: String? = null,
    val approved_at: String? = null
) {
    val isApproved: Boolean get() = status.equals("approved", ignoreCase = true)
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
}
