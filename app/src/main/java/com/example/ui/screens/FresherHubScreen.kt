package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CardContainer
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.ChartDarkBg
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FresherHubScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChartDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChartCardBg)
                .border(1.dp, ChartBorder)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = HoldAmber, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Fresher Stock Guide (सरल सीख)",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "नए ट्रेडर्स के लिए चार्ट, संकेतक और रिस्क मैनेजमेंट गाइड",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Golden Rules of Trading
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(BullishGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3 गोल्डन रूल्स (हर फ्रेशर को याद रखना चाहिए)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                RuleItem(
                    number = "1",
                    title = "बिना स्टॉप लॉस कभी ट्रेड न लें (Stop Loss is Must)",
                    description = "अगर शेयर की दिशा आपके खिलाफ जाती है, तो स्टॉप लॉस आपकी 95% पूंजी को बचा लेता है। छोटा नुकसान स्वीकार करें ताकि बड़ा नुकसान न हो।"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleItem(
                    number = "2",
                    title = "1:2 रिस्क टू रिवॉर्ड नियम (Risk:Reward Ratio)",
                    description = "अगर आप ₹10 का रिस्क (स्टॉप लॉस) ले रहे हैं, तो कम से कम ₹20 का टारगेट रखें। इससे अगर आपके 50% ट्रेड भी गलत होंगे तब भी आप फायदे में रहेंगे।"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleItem(
                    number = "3",
                    title = "FOMO से बचें (Don't Buy at Top)",
                    description = "जब कोई शेयर बहुत तेजी से भाग चुका हो और RSI 70 से ऊपर हो, तो कभी लालच में न खरीदें। थोड़ा नीचे आने (Pullback) का इंतजार करें।"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Candlestick Patterns Decoded
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CandlestickChart, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("कैंडलस्टिक पैटर्न्स कैसे समझें?", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                PatternCard(
                    name = "Hammer (हथौड़ा कैंडल)",
                    type = "Bullish Reversal (तेजी का संकेत)",
                    typeColor = BullishGreen,
                    desc = "लंबी नीचे की पूंछ (Wick) और छोटा शरीर। इसका मतलब खरीदारों ने नीचे के भाव पर जोरदार खरीदारी की है।"
                )

                Spacer(modifier = Modifier.height(8.dp))

                PatternCard(
                    name = "Bullish Engulfing (हरी बड़ी कैंडल)",
                    type = "Strong Buy Signal",
                    typeColor = BullishGreen,
                    desc = "जब एक बड़ी हरी कैंडल पिछली लाल कैंडल को पूरी तरह निगल लेती है। यह तेजी के नए दौर का संकेत है।"
                )

                Spacer(modifier = Modifier.height(8.dp))

                PatternCard(
                    name = "Doji (क्रॉस जैसी कैंडल)",
                    type = "Indecision (अनिश्चितता)",
                    typeColor = HoldAmber,
                    desc = "ओपन और क्लोज भाव लगभग बराबर होते हैं। यह संकेत देता है कि बायर्स और सेलर्स बराबर ताकत में हैं; ब्रेकआउट का इंतजार करें।"
                )

                Spacer(modifier = Modifier.height(8.dp))

                PatternCard(
                    name = "Shooting Star (उल्टा हथौड़ा)",
                    type = "Bearish Warning (मंदी का संकेत)",
                    typeColor = BearishRed,
                    desc = "ऊपर की तरफ बहुत लंबी पूंछ। इसका मतलब ऊपरी स्तर पर प्रॉफिट बुकिंग हुई है, यहां फ्रेश बाय न करें।"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Technical Indicators Explained Simply
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(HoldAmber.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = HoldAmber, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("प्रमुख संकेतक (Indicators 101)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                IndicatorExplainer(
                    name = "RSI (Relative Strength Index)",
                    short = "स्पीडोमीटर",
                    body = "• 30 से नीचे: Oversold (सस्ता हो चुका है, बाउंस आ सकता है)\n• 30 से 70: Neutral (संतुलित स्थिति)\n• 70 से ऊपर: Overbought (बहुत महंगा हो चुका है, प्रॉफिट बुक करें)"
                )

                Spacer(modifier = Modifier.height(8.dp))

                IndicatorExplainer(
                    name = "20 EMA (Exponential Moving Average)",
                    short = "ट्रेंड सपोर्ट लाइन",
                    body = "अगर भाव 20 EMA के ऊपर चल रहा है तो शेयर मजबूत है। जब भी भाव 20 EMA के पास आए, वह खरीदारी का अच्छा अवसर हो सकता है।"
                )

                Spacer(modifier = Modifier.height(8.dp))

                IndicatorExplainer(
                    name = "MACD (Moving Average Convergence Divergence)",
                    short = "मोमेंटम इंडिकेटर",
                    body = "जब MACD लाइन सिग्नल लाइन को नीचे से ऊपर काटती है तो उसे 'Bullish Crossover' कहते हैं—यह नई तेजी की शुरुआत है।"
                )
            }
        }
    }
}

@Composable
private fun RuleItem(number: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(BullishGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PatternCard(name: String, type: String, typeColor: Color, desc: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
            .border(1.dp, ChartBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(type, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun IndicatorExplainer(name: String, short: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
            .border(1.dp, ChartBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("($short)", color = TextMuted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}
