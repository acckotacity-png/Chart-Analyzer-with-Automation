package com.example

import com.example.data.supabase.SupabaseClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupabaseClientTest {

    @Test
    fun testSupabaseClientConfiguration() {
        val testUrl = "https://my-project-ref.supabase.co"
        val testKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_key"

        SupabaseClient.configure(testUrl, testKey)

        assertEquals(testUrl, SupabaseClient.supabaseUrl)
        assertEquals(testKey, SupabaseClient.supabaseAnonKey)
        assertTrue(SupabaseClient.isConfigured)
    }

    @Test
    fun testSupabaseClientEmptyConfiguration() {
        SupabaseClient.configure("", "")
        assertFalse(SupabaseClient.isConfigured)
    }
}
