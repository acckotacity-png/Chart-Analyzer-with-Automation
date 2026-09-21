// Supabase Credentials
const SUPABASE_URL = "https://qhqbporwncgccpcgrurl.supabase.co";
const SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFocWJwb3J3bmNnY2NwY2dydXJsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5NjMxOTIsImV4cCI6MjEwNTUzOTE5Mn0.o0EgKYIv0JQSxJJftgIpZwFA49br7tgTNdSkSmgYwMs";

let currentUser = JSON.parse(localStorage.getItem("app_user") || "null");

// Indian Stock Data & Indicators
const STOCKS = [
  {
    symbol: "RELIANCE",
    name: "Reliance Industries Ltd",
    price: 2865.40,
    change: 38.20,
    changePercent: 1.35,
    rsi: 64.2,
    ema20: 2820.50,
    sma50: 2785.00,
    macd: "Bullish +14.2",
    signal: "BUY",
    aiText: "Reliance 20 EMA से ऊपर ट्रेड कर रहा है। RSI 64.2 स्ट्रॉन्ग बुलिश मोमेंटम दिखाता है। स्टॉपलॉस ₹2,810 के साथ बाय कॉल एक्टिव है।",
    chartData: [2780, 2795, 2810, 2805, 2835, 2840, 2865.40]
  },
  {
    symbol: "TCS",
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
  }
];

let selectedStock = STOCKS[0];
let stockChartInstance = null;
let liveDataInterval = null;

// Initialize on load
window.addEventListener("DOMContentLoaded", () => {
  if (currentUser) {
    checkActiveStatus();
  } else {
    showView("auth");
  }
});

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

function renderSharesList() {
  if (!currentUser || currentUser.status !== "approved") return;
  const container = document.getElementById("stocksListContainer");
  if (!container) return;
  container.innerHTML = "";

  STOCKS.forEach(stock => {
    const isPositive = stock.change >= 0;
    const isSelected = stock.symbol === selectedStock.symbol;

    const div = document.createElement("div");
    div.className = `stock-item ${isSelected ? "active" : ""}`;
    div.onclick = () => selectStock(stock);

    div.innerHTML = `
      <div class="stock-row-top">
        <span class="stock-symbol">${stock.symbol}</span>
        <span class="stock-price">₹${stock.price.toFixed(2)}</span>
      </div>
      <div class="stock-row-sub">
        <span>${stock.name.substring(0, 20)}</span>
        <span class="stock-change ${isPositive ? 'positive' : 'negative'}">
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
  renderChart(stock);
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

    // Small realistic market price movement (+/- 0.15%)
    const delta = (Math.random() - 0.48) * (selectedStock.price * 0.002);
    selectedStock.price = parseFloat((selectedStock.price + delta).toFixed(2));
    selectedStock.change = parseFloat((selectedStock.change + delta).toFixed(2));
    selectedStock.changePercent = parseFloat(((selectedStock.change / (selectedStock.price - selectedStock.change)) * 100).toFixed(2));

    // Update last chart candle point
    if (selectedStock.chartData && selectedStock.chartData.length > 0) {
      selectedStock.chartData[selectedStock.chartData.length - 1] = selectedStock.price;
    }

    // Refresh UI elements
    const priceElem = document.getElementById("selectedStockPrice");
    const changeElem = document.getElementById("selectedStockChange");
    if (priceElem && changeElem) {
      const isPos = selectedStock.change >= 0;
      priceElem.innerText = `₹${selectedStock.price.toFixed(2)}`;
      priceElem.style.color = isPos ? "var(--green)" : "var(--red)";
      changeElem.innerText = `${isPos ? '+' : ''}${selectedStock.change.toFixed(2)} (${isPos ? '+' : ''}${selectedStock.changePercent}%)`;
      changeElem.style.color = isPos ? "var(--green)" : "var(--red)";
    }

    // Update Chart without full recreation
    if (stockChartInstance && stockChartInstance.data && stockChartInstance.data.datasets.length > 0) {
      stockChartInstance.data.datasets[0].data = selectedStock.chartData;
      stockChartInstance.update('none');
    }
  }, 3500);
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
