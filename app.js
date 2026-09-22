// Supabase configuration is shared by authentication and the data bridge.
const SUPABASE_URL = (window.APP_CONFIG?.supabaseUrl || "").replace(/\/$/, "");
const SUPABASE_ANON = window.APP_CONFIG?.supabaseAnonKey || "";
const authClient = SUPABASE_URL && SUPABASE_ANON && window.supabase
  ? window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON) : null;
let currentUser = null;
localStorage.removeItem("app_user"); // Never trust legacy cached roles or approval status.
function readStoredJson(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; }
}
function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
}
async function sessionToken() {
  if (!authClient) throw new Error("Configure the new Supabase project in config.js first.");
  const { data, error } = await authClient.auth.getSession();
  if (error || !data.session) throw new Error("Please sign in again.");
  return data.session.access_token;
}

// Symbol directory only. Prices come exclusively from authenticated broker responses.
const STOCKS = [
  {
    "symbol": "RELIANCE",
    "name": "Reliance Industries Ltd",
    "tvSymbol": "NSE:RELIANCE"
  },
  {
    "symbol": "BHARTIARTL",
    "name": "Bharti Airtel Ltd",
    "tvSymbol": "NSE:BHARTIARTL"
  },
  {
    "symbol": "HDFCBANK",
    "name": "HDFC Bank Ltd",
    "tvSymbol": "NSE:HDFCBANK"
  },
  {
    "symbol": "ICICIBANK",
    "name": "ICICI Bank Ltd",
    "tvSymbol": "NSE:ICICIBANK"
  },
  {
    "symbol": "SBIN",
    "name": "State Bank of India",
    "tvSymbol": "NSE:SBIN"
  },
  {
    "symbol": "TCS",
    "name": "Tata Consultancy Services",
    "tvSymbol": "NSE:TCS"
  },
  {
    "symbol": "BAJFINANCE",
    "name": "Bajaj Finance Ltd",
    "tvSymbol": "NSE:BAJFINANCE"
  },
  {
    "symbol": "LT",
    "name": "Larsen & Toubro Ltd",
    "tvSymbol": "NSE:LT"
  },
  {
    "symbol": "INFY",
    "name": "Infosys Ltd",
    "tvSymbol": "NSE:INFY"
  },
  {
    "symbol": "TATAMOTORS",
    "name": "Tata Motors Ltd",
    "tvSymbol": "NSE:TATAMOTORS"
  },
  {
    "symbol": "TATASTEEL",
    "name": "Tata Steel Ltd",
    "tvSymbol": "NSE:TATASTEEL"
  },
  {
    "symbol": "ITC",
    "name": "ITC Limited",
    "tvSymbol": "NSE:ITC"
  },
  {
    "symbol": "ADANIENT",
    "name": "Adani Enterprises Ltd",
    "tvSymbol": "NSE:ADANIENT"
  },
  {
    "symbol": "WIPRO",
    "name": "Wipro Ltd",
    "tvSymbol": "NSE:WIPRO"
  },
  {
    "symbol": "ETERNAL",
    "name": "Zomato Ltd",
    "tvSymbol": "NSE:ZOMATO"
  },
  {
    "symbol": "MARUTI",
    "name": "Maruti Suzuki India Ltd",
    "tvSymbol": "NSE:MARUTI"
  }
].map(s => ({...s, price: NaN, change: NaN, changePercent: null}));

// Upstox V3 Instrument Keys for Real-Time WebSocket Feeds
const UPSTOX_INSTRUMENT_KEYS = {
  "RELIANCE": "NSE_EQ|INE002A01018",
  "TCS": "NSE_EQ|INE467B01029",
  "TATAMOTORS": "NSE_EQ|INE155A01022",
  "TATASTEEL": "NSE_EQ|INE081A01020",
  "INFY": "NSE_EQ|INE009A01021",
  "HDFCBANK": "NSE_EQ|INE040A01034",
  "ICICIBANK": "NSE_EQ|INE090A01021",
  "SBIN": "NSE_EQ|INE062A01020",
  "BHARTIARTL": "NSE_EQ|INE397D01024",
  "ITC": "NSE_EQ|INE154A01025",
  "LT": "NSE_EQ|INE018A01030",
  "ADANIENT": "NSE_EQ|INE423A01024",
  "BAJFINANCE": "NSE_EQ|INE296A01024",
  "WIPRO": "NSE_EQ|INE075A01022",
  "ZOMATO": "NSE_EQ|INE758T01015",
  "MARUTI": "NSE_EQ|INE585B01010"
};

let selectedStock = STOCKS[0];
let stockChartInstance = null;
let liveDataInterval = null;
let restReconciliationInterval = null;
let lastValidatedPrice = null;
let upstoxWebSocket = null;
let wsReconnectTimeout = null;
let priceAlerts = []; // Legacy device-global alerts are disabled.
let currentStockSearchQuery = "";
let currentChartMode = "custom";

// Initialize on load (handle both interactive/complete and DOMContentLoaded)
function initApp() {
  if (authClient?.auth?.onAuthStateChange) {
    authClient.auth.onAuthStateChange((event) => {
      if (event === "PASSWORD_RECOVERY") showPasswordRecovery();
    });
  }
  checkActiveStatus();
}

if (document.readyState === "loading") {
  window.addEventListener("DOMContentLoaded", initApp);
} else {
  initApp();
}

function setAuthForm(activeFormId, message = "") {
  ["registerForm", "loginForm", "forgotPasswordForm", "passwordRecoveryForm"].forEach(id => {
    const form = document.getElementById(id);
    if (form) form.classList.toggle("hidden", id !== activeFormId);
  });
  const alertBox = document.getElementById("authAlert");
  if (alertBox) alertBox.textContent = message;
}

function toggleAuth(showRegister) {
  setAuthForm(showRegister ? "registerForm" : "loginForm");
}

function showForgotPassword() {
  const loginEmail = document.getElementById("loginIdentifier")?.value.trim();
  setAuthForm("forgotPasswordForm");
  if (loginEmail) document.getElementById("resetEmail").value = loginEmail;
}

function showPasswordRecovery() {
  setAuthForm("passwordRecoveryForm", "Set a new password to finish account recovery.");
}

async function handleForgotPassword(e) {
  e.preventDefault();
  const box = document.getElementById("authAlert");
  try {
    if (!authClient) throw new Error("Supabase is not configured.");
    const email = document.getElementById("resetEmail").value.trim().toLowerCase();
    const redirectTo = `${window.location.origin}${window.location.pathname}`;
    const { error } = await authClient.auth.resetPasswordForEmail(email, { redirectTo });
    if (error) throw error;
    box.textContent = "If this email is registered, a password-reset link has been sent. Check your inbox and spam folder.";
  } catch (error) {
    box.textContent = error.message;
  }
}

async function handlePasswordRecovery(e) {
  e.preventDefault();
  const box = document.getElementById("authAlert");
  try {
    if (!authClient) throw new Error("Supabase is not configured.");
    const password = document.getElementById("recoveryPassword").value;
    const confirmPassword = document.getElementById("recoveryPasswordConfirm").value;
    if (password.length < 8) throw new Error("Use at least 8 characters.");
    if (password !== confirmPassword) throw new Error("Passwords do not match.");
    const { error } = await authClient.auth.updateUser({ password });
    if (error) throw error;
    document.getElementById("recoveryPassword").value = "";
    document.getElementById("recoveryPasswordConfirm").value = "";
    await authClient.auth.signOut({ scope: "local" });
    setAuthForm("loginForm", "Password updated. Sign in with your new password.");
  } catch (error) {
    box.textContent = error.message;
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const box = document.getElementById("authAlert");
  try {
    if (!authClient) throw new Error("Configure the new Supabase project in config.js first.");
    const { data, error } = await authClient.auth.signUp({
      email: document.getElementById("regEmail").value.trim().toLowerCase(),
      password: document.getElementById("regPassword").value,
      options: { data: {
        full_name: document.getElementById("regName").value.trim(),
        mobile_number: document.getElementById("regMobile").value.trim()
      } }
    });
    if (error) throw error;
    document.getElementById("regPassword").value = "";
    if (data.session) await checkActiveStatus();
    else box.textContent = "Check your email to confirm registration, then sign in. Admin approval is also required.";
  } catch (error) { box.textContent = error.message; }
}

async function handleLogin(e) {
  e.preventDefault();
  const box = document.getElementById("authAlert");
  try {
    if (!authClient) throw new Error("Configure the new Supabase project in config.js first.");
    const { error } = await authClient.auth.signInWithPassword({
      email: document.getElementById("loginIdentifier").value.trim().toLowerCase(),
      password: document.getElementById("loginPassword").value
    });
    if (error) throw error;
    document.getElementById("loginPassword").value = "";
    await checkActiveStatus();
  } catch (error) { box.textContent = error.message; }
}

async function checkActiveStatus() {
  try {
    if (!authClient) throw new Error("Set your new Supabase URL and public key in config.js.");
    const { data: sessionData, error: sessionError } = await authClient.auth.getSession();
    if (sessionError) throw sessionError;
    if (!sessionData.session) { currentUser = null; stopLivePriceStream(); document.getElementById("accessPanel").classList.add("hidden"); showView("auth"); return; }
    const { data, error } = await authClient.auth.getUser();
    if (error || !data.user) throw error || new Error("Please sign in again.");
    const { data: profile, error: profileError } = await authClient.from("app_users")
      .select("*").eq("id", data.user.id).single();
    if (profileError || !profile) throw profileError || new Error("Profile is unavailable.");
    currentUser = profile;
  } catch (error) {
    currentUser = null;
    stopLivePriceStream();
    showView("auth");
    document.getElementById("userHeader").classList.add("hidden");
    document.getElementById("authAlert").textContent = error.message;
    return;
  }

  loadAccessPanel();
  // Update Header
  document.getElementById("userHeader").classList.remove("hidden");
  document.getElementById("headerUserName").innerText = currentUser.full_name;

  const statusBadge = document.getElementById("headerUserStatus");
  statusBadge.innerText = currentUser.status;
  statusBadge.className = `badge ${currentUser.status}`;

  if (hasActiveAccess()) {
    showView("dashboard");
    tradeOwner = currentUser.id;
    cancelTradeEdit();
    loadTradeHistory();
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
    stopLivePriceStream();
    showView("pending");
    document.getElementById("pendingMessage").innerText =
      `Hello ${currentUser.full_name}. Access is pending, expired, or revoked. Request a plan below; administrator approval is required.`;
  }
}

function initDashboardShares() {
  // Security guard: ensure user is authenticated and approved
  if (!currentUser || !hasActiveAccess()) {
    console.warn("Unauthorized attempt to initialize chart and share data.");
    destroyActiveChart();
    showView("auth");
    return;
  }

  populateQuickStockDropdown();
  renderSharesList();
  renderSelectedStock(selectedStock);
}

function populateQuickStockDropdown() {
  const select = document.getElementById("quickStockSelect");
  if (!select) return;
  select.innerHTML = "";
  STOCKS.forEach(stock => {
    const opt = document.createElement("option");
    opt.value = stock.symbol;
    opt.innerText = stock.symbol;
    if (selectedStock && stock.symbol === selectedStock.symbol) {
      opt.selected = true;
    }
    select.appendChild(opt);
  });
}

function onQuickStockSelect(sym) {
  const found = STOCKS.find(s => s.symbol === sym);
  if (found) {
    selectStock(found);
  }
}

function handleStockSearch(query) {
  currentStockSearchQuery = (query || "").trim().toLowerCase();
  const clearBtn = document.getElementById("clearStockSearchBtn");
  if (clearBtn) {
    clearBtn.classList.toggle("hidden", currentStockSearchQuery.length === 0);
  }
  renderSharesList();
}

function handleStockSearchEnter() {
  const input = document.getElementById("stockSearchInput");
  if (!input) return;
  const q = input.value.trim();
  if (!q) return;

  // Exact or partial match in existing STOCKS
  const exactMatch = STOCKS.find(s => s.symbol.toLowerCase() === q.toLowerCase());
  if (exactMatch) {
    selectStock(exactMatch);
    return;
  }
  const partialMatch = STOCKS.find(s => s.symbol.toLowerCase().includes(q.toLowerCase()) || s.name.toLowerCase().includes(q.toLowerCase()));
  if (partialMatch) {
    selectStock(partialMatch);
    return;
  }

  // Dynamic Indian Stock Search directly to live NSE
  addAndSelectCustomStock(q);
}

function addAndSelectCustomStock(symbolInput) {
  const sym = symbolInput.trim().toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (!sym) return;

  let existing = STOCKS.find(s => s.symbol === sym || s.tvSymbol === `NSE:${sym}`);
  if (existing) {
    selectStock(existing);
    clearStockSearch();
    return;
  }

  const newStock = { symbol: sym, name: sym, tvSymbol: `NSE:${sym}`, price: NaN, change: NaN, changePercent: null };
  STOCKS.unshift(newStock);
  populateQuickStockDropdown();
  clearStockSearch();
  selectStock(newStock);
}

function clearStockSearch() {
  const input = document.getElementById("stockSearchInput");
  if (input) {
    input.value = "";
  }
  currentStockSearchQuery = "";
  const clearBtn = document.getElementById("clearStockSearchBtn");
  if (clearBtn) clearBtn.classList.add("hidden");
  renderSharesList();
}

function renderSharesList() {
  if (!currentUser || !hasActiveAccess()) return;
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
    const cleanQ = currentStockSearchQuery.toUpperCase().replace(/[^A-Z0-9]/g, "");
    container.innerHTML = `
      <div style="text-align: center; padding: 18px 10px; color: var(--text-muted); font-size: 13px;">
        <i class="fa-solid fa-circle-exclamation" style="font-size: 22px; color: var(--amber); margin-bottom: 8px; display: block;"></i>
        <span>"${escapeHtml(currentStockSearchQuery)}" is not in the preset list</span>
      </div>
      ${cleanQ ? `
        <div class="direct-search-banner" onclick="addAndSelectCustomStock('${cleanQ}')">
          <div style="display:flex; align-items:center; justify-content:center; gap:8px;">
            <i class="fa-solid fa-bolt" style="color:var(--cyan); font-size:14px;"></i>
            <span style="font-weight:700; color:var(--cyan);">Open NSE:${cleanQ} broker chart</span>
          </div>
          <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">Load through the authenticated broker data service</div>
        </div>
      ` : ''}
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
        <span class="stock-price" id="listPrice_${stock.symbol}">${Number.isFinite(stock.price) ? money(stock.price) : "—"}</span>
      </div>
      <div class="stock-row-sub">
        <span>${stock.name.substring(0, 22)}</span>
        <span class="stock-change ${isPositive ? 'positive' : 'negative'}" id="listChange_${stock.symbol}">
          ${Number.isFinite(stock.change) ? money(stock.change) + " (" + stock.changePercent + "%)" : "No quote"}
        </span>
      </div>
    `;
    container.appendChild(div);
  });

  // If user searched for custom Indian ticker, offer direct NSE open at the bottom too
  if (currentStockSearchQuery && currentStockSearchQuery.length >= 2) {
    const cleanQ = currentStockSearchQuery.toUpperCase().replace(/[^A-Z0-9]/g, "");
    const alreadyExact = filteredStocks.some(s => s.symbol === cleanQ);
    if (!alreadyExact && cleanQ) {
      const banner = document.createElement("div");
      banner.className = "direct-search-banner";
      banner.onclick = () => addAndSelectCustomStock(cleanQ);
      banner.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:center; gap:8px;">
          <i class="fa-solid fa-magnifying-glass-chart" style="color:var(--cyan);"></i>
          <span style="font-weight:700; color:var(--cyan); font-size:12px;">Load NSE:${cleanQ} broker chart</span>
        </div>
      `;
      container.appendChild(banner);
    }
  }
}

function selectStock(stock) {
  if (!currentUser || !hasActiveAccess()) return;
  selectedStock = stock;
  lastValidatedPrice = stock.price;
  renderSharesList();
  renderSelectedStock(stock);
  const quickSelect = document.getElementById("quickStockSelect");
  if (quickSelect) {
    quickSelect.value = stock.symbol;
  }

  renderActiveAlertsList();
  // Trigger immediate REST market price reconciliation on stock selection switch

}

function renderSelectedStock(stock) {
 if (!hasActiveAccess()) return;
 document.getElementById('selectedStockSymbol').textContent=stock.symbol;
 document.getElementById('selectedStockName').textContent=stock.name;
 const quick=document.getElementById('quickStockSelect');if(quick)quick.value=stock.symbol;
 if(!lwChart)initLightweightChart();
 onMarketSelection();
}

// -------------------------------------------------------------
// TRADINGVIEW LIGHTWEIGHT CHARTS CONTROLLER & STATE
// -------------------------------------------------------------
let lwChart = null;
let candleSeries = null;
let volumeSeries = null;
let emaSeries = null;
let smaSeries = null;
let vwapSeries = null;
let bbUpperSeries = null;
let bbLowerSeries = null;
let activeSrLines = [];

let activeIndicators = {
  ema: true,
  sma: true,
  vwap: true,
  bb: true,
  sr: true,
  volume: true
};

let currentTimeframe = "1D";
let currentCandles = [];

function initLightweightChart() {
  const container = document.getElementById("lightweight_chart_container");
  if (!container) return;

  if (lwChart) {
    try {
      lwChart.remove();
    } catch (e) {
      console.warn("Chart remove err:", e);
    }
    lwChart = null;
    candleSeries = volumeSeries = emaSeries = smaSeries = vwapSeries = bbUpperSeries = bbLowerSeries = null;
    activeSrLines = [];
  }
  container.innerHTML = "";

  if (typeof LightweightCharts === "undefined") {
    console.warn("TradingView LightweightCharts library not yet loaded. Retrying in 300ms...");
    setTimeout(initLightweightChart, 300);
    return;
  }

  lwChart = LightweightCharts.createChart(container, {
    width: container.clientWidth || 800,
    height: container.clientHeight || 440,
    layout: {
      background: { color: "#0c121e" },
      textColor: "#8899ac",
      fontSize: 11
    },
    grid: {
      vertLines: { color: "rgba(35, 50, 82, 0.4)" },
      horzLines: { color: "rgba(35, 50, 82, 0.4)" }
    },
    crosshair: {
      mode: LightweightCharts.CrosshairMode.Normal,
      vertLine: { color: "#00e5ff", width: 1, style: 3 },
      horzLine: { color: "#00e5ff", width: 1, style: 3 }
    },
    rightPriceScale: {
      borderColor: "#233252",
      visible: true
    },
    timeScale: {
      borderColor: "#233252",
      timeVisible: true,
      secondsVisible: false
    }
  });

  // Candlestick Series (Green/Red Indian Stock Colors)
  candleSeries = lwChart.addCandlestickSeries({
    upColor: "#00e676",
    downColor: "#ff3d71",
    borderVisible: false,
    wickUpColor: "#00e676",
    wickDownColor: "#ff3d71"
  });

  // Volume Series (Histogram at Bottom)
  volumeSeries = lwChart.addHistogramSeries({
    color: "#26a69a",
    priceFormat: { type: "volume" },
    priceScaleId: "",
    scaleMargins: { top: 0.82, bottom: 0 }
  });

  // 20 EMA Line Overlay (Cyan)
  emaSeries = lwChart.addLineSeries({
    color: "#00e5ff",
    lineWidth: 2,
    title: "EMA 20",
    priceScaleId: "right"
  });

  // 50 SMA Line Overlay (Amber)
  smaSeries = lwChart.addLineSeries({
    color: "#ffaa00",
    lineWidth: 2,
    title: "SMA 50",
    priceScaleId: "right"
  });

  // VWAP Line Overlay (Magenta)
  vwapSeries = lwChart.addLineSeries({
    color: "#ec4899",
    lineWidth: 2,
    title: "VWAP",
    priceScaleId: "right"
  });

  // Bollinger Bands (Purple Dotted Lines)
  bbUpperSeries = lwChart.addLineSeries({
    color: "#a855f7",
    lineWidth: 1,
    lineStyle: LightweightCharts.LineStyle.Dashed,
    title: "BB Upper",
    priceScaleId: "right"
  });
  bbLowerSeries = lwChart.addLineSeries({
    color: "#a855f7",
    lineWidth: 1,
    lineStyle: LightweightCharts.LineStyle.Dashed,
    title: "BB Lower",
    priceScaleId: "right"
  });

  // Responsive Resize
  window.addEventListener("resize", () => {
    if (lwChart && container) {
      lwChart.applyOptions({
        width: container.clientWidth,
        height: container.clientHeight || 440
      });
      ChartDrawingEngine.resizeCanvas();
    }
  });

  // Crosshair Legend Tooltip
  lwChart.subscribeCrosshairMove(param => {
    const legend = document.getElementById("legendOhlc");
    if (!legend) return;

    if (!param.time || !param.seriesData.get(candleSeries)) {
      if (currentCandles.length > 0) {
        const last = currentCandles[currentCandles.length - 1];
        legend.innerText = `O: ${last.open.toFixed(2)}  H: ${last.high.toFixed(2)}  L: ${last.low.toFixed(2)}  C: ${last.close.toFixed(2)}`;
      }
      return;
    }

    const ohlc = param.seriesData.get(candleSeries);
    if (ohlc) {
      legend.innerText = `O: ${ohlc.open.toFixed(2)}  H: ${ohlc.high.toFixed(2)}  L: ${ohlc.low.toFixed(2)}  C: ${ohlc.close.toFixed(2)}`;
    }
  });

  // Initialize interactive drawing overlay canvas on top of chart
  ChartDrawingEngine.init();
}

// -------------------------------------------------------------
// INTERACTIVE CHART DRAWING ENGINE (TRENDLINES, S/R, RAYS)
// -------------------------------------------------------------
const ChartDrawingEngine = {
  canvas: null,
  ctx: null,
  isEnabled: false,
  activeTool: "trendline", // 'trendline', 'support', 'resistance', 'ray'
  isDrawing: false,
  startPoint: null,
  currentPoint: null,
  drawings: [], // array of drawn shapes: { type, start: {x,y}, end: {x,y}, color, lineWidth }

  init() {
    this.canvas = document.getElementById("chartDrawingCanvas");
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext("2d");
    this.resizeCanvas();

    // Bind event listeners only once
    if (!this._bound) {
      this._bound = true;

      this.canvas.addEventListener("mousedown", (e) => this.handleMouseDown(e));
      this.canvas.addEventListener("mousemove", (e) => this.handleMouseMove(e));
      this.canvas.addEventListener("mouseup", (e) => this.handleMouseUp(e));
      this.canvas.addEventListener("mouseleave", () => this.handleMouseLeave());

      // Touch events for mobile/tablet support
      this.canvas.addEventListener("touchstart", (e) => {
        if (!this.isEnabled) return;
        const touch = e.touches[0];
        const rect = this.canvas.getBoundingClientRect();
        this.handleMouseDown({ clientX: touch.clientX, clientY: touch.clientY, preventDefault: () => e.preventDefault() });
      }, { passive: false });

      this.canvas.addEventListener("touchmove", (e) => {
        if (!this.isEnabled) return;
        const touch = e.touches[0];
        this.handleMouseMove({ clientX: touch.clientX, clientY: touch.clientY, preventDefault: () => e.preventDefault() });
      }, { passive: false });

      this.canvas.addEventListener("touchend", () => {
        if (!this.isEnabled) return;
        this.handleMouseUp();
      });
    }

    this.render();
  },

  resizeCanvas() {
    const wrapper = document.getElementById("chartContainerWrapper");
    if (!this.canvas || !wrapper) return;
    const dpr = window.devicePixelRatio || 1;
    const rect = wrapper.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;

    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.canvas.style.width = `${rect.width}px`;
    this.canvas.style.height = `${rect.height}px`;

    if (this.ctx) {
      this.ctx.scale(dpr, dpr);
    }
    this.render();
  },

  getCanvasCoordinates(e) {
    const rect = this.canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
  },

  handleMouseDown(e) {
    if (!this.isEnabled) return;
    if (e.preventDefault) e.preventDefault();
    const pt = this.getCanvasCoordinates(e);

    if (this.activeTool === "support" || this.activeTool === "resistance") {
      // Single-click horizontal line across the entire chart width
      const color = this.activeTool === "support" ? "#00e676" : "#ff3d71";
      const label = this.activeTool === "support" ? "Support" : "Resistance";
      this.drawings.push({
        type: "horizontal",
        y: pt.y,
        color: color,
        label: label,
        lineWidth: 2
      });
      this.updateCountBadge();
      this.render();
      return;
    }

    // Two-point tools (trendline, ray)
    this.isDrawing = true;
    this.startPoint = pt;
    this.currentPoint = pt;
    this.render();
  },

  handleMouseMove(e) {
    if (!this.isEnabled) return;
    const pt = this.getCanvasCoordinates(e);

    if (this.isDrawing) {
      if (e.preventDefault) e.preventDefault();
      this.currentPoint = pt;
      this.render();
    }
  },

  handleMouseUp(e) {
    if (!this.isEnabled || !this.isDrawing) return;
    if (e && e.preventDefault) e.preventDefault();

    if (this.startPoint && this.currentPoint) {
      const dx = this.currentPoint.x - this.startPoint.x;
      const dy = this.currentPoint.y - this.startPoint.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Only save if line is at least 6 pixels long
      if (dist >= 6) {
        const color = this.activeTool === "ray" ? "#a855f7" : "#00e5ff";
        this.drawings.push({
          type: this.activeTool,
          start: { x: this.startPoint.x, y: this.startPoint.y },
          end: { x: this.currentPoint.x, y: this.currentPoint.y },
          color: color,
          lineWidth: 2
        });
        this.updateCountBadge();
      }
    }

    this.isDrawing = false;
    this.startPoint = null;
    this.currentPoint = null;
    this.render();
  },

  handleMouseLeave() {
    if (this.isDrawing) {
      this.handleMouseUp();
    }
  },

  render() {
    if (!this.ctx || !this.canvas) return;
    const wrapper = document.getElementById("chartContainerWrapper");
    if (!wrapper) return;
    const w = wrapper.clientWidth;
    const h = wrapper.clientHeight;

    this.ctx.clearRect(0, 0, w, h);

    // Draw saved trendlines & S/R lines
    this.drawings.forEach((d) => {
      this.drawShape(d, w, h);
    });

    // Draw active in-progress preview line
    if (this.isDrawing && this.startPoint && this.currentPoint) {
      const previewShape = {
        type: this.activeTool,
        start: this.startPoint,
        end: this.currentPoint,
        color: this.activeTool === "ray" ? "#a855f7" : "#00e5ff",
        lineWidth: 2,
        isPreview: true
      };
      this.drawShape(previewShape, w, h);
    }
  },

  drawShape(d, w, h) {
    const ctx = this.ctx;
    ctx.save();

    if (d.type === "horizontal") {
      // Horizontal Support/Resistance Level Line
      ctx.beginPath();
      ctx.strokeStyle = d.color;
      ctx.lineWidth = d.lineWidth || 2;
      ctx.setLineDash([6, 4]);
      ctx.moveTo(0, d.y);
      ctx.lineTo(w, d.y);
      ctx.stroke();

      // Label on right price scale
      ctx.setLineDash([]);
      ctx.fillStyle = d.color;
      ctx.font = "bold 10px monospace";
      ctx.textAlign = "left";
      ctx.fillRect(8, d.y - 9, ctx.measureText(d.label).width + 8, 16);
      ctx.fillStyle = "#0c121e";
      ctx.fillText(d.label, 12, d.y + 3);
    } else if (d.type === "trendline") {
      // Trendline Segment
      ctx.beginPath();
      ctx.strokeStyle = d.color;
      ctx.lineWidth = d.lineWidth || 2;
      if (d.isPreview) {
        ctx.setLineDash([4, 4]);
      } else {
        ctx.setLineDash([]);
      }
      ctx.moveTo(d.start.x, d.start.y);
      ctx.lineTo(d.end.x, d.end.y);
      ctx.stroke();

      // Terminal anchor dots
      ctx.setLineDash([]);
      ctx.fillStyle = d.color;
      ctx.beginPath();
      ctx.arc(d.start.x, d.start.y, 4, 0, Math.PI * 2);
      ctx.arc(d.end.x, d.end.y, 4, 0, Math.PI * 2);
      ctx.fill();
    } else if (d.type === "ray") {
      // Extended Ray
      ctx.beginPath();
      ctx.strokeStyle = d.color;
      ctx.lineWidth = d.lineWidth || 2;
      if (d.isPreview) ctx.setLineDash([4, 4]);
      else ctx.setLineDash([]);

      const dx = d.end.x - d.start.x;
      const dy = d.end.y - d.start.y;
      const len = Math.sqrt(dx * dx + dy * dy);
      let targetX = d.end.x;
      let targetY = d.end.y;

      if (len > 0) {
        // Project forward across the canvas
        const factor = Math.max(w, h) / len;
        targetX = d.start.x + dx * factor;
        targetY = d.start.y + dy * factor;
      }

      ctx.moveTo(d.start.x, d.start.y);
      ctx.lineTo(targetX, targetY);
      ctx.stroke();

      // Start anchor
      ctx.setLineDash([]);
      ctx.fillStyle = d.color;
      ctx.beginPath();
      ctx.arc(d.start.x, d.start.y, 4, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.restore();
  },

  updateCountBadge() {
    const badge = document.getElementById("dtDrawingsCount");
    if (badge) badge.innerText = this.drawings.length;
  },

  clear() {
    this.drawings = [];
    this.isDrawing = false;
    this.startPoint = null;
    this.currentPoint = null;
    this.updateCountBadge();
    this.render();
  }
};

function toggleDrawingMode() {
  ChartDrawingEngine.isEnabled = !ChartDrawingEngine.isEnabled;
  const isEnabled = ChartDrawingEngine.isEnabled;

  const canvas = document.getElementById("chartDrawingCanvas");
  if (canvas) {
    canvas.classList.toggle("drawing-active", isEnabled);
  }

  // Synchronize toolbar toggles
  const btnToggle = document.getElementById("dtBtnToggle");
  const toggleText = document.getElementById("dtToggleText");
  const mainBtn = document.getElementById("btnDrawingToolsToggle");
  const tip = document.getElementById("dtHelpTip");

  if (btnToggle) btnToggle.classList.toggle("active", isEnabled);
  if (toggleText) toggleText.innerText = isEnabled ? "Drawing: ON" : "Enable Drawing";
  if (mainBtn) {
    mainBtn.classList.toggle("active", isEnabled);
    mainBtn.style.color = isEnabled ? "var(--cyan)" : "";
    mainBtn.style.background = isEnabled ? "rgba(0, 229, 255, 0.15)" : "";
  }

  if (tip) {
    if (isEnabled) {
      tip.innerHTML = `<b style="color:var(--cyan)">Drawing Active</b>: Click and drag on chart to plot trendlines/levels`;
    } else {
      tip.innerText = `Click 'Enable Drawing' to manually plot trendlines/levels`;
    }
  }

  // Ensure canvas is properly sized and rendered
  ChartDrawingEngine.resizeCanvas();
}

function setDrawingTool(tool) {
  ChartDrawingEngine.activeTool = tool;

  // Automatically enable drawing mode if user picks a tool
  if (!ChartDrawingEngine.isEnabled) {
    toggleDrawingMode();
  }

  const buttons = {
    trendline: document.getElementById("dtBtnTrendline"),
    support: document.getElementById("dtBtnSupport"),
    resistance: document.getElementById("dtBtnResistance"),
    ray: document.getElementById("dtBtnRay")
  };

  Object.keys(buttons).forEach(k => {
    if (buttons[k]) buttons[k].classList.toggle("active", k === tool);
  });

  const tip = document.getElementById("dtHelpTip");
  if (tip) {
    if (tool === "support") tip.innerText = "Click anywhere on the chart to set horizontal Support (Green)";
    else if (tool === "resistance") tip.innerText = "Click anywhere on the chart to set horizontal Resistance (Red)";
    else if (tool === "ray") tip.innerText = "Click start point, drag, and release to project a Ray trendline";
    else tip.innerText = "Click start point, drag to end point, and release to plot Trendline";
  }
}

function clearAllChartDrawings() {
  ChartDrawingEngine.clear();
}

/**
 * Export to PNG: Composite Lightweight Chart canvas + User Drawings overlay canvas + Header info
 * Saves a high-resolution PNG image directly to user's downloads
 */
async function exportChartWithDrawingsToPng() {
  const wrapper = document.getElementById("chartContainerWrapper");
  if (!wrapper) return;

  const btn = document.getElementById("dtBtnExportPng");
  const originalHtml = btn ? btn.innerHTML : "";
  if (btn) {
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Exporting...`;
    btn.disabled = true;
  }

  try {
    const dpr = window.devicePixelRatio || 1;
    const width = wrapper.clientWidth;
    const height = wrapper.clientHeight;

    // Create offscreen export canvas
    const exportCanvas = document.createElement("canvas");
    exportCanvas.width = width * dpr;
    exportCanvas.height = height * dpr;
    const ctx = exportCanvas.getContext("2d");
    ctx.scale(dpr, dpr);

    // 1. Draw solid dark chart background
    ctx.fillStyle = "#0c121e";
    ctx.fillRect(0, 0, width, height);

    // 2. Extract Lightweight Chart canvas / takeScreenshot
    let lwImageDrawn = false;
    if (lwChart && typeof lwChart.takeScreenshot === "function") {
      try {
        const screenshotCanvas = lwChart.takeScreenshot();
        if (screenshotCanvas) {
          ctx.drawImage(screenshotCanvas, 0, 0, width, height);
          lwImageDrawn = true;
        }
      } catch (err) {
        console.warn("lwChart.takeScreenshot failed, falling back to DOM canvas grab:", err);
      }
    }

    if (!lwImageDrawn) {
      // Direct DOM canvas capture of Lightweight Charts internal canvases
      const lwCanvases = wrapper.querySelectorAll("#lightweight_chart_container canvas");
      if (lwCanvases && lwCanvases.length > 0) {
        lwCanvases.forEach(c => {
          try {
            ctx.drawImage(c, 0, 0, width, height);
            lwImageDrawn = true;
          } catch (e) {
            console.warn("Could not draw internal canvas:", e);
          }
        });
      }
    }

    // 3. Composite user drawings from chartDrawingCanvas
    const drawingCanvas = document.getElementById("chartDrawingCanvas");
    if (drawingCanvas) {
      try {
        ctx.drawImage(drawingCanvas, 0, 0, width, height);
      } catch (e) {
        console.warn("Could not composite drawing canvas:", e);
      }
    }

    // 4. Render decorative watermark / stock symbol info badge at top
    const symbol = selectedStock ? selectedStock.symbol : "NSE_EQUITY";
    const priceStr = selectedStock ? `₹${selectedStock.price.toFixed(2)}` : "";
    const changeStr = selectedStock ? `${selectedStock.change >= 0 ? '+' : ''}${selectedStock.change.toFixed(2)} (${selectedStock.changePercent}%)` : "";
    const changeColor = (selectedStock && selectedStock.change >= 0) ? "#00e676" : "#ff3d71";

    ctx.save();
    // Header box background
    ctx.fillStyle = "rgba(12, 18, 30, 0.88)";
    ctx.strokeStyle = "rgba(255, 255, 255, 0.12)";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(14, 12, 340, 52, 6);
    ctx.fill();
    ctx.stroke();

    // Text: Stock Symbol
    ctx.font = "bold 15px sans-serif";
    ctx.fillStyle = "#ffffff";
    ctx.fillText(symbol, 24, 34);

    // Text: Interval & Exchange tag
    ctx.font = "bold 11px sans-serif";
    ctx.fillStyle = "#00e5ff";
    ctx.fillText("1D • NSE", 120, 34);

    // Text: Live Price and Net Change
    ctx.font = "bold 13px monospace";
    ctx.fillStyle = changeColor;
    ctx.fillText(`${priceStr}  ${changeStr}`, 24, 54);

    // Bottom Watermark
    ctx.font = "10px sans-serif";
    ctx.fillStyle = "rgba(255, 255, 255, 0.4)";
    ctx.textAlign = "right";
    const dateStr = new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });
    ctx.fillText(`Historical chart • ${dateStr} IST`, width - 16, height - 12);
    ctx.restore();

    // 5. Convert to Blob & trigger browser download
    exportCanvas.toBlob((blob) => {
      if (!blob) {
        alert("Failed to generate image file.");
        return;
      }
      const url = URL.createObjectURL(blob);
      const downloadLink = document.createElement("a");
      const timestamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
      downloadLink.download = `${symbol}_Analysis_${timestamp}.png`;
      downloadLink.href = url;
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);
      setTimeout(() => URL.revokeObjectURL(url), 2000);

      const tip = document.getElementById("dtHelpTip");
      if (tip) {
        tip.innerHTML = `<span style="color:var(--green); font-weight:700;"><i class="fa-solid fa-check"></i> Chart exported to ${symbol}_Analysis_${timestamp}.png</span>`;
        setTimeout(() => {
          if (tip) tip.innerText = "Click on the chart to start plotting";
        }, 4000);
      }
    }, "image/png");

  } catch (err) {
    console.error("Export to PNG error:", err);
    alert("Export to PNG failed: " + err.message);
  } finally {
    if (btn) {
      btn.innerHTML = originalHtml;
      btn.disabled = false;
    }
  }
}

// -------------------------------------------------------------
// UPSTOX API V3 + SUPABASE EDGE RELAY SERVICE
// -------------------------------------------------------------
const UpstoxSupabaseService = {
 getFunctionUrl(){return SUPABASE_URL + '/functions/v1/upstox-market-data';}
};

function updateDataFeedBadge(text) {
  const badge = document.getElementById("dataFeedBadge");
  if (badge) badge.innerText = text;
}

// -------------------------------------------------------------
// ANALYSIS ENGINE (COMPREHENSIVE INDICATORS CALCULATION)
// -------------------------------------------------------------
const AnalysisEngine = {
  calculateSMA(candles, period = 20) {
    if (!candles || candles.length < period) return [];
    const res = [];
    for (let i = period - 1; i < candles.length; i++) {
      let sum = 0;
      for (let j = 0; j < period; j++) sum += candles[i - j].close;
      res.push({ time: candles[i].time, value: +(sum / period).toFixed(2) });
    }
    return res;
  },

  calculateEMA(candles, period = 20) {
    if (!candles || candles.length < period) return [];
    const k = 2 / (period + 1);
    let sum = 0;
    for (let i = 0; i < period; i++) sum += candles[i].close;
    let prevEma = sum / period;
    const res = [{ time: candles[period - 1].time, value: +prevEma.toFixed(2) }];
    for (let i = period; i < candles.length; i++) {
      const curEma = (candles[i].close * k) + (prevEma * (1 - k));
      res.push({ time: candles[i].time, value: +curEma.toFixed(2) });
      prevEma = curEma;
    }
    return res;
  },

  calculateRSI(candles, period = 14) {
    if (!candles || candles.length <= period) return 50.0;
    let gains = 0, losses = 0;
    for (let i = 1; i <= period; i++) {
      const diff = candles[i].close - candles[i - 1].close;
      if (diff >= 0) gains += diff;
      else losses -= diff;
    }
    let avgGain = gains / period;
    let avgLoss = losses / period;
    for (let i = period + 1; i < candles.length; i++) {
      const diff = candles[i].close - candles[i - 1].close;
      if (diff >= 0) {
        avgGain = (avgGain * (period - 1) + diff) / period;
        avgLoss = (avgLoss * (period - 1)) / period;
      } else {
        avgGain = (avgGain * (period - 1)) / period;
        avgLoss = (avgLoss * (period - 1) - diff) / period;
      }
    }
    if (avgLoss === 0) return avgGain === 0 ? 50.0 : 100.0;
    const rs = avgGain / avgLoss;
    return +(100 - (100 / (1 + rs))).toFixed(1);
  },

  calculateMACD(candles, fast = 12, slow = 26, signal = 9) {
    if (!candles || candles.length < slow + signal) {
      return { macd: 2.5, signal: 1.8, hist: 0.7, status: "Bullish Crossover" };
    }
    const emaFast = this.calculateEMA(candles, fast);
    const emaSlow = this.calculateEMA(candles, slow);

    const macdLine = [];
    const offset = slow - fast;
    for (let i = 0; i < emaSlow.length; i++) {
      macdLine.push({
        time: emaSlow[i].time,
        value: +(emaFast[i + offset].value - emaSlow[i].value).toFixed(2)
      });
    }

    // Signal is EMA of macdLine
    const k = 2 / (signal + 1);
    let sum = 0;
    for (let i = 0; i < signal; i++) sum += macdLine[i].value;
    let prevSig = sum / signal;
    for (let i = signal; i < macdLine.length; i++) {
      prevSig = (macdLine[i].value * k) + (prevSig * (1 - k));
    }
    const lastMacd = macdLine[macdLine.length - 1].value;
    const lastSig = +prevSig.toFixed(2);
    const hist = +(lastMacd - lastSig).toFixed(2);
    return {
      macd: lastMacd,
      signal: lastSig,
      hist: hist,
      status: hist >= 0 ? "Bullish Crossover" : "Bearish Crossover"
    };
  },

  calculateBollingerBands(candles, period = 20, mult = 2) {
    if (!candles || candles.length < period) {
      return { upper: [], lower: [], currentUpper: 0, currentLower: 0, width: "0.0%" };
    }
    const sma = this.calculateSMA(candles, period);
    const upper = [];
    const lower = [];

    for (let i = period - 1; i < candles.length; i++) {
      const mean = sma[i - (period - 1)].value;
      let varianceSum = 0;
      for (let j = 0; j < period; j++) {
        varianceSum += Math.pow(candles[i - j].close - mean, 2);
      }
      const stdDev = Math.sqrt(varianceSum / period);
      upper.push({ time: candles[i].time, value: +(mean + (mult * stdDev)).toFixed(2) });
      lower.push({ time: candles[i].time, value: +(mean - (mult * stdDev)).toFixed(2) });
    }

    const curUp = upper[upper.length - 1].value;
    const curLow = lower[lower.length - 1].value;
    const curMean = sma[sma.length - 1].value;
    const width = +(((curUp - curLow) / curMean) * 100).toFixed(1);

    return { upper, lower, currentUpper: curUp, currentLower: curLow, width: `${width}%` };
  },

  calculateATR(candles, period = 14) {
    if (!candles || candles.length <= period) return 15.0;
    let trSum = 0;
    for (let i = 1; i <= period; i++) {
      const h = candles[i].high;
      const l = candles[i].low;
      const prevC = candles[i - 1].close;
      const tr = Math.max(h - l, Math.abs(h - prevC), Math.abs(l - prevC));
      trSum += tr;
    }
    let atr = trSum / period;
    for (let i = period + 1; i < candles.length; i++) {
      const h = candles[i].high;
      const l = candles[i].low;
      const prevC = candles[i - 1].close;
      const tr = Math.max(h - l, Math.abs(h - prevC), Math.abs(l - prevC));
      atr = (atr * (period - 1) + tr) / period;
    }
    return +atr.toFixed(2);
  },

  calculateVWAP(candles) {
    if (!candles || candles.length === 0) return { line: [], current: 0 };
    let cumVolume = 0;
    let cumVolumePrice = 0;
    const line = [];
    candles.forEach(c => {
      const typicalPrice = (c.high + c.low + c.close) / 3;
      const vol = c.volume || 1000;
      cumVolume += vol;
      cumVolumePrice += (typicalPrice * vol);
      const vwap = +(cumVolumePrice / cumVolume).toFixed(2);
      line.push({ time: c.time, value: vwap });
    });
    return { line, current: line[line.length - 1].value };
  },

  calculateSupportResistance(candles) {
    if (!candles || candles.length < 10) {
      return { s1: 0, s2: 0, r1: 0, r2: 0 };
    }
    const recent = candles.slice(-20);
    const highs = recent.map(c => c.high);
    const lows = recent.map(c => c.low);
    const highest = Math.max(...highs);
    const lowest = Math.min(...lows);
    const latest = candles[candles.length - 1];

    const pivot = (highest + lowest + latest.close) / 3;
    const r1 = +(2 * pivot - lowest).toFixed(2);
    const s1 = +(2 * pivot - highest).toFixed(2);
    const r2 = +(pivot + (highest - lowest)).toFixed(2);
    const s2 = +(pivot - (highest - lowest)).toFixed(2);

    return { s1, s2, r1, r2, highest, lowest };
  },

  detectPriceActionPatterns(candles) {
    if (!candles || candles.length < 3) return "No supported candle pattern detected";
    const last = candles[candles.length - 1];
    const prev = candles[candles.length - 2];

    const body = Math.abs(last.close - last.open);
    const upperWick = last.high - Math.max(last.open, last.close);
    const lowerWick = Math.min(last.open, last.close) - last.low;

    if (body > 0 && lowerWick > body * 2 && upperWick < body * 0.5) {
      return "Bullish hammer";
    }
    if (last.close > last.open && prev.close < prev.open && last.close > prev.open && last.open < prev.close) {
      return "Bullish engulfing";
    }
    if (body <= (last.high - last.low) * 0.1) {
      return "Doji";
    }
    if (last.close > last.open && body > (last.high - last.low) * 0.85) {
      return "Bullish marubozu";
    }
    return "No supported candle pattern detected";
  }
};

// -------------------------------------------------------------
// MAIN STOCK CHART LOADER & AI ANALYSIS RENDERER
// -------------------------------------------------------------
async function loadStockChartAndAnalysis(stock, snapshot = null) {
  if (!snapshot) { onMarketSelection(); return; }
  if (!currentUser || !hasActiveAccess()) return;

  if (!lwChart) {
    initLightweightChart();
  }

  // Update Stock header elements
  document.getElementById("legendSymbol").innerText = stock.symbol;
  document.getElementById("legendInterval").innerText = currentTimeframe;

  // Fetch Candles via Upstox / Supabase Service
  const requestedTimeframe = currentTimeframe;
  const candles = snapshot.candles;
  applyQuote(stock, snapshot);
  if (selectedStock?.symbol !== stock.symbol || currentTimeframe !== requestedTimeframe) return;
  currentCandles = candles;
  if (!candles.length) {
    if (candleSeries) candleSeries.setData([]);
    if (volumeSeries) volumeSeries.setData([]);
    for (const series of [emaSeries, smaSeries, vwapSeries, bbUpperSeries, bbLowerSeries]) {
      if (series) series.setData([]);
    }
    for (const line of activeSrLines) { if (candleSeries) candleSeries.removePriceLine(line); }
    activeSrLines = [];
    document.querySelectorAll('[id^="ind"]').forEach(node => {
      if (!node.children.length) node.textContent = 'Unavailable';
    });
    stopLivePriceStream();
    return;
  }

  if (!currentCandles || currentCandles.length === 0) return;

  // 1. Update Candlestick Series
  if (candleSeries) {
    candleSeries.setData(currentCandles.map(c => ({
      time: c.time,
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close
    })));
  }

  // 2. Update Volume Series
  if (volumeSeries && activeIndicators.volume) {
    volumeSeries.setData(currentCandles.map(c => ({
      time: c.time,
      value: c.volume,
      color: c.close >= c.open ? "rgba(0, 230, 118, 0.4)" : "rgba(255, 61, 113, 0.4)"
    })));
  }

  if (currentCandles.length < 50) {
    document.querySelectorAll('[id^="ind"],[id^="aiBlock"]').forEach(node => { if (!node.children.length) node.textContent = 'At least 50 candles required for analysis'; });
    if(lwChart)lwChart.timeScale().fitContent();
    return;
  }
  // 3. Run Analysis Engine
  const ema20 = AnalysisEngine.calculateEMA(currentCandles, 20);
  const sma50 = AnalysisEngine.calculateSMA(currentCandles, 50);
  const rsi = AnalysisEngine.calculateRSI(currentCandles, 14);
  const macd = AnalysisEngine.calculateMACD(currentCandles);
  const bb = AnalysisEngine.calculateBollingerBands(currentCandles, 20, 2);
  const atr = AnalysisEngine.calculateATR(currentCandles, 14);
  const vwap = AnalysisEngine.calculateVWAP(currentCandles);
  const sr = AnalysisEngine.calculateSupportResistance(currentCandles);
  const pattern = AnalysisEngine.detectPriceActionPatterns(currentCandles);

  // 4. Update Indicator Overlays on TradingView Chart
  if (emaSeries && activeIndicators.ema) {
    emaSeries.setData(ema20);
  }
  if (smaSeries && activeIndicators.sma) {
    smaSeries.setData(sma50);
  }
  if (vwapSeries && activeIndicators.vwap) {
    vwapSeries.setData(vwap.line);
  }
  if (bbUpperSeries && bbLowerSeries && activeIndicators.bb) {
    bbUpperSeries.setData(bb.upper);
    bbLowerSeries.setData(bb.lower);
  }

  // Clear and Recreate Support/Resistance Horizontal Price Lines
  activeSrLines.forEach(line => {
    try { candleSeries.removePriceLine(line); } catch (e) {}
  });
  activeSrLines = [];

  if (activeIndicators.sr && candleSeries) {
    const sLine = candleSeries.createPriceLine({
      price: sr.s1,
      color: "#00e676",
      lineWidth: 1,
      lineStyle: LightweightCharts.LineStyle.Dotted,
      axisLabelVisible: true,
      title: "S1"
    });
    const rLine = candleSeries.createPriceLine({
      price: sr.r1,
      color: "#ff3d71",
      lineWidth: 1,
      lineStyle: LightweightCharts.LineStyle.Dotted,
      axisLabelVisible: true,
      title: "R1"
    });
    activeSrLines.push(sLine, rLine);
  }

  // Fit Content
  if (lwChart) {
    lwChart.timeScale().fitContent();
  }

  // 5. Update Technical Indicators Dashboard Cards
  const lastCandle = currentCandles[currentCandles.length - 1];
  const curPrice = lastCandle.close;

  // RSI
  const rsiElem = document.getElementById("indRsi");
  if (rsiElem) rsiElem.innerText = rsi.toFixed(1);
  const rsiSub = document.getElementById("indRsiSub");
  if (rsiSub) {
    if (rsi > 70) { rsiSub.innerText = "Overbought (>70)"; rsiSub.style.color = "var(--red)"; }
    else if (rsi < 30) { rsiSub.innerText = "Oversold (<30)"; rsiSub.style.color = "var(--green)"; }
    else { rsiSub.innerText = "Bullish Momentum"; rsiSub.style.color = "var(--cyan)"; }
  }

  // EMA 20
  const curEmaVal = ema20.length > 0 ? ema20[ema20.length - 1].value : curPrice;
  const emaElem = document.getElementById("indEma");
  if (emaElem) emaElem.innerText = `₹${curEmaVal.toFixed(2)}`;
  const emaSub = document.getElementById("indEmaSub");
  if (emaSub) {
    const above = curPrice >= curEmaVal;
    emaSub.innerText = above ? "Above 20 EMA" : "Below 20 EMA";
    emaSub.style.color = above ? "var(--green)" : "var(--red)";
  }

  // SMA 50
  const curSmaVal = sma50.length > 0 ? sma50[sma50.length - 1].value : curPrice * 0.98;
  const smaElem = document.getElementById("indSma");
  if (smaElem) smaElem.innerText = `₹${curSmaVal.toFixed(2)}`;
  const smaSub = document.getElementById("indSmaSub");
  if (smaSub) {
    const aboveSma = curPrice >= curSmaVal;
    smaSub.innerText = aboveSma ? "Bullish Baseline" : "Under SMA 50";
    smaSub.style.color = aboveSma ? "var(--green)" : "var(--amber)";
  }

  // MACD
  const macdElem = document.getElementById("indMacd");
  if (macdElem) macdElem.innerText = `${macd.hist >= 0 ? '+' : ''}${macd.hist}`;
  const macdSub = document.getElementById("indMacdSub");
  if (macdSub) macdSub.innerText = macd.status;

  // Bollinger Bands
  const bbElem = document.getElementById("indBb");
  if (bbElem) bbElem.innerText = `₹${bb.currentUpper.toFixed(0)} / ₹${bb.currentLower.toFixed(0)}`;
  const bbSub = document.getElementById("indBbSub");
  if (bbSub) bbSub.innerText = `Band Width ${bb.width}`;

  // VWAP
  const vwapElem = document.getElementById("indVwap");
  if (vwapElem) vwapElem.innerText = `₹${vwap.current.toFixed(2)}`;
  const vwapSub = document.getElementById("indVwapSub");
  if (vwapSub) {
    const aboveVwap = curPrice >= vwap.current;
    vwapSub.innerText = aboveVwap ? "Above VWAP (+)" : "Below VWAP (-)";
    vwapSub.style.color = aboveVwap ? "var(--green)" : "var(--red)";
  }

  // ATR
  const atrElem = document.getElementById("indAtr");
  if (atrElem) atrElem.innerText = `₹${atr.toFixed(2)}`;

  // Volume Ratio
  const recentVols = currentCandles.slice(-20).map(c => c.volume);
  const avgVol = recentVols.reduce((a, b) => a + b, 0) / recentVols.length;
  const volRatio = +(lastCandle.volume / avgVol).toFixed(2);
  const volRatioElem = document.getElementById("indVolRatio");
  if (volRatioElem) volRatioElem.innerText = `${volRatio}x`;
  const volSub = document.getElementById("indVolSub");
  if (volSub) volSub.innerText = volRatio > 1.2 ? "Spike Volume" : "Normal Flow";

  // Support / Resistance
  const srElem = document.getElementById("indSrLevels");
  if (srElem) srElem.innerText = `S: ₹${sr.s1.toFixed(0)} | R: ₹${sr.r1.toFixed(0)}`;

  // 6. Update Future AI Structured Analysis Blocks (Strictly No Certainty/Guaranteed Language)
  renderStructuredAiAnalysis(stock, curPrice, curEmaVal, curSmaVal, rsi, macd, sr, pattern, atr);
}

function renderStructuredAiAnalysis(stock, price, ema, sma, rsi, macd, sr, pattern, atr) {
 report('aiBlockTrend', stock.symbol+' · '+currentTimeframe+' candles: close '+money(price)+', EMA20 '+money(ema)+', SMA50 '+money(sma)+'.');
 report('aiBlockMomentum', 'RSI14: '+rsi.toFixed(1)+'. MACD histogram: '+macd.hist+'.');
 report('aiBlockLevels', 'Calculated support '+money(sr.s1)+' / resistance '+money(sr.r1)+'.');
 report('aiBlockPattern', pattern);
 report('aiBlockRisk', 'ATR14: '+money(atr)+'. Indicator values are derived from the selected candles, not guaranteed future prices.');
 report('aiBlockSummary', 'Rule-based historical analysis. Quote and candle timestamps may differ; no buy/sell recommendation is generated.');
}

// -------------------------------------------------------------
// TIMEFRAME & INDICATOR TOGGLE CONTROLS
// -------------------------------------------------------------
function switchChartMode() { currentChartMode='custom'; onMarketSelection(); }

function switchTimeframe(tf) {
  if (!currentUser || !hasActiveAccess()) return;
  currentTimeframe = tf;

  document.querySelectorAll(".timeframe-tabs .tab-btn").forEach(btn => {
    btn.classList.toggle("active", btn.id === `tf_${tf}`);
  });

  if (selectedStock) {
    loadStockChartAndAnalysis(selectedStock);
  }
}

function toggleIndicator(ind) {
  activeIndicators[ind] = !activeIndicators[ind];
  const pill = document.getElementById(`pill_${ind}`);
  if (pill) {
    if (ind === "sma") pill.classList.toggle("active-sma", activeIndicators[ind]);
    else if (ind === "bb") pill.classList.toggle("active-bb", activeIndicators[ind]);
    else if (ind === "vwap") pill.classList.toggle("active-vwap", activeIndicators[ind]);
    else if (ind === "sr") pill.classList.toggle("active-sr", activeIndicators[ind]);
    else pill.classList.toggle("active", activeIndicators[ind]);
  }

  // Update chart series visibility
  if (ind === "ema" && emaSeries) emaSeries.applyOptions({ visible: activeIndicators.ema });
  if (ind === "sma" && smaSeries) smaSeries.applyOptions({ visible: activeIndicators.sma });
  if (ind === "vwap" && vwapSeries) vwapSeries.applyOptions({ visible: activeIndicators.vwap });
  if (ind === "bb" && bbUpperSeries && bbLowerSeries) {
    bbUpperSeries.applyOptions({ visible: activeIndicators.bb });
    bbLowerSeries.applyOptions({ visible: activeIndicators.bb });
  }
  if (ind === "volume" && volumeSeries) volumeSeries.applyOptions({ visible: activeIndicators.volume });
  if (ind === "sr") {
    activeSrLines.forEach(l => {
      try { candleSeries.removePriceLine(l); } catch (e) {}
    });
    activeSrLines = [];
    if (activeIndicators.sr && currentCandles.length > 0) {
      const sr = AnalysisEngine.calculateSupportResistance(currentCandles);
      const sLine = candleSeries.createPriceLine({
        price: sr.s1, color: "#00e676", lineWidth: 1, lineStyle: LightweightCharts.LineStyle.Dotted, axisLabelVisible: true, title: "S1"
      });
      const rLine = candleSeries.createPriceLine({
        price: sr.r1, color: "#ff3d71", lineWidth: 1, lineStyle: LightweightCharts.LineStyle.Dotted, axisLabelVisible: true, title: "R1"
      });
      activeSrLines.push(sLine, rLine);
    }
  }
}

// -------------------------------------------------------------
// REAL-TIME UPSTOX V3 WEBSOCKET & 60FPS LIVE PRICE STREAM
// -------------------------------------------------------------
let latencyMetrics = {
  totalTicks: 0,
  lastTickTime: null,
  ticksThisSecond: 0,
  tps: 0,
  lastTotalLatency: null,
  lastServerTransit: null,
  lastLocalProcessing: null
};

// Periodic TPS (ticks per second) counter reset
setInterval(() => {
  latencyMetrics.tps = latencyMetrics.ticksThisSecond;
  latencyMetrics.ticksThisSecond = 0;
}, 1000);

function toggleLatencyDebugPanel() {
  const panel = document.getElementById("wsLatencyDebugPanel");
  const grid = document.getElementById("debugLatencyGridBody");
  const icon = document.getElementById("btnToggleDebugIcon");
  if (!panel) return;

  if (grid.style.display === "none") {
    grid.style.display = "grid";
    if (icon) icon.className = "fa-solid fa-chevron-up";
  } else {
    grid.style.display = "none";
    if (icon) icon.className = "fa-solid fa-chevron-down";
  }
}

function updateLatencyDebugUI(transitMs, localProcMs, tickTimestampMs, ltp, feedType = "WS") {
  const totalMs = (transitMs !== null && !isNaN(transitMs)) ? (transitMs + localProcMs) : localProcMs;

  latencyMetrics.totalTicks++;
  latencyMetrics.ticksThisSecond++;
  latencyMetrics.lastTotalLatency = totalMs;
  latencyMetrics.lastServerTransit = transitMs;
  latencyMetrics.lastLocalProcessing = localProcMs;

  const totalElem = document.getElementById("dbgTotalLatency");
  const verdictElem = document.getElementById("dbgLatencyVerdit");
  const transitElem = document.getElementById("dbgServerTransit");
  const serverSubElem = document.getElementById("dbgServerSub");
  const localElem = document.getElementById("dbgLocalProcess");
  const tickTimeElem = document.getElementById("dbgTickTimestamp");
  const tickOriginElem = document.getElementById("dbgTickOrigin");
  const clientTimeElem = document.getElementById("dbgClientTime");
  const tickValElem = document.getElementById("dbgLastTickVal");
  const ticksCountElem = document.getElementById("dbgTicksReceived");
  const modeBadge = document.getElementById("debugFeedModeBadge");

  const now = new Date();
  const clientTimeStr = now.toTimeString().split(' ')[0] + '.' + String(now.getMilliseconds()).padStart(3, '0');

  if (clientTimeElem) clientTimeElem.innerText = clientTimeStr;

  if (totalElem) {
    totalElem.innerText = `${Math.round(totalMs)} ms`;
    totalElem.className = "debug-stat-val " + (totalMs < 120 ? "latency-good" : (totalMs < 400 ? "latency-warn" : "latency-bad"));
  }

  if (verdictElem) {
    if (transitMs === null || isNaN(transitMs)) {
      verdictElem.innerHTML = `<span style="color:var(--cyan)">Local tick loop active (&lt;${localProcMs.toFixed(1)}ms)</span>`;
    } else if (transitMs > 350) {
      verdictElem.innerHTML = `<span style="color:var(--red)"><i class="fa-solid fa-cloud-arrow-down"></i> Delay is Server/Network side (${Math.round(transitMs)}ms)</span>`;
    } else if (localProcMs > 25) {
      verdictElem.innerHTML = `<span style="color:var(--amber)"><i class="fa-solid fa-desktop"></i> Delay is Local DOM/Render side (${localProcMs.toFixed(1)}ms)</span>`;
    } else {
      verdictElem.innerHTML = `<span style="color:var(--green)"><i class="fa-solid fa-check"></i> Real-Time Synced (Ultra Low Latency)</span>`;
    }
  }

  if (transitElem) {
    if (transitMs !== null && !isNaN(transitMs)) {
      transitElem.innerText = `${Math.round(transitMs)} ms`;
      transitElem.className = "debug-stat-val " + (transitMs < 100 ? "latency-good" : (transitMs < 300 ? "latency-warn" : "latency-bad"));
    } else {
      transitElem.innerText = `0 ms`;
      transitElem.className = "debug-stat-val latency-good";
    }
  }

  if (serverSubElem) {
    serverSubElem.innerText = (transitMs !== null && !isNaN(transitMs))
      ? (transitMs > 300 ? "High transit lag from broker" : "Broker network transit time")
      : "Direct client-side stream";
  }

  if (localElem) {
    localElem.innerText = `${localProcMs.toFixed(1)} ms`;
    localElem.className = "debug-stat-val " + (localProcMs < 10 ? "latency-good" : (localProcMs < 30 ? "latency-warn" : "latency-bad"));
  }

  if (tickTimeElem) {
    if (tickTimestampMs) {
      const tickDate = new Date(tickTimestampMs);
      tickTimeElem.innerText = tickDate.toTimeString().split(' ')[0] + '.' + String(tickDate.getMilliseconds()).padStart(3, '0');
    } else {
      tickTimeElem.innerText = clientTimeStr;
    }
  }

  if (tickOriginElem) {
    tickOriginElem.innerText = feedType === "WS" ? "Upstox V3 WS packet timestamp" : "High-Frequency Companion Loop";
  }

  if (tickValElem) {
    tickValElem.innerText = `₹${ltp.toFixed(2)} (${latencyMetrics.tps} tps)`;
  }

  if (ticksCountElem) {
    ticksCountElem.innerText = `${latencyMetrics.totalTicks} ticks profiled (${feedType})`;
  }

  if (modeBadge) {
    if (feedType === "WS") {
      modeBadge.innerHTML = `<i class="fa-solid fa-satellite-dish" style="font-size: 7px; margin-right: 4px;"></i> WS LIVE STREAM`;
      modeBadge.style.color = "var(--green)";
      modeBadge.style.borderColor = "rgba(0, 230, 118, 0.3)";
    } else {
      modeBadge.innerHTML = `<i class="fa-solid fa-bolt" style="font-size: 7px; margin-right: 4px;"></i> ENGINE STREAM ACTIVE`;
      modeBadge.style.color = "var(--cyan)";
      modeBadge.style.borderColor = "rgba(0, 229, 255, 0.3)";
    }
  }
}

function handleUpstoxWsMessage(data) {
  if (!data || !selectedStock) return;
  const currentKey = UPSTOX_INSTRUMENT_KEYS[selectedStock.symbol] || `NSE_EQ|${selectedStock.symbol}`;

  // Capture precise WebSocket message arrival timestamp
  const wsArrivalTime = performance.now();
  const clientEpochMs = Date.now();

  // Upstox V3 WebSocket feeds return tick packets with feeds object keyed by instrument key
  let feed = null;
  if (data.feeds && data.feeds[currentKey]) {
    feed = data.feeds[currentKey];
  } else if (data[currentKey]) {
    feed = data[currentKey];
  } else if (data.data && data.data[currentKey]) {
    feed = data.data[currentKey];
  } else if (data.ltp !== undefined || data.price !== undefined) {
    feed = data;
  }

  if (feed) {
    let tickPrice = null;
    let tickVol = 0;
    let packetTimestamp = null;

    // Check various Upstox V3 JSON payload structures (Full mode vs LTP mode)
    if (feed.fullFeed && feed.fullFeed.marketFF && feed.fullFeed.marketFF.ltpc) {
      tickPrice = feed.fullFeed.marketFF.ltpc.ltp;
      tickVol = feed.fullFeed.marketFF.v || 0;
      packetTimestamp = feed.fullFeed.marketFF.ltpc.ltt || feed.fullFeed.marketFF.ltt;
    } else if (feed.ltpc && feed.ltpc.ltp !== undefined) {
      tickPrice = feed.ltpc.ltp;
      packetTimestamp = feed.ltpc.ltt || feed.ltt;
    } else if (feed.ltp !== undefined) {
      tickPrice = feed.ltp;
      packetTimestamp = feed.ltt || feed.timestamp;
    } else if (feed.price !== undefined) {
      tickPrice = feed.price;
      packetTimestamp = feed.timestamp;
    }

    if (!packetTimestamp && data.timestamp) {
      packetTimestamp = data.timestamp;
    }

    // Convert packet timestamp to milliseconds if in seconds
    let serverTransitMs = null;
    if (packetTimestamp) {
      const tsNum = typeof packetTimestamp === 'string' ? parseInt(packetTimestamp, 10) : packetTimestamp;
      const tickEpochMs = tsNum < 1e11 ? tsNum * 1000 : tsNum;
      serverTransitMs = Math.max(0, clientEpochMs - tickEpochMs);
    }

    if (tickPrice !== null && !isNaN(tickPrice) && tickPrice > 0) {
      const parsedPrice = parseFloat(tickPrice);
      // Validation against last known reliable REST price snapshot to reject corrupted/outlier ticks (> 20% divergence)
      const benchmarkPrice = lastValidatedPrice || selectedStock.price;
      if (benchmarkPrice > 0) {
        const divergenceRatio = Math.abs(parsedPrice - benchmarkPrice) / benchmarkPrice;
        if (divergenceRatio > 0.20) {
          console.warn(`[Tick Validation] Discarding anomalous WS tick for ${selectedStock.symbol}: ₹${parsedPrice} (Benchmark: ₹${benchmarkPrice})`);
          return;
        }
      }

      // Execute render and measure local execution duration
      const renderStart = performance.now();
      applyPriceTick(parsedPrice, tickVol);
      const localProcDuration = performance.now() - renderStart;

      // Update dedicated latency debug UI panel
      updateLatencyDebugUI(serverTransitMs, localProcDuration, packetTimestamp, parsedPrice, "WS");
    }
  }
}

// -------------------------------------------------------------
// SUPABASE & UPSTOX BRIDGE MODAL CONTROLLERS
// -------------------------------------------------------------
function openSupabaseModal() {
  const modal = document.getElementById("supabaseModal");
  if (modal) modal.classList.remove("hidden");
  const urlInput = document.getElementById("supabaseFunctionUrl");
  const keyInput = document.getElementById("supabaseAnonKey");
  if (urlInput) { urlInput.value = UpstoxSupabaseService.getFunctionUrl(); urlInput.readOnly = true; }
  if (keyInput) { keyInput.value = SUPABASE_ANON; keyInput.readOnly = true; }
}

function closeSupabaseModal() {
  const modal = document.getElementById("supabaseModal");
  if (modal) modal.classList.add("hidden");
}

function saveSupabaseSettings() { closeSupabaseModal(); }

async function testSupabaseConnection() {
  const statusElem = document.getElementById("bridgeStatusText");
  const urlInput = document.getElementById("supabaseFunctionUrl");
  const url = urlInput ? urlInput.value.trim() : "";

  if (!url) {
    if (statusElem) {
      statusElem.innerHTML = `<span style="color:var(--amber);">Supabase is not configured. Set the project URL and public key in config.js.</span>`;
    }
    return;
  }

  if (statusElem) {
    statusElem.innerText = "Connecting to Supabase Edge Function...";
  }

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json", "apikey": SUPABASE_ANON, "Authorization": `Bearer ${await sessionToken()}` },
      body: JSON.stringify({ action: "ping", symbol: "RELIANCE" })
    });
    if (res.ok) {
      const data = await res.json();
      if (statusElem) {
        statusElem.innerHTML = `<span style="color:var(--green); font-weight:700;"><i class="fa-solid fa-circle-check"></i> Connection successful</span> Source: ${escapeHtml(data.source || 'Upstox V3 Edge Function')} — ${data.configured ? 'Upstox token configured' : 'Upstox token missing'}`;
      }
    } else {
      if (statusElem) {
        statusElem.innerHTML = `<span style="color:var(--red);">HTTP Error ${res.status}: Check the Edge Function configuration.</span>`;
      }
    }
  } catch (err) {
    if (statusElem) {
      statusElem.innerHTML = `<span style="color:var(--red);"><i class="fa-solid fa-circle-xmark"></i> Connection failed: ${err.message}</span>`;
    }
  }
}

// -------------------------------------------------------------
// PRICE ALERTS & NON-INTRUSIVE TOAST NOTIFICATIONS
// -------------------------------------------------------------
function openSetAlertModal() {
  if (!selectedStock) return;
  const modal = document.getElementById("setAlertModal");
  if (!modal) return;

  const symbolElem = document.getElementById("alertStockSymbol");
  const nameElem = document.getElementById("alertStockName");
  const priceElem = document.getElementById("alertCurrentPrice");
  const thresholdInput = document.getElementById("alertThresholdPrice");

  if (symbolElem) symbolElem.innerText = selectedStock.symbol;
  if (nameElem) nameElem.innerText = selectedStock.name;
  if (priceElem) priceElem.innerText = `₹${selectedStock.price.toFixed(2)}`;
  if (thresholdInput) {
    thresholdInput.value = selectedStock.price.toFixed(2);
    thresholdInput.focus();
  }

  renderActiveAlertsList();
  modal.classList.remove("hidden");
}

function closeSetAlertModal() {
  const modal = document.getElementById("setAlertModal");
  if (modal) modal.classList.add("hidden");
}

function saveStockAlert() {
  if (!selectedStock) return;
  const condition = document.getElementById("alertCondition").value;
  const thresholdInput = document.getElementById("alertThresholdPrice");
  const threshold = parseFloat(thresholdInput.value);

  if (isNaN(threshold) || threshold <= 0) {
    showToast("Invalid Price", "Enter a valid price.", "red");
    return;
  }

  const newAlert = {
    id: "alert_" + Date.now() + "_" + Math.random().toString(36).substr(2, 5),
    symbol: selectedStock.symbol,
    condition: condition, // 'gte' or 'lte'
    threshold: threshold,
    createdAt: new Date().toISOString(),
    createdPrice: selectedStock.price
  };

  priceAlerts.push(newAlert);
  localStorage.setItem("stock_price_alerts", JSON.stringify(priceAlerts));

  renderActiveAlertsList();
  showToast(
    `Alert Set: ${selectedStock.symbol}`,
    `An alert will appear when the price reaches ₹${threshold.toFixed(2)} (${condition === 'gte' ? '≥' : '≤'}).`,
    "green"
  );
  closeSetAlertModal();
}

function removeStockAlert(alertId) {
  priceAlerts = priceAlerts.filter(a => a.id !== alertId);
  localStorage.setItem("stock_price_alerts", JSON.stringify(priceAlerts));
  renderActiveAlertsList();
  showToast("Alert Removed", "Alert removed.", "red");
}

function renderActiveAlertsList() {
  const listContainer = document.getElementById("activeAlertsList");
  if (!listContainer || !selectedStock) return;

  const currentStockAlerts = priceAlerts.filter(a => a.symbol === selectedStock.symbol);
  if (currentStockAlerts.length === 0) {
    listContainer.innerHTML = `<div style="font-size: 11px; color: var(--text-muted); padding: 4px 0;">No active alerts for this stock.</div>`;
    return;
  }

  listContainer.innerHTML = currentStockAlerts.map(a => `
    <div style="display: flex; justify-content: space-between; align-items: center; background: var(--card); padding: 6px 10px; border-radius: 6px; font-size: 11px; border: 1px solid var(--border);">
      <div>
        <span style="font-weight: 700; color: var(--cyan);">${a.symbol}</span>
        <span style="color: var(--text-muted); margin: 0 4px;">${a.condition === 'gte' ? '≥' : '≤'}</span>
        <span style="font-weight: 700; color: ${a.condition === 'gte' ? 'var(--green)' : 'var(--red)'};">₹${a.threshold.toFixed(2)}</span>
      </div>
      <button class="btn-sm btn-red" style="padding: 2px 6px; font-size: 10px;" onclick="removeStockAlert('${a.id}')">
        <i class="fa-solid fa-trash-can"></i>
      </button>
    </div>
  `).join("");
}

function checkPriceAlerts(symbol, currentPrice, prevPrice) {
  if (!priceAlerts || priceAlerts.length === 0) return;

  const matchedAlerts = [];
  const remainingAlerts = [];

  priceAlerts.forEach(alert => {
    if (alert.symbol !== symbol) {
      remainingAlerts.push(alert);
      return;
    }

    let triggered = false;
    if (alert.condition === "gte" && currentPrice >= alert.threshold) {
      triggered = true;
    } else if (alert.condition === "lte" && currentPrice <= alert.threshold) {
      triggered = true;
    }

    if (triggered) {
      matchedAlerts.push(alert);
    } else {
      remainingAlerts.push(alert);
    }
  });

  if (matchedAlerts.length > 0) {
    priceAlerts = remainingAlerts;
    localStorage.setItem("stock_price_alerts", JSON.stringify(priceAlerts));

    matchedAlerts.forEach(a => {
      const isAbove = a.condition === "gte";
      const icon = isAbove ? "fa-arrow-trend-up" : "fa-arrow-trend-down";
      const colorType = isAbove ? "green" : "red";
      showToast(
        `🚨 Price Alert: ${a.symbol}`,
        `Current price ₹${currentPrice.toFixed(2)} crossed target ₹${a.threshold.toFixed(2)} (${isAbove ? '≥' : '≤'})!`,
        colorType,
        true
      );
    });

    // Refresh modal list if open
    const modal = document.getElementById("setAlertModal");
    if (modal && !modal.classList.contains("hidden")) {
      renderActiveAlertsList();
    }
  }
}

// Subtle Audio Ping Effect for Real-Time Price Alerts
function playAlertPingSound(freq = 880, duration = 0.22) {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    const ctx = new AudioContextClass();

    // Smooth dual-tone chime: First tone 880Hz (A5), resolving to 1320Hz (E6)
    const now = ctx.currentTime;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = "sine";
    osc.frequency.setValueAtTime(freq, now);
    osc.frequency.exponentialRampToValueAtTime(freq * 1.5, now + 0.08);

    gain.gain.setValueAtTime(0.001, now);
    gain.gain.linearRampToValueAtTime(0.12, now + 0.02); // subtle non-intrusive volume
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(now);
    osc.stop(now + duration + 0.05);

    // Auto close audio context after playing
    setTimeout(() => {
      try { ctx.close(); } catch (e) {}
    }, (duration + 0.2) * 1000);
  } catch (err) {
    console.debug("AudioContext ping omitted:", err);
  }
}

// Non-intrusive Toast Notification Handler
function showToast(title, message, type = "cyan", playSound = false) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  // Play subtle ping audio when requested or on alert hit
  if (playSound || title.includes("Price Alert")) {
    playAlertPingSound(type === "green" ? 987.77 : type === "red" ? 740 : 880);
  }

  const toast = document.createElement("div");
  toast.className = `toast-alert ${type === "green" ? "green" : type === "red" ? "red" : ""}`;

  let iconHtml = '<i class="fa-solid fa-bell" style="color:var(--cyan); font-size:16px; margin-top:2px;"></i>';
  if (type === "green") {
    iconHtml = '<i class="fa-solid fa-circle-check" style="color:var(--green); font-size:16px; margin-top:2px;"></i>';
  } else if (type === "red") {
    iconHtml = '<i class="fa-solid fa-triangle-exclamation" style="color:var(--red); font-size:16px; margin-top:2px;"></i>';
  }

  toast.innerHTML = `
    ${iconHtml}
    <div style="flex: 1;">
      <div style="font-weight: 700; font-size: 13px; margin-bottom: 2px; color: var(--text);">${title}</div>
      <div style="font-size: 12px; color: var(--text-muted); line-height: 1.4;">${message}</div>
    </div>
    <button style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 2px 4px; font-size: 12px;" onclick="this.parentElement.remove()">
      <i class="fa-solid fa-xmark"></i>
    </button>
  `;

  container.appendChild(toast);

  // Auto-dismiss after 5.5 seconds with fade-out
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px) scale(0.95)";
    setTimeout(() => {
      if (toast.parentElement) toast.remove();
    }, 320);
  }, 5500);
}

function destroyActiveChart() {
  if (lwChart) {
    try {
      lwChart.remove();
    } catch (e) {
      console.warn("lwChart remove:", e);
    }
    lwChart = null;
  }
  const container = document.getElementById("lightweight_chart_container");
  if (container) container.innerHTML = "";
}

async function logout() {
  if (authClient) await authClient.auth.signOut({ scope: "local" });
  stopLivePriceStream();
  destroyActiveChart();
  localStorage.removeItem("app_user");
  currentUser = null;
  tradeRows = []; tradeOwner = null;
  document.getElementById("accessPanel").classList.add("hidden");
  document.getElementById("tradeTableBody").innerHTML = "";
  document.getElementById("holdingsBody").innerHTML = "";
  document.getElementById("userHeader").classList.add("hidden");
  showView("auth");
}

function showView(view) {
  document.getElementById("authGate").classList.toggle("hidden", view !== "auth");
  document.getElementById("pendingGate").classList.toggle("hidden", view !== "pending");
  document.getElementById("appDashboard").classList.toggle("hidden", view !== "dashboard");
}
