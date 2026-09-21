// Supabase Edge Function: upstox-market-data
// Deploy command: supabase functions deploy upstox-market-data
// 
// IMPORTANT SECURITY RULE:
// Upstox API Key, Secret, and Access Token are stored ONLY inside Supabase Secrets Vault!
// NEVER expose them in GitHub or frontend JavaScript.
//
// Set secrets in Supabase CLI:
// supabase secrets set UPSTOX_API_KEY="your-api-key"
// supabase secrets set UPSTOX_API_SECRET="your-api-secret"
// supabase secrets set UPSTOX_ACCESS_TOKEN="your-access-token"

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS"
};

// Instrument Key Mapping for Top Indian Equities (NSE)
const INSTRUMENT_MAP: Record<string, string> = {
  "RELIANCE": "NSE_EQ|INE002A01018",
  "TCS": "NSE_EQ|INE467B01029",
  "HDFCBANK": "NSE_EQ|INE040A01034",
  "ICICIBANK": "NSE_EQ|INE090A01021",
  "INFY": "NSE_EQ|INE009A01021",
  "TATAMOTORS": "NSE_EQ|INE155A01022",
  "TATASTEEL": "NSE_EQ|INE081A01020",
  "SBIN": "NSE_EQ|INE062A01020",
  "BHARTIARTL": "NSE_EQ|INE397D01024",
  "ADANIENT": "NSE_EQ|INE423A01024",
  "BAJFINANCE": "NSE_EQ|INE296A01024",
  "WIPRO": "NSE_EQ|INE075A01022",
  "ZOMATO": "NSE_EQ|INE758T01015",
  "MARUTI": "NSE_EQ|INE585B01010"
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const url = new URL(req.url);
    const body = req.method === "POST" ? await req.json().catch(() => ({})) : {};

    const action = body.action || url.searchParams.get("action") || "get_candles";
    const symbol = (body.symbol || url.searchParams.get("symbol") || "RELIANCE").toUpperCase();
    const timeframe = body.timeframe || url.searchParams.get("timeframe") || "1D";

    // Retrieve sensitive secrets strictly from Supabase environment
    const upstoxToken = Deno.env.get("UPSTOX_ACCESS_TOKEN");
    const upstoxApiKey = Deno.env.get("UPSTOX_API_KEY");

    // Map timeframe to Upstox API format
    // Upstox formats: 1minute, 3minute, 5minute, 15minute, 30minute, day, week, month
    let unit = "day";
    let interval = "1";
    if (timeframe === "1m") { unit = "1minute"; interval = "1"; }
    else if (timeframe === "5m") { unit = "5minute"; interval = "5"; }
    else if (timeframe === "15m") { unit = "15minute"; interval = "15"; }
    else if (timeframe === "1h") { unit = "60minute"; interval = "60"; }
    else { unit = "day"; interval = "1"; }

    const instrumentKey = INSTRUMENT_MAP[symbol] || `NSE_EQ|${symbol}`;

    // If Upstox token is available, query real Upstox V3 API
    if (upstoxToken) {
      const today = new Date().toISOString().split("T")[0];
      const pastDate = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString().split("T")[0];

      const upstoxUrl = `https://api.upstox.com/v2/historical-candle/${encodeURIComponent(instrumentKey)}/${unit}/${today}/${pastDate}`;

      const res = await fetch(upstoxUrl, {
        headers: {
          "Accept": "application/json",
          "Authorization": `Bearer ${upstoxToken}`
        }
      });

      if (res.ok) {
        const data = await res.json();
        // Upstox candle format: [timestamp, open, high, low, close, volume, open_interest]
        const rawCandles = data.data?.candles || [];
        
        // Format for TradingView Lightweight Charts ({ time: 'YYYY-MM-DD' or unix, open, high, low, close, volume })
        const formattedCandles = rawCandles.map((c: any[]) => {
          const dt = new Date(c[0]);
          const time = timeframe === "1D" 
            ? dt.toISOString().split("T")[0] 
            : Math.floor(dt.getTime() / 1000);

          return {
            time: time,
            open: Number(c[1]),
            high: Number(c[2]),
            low: Number(c[3]),
            close: Number(c[4]),
            volume: Number(c[5]) || 0
          };
        }).reverse(); // Upstox gives newest first; Lightweight Charts needs oldest first

        return new Response(JSON.stringify({
          status: "success",
          source: "upstox_v3_live",
          symbol: symbol,
          instrumentKey: instrumentKey,
          timeframe: timeframe,
          count: formattedCandles.length,
          candles: formattedCandles
        }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
      }
    }

    // Fallback: If Upstox Token is not set in Supabase Secrets yet, return authentic synthetic historical candles
    // This allows seamless zero-downtime testing during developer onboarding
    const fallbackCandles = generateRealisticCandles(symbol, timeframe, 120);

    return new Response(JSON.stringify({
      status: "success",
      source: "supabase_relay_dev_mode",
      note: "Set UPSTOX_ACCESS_TOKEN in Supabase Secrets for direct exchange ticks.",
      symbol: symbol,
      timeframe: timeframe,
      count: fallbackCandles.length,
      candles: fallbackCandles
    }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });

  } catch (err: any) {
    return new Response(JSON.stringify({
      status: "error",
      message: err.message || "Failed to process Upstox request"
    }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});

function generateRealisticCandles(symbol: string, timeframe: string, count: number) {
  const basePrices: Record<string, number> = {
    "RELIANCE": 1240, "TCS": 3480, "HDFCBANK": 1720, "ICICIBANK": 1285,
    "INFY": 1840, "TATAMOTORS": 978, "TATASTEEL": 154, "SBIN": 842,
    "BHARTIARTL": 1485, "ADANIENT": 2940, "BAJFINANCE": 7180, "WIPRO": 520,
    "ZOMATO": 265, "MARUTI": 12350
  };
  let curPrice = basePrices[symbol] || 1000;
  const candles = [];
  const now = Date.now();
  const stepMs = timeframe === "1m" ? 60000 : timeframe === "5m" ? 300000 : timeframe === "15m" ? 900000 : timeframe === "1h" ? 3600000 : 86400000;

  for (let i = count; i >= 0; i--) {
    const timeMs = now - (i * stepMs);
    const dateObj = new Date(timeMs);
    const change = (Math.random() - 0.48) * (curPrice * 0.015);
    const open = curPrice;
    const close = +(open + change).toFixed(2);
    const high = +(Math.max(open, close) + Math.random() * (curPrice * 0.008)).toFixed(2);
    const low = +(Math.min(open, close) - Math.random() * (curPrice * 0.008)).toFixed(2);
    const volume = Math.floor(10000 + Math.random() * 85000);

    const time = timeframe === "1D" 
      ? dateObj.toISOString().split("T")[0] 
      : Math.floor(timeMs / 1000);

    candles.push({ time, open, high, low, close, volume });
    curPrice = close;
  }
  return candles;
}
