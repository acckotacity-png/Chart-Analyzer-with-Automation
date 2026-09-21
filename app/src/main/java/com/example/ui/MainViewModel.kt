package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiStockService
import com.example.data.local.ChartDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.Candle
import com.example.data.model.ChatMessage
import com.example.data.model.MarketIndex
import com.example.data.model.SavedAnalysis
import com.example.data.model.SignalType
import com.example.data.model.Stock
import com.example.data.model.StockRecommendation
import com.example.data.model.SupabaseConfig
import com.example.data.model.TechnicalIndicators
import com.example.data.model.WatchlistItem
import com.example.data.repository.StockRepository
import com.example.data.api.FinancialDataProviderClient
import com.example.data.repository.FinancialMarketDataRepository
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseWatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StockRepository()
    private val geminiService = GeminiStockService()
    private val prefsManager = PreferencesManager(application)
    private val database = ChartDatabase.getDatabase(application)
    private val watchlistDao = database.watchlistDao()
    private val supabaseWatchlistRepo = SupabaseWatchlistRepository()
    private val supabaseAuthRepo = com.example.data.supabase.SupabaseAuthRepository()
    private val financialMarketRepo = FinancialMarketDataRepository()

    // Auth & Access Control
    private val _currentUser = MutableStateFlow<com.example.data.model.AppUser?>(prefsManager.getLoggedUser())
    val currentUser: StateFlow<com.example.data.model.AppUser?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _allUsersList = MutableStateFlow<List<com.example.data.model.AppUser>>(emptyList())
    val allUsersList: StateFlow<List<com.example.data.model.AppUser>> = _allUsersList.asStateFlow()

    // Real-time Provider status
    private val _isFetchingLiveProvider = MutableStateFlow(false)
    val isFetchingLiveProvider: StateFlow<Boolean> = _isFetchingLiveProvider.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _marketProviderStatus = MutableStateFlow<String?>("Alpha Vantage / Global Quote Feed Ready")
    val marketProviderStatus: StateFlow<String?> = _marketProviderStatus.asStateFlow()

    // Stocks & Active Stock
    private val _allStocks = MutableStateFlow<List<Stock>>(repository.getAllStocks())
    val allStocks: StateFlow<List<Stock>> = _allStocks.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock>(_allStocks.value.first())
    val selectedStock: StateFlow<Stock> = _selectedStock.asStateFlow()

    // Timeframe
    private val _selectedTimeframe = MutableStateFlow("1D")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // Candles
    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    // Technical Indicators
    private val _indicators = MutableStateFlow<TechnicalIndicators>(
        TechnicalIndicators(54.0, "Neutral", 2.4, 1.8, 0.6, "Bullish Crossover", 2840.0, 2810.0, "Strong Uptrend", 2790.0, 2920.0)
    )
    val indicators: StateFlow<TechnicalIndicators> = _indicators.asStateFlow()

    // Recommendation
    private val _recommendation = MutableStateFlow<StockRecommendation>(
        repository.generateRecommendation(_selectedStock.value, _indicators.value)
    )
    val recommendation: StateFlow<StockRecommendation> = _recommendation.asStateFlow()

    // Indices
    private val _marketIndices = MutableStateFlow<List<MarketIndex>>(repository.getMarketIndices())
    val marketIndices: StateFlow<List<MarketIndex>> = _marketIndices.asStateFlow()

    // Live Tick Simulation
    private val _isLiveUpdating = MutableStateFlow(true)
    val isLiveUpdating: StateFlow<Boolean> = _isLiveUpdating.asStateFlow()
    private var liveTickJob: Job? = null

    // Watchlist from DB
    private val _watchlist = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val watchlist: StateFlow<List<WatchlistItem>> = _watchlist.asStateFlow()

    private val _isCurrentStockWatchlisted = MutableStateFlow(false)
    val isCurrentStockWatchlisted: StateFlow<Boolean> = _isCurrentStockWatchlisted.asStateFlow()

    // Assistant Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // Cloud & Supabase Config
    private val _supabaseConfig = MutableStateFlow(prefsManager.getSupabaseConfig())
    val supabaseConfig: StateFlow<SupabaseConfig> = _supabaseConfig.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    // Security
    private val _isPinLockEnabled = MutableStateFlow(prefsManager.isPinLockEnabled())
    val isPinLockEnabled: StateFlow<Boolean> = _isPinLockEnabled.asStateFlow()

    init {
        // Initialize SupabaseClient from environment or stored preferences
        val storedSupabase = prefsManager.getSupabaseConfig()
        if (storedSupabase.projectUrl.isNotBlank() && storedSupabase.anonKey.isNotBlank()) {
            SupabaseClient.configure(storedSupabase.projectUrl, storedSupabase.anonKey)
        } else if (SupabaseClient.isConfigured) {
            // Picked up from BuildConfig / environment
            _supabaseConfig.value = _supabaseConfig.value.copy(
                projectUrl = SupabaseClient.supabaseUrl,
                anonKey = SupabaseClient.supabaseAnonKey,
                isConnected = true
            )
        }

        loadStockData(_selectedStock.value, _selectedTimeframe.value)
        startLivePriceUpdates()
        observeWatchlist()
        initAssistantGreeting()
    }

    fun selectStock(stock: Stock) {
        _selectedStock.value = stock
        loadStockData(stock, _selectedTimeframe.value)
        checkIfWatchlisted(stock.symbol)
    }

    fun selectStockBySymbol(symbol: String) {
        val stock = repository.getStockBySymbol(symbol)
        selectStock(stock)
        fetchRealTimeMarketPrice(symbol)
    }

    /**
     * Connects to Alpha Vantage via Retrofit to fetch real-time market data
     */
    fun fetchRealTimeMarketPrice(symbol: String? = null) {
        val targetSymbol = symbol ?: _selectedStock.value.symbol
        viewModelScope.launch {
            _isFetchingLiveProvider.value = true
            _marketProviderStatus.value = "Fetching live quote for $targetSymbol via Retrofit..."

            val quoteResult = financialMarketRepo.fetchRealTimeQuote(targetSymbol)
            quoteResult.onSuccess { liveStock ->
                _marketProviderStatus.value = "Live quote fetched: ${liveStock.symbol} at $${liveStock.price}"
                // Update active stock price
                val current = _selectedStock.value
                val updated = current.copy(
                    price = liveStock.price,
                    change = liveStock.change,
                    changePercent = liveStock.changePercent,
                    high24h = maxOf(current.high24h, liveStock.high24h),
                    low24h = minOf(current.low24h, liveStock.low24h),
                    volume = if (liveStock.volume != "--") liveStock.volume else current.volume
                )
                _selectedStock.value = updated

                // Also fetch daily candles and indicators calculated by TechnicalAnalysisService
                val candlesResult = financialMarketRepo.fetchDailyCandles(targetSymbol)
                candlesResult.onSuccess { realCandles ->
                    if (realCandles.isNotEmpty()) {
                        _candles.value = realCandles
                    }
                }

                val indicatorsResult = financialMarketRepo.fetchTechnicalAnalysis(targetSymbol, updated.price)
                indicatorsResult.onSuccess { techIndicators ->
                    _indicators.value = techIndicators
                    val rec = repository.generateRecommendation(updated, techIndicators)
                    _recommendation.value = rec
                }.onFailure {
                    loadStockData(updated, _selectedTimeframe.value)
                }
            }.onFailure { err ->
                _marketProviderStatus.value = "Live feed note: ${err.message ?: "Using high-frequency simulated ticks"}"
            }

            _isFetchingLiveProvider.value = false
            _isRefreshing.value = false
        }
    }

    /**
     * Triggered by SwipeRefresh on dashboard to refresh prices, candles, and technical indicators.
     */
    fun refreshDashboardData() {
        _isRefreshing.value = true
        // Refresh indices
        _marketIndices.value = repository.getMarketIndices()
        // Refresh selected stock via live provider & technical analysis
        fetchRealTimeMarketPrice()
    }

    fun setFinancialProviderApiKey(apiKey: String) {
        FinancialDataProviderClient.setApiKey(apiKey)
        _marketProviderStatus.value = "Provider API Key set. Ready for real-time requests."
    }

    fun setTimeframe(tf: String) {
        _selectedTimeframe.value = tf
        loadStockData(_selectedStock.value, tf)
    }

    fun toggleLiveUpdates() {
        _isLiveUpdating.value = !_isLiveUpdating.value
        if (_isLiveUpdating.value) {
            startLivePriceUpdates()
        } else {
            liveTickJob?.cancel()
        }
    }

    private fun loadStockData(stock: Stock, timeframe: String) {
        val generatedCandles = repository.generateCandles(stock, timeframe)
        _candles.value = generatedCandles

        val computedIndicators = repository.computeTechnicalIndicators(generatedCandles, stock.price)
        _indicators.value = computedIndicators

        val rec = repository.generateRecommendation(stock, computedIndicators)
        _recommendation.value = rec
    }

    private fun startLivePriceUpdates() {
        liveTickJob?.cancel()
        liveTickJob = viewModelScope.launch {
            repository.getLivePriceStream(_selectedStock.value.symbol, _selectedStock.value.price)
                .collectLatest { newPrice ->
                    if (_isLiveUpdating.value) {
                        val current = _selectedStock.value
                        val diff = newPrice - current.price
                        val pct = (diff / current.price) * 100.0
                        val updatedStock = current.copy(
                            price = newPrice,
                            change = (current.change + diff),
                            changePercent = (current.changePercent + pct)
                        )
                        _selectedStock.value = updatedStock

                        // Update latest candle
                        val currentCandles = _candles.value.toMutableList()
                        if (currentCandles.isNotEmpty()) {
                            val last = currentCandles.last()
                            val updatedLast = last.copy(
                                close = newPrice.toFloat(),
                                high = maxOf(last.high, newPrice.toFloat()),
                                low = minOf(last.low, newPrice.toFloat())
                            )
                            currentCandles[currentCandles.size - 1] = updatedLast
                            _candles.value = currentCandles
                        }
                    }
                }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchlistDao.getAllWatchlist().collectLatest { list ->
                _watchlist.value = list
                checkIfWatchlisted(_selectedStock.value.symbol)
            }
        }
    }

    private fun checkIfWatchlisted(symbol: String) {
        viewModelScope.launch {
            _isCurrentStockWatchlisted.value = watchlistDao.isWatchlisted(symbol)
        }
    }

    fun toggleWatchlistCurrentStock() {
        val stock = _selectedStock.value
        viewModelScope.launch {
            if (_isCurrentStockWatchlisted.value) {
                watchlistDao.deleteBySymbol(stock.symbol)
                _isCurrentStockWatchlisted.value = false
            } else {
                watchlistDao.insertWatchlist(
                    WatchlistItem(
                        symbol = stock.symbol,
                        name = stock.name,
                        exchange = stock.exchange,
                        addedPrice = stock.price
                    )
                )
                _isCurrentStockWatchlisted.value = true
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistDao.deleteBySymbol(symbol)
        }
    }

    fun saveCurrentAnalysis() {
        viewModelScope.launch {
            val stock = _selectedStock.value
            val rec = _recommendation.value
            watchlistDao.insertAnalysis(
                SavedAnalysis(
                    symbol = stock.symbol,
                    stockName = stock.name,
                    priceAtAnalysis = stock.price,
                    signal = rec.signal.label,
                    confidence = rec.confidence,
                    targetPrice = rec.targetPrice,
                    stopLoss = rec.stopLoss,
                    summary = rec.fresherExplanationHindi
                )
            )
            _syncStatus.value = "Analysis saved to local memory & queued for Supabase sync!"
        }
    }

    // AI Assistant Methods
    private fun initAssistantGreeting() {
        _chatMessages.value = listOf(
            ChatMessage(
                isFromUser = false,
                text = "नमस्ते! मैं आपका **ChartAI पर्सनल स्टॉक एनालिस्ट** हूँ।\n\nआप किसी भी शेयर के बारे में पूछ सकते हैं—चाहे आप बिल्कुल नए हों या अनुभवी।\n\n🎯 **${_selectedStock.value.symbol}** के लिए अभी मेरी सलाह: **${_recommendation.value.signal.label}** है।\n\nनीचे दिए गए बटनों पर टैप करें या अपना सवाल पूछें!",
                signalTag = _recommendation.value.signal
            )
        )
    }

    /**
     * Generates a natural language Buy/Hold/Sell recommendation using Gemini
     * taking the calculated technical indicators (SMA, EMA, RSI, MACD).
     */
    fun generateAiRecommendationForIndicators() {
        _isAiTyping.value = true
        viewModelScope.launch {
            val stock = _selectedStock.value
            val ind = _indicators.value
            val baseRec = _recommendation.value

            val (analysisText, signal) = geminiService.generateIndicatorRecommendation(
                currentStock = stock,
                indicators = ind,
                baseRecommendation = baseRec
            )

            val aiMsg = ChatMessage(
                isFromUser = false,
                text = analysisText,
                signalTag = signal
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiTyping.value = false
        }
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(isFromUser = true, text = text)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiTyping.value = true

        viewModelScope.launch {
            val (reply, signalTag) = geminiService.askAssistant(
                userPrompt = text,
                currentStock = _selectedStock.value,
                recommendation = _recommendation.value,
                indicators = _indicators.value
            )

            val aiMsg = ChatMessage(
                isFromUser = false,
                text = reply,
                signalTag = signalTag
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiTyping.value = false
        }
    }

    // Cloud / Supabase / GitHub Connection
    fun updateSupabaseCredentials(url: String, anonKey: String) {
        val isConn = url.isNotBlank() && anonKey.isNotBlank() && url.startsWith("https://")
        prefsManager.saveSupabaseConfig(url, anonKey, isConn)
        SupabaseClient.configure(url, anonKey)
        _supabaseConfig.value = prefsManager.getSupabaseConfig()
        _syncStatus.value = if (isConn) "Supabase credentials updated & client initialized!" else "Configuration saved locally."
    }

    fun syncDataToSupabase() {
        viewModelScope.launch {
            _syncStatus.value = "Testing Supabase connection..."
            val connResult = SupabaseClient.testConnection()
            if (connResult.isSuccess) {
                _syncStatus.value = "Connected! Syncing watchlist & analyses to Supabase PostgreSQL..."
                // Push local watchlist items via SupabaseWatchlistRepository
                val localItems = _watchlist.value
                for (item in localItems) {
                    supabaseWatchlistRepo.addToWatchlist(item)
                }
                _syncStatus.value = "Synced ${localItems.size} watchlist items to Supabase successfully!"
                _supabaseConfig.value = _supabaseConfig.value.copy(
                    isConnected = true,
                    lastSyncTime = java.text.SimpleDateFormat("dd MMM, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
            } else {
                _syncStatus.value = "Connection note: ${connResult.exceptionOrNull()?.message ?: "Check URL and Key"}. Saved offline."
            }
        }
    }

    fun setPinLock(enabled: Boolean, pin: String) {
        prefsManager.setPinLockEnabled(enabled)
        prefsManager.setPinCode(pin)
        _isPinLockEnabled.value = enabled
    }

    // User Registration & Supabase Access Approval
    fun requestUserAccess(fullName: String, mobileNumber: String, email: String) {
        _authLoading.value = true
        _authMessage.value = null
        viewModelScope.launch {
            val result = supabaseAuthRepo.registerUser(fullName, mobileNumber, email)
            _authLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                prefsManager.saveLoggedUser(user)
                if (user?.isApproved == true) {
                    _authMessage.value = "Login successful! Welcome ${user.full_name}"
                } else {
                    _authMessage.value = "Access Request Submitted! Waiting for Admin Approval."
                }
            } else {
                _authMessage.value = "Error: ${result.exceptionOrNull()?.message ?: "Failed to connect to Supabase"}"
            }
        }
    }

    fun loginUser(emailOrMobile: String) {
        if (emailOrMobile.isBlank()) return
        _authLoading.value = true
        _authMessage.value = null
        viewModelScope.launch {
            val result = supabaseAuthRepo.checkUserStatus(emailOrMobile.trim())
            _authLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    _currentUser.value = user
                    prefsManager.saveLoggedUser(user)
                    if (user.isApproved) {
                        _authMessage.value = "Welcome back, ${user.full_name}!"
                    } else if (user.status == "rejected") {
                        _authMessage.value = "Your request was declined by Admin."
                    } else {
                        _authMessage.value = "Your access request is still PENDING Admin approval."
                    }
                } else {
                    _authMessage.value = "User not found. Please submit registration request."
                }
            } else {
                _authMessage.value = "Connection error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun refreshUserStatus() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = supabaseAuthRepo.checkUserStatus(user.email)
            if (res.isSuccess && res.getOrNull() != null) {
                val updated = res.getOrNull()
                _currentUser.value = updated
                prefsManager.saveLoggedUser(updated)
            }
        }
    }

    fun loadAllUsersForAdmin() {
        viewModelScope.launch {
            val res = supabaseAuthRepo.getAllUsers()
            if (res.isSuccess) {
                _allUsersList.value = res.getOrNull() ?: emptyList()
            }
        }
    }

    fun approveUser(targetUserId: String) {
        viewModelScope.launch {
            val adminEmail = _currentUser.value?.email ?: "admin"
            val res = supabaseAuthRepo.updateUserStatus(targetUserId, "approved", adminEmail)
            if (res.isSuccess) {
                loadAllUsersForAdmin()
            }
        }
    }

    fun rejectUser(targetUserId: String) {
        viewModelScope.launch {
            val adminEmail = _currentUser.value?.email ?: "admin"
            val res = supabaseAuthRepo.updateUserStatus(targetUserId, "rejected", adminEmail)
            if (res.isSuccess) {
                loadAllUsersForAdmin()
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        prefsManager.saveLoggedUser(null)
        _authMessage.value = "Logged out successfully."
    }
}
