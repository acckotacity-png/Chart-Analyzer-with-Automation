package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Stock
import com.example.ui.MainViewModel
import com.example.ui.components.CandlestickChart
import com.example.ui.components.CardContainer
import com.example.ui.components.MarketTickerBar
import com.example.ui.components.RecommendationCard
import com.example.ui.components.TechnicalIndicatorsView
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.ChartDarkBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAssistant: () -> Unit,
    onNavigateToCloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allStocks by viewModel.allStocks.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val marketIndices by viewModel.marketIndices.collectAsState()
    val isLiveUpdating by viewModel.isLiveUpdating.collectAsState()
    val isWatchlisted by viewModel.isCurrentStockWatchlisted.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isFetchingLiveProvider by viewModel.isFetchingLiveProvider.collectAsState()
    val marketProviderStatus by viewModel.marketProviderStatus.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val categories = listOf("All", "Nifty 50", "Tech", "Auto", "US Tech", "Crypto")

    Box(modifier = modifier.fillMaxSize().background(ChartDarkBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CHART",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ANALYZER",
                                color = NeonCyan,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .border(1.dp, ElectricBlue, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SaaS AI", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "AI-Driven Real-time Share Analyzer for Freshers & Pros",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                viewModel.fetchRealTimeMarketPrice()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Connecting to Alpha Vantage / Provider via Retrofit...")
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(ChartCardBg, CircleShape)
                                .border(1.dp, ChartBorder, CircleShape)
                                .testTag("fetch_realtime_provider_button")
                        ) {
                            if (isFetchingLiveProvider) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Fetch Real-Time Quote",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSearchDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .background(ChartCardBg, CircleShape)
                                .border(1.dp, ChartBorder, CircleShape)
                                .testTag("search_stock_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Stock",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleLiveUpdates() },
                            modifier = Modifier
                                .size(38.dp)
                                .background(ChartCardBg, CircleShape)
                                .border(1.dp, ChartBorder, CircleShape)
                                .testTag("toggle_live_updates_button")
                        ) {
                            Icon(
                                imageVector = if (isLiveUpdating) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Live Feed",
                                tint = if (isLiveUpdating) BullishGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 1b. Real-Time Provider Banner if active or updating
            if (!marketProviderStatus.isNullOrBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, ChartBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isFetchingLiveProvider) ElectricBlue else BullishGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = marketProviderStatus ?: "",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Market Ticker Tape
            item {
                MarketTickerBar(indices = marketIndices)
            }

            // 3. Stock Categories Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else ChartCardBg)
                                .border(1.dp, if (isSel) NeonCyan else ChartBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSel) NeonCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 4. Quick Stock Horizontal Picker
            item {
                val filteredStocks = when (selectedCategory) {
                    "Nifty 50" -> allStocks.filter { it.exchange == "NSE" }
                    "Tech" -> allStocks.filter { it.sector.contains("IT") || it.sector.contains("AI") }
                    "Auto" -> allStocks.filter { it.sector.contains("Auto") || it.sector.contains("Vehicles") }
                    "US Tech" -> allStocks.filter { it.exchange == "NASDAQ" }
                    "Crypto" -> allStocks.filter { it.exchange == "CRYPTO" }
                    else -> allStocks
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredStocks.forEach { stock ->
                        val isStockActive = stock.symbol == selectedStock.symbol
                        val isPositive = stock.change >= 0

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isStockActive) ChartCardBgElevated else ChartCardBg)
                                .border(
                                    1.dp,
                                    if (isStockActive) NeonCyan else ChartBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectStock(stock) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("stock_chip_${stock.symbol}")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stock.symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isStockActive) NeonCyan else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stock.exchange,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (stock.exchange == "NASDAQ") "$${stock.price}" else "₹${stock.price}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${String.format("%.1f", stock.changePercent)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) BullishGreen else BearishRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Active Stock Hero Card
            item {
                val isPositive = selectedStock.change >= 0
                val priceColor = if (isPositive) BullishGreen else BearishRed

                CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedStock.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(ChartCardBgElevated, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(selectedStock.exchange, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "${selectedStock.symbol} • ${selectedStock.sector}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            // Watchlist bookmark icon
                            IconButton(
                                onClick = {
                                    viewModel.toggleWatchlistCurrentStock()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (!isWatchlisted) "${selectedStock.symbol} added to Watchlist"
                                            else "${selectedStock.symbol} removed from Watchlist"
                                        )
                                    }
                                },
                                modifier = Modifier.size(34.dp).testTag("bookmark_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isWatchlisted) NeonCyan else TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Big Price & Change Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (selectedStock.exchange == "NASDAQ") "$${String.format("%,.2f", selectedStock.price)}" else "₹${String.format("%,.2f", selectedStock.price)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", selectedStock.change)} (${if (isPositive) "+" else ""}${String.format("%.2f", selectedStock.changePercent)}%)",
                                        color = priceColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Today", color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            // 24h High/Low Gauge
                            Column(horizontalAlignment = Alignment.End) {
                                Text("24h Range", color = TextMuted, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.width(130.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("L: ${selectedStock.low24h.toInt()}", color = BearishRed, fontSize = 10.sp)
                                    Text("H: ${selectedStock.high24h.toInt()}", color = BullishGreen, fontSize = 10.sp)
                                }
                                val range = (selectedStock.high24h - selectedStock.low24h).coerceAtLeast(1.0)
                                val currentRatio = ((selectedStock.price - selectedStock.low24h) / range).toFloat().coerceIn(0f, 1f)
                                Box(modifier = Modifier.width(130.dp).padding(top = 4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { currentRatio },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                        color = NeonCyan,
                                        trackColor = ChartBorder
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Interactive Candlestick Chart Component
            item {
                CandlestickChart(
                    candles = candles,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeChange = { viewModel.setTimeframe(it) },
                    isLiveUpdating = isLiveUpdating,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 7. Core AI Recommendation Card (Buy / Hold / Sell + Hindi Fresher explanation)
            item {
                RecommendationCard(
                    recommendation = recommendation,
                    onOpenAssistant = onNavigateToAssistant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 8. Technical Indicators View (RSI, MACD, 20 EMA, Support / Resistance)
            item {
                TechnicalIndicatorsView(
                    indicators = indicators,
                    currentPrice = selectedStock.price,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 9. Quick Actions: Save Analysis + Cloud Connect
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCurrentAnalysis()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Analysis saved locally & ready for cloud sync!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChartCardBg,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ChartBorder, RoundedCornerShape(10.dp))
                            .testTag("save_analysis_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Analysis", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onNavigateToCloud,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChartCardBg,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ChartBorder, RoundedCornerShape(10.dp))
                            .testTag("open_cloud_sync_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cloud & API", fontSize = 12.sp)
                    }
                }
            }

            // 10. Disclaimer for Freshers
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ChartCardBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ फ्रेशर सूचना (Educational Notice): यह AI सिस्टम तकनीकी एनालिसिस और प्राइस एक्शन पर आधारित अनुशंसाएं देता है। वास्तविक पैसे लगाने से पहले हमेशा अपना स्टॉप लॉस जरूर लगाएं और अपनी वित्तीय क्षमता के अनुसार ही निवेश करें।",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Search Stock Dialog
        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = { showSearchDialog = false },
                title = { Text("Search Any Share / Symbol", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("e.g. RELIANCE, TCS, AAPL, BTC, ITC...", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("search_stock_text_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = ChartBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Popular Recommendations:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val quickSymbols = listOf("RELIANCE", "TCS", "TATAMOTORS", "INFY", "HDFCBANK", "NVDA", "TSLA", "BTC-USD")
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickSymbols.forEach { sym ->
                                Box(
                                    modifier = Modifier
                                        .background(ChartCardBgElevated, RoundedCornerShape(6.dp))
                                        .border(1.dp, ChartBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.selectStockBySymbol(sym)
                                            showSearchDialog = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(sym, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.selectStockBySymbol(searchQuery.trim().uppercase())
                                showSearchDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_search_stock_button")
                    ) {
                        Text("Analyze Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSearchDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = ChartCardBg,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}
