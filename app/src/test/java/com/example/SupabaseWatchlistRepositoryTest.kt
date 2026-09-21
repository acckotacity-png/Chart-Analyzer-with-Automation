package com.example

import com.example.data.model.WatchlistItem
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseWatchlistRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupabaseWatchlistRepositoryTest {

    private lateinit var repository: SupabaseWatchlistRepository

    @Before
    fun setup() {
        SupabaseClient.configure(
            url = "https://mock-project.supabase.co",
            anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock_anon_key"
        )
        repository = SupabaseWatchlistRepository(client = SupabaseClient)
    }

    @Test
    fun testRepositoryInitialization() {
        assertNotNull(repository)
        assertTrue(SupabaseClient.isConfigured)
        assertEquals("https://mock-project.supabase.co", SupabaseClient.supabaseUrl)
    }

    @Test
    fun testUnconfiguredRepositoryReturnsFailure() = runBlocking {
        SupabaseClient.configure("", "")
        val unconfiguredRepo = SupabaseWatchlistRepository(client = SupabaseClient)

        val item = WatchlistItem(
            symbol = "TCS",
            name = "Tata Consultancy Services",
            exchange = "NSE",
            addedPrice = 3850.0
        )

        val result = unconfiguredRepo.addToWatchlist(item)
        assertTrue(result.isFailure)
        assertEquals("Supabase is not configured", result.exceptionOrNull()?.message)
    }
}
