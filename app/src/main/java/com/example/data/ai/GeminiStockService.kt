package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.SignalType
import com.example.data.model.Stock
import com.example.data.model.StockRecommendation
import com.example.data.model.TechnicalIndicators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiStockService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateIndicatorRecommendation(
        currentStock: Stock,
        indicators: TechnicalIndicators,
        baseRecommendation: StockRecommendation
    ): Pair<String, SignalType> = withContext(Dispatchers.IO) {
        val prompt = "Based on RSI (${indicators.rsi} - ${indicators.rsiStatus}), SMA 50 (₹${indicators.sma50}), 20 EMA (₹${indicators.ema20}), and MACD (${indicators.macdCross}) for ${currentStock.symbol} priced at ₹${currentStock.price}, give a natural language Buy/Hold/Sell recommendation for beginners."
        
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY") && apiKey.length > 10) {
            try {
                val responseText = callGeminiRest(apiKey, prompt, currentStock, baseRecommendation, indicators)
                if (responseText.isNotBlank()) {
                    val detectedSignal = extractSignal(responseText) ?: baseRecommendation.signal
                    return@withContext Pair(responseText, detectedSignal)
                }
            } catch (e: Exception) {
                // Fallback gracefully to offline intelligence
            }
        }

        val (offlineText, signal) = generateExpertOfflineResponse(
            prompt = "Hold karun ya sell?",
            stock = currentStock,
            rec = baseRecommendation,
            ind = indicators
        )
        Pair(offlineText, signal ?: baseRecommendation.signal)
    }

    suspend fun askAssistant(
        userPrompt: String,
        currentStock: Stock,
        recommendation: StockRecommendation,
        indicators: TechnicalIndicators
    ): Pair<String, SignalType?> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY") && apiKey.length > 10) {
            try {
                val responseText = callGeminiRest(apiKey, userPrompt, currentStock, recommendation, indicators)
                if (responseText.isNotBlank()) {
                    val detectedSignal = extractSignal(responseText)
                    return@withContext Pair(responseText, detectedSignal)
                }
            } catch (e: Exception) {
                // Fallback gracefully to offline expert intelligence
            }
        }

        // Offline Heuristic Expert Stock Assistant (Hindi & English Fresher Friendly)
        return@withContext generateExpertOfflineResponse(userPrompt, currentStock, recommendation, indicators)
    }

    private fun callGeminiRest(
        apiKey: String,
        prompt: String,
        stock: Stock,
        rec: StockRecommendation,
        ind: TechnicalIndicators
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are ChartAI, an expert Stock Market Analyst and Mentor who simplifies chart insights for beginners and freshers.
            Current Stock: ${stock.symbol} (${stock.name})
            Current Price: ₹${stock.price}
            System Recommendation: ${rec.signal.label} (Confidence: ${rec.confidence}%)
            Target: ₹${rec.targetPrice}, Stop Loss: ₹${rec.stopLoss}, Entry: ${rec.entryRange}
            RSI: ${ind.rsi} (${ind.rsiStatus}), MACD: ${ind.macdCross}, Trend: ${ind.trend}
            
            Guidelines:
            1. Always provide clear, direct advice: whether to BUY, HOLD, or SELL.
            2. Explain in simple, friendly Hinglish/Hindi mixed with English terms so even a fresher with 0 experience can easily understand.
            3. Always highlight Stop Loss and Risk Management rules.
            4. Keep answers concise, structured in 3-4 bullet points.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nUser Question: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 600)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return ""
        }

        val root = JSONObject(bodyString)
        val candidates = root.optJSONArray("candidates") ?: return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        return parts.optJSONObject(0)?.optString("text") ?: ""
    }

    private fun extractSignal(text: String): SignalType? {
        val upper = text.uppercase()
        return when {
            upper.contains("STRONG BUY") -> SignalType.STRONG_BUY
            upper.contains("BUY") && !upper.contains("DON'T BUY") && !upper.contains("NOT BUY") -> SignalType.BUY
            upper.contains("STRONG SELL") -> SignalType.STRONG_SELL
            upper.contains("SELL") -> SignalType.SELL
            upper.contains("HOLD") -> SignalType.HOLD
            else -> null
        }
    }

    private fun generateExpertOfflineResponse(
        prompt: String,
        stock: Stock,
        rec: StockRecommendation,
        ind: TechnicalIndicators
    ): Pair<String, SignalType?> {
        val lower = prompt.lowercase()
        val symbol = stock.symbol
        val price = stock.price
        val target = rec.targetPrice
        val sl = rec.stopLoss

        return when {
            lower.contains("hold") || lower.contains("sell") || lower.contains("kya karu") || lower.contains("action") -> {
                val advice = StringBuilder()
                advice.append("📊 **${symbol} पर AI की स्पष्ट सलाह: ${rec.signal.label}**\n\n")
                advice.append("1. **फैसला (Action):** अभी इस शेयर पर **${rec.signal.label}** की रणनीति सबसे सही है (विश्वास स्तर: ${rec.confidence}%).\n")
                advice.append("2. **अगर आप फ्रेशर हैं:** ${rec.fresherExplanationHindi}\n")
                advice.append("3. **टारगेट (Target Price):** ₹$target (अनुमानित रिटर्न: +${String.format("%.1f", ((target - price)/price)*100)}%)\n")
                advice.append("4. **स्टॉप लॉस (Stop Loss):** ₹$sl पर सख्त स्टॉप लॉस रखें। किसी भी परिस्थिति में बिना स्टॉप लॉस के ट्रेड न करें!\n")
                advice.append("5. **संकेतक (Indicators):** RSI अभी ${ind.rsi} (${ind.rsiStatus}) पर है और ट्रेंड '${ind.trend}' दिखा रहा है।")
                Pair(advice.toString(), rec.signal)
            }
            lower.contains("stop loss") || lower.contains("sl") || lower.contains("risk") -> {
                val text = """
                    🛡️ **स्टॉप लॉस & रिस्क मैनेजमेंट गाइड for ${symbol}**
                    
                    • **अनुशंसित स्टॉप लॉस:** **₹$sl** (जो मौजूदा भाव ₹$price से लगभग 3-4% नीचे है)
                    • **यह क्यों जरूरी है?** अगर मार्केट आपके एनालिसिस के विपरीत अचानक गिरता है, तो स्टॉप लॉस आपकी पूंजी को सुरक्षित रखेगा और केवल छोटा नुकसान होने देगा।
                    • **1:2 नियम (Fresher Tip):** हमेशा जितना नुकसान उठाने को तैयार हैं, उससे दोगुना मुनाफा टारगेट रखें। यहां रिस्क:रिवॉर्ड रेशियो **${rec.riskRewardRatio}** है।
                    • **टारगेट स्तर:** **₹$target**
                """.trimIndent()
                Pair(text, null)
            }
            lower.contains("target") || lower.contains("kab tak") || lower.contains("profit") -> {
                val text = """
                    🎯 **${symbol} का लक्ष्य मूल्य (Target Breakdown)**
                    
                    • **पहला टारगेट (T1):** **₹${(price + (target - price) * 0.6).toInt()}** (शॉर्ट टर्म - 2 से 5 दिन)
                    • **मुख्य टारगेट (T2):** **₹$target** (स्विंग ट्रेड - 1 से 3 हफ्ते)
                    • **स्टॉप लॉस सुरक्षा:** ₹$sl
                    • **रणनीति:** जब शेयर T1 तक पहुंचे, तो आधा मुनाफा बुक कर लें और बाकी का स्टॉप लॉस बढ़ाकर अपने बाइंग प्राइस पर ले आएं (Trailing Stop Loss).
                """.trimIndent()
                Pair(text, rec.signal)
            }
            lower.contains("fresher") || lower.contains("beginner") || lower.contains("samjhao") || lower.contains("naye") -> {
                val text = """
                    💡 **फ्रेशर के लिए आसान गाइड (Beginner Friendly Explanation)**
                    
                    • **शेयर का नाम:** ${stock.name} (${stock.symbol})
                    • **मौजूदा भाव:** ₹$price
                    • **आसान शब्दों में समझें:** 
                    ${rec.fresherExplanationHindi}
                    
                    • **3 गोल्डन रूल्स (नए ट्रेडर्स के लिए):**
                    1. कभी भी अपनी सारी पूंजी एक ही शेयर में न लगाएं (Diversify).
                    2. बिना स्टॉप लॉस (₹$sl) के कभी कोई सौदा न लें।
                    3. भावनाओं (FOMO या डर) में आकर जल्दबाजी में न खरीदें।
                """.trimIndent()
                Pair(text, rec.signal)
            }
            else -> {
                val text = """
                    🤖 **ChartAI असिस्टेंट - ${stock.symbol} विश्लेषण**
                    
                    • **सिग्नल:** **${rec.signal.label}** (Confidence: ${rec.confidence}%)
                    • **एंट्री रेंज:** ${rec.entryRange}
                    • **टारगेट:** ₹$target | **स्टॉप लॉस:** ₹$sl
                    • **RSI (14):** ${ind.rsi} - ${ind.rsiStatus}
                    • **MACD:** ${ind.macdCross}
                    • **20 EMA:** ₹${ind.ema20} (सपोर्ट स्तर)
                    
                    ${rec.fresherExplanationHindi}
                    
                    *आप मुझसे पूछ सकते हैं: "Hold karun ya sell?", "Stop loss kahan lagaye?", या "Risk analysis batao".*
                """.trimIndent()
                Pair(text, rec.signal)
            }
        }
    }
}
