// Supabase Credentials
const SUPABASE_URL = "https://qhqbporwncgccpcgrurl.supabase.co";
const SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFocWJwb3J3bmNnY2NwY2dydXJsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5NjMxOTIsImV4cCI6MjEwNTUzOTE5Mn0.o0EgKYIv0JQSxJJftgIpZwFA49br7tgTNdSkSmgYwMs";

let currentUser = JSON.parse(localStorage.getItem("app_user") || "null");

// Indian Stock Data & Indicators
const STOCKS = [
  {
    symbol: "RELIANCE",
    tvSymbol: "NSE:RELIANCE",
    name: "Reliance Industries Ltd",
    price: 1240.30,
    change: 14.80,
    changePercent: 1.21,
    rsi: 62.4,
    ema20: 1224.50,
    sma50: 1210.00,
    macd: "Bullish +4.2",
    signal: "BUY",
    aiText: "Reliance Industries (पोस्ट-बोनस 1:1) ₹1,240.30 के स्तर पर स्ट्रॉन्ग कंसोलिडेशन के बाद अपट्रेंड में है। 20 EMA (₹1,224.50) पर मजबूत सपोर्ट है। स्टॉपलॉस ₹1,215 के साथ बुलिश मोमेंटम एक्टिव है।",
    chartData: [1210, 1218, 1225, 1220, 1232, 1236, 1240.30]
  },
  {
    symbol: "TCS",
    tvSymbol: "NSE:TCS",
    name: "Tata Consultancy Services",
    price: 4125.00,
    change: -18.50,
    changePercent: -0.45,
    rsi: 52.8,
    ema20: 4140.00,
    sma50: 4110.00,
    macd: "Neutral 0.8",
    signal: "HOLD",
    aiText: "TCS इस समय 50 SMA और 20 EMA के बीच कंसोलिडेशन मोड में है। फ्रेश ब्रेकआउट के लिए ₹4,160 का इंतज़ार करें।",
    chartData: [4160, 4150, 4135, 4145, 4115, 4130, 4125.00]
  },
  {
    symbol: "TATAMOTORS",
    tvSymbol: "NSE:TATAMOTORS",
    name: "Tata Motors Ltd",
    price: 978.60,
    change: 24.80,
    changePercent: 2.60,
    rsi: 71.4,
    ema20: 945.20,
    sma50: 920.00,
    macd: "Strong Bullish +8.5",
    signal: "STRONG BUY",
    aiText: "Tata Motors में भारी वॉल्यूम के साथ 52-वीक हाई की तरफ मूवमेंट देखा जा रहा है। लक्ष्य ₹1,020 संभावित है।",
    chartData: [920, 935, 942, 955, 960, 968, 978.60]
  },
  {
    symbol: "INFY",
    tvSymbol: "NSE:INFY",
    name: "Infosys Ltd",
    price: 1540.25,
    change: 12.30,
    changePercent: 0.81,
    rsi: 58.6,
    ema20: 1520.00,
    sma50: 1505.00,
    macd: "Bullish +4.2",
    signal: "BUY",
    aiText: "Infosys 200 DMA के ऊपर सस्टेन कर रहा है। RSI 58.6 स्थिर बढ़त का संकेत दे रहा है।",
    chartData: [1500, 1512, 1518, 1530, 1525, 1535, 1540.25]
  },
  {
    symbol: "HDFCBANK",
    tvSymbol: "NSE:HDFCBANK",
    name: "HDFC Bank Ltd",
    price: 1640.20,
    change: 8.50,
    changePercent: 0.52,
    rsi: 56.4,
    ema20: 1625.00,
    sma50: 1612.00,
    macd: "Bullish +3.1",
    signal: "BUY",
    aiText: "बैंकिंग सेक्टर में खरीदारी से HDFC Bank में अच्छा सपोर्ट बन चुका है। ₹1,610 का स्टॉपलॉस रखें।",
    chartData: [1610, 1615, 1622, 1628, 1635, 1632, 1640.20]
  },
  {
    symbol: "ICICIBANK",
    tvSymbol: "NSE:ICICIBANK",
    name: "ICICI Bank Ltd",
    price: 1118.50,
    change: 14.20,
    changePercent: 1.29,
    rsi: 66.8,
    ema20: 1095.00,
    sma50: 1070.00,
    macd: "Bullish +6.8",
    signal: "STRONG BUY",
    aiText: "ICICI Bank लगातार नए शिखर छू रहा है। तकनीकी इंडिकेटर सुपर-बुलिश ट्रेंड दर्शाते हैं।",
    chartData: [1065, 1080, 1092, 1100, 1105, 1112, 1118.50]
  },
  {
    symbol: "SBIN",
    tvSymbol: "NSE:SBIN",
    name: "State Bank of India",
    price: 812.30,
    change: 9.40,
    changePercent: 1.17,
    rsi: 62.1,
    ema20: 798.50,
    sma50: 780.00,
    macd: "Bullish +5.3",
    signal: "BUY",
    aiText: "State Bank of India (SBI) अपने ऑल-टाइम हाई लेवल्स के करीब कंसोलिडेट कर रहा है। टारगेट ₹845।",
    chartData: [780, 788, 795, 792, 804, 808, 812.30]
  },
  {
    symbol: "ITC",
    tvSymbol: "NSE:ITC",
    name: "ITC Limited",
    price: 492.15,
    change: 3.25,
    changePercent: 0.66,
    rsi: 54.3,
    ema20: 488.00,
    sma50: 482.50,
    macd: "Neutral +1.2",
    signal: "HOLD",
    aiText: "ITC मजबूत डिविडेंड यील्ड और स्थिर वॉल्यूम के साथ साइडवेज़ ट्रेंड दिखा रहा है।",
    chartData: [482, 485, 487, 484, 490, 489, 492.15]
  },
  {
    symbol: "LT",
    tvSymbol: "NSE:LT",
    name: "Larsen & Toubro Ltd",
    price: 3620.00,
    change: 45.80,
    changePercent: 1.28,
    rsi: 65.5,
    ema20: 3560.00,
    sma50: 3510.00,
    macd: "Bullish +18.4",
    signal: "BUY",
    aiText: "इन्फ्रास्ट्रक्चर ऑर्डर बुक मजबूत होने के कारण L&T में निरंतर बाइंग फ्लो जारी है।",
    chartData: [3510, 3535, 3550, 3575, 3590, 3605, 3620.00]
  },
  {
    symbol: "BHARTIARTL",
    tvSymbol: "NSE:BHARTIARTL",
    name: "Bharti Airtel Ltd",
    price: 1485.60,
    change: 21.30,
    changePercent: 1.45,
    rsi: 68.2,
    ema20: 1450.00,
    sma50: 1420.00,
    macd: "Strong Bullish +9.1",
    signal: "STRONG BUY",
    aiText: "टेलीकॉम एआरपीयू बढ़ने की उम्मीद से भारती एयरटेल मजबूत अपट्रेंड में ट्रेड कर रहा है।",
    chartData: [1420, 1435, 1448, 1460, 1472, 1475, 1485.60]
  }
];

let selectedStock = STOCKS[0];
let stockChartInstance = null;
let liveDataInterval = null;
let currentStockSearchQuery = "";
let currentChartMode = "tradingview";

// Initialize on load (handle both interactive/complete and DOMContentLoaded)
function initApp() {
  if (currentUser) {
    checkActiveStatus();
  } else {
    showView("auth");
  }
}

if (document.readyState === "loading") {
  window.addEventListener("DOMContentLoaded", initApp);
} else {
  initApp();
}

function toggleAuth(showRegister) {
  document.getElementById("registerForm").classList.toggle("hidden", !showRegister);
  document.getElementById("loginForm").classList.toggle("hidden", showRegister);
  document.getElementById("authAlert").innerText = "";
}

async function handleRegister(e) {
  e.preventDefault();
  const name = document.getElementById("regName").value.trim();
  const mobile = document.getElementById("regMobile").value.trim();
  const email = document.getElementById("regEmail").value.trim().toLowerCase();
  const alertBox = document.getElementById("authAlert");

  alertBox.style.color = "var(--cyan)";
  alertBox.innerText = "Submitting registration request to Supabase...";

  try {
    const checkResp = await fetch(`${SUPABASE_URL}/rest/v1/app_users?email=eq.${email}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const existing = await checkResp.json();
    if (existing && existing.length > 0) {
      currentUser = existing[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
      return;
    }

    const insertResp = await fetch(`${SUPABASE_URL}/rest/v1/app_users`, {
      method: "POST",
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Content-Type": "application/json",
        "Prefer": "return=representation"
      },
      body: JSON.stringify({
        full_name: name,
        mobile_number: mobile,
        email: email,
        status: "pending",
        role: "user"
      })
    });

    const data = await insertResp.json();
    if (insertResp.ok && data && data.length > 0) {
      currentUser = data[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
    } else {
      alertBox.style.color = "var(--red)";
      alertBox.innerText = "Error registering user: " + (data.message || "Please check details");
    }
  } catch (err) {
    alertBox.style.color = "var(--red)";
    alertBox.innerText = "Network error: " + err.message;
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const identifier = document.getElementById("loginIdentifier").value.trim().toLowerCase();
  const alertBox = document.getElementById("authAlert");

  alertBox.style.color = "var(--cyan)";
  alertBox.innerText = "Verifying with Supabase...";

  const query = identifier.includes("@") ? `email=eq.${identifier}` : `mobile_number=eq.${identifier}`;

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?${query}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    if (users && users.length > 0) {
      currentUser = users[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
    } else {
      alertBox.style.color = "var(--red)";
      alertBox.innerText = "User record not found. Please register first.";
    }
  } catch (err) {
    alertBox.style.color = "var(--red)";
    alertBox.innerText = "Login error: " + err.message;
  }
}

function adminQuickLogin() {
  document.getElementById("loginIdentifier").value = "arjunmalviya166@gmail.com";
  toggleAuth(false);
  handleLogin({ preventDefault: () => {} });
}

async function checkActiveStatus() {
  if (!currentUser) {
    showView("auth");
    return;
  }

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?id=eq.${currentUser.id}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    if (users && users.length > 0) {
      currentUser = users[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
    }
  } catch (e) {
    console.warn("Could not refresh live status", e);
  }

  // Update Header
  document.getElementById("userHeader").classList.remove("hidden");
  document.getElementById("headerUserName").innerText = currentUser.full_name;
  
  const statusBadge = document.getElementById("headerUserStatus");
  statusBadge.innerText = currentUser.status;
  statusBadge.className = `badge ${currentUser.status}`;

  if (currentUser.status === "approved") {
    showView("dashboard");
    if (currentUser.role === "admin") {
      document.getElementById("adminPanel").classList.remove("hidden");
      loadAdminUsers();
    } else {
      document.getElementById("adminPanel").classList.add("hidden");
    }
    
    // Defer chart canvas initialization until after the dashboard view is fully displayed and measured
    requestAnimationFrame(() => {
      initDashboardShares();
    });
  } else {
    showView("pending");
    document.getElementById("pendingMessage").innerText = 
      `नमस्ते ${currentUser.full_name}, आपकी रिक्वेस्ट (Mobile: ${currentUser.mobile_number}, Email: ${currentUser.email}) एडमिन के पास पहुँच गई है। एडमिन द्वारा Rights (Approval) देने के बाद ही आप शेयर्स और लाइव चार्ट देख सकेंगे।`;
  }
}

function initDashboardShares() {
  // Security guard: ensure user is authenticated and approved
  if (!currentUser || currentUser.status !== "approved") {
    console.warn("Unauthorized attempt to initialize chart and share data.");
    destroyActiveChart();
    showView("auth");
    return;
  }

  renderSharesList();
  renderSelectedStock(selectedStock);
  startLivePriceStream();
}

function handleStockSearch(query) {
  currentStockSearchQuery = (query || "").trim().toLowerCase();
  const clearBtn = document.getElementById("clearStockSearchBtn");
  if (clearBtn) {
    clearBtn.classList.toggle("hidden", currentStockSearchQuery.length === 0);
  }
  renderSharesList();
}

function clearStockSearch() {
  const input = document.getElementById("stockSearchInput");
  if (input) {
    input.value = "";
    input.focus();
  }
  handleStockSearch("");
}

function renderSharesList() {
  if (!currentUser || currentUser.status !== "approved") return;
  const container = document.getElementById("stocksListContainer");
  if (!container) return;
  container.innerHTML = "";

  const filteredStocks = STOCKS.filter(stock => {
    if (!currentStockSearchQuery) return true;
    const matchSymbol = stock.symbol.toLowerCase().includes(currentStockSearchQuery);
    const matchName = stock.name.toLowerCase().includes(currentStockSearchQuery);
    return matchSymbol || matchName;
  });

  if (filteredStocks.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 24px 12px; color: var(--text-muted); font-size: 13px;">
        <i class="fa-solid fa-circle-exclamation" style="font-size: 24px; color: var(--amber); margin-bottom: 8px; display: block;"></i>
        <span>No stocks found matching "<b>${currentStockSearchQuery}</b>"</span>
      </div>
    `;
    return;
  }

  filteredStocks.forEach(stock => {
    const isPositive = stock.change >= 0;
    const isSelected = stock.symbol === selectedStock.symbol;

    const div = document.createElement("div");
    div.className = `stock-item ${isSelected ? "active" : ""}`;
    div.dataset.symbol = stock.symbol;
    div.onclick = () => selectStock(stock);

    div.innerHTML = `
      <div class="stock-row-top">
        <span class="stock-symbol">${stock.symbol}</span>
        <span class="stock-price" id="listPrice_${stock.symbol}">₹${stock.price.toFixed(2)}</span>
      </div>
      <div class="stock-row-sub">
        <span>${stock.name.substring(0, 22)}</span>
        <span class="stock-change ${isPositive ? 'positive' : 'negative'}" id="listChange_${stock.symbol}">
          ${isPositive ? '+' : ''}${stock.change.toFixed(2)} (${isPositive ? '+' : ''}${stock.changePercent}%)
        </span>
      </div>
    `;
    container.appendChild(div);
  });
}

function selectStock(stock) {
  if (!currentUser || currentUser.status !== "approved") return;
  selectedStock = stock;
  renderSharesList();
  renderSelectedStock(stock);
}

function renderSelectedStock(stock) {
  if (!currentUser || currentUser.status !== "approved") return;
  document.getElementById("selectedStockSymbol").innerText = stock.symbol;
  document.getElementById("selectedStockName").innerText = stock.name;
  
  const isPos = stock.change >= 0;
  const priceElem = document.getElementById("selectedStockPrice");
  priceElem.innerText = `₹${stock.price.toFixed(2)}`;
  priceElem.style.color = isPos ? "var(--green)" : "var(--red)";

  const changeElem = document.getElementById("selectedStockChange");
  changeElem.innerText = `${isPos ? '+' : ''}${stock.change.toFixed(2)} (${isPos ? '+' : ''}${stock.changePercent}%)`;
  changeElem.style.color = isPos ? "var(--green)" : "var(--red)";

  // Signal Badge
  const signalBadge = document.getElementById("signalBadge");
  signalBadge.innerText = stock.signal;
  signalBadge.className = `badge ${stock.signal.includes("BUY") ? "approved" : "pending"}`;

  // Indicators
  document.getElementById("indRsi").innerText = stock.rsi;
  document.getElementById("indEma").innerText = `₹${stock.ema20.toFixed(2)}`;
  document.getElementById("indSma").innerText = `₹${stock.sma50.toFixed(2)}`;
  document.getElementById("indMacd").innerText = stock.macd;

  // AI Insight Text
  document.getElementById("aiInsightText").innerText = stock.aiText;

  // Render Chart
  if (currentChartMode === "tradingview") {
    renderTradingViewChart(stock.tvSymbol || `NSE:${stock.symbol}`);
  } else {
    renderChart(stock);
  }
}

function switchChartMode(mode) {
  if (!currentUser || currentUser.status !== "approved") return;
  currentChartMode = mode;
  const tvBtn = document.getElementById("modeTvBtn");
  const aiBtn = document.getElementById("modeAiBtn");
  const tvWrapper = document.getElementById("tvChartWrapper");
  const aiWrapper = document.getElementById("aiChartWrapper");
  const aiTimeframeTabs = document.getElementById("aiTimeframeTabs");
  const tvLiveInfo = document.getElementById("tvLiveInfo");

  if (tvBtn && aiBtn) {
    tvBtn.classList.toggle("active", mode === "tradingview");
    aiBtn.classList.toggle("active", mode === "ai");
  }

  if (tvWrapper && aiWrapper) {
    tvWrapper.classList.toggle("hidden", mode !== "tradingview");
    aiWrapper.classList.toggle("hidden", mode !== "ai");
  }

  if (aiTimeframeTabs) {
    aiTimeframeTabs.classList.toggle("hidden", mode !== "ai");
  }
  if (tvLiveInfo) {
    tvLiveInfo.classList.toggle("hidden", mode !== "tradingview");
  }

  if (mode === "tradingview") {
    renderTradingViewChart(selectedStock.tvSymbol || `NSE:${selectedStock.symbol}`);
  } else {
    renderChart(selectedStock);
  }
}

function renderTradingViewChart(symbol) {
  if (!currentUser || currentUser.status !== "approved") return;
  const container = document.getElementById("tradingview_chart_container");
  if (!container) return;

  const cleanSymbol = symbol.includes(":") ? symbol : `NSE:${symbol}`;
  container.innerHTML = "";

  if (typeof TradingView !== 'undefined' && TradingView.widget) {
    try {
      new TradingView.widget({
        "autosize": true,
        "symbol": cleanSymbol,
        "interval": "D",
        "timezone": "Asia/Kolkata",
        "theme": "dark",
        "style": "1",
        "locale": "in",
        "toolbar_bg": "#111827",
        "enable_publishing": false,
        "allow_symbol_change": true,
        "hide_side_toolbar": false,
        "withdateranges": true,
        "save_image": false,
        "container_id": "tradingview_chart_container"
      });
      return;
    } catch (e) {
      console.warn("TradingView widget init error, fallback to iframe:", e);
    }
  }

  // Reliable iframe fallback
  const iframe = document.createElement("iframe");
  iframe.src = `https://s.tradingview.com/widgetembed/?frameElementId=tradingview_widget&symbol=${encodeURIComponent(cleanSymbol)}&interval=D&hidesidetoolbar=0&symboledit=1&saveimage=0&toolbarbg=111827&studies=%5B%5D&theme=dark&style=1&timezone=Asia%2FKolkata&locale=in&utm_source=&utm_medium=widget&utm_campaign=chart&utm_term=${encodeURIComponent(cleanSymbol)}`;
  iframe.style.width = "100%";
  iframe.style.height = "100%";
  iframe.style.border = "none";
  iframe.style.borderRadius = "8px";
  container.appendChild(iframe);
}

function destroyActiveChart() {
  if (stockChartInstance) {
    stockChartInstance.destroy();
    stockChartInstance = null;
  }
}

function renderChart(stock) {
  // Enforce session check: Chart rendering occurs only for authenticated & approved users
  if (!currentUser || currentUser.status !== "approved") {
    destroyActiveChart();
    return;
  }

  const canvas = document.getElementById("stockChartCanvas");
  if (!canvas) return;

  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  destroyActiveChart();

  const labels = ["Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Today"];
  const isPositive = stock.change >= 0;
  const strokeColor = isPositive ? "#00e676" : "#ff3d71";
  const fillColor = isPositive ? "rgba(0, 230, 118, 0.12)" : "rgba(255, 61, 113, 0.12)";

  if (typeof Chart === 'undefined') {
    console.warn("Chart.js is still loading...");
    return;
  }

  stockChartInstance = new Chart(ctx, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: `${stock.symbol} Price (₹)`,
          data: stock.chartData,
          borderColor: strokeColor,
          backgroundColor: fillColor,
          borderWidth: 2.5,
          fill: true,
          tension: 0.35,
          pointBackgroundColor: strokeColor,
          pointBorderColor: "#fff",
          pointRadius: 4,
          pointHoverRadius: 6
        },
        {
          label: `20 EMA (₹${stock.ema20.toFixed(0)})`,
          data: stock.chartData.map(() => stock.ema20),
          borderColor: "#00e5ff",
          borderWidth: 1.5,
          borderDash: [5, 5],
          fill: false,
          pointRadius: 0
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 400 },
      plugins: {
        legend: {
          labels: { color: "#8899ac", font: { size: 11 } }
        },
        tooltip: {
          mode: 'index',
          intersect: false
        }
      },
      scales: {
        x: {
          grid: { color: "rgba(35, 50, 82, 0.4)" },
          ticks: { color: "#8899ac" }
        },
        y: {
          grid: { color: "rgba(35, 50, 82, 0.4)" },
          ticks: { color: "#8899ac" }
        }
      }
    }
  });
}

function startLivePriceStream() {
  if (liveDataInterval) clearInterval(liveDataInterval);

  // Periodic real-time market tick simulation for approved users
  liveDataInterval = setInterval(() => {
    if (!currentUser || currentUser.status !== "approved") {
      clearInterval(liveDataInterval);
      return;
    }

    if (!selectedStock) return;

    // Small realistic market price movement (+/- 0.15%)
    const isTickPositive = Math.random() > 0.47;
    const tickMagnitude = (Math.random() * 0.0025 + 0.0005);
    const delta = (isTickPositive ? 1 : -1) * (selectedStock.price * tickMagnitude);

    const oldPrice = selectedStock.price;
    selectedStock.price = parseFloat(Math.max(1, selectedStock.price + delta).toFixed(2));
    selectedStock.change = parseFloat((selectedStock.change + delta).toFixed(2));
    const baseRef = Math.max(1, selectedStock.price - selectedStock.change);
    selectedStock.changePercent = parseFloat(((selectedStock.change / baseRef) * 100).toFixed(2));

    // Update last chart candle point
    if (selectedStock.chartData && selectedStock.chartData.length > 0) {
      selectedStock.chartData[selectedStock.chartData.length - 1] = selectedStock.price;
    }

    // Refresh chart main header UI elements
    const priceElem = document.getElementById("selectedStockPrice");
    const changeElem = document.getElementById("selectedStockChange");
    if (priceElem && changeElem) {
      const isPos = selectedStock.change >= 0;
      priceElem.innerText = `₹${selectedStock.price.toFixed(2)}`;
      priceElem.style.color = isPos ? "var(--green)" : "var(--red)";
      
      // Trigger subtle pulse animation on price change
      priceElem.classList.remove("flash-green", "flash-red");
      void priceElem.offsetWidth; // Trigger reflow
      priceElem.classList.add(delta >= 0 ? "flash-green" : "flash-red");

      changeElem.innerText = `${isPos ? '+' : ''}${selectedStock.change.toFixed(2)} (${isPos ? '+' : ''}${selectedStock.changePercent}%)`;
      changeElem.style.color = isPos ? "var(--green)" : "var(--red)";
    }

    // Also update the stock card in the Indian Shares list if visible
    const listPriceElem = document.getElementById(`listPrice_${selectedStock.symbol}`);
    const listChangeElem = document.getElementById(`listChange_${selectedStock.symbol}`);
    if (listPriceElem) {
      listPriceElem.innerText = `₹${selectedStock.price.toFixed(2)}`;
    }
    if (listChangeElem) {
      const isPos = selectedStock.change >= 0;
      listChangeElem.className = `stock-change ${isPos ? 'positive' : 'negative'}`;
      listChangeElem.innerText = `${isPos ? '+' : ''}${selectedStock.change.toFixed(2)} (${isPos ? '+' : ''}${selectedStock.changePercent}%)`;
    }

    // Periodically update one random background stock in the list to make the whole market feel alive
    const otherStocks = STOCKS.filter(s => s.symbol !== selectedStock.symbol);
    if (otherStocks.length > 0 && Math.random() > 0.4) {
      const bgStock = otherStocks[Math.floor(Math.random() * otherStocks.length)];
      const bgDelta = (Math.random() > 0.5 ? 1 : -1) * (bgStock.price * 0.0015);
      bgStock.price = parseFloat(Math.max(1, bgStock.price + bgDelta).toFixed(2));
      bgStock.change = parseFloat((bgStock.change + bgDelta).toFixed(2));
      const bgBase = Math.max(1, bgStock.price - bgStock.change);
      bgStock.changePercent = parseFloat(((bgStock.change / bgBase) * 100).toFixed(2));

      const bgListPrice = document.getElementById(`listPrice_${bgStock.symbol}`);
      const bgListChange = document.getElementById(`listChange_${bgStock.symbol}`);
      if (bgListPrice) bgListPrice.innerText = `₹${bgStock.price.toFixed(2)}`;
      if (bgListChange) {
        const bgPos = bgStock.change >= 0;
        bgListChange.className = `stock-change ${bgPos ? 'positive' : 'negative'}`;
        bgListChange.innerText = `${bgPos ? '+' : ''}${bgStock.change.toFixed(2)} (${bgPos ? '+' : ''}${bgStock.changePercent}%)`;
      }
    }

    // Update Chart without full recreation
    if (stockChartInstance && stockChartInstance.data && stockChartInstance.data.datasets.length > 0) {
      stockChartInstance.data.datasets[0].data = [...selectedStock.chartData];
      stockChartInstance.update('none');
    }
  }, 2500);
}

function switchTimeframe(tf) {
  if (!currentUser || currentUser.status !== "approved") return;
  const eventTarget = window.event ? window.event.target : null;
  document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
  if (eventTarget) eventTarget.classList.add("active");
  
  if (selectedStock) {
    if (tf === '1D') {
      selectedStock.chartData = [selectedStock.price - 12, selectedStock.price - 6, selectedStock.price + 4, selectedStock.price - 2, selectedStock.price + 8, selectedStock.price];
    } else if (tf === '1W') {
      selectedStock.chartData = [selectedStock.price - 40, selectedStock.price - 25, selectedStock.price - 30, selectedStock.price - 10, selectedStock.price + 15, selectedStock.price];
    } else if (tf === '1M') {
      selectedStock.chartData = [selectedStock.price - 120, selectedStock.price - 80, selectedStock.price - 40, selectedStock.price - 50, selectedStock.price - 10, selectedStock.price];
    } else {
      selectedStock.chartData = [selectedStock.price - 350, selectedStock.price - 220, selectedStock.price - 150, selectedStock.price - 80, selectedStock.price - 20, selectedStock.price];
    }
    renderChart(selectedStock);
  }
}

async function loadAdminUsers() {
  if (!currentUser || currentUser.role !== "admin") return;
  const tbody = document.getElementById("userTableBody");
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--text-muted);">Loading registered users...</td></tr>`;

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?select=*&order=created_at.desc`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    tbody.innerHTML = "";

    users.forEach(u => {
      const isApproved = u.status === "approved";
      const isRejected = u.status === "rejected";
      const isAdmin = u.role === "admin";

      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>
          <div style="font-weight:600;">${u.full_name} ${isAdmin ? '<span style="color:var(--cyan);">(Admin)</span>' : ''}</div>
          <div style="color:var(--text-muted); font-size:11px;">${u.email}</div>
        </td>
        <td>${u.mobile_number}</td>
        <td>
          <span class="badge ${u.status}">${u.status}</span>
        </td>
        <td>
          <div class="action-btn-group">
            ${!isApproved ? `<button class="btn-sm btn-green" onclick="updateUserStatus('${u.id}', 'approved')"><i class="fa-solid fa-check"></i> Approve</button>` : ''}
            ${!isRejected && !isAdmin ? `<button class="btn-sm btn-red" onclick="updateUserStatus('${u.id}', 'rejected')"><i class="fa-solid fa-xmark"></i> Reject</button>` : ''}
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" style="color:var(--red);">Failed to load users: ${err.message}</td></tr>`;
  }
}

async function updateUserStatus(userId, status) {
  try {
    await fetch(`${SUPABASE_URL}/rest/v1/app_users?id=eq.${userId}`, {
      method: "PATCH",
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        status: status,
        approved_by: currentUser.email,
        approved_at: new Date().toISOString()
      })
    });
    loadAdminUsers();
  } catch (e) {
    alert("Error updating status: " + e.message);
  }
}

function logout() {
  if (liveDataInterval) clearInterval(liveDataInterval);
  destroyActiveChart();
  const tvContainer = document.getElementById("tradingview_chart_container");
  if (tvContainer) tvContainer.innerHTML = "";
  localStorage.removeItem("app_user");
  currentUser = null;
  document.getElementById("userHeader").classList.add("hidden");
  showView("auth");
}

function showView(view) {
  document.getElementById("authGate").classList.toggle("hidden", view !== "auth");
  document.getElementById("pendingGate").classList.toggle("hidden", view !== "pending");
  document.getElementById("appDashboard").classList.toggle("hidden", view !== "dashboard");
}
