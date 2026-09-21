package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalType
import com.example.data.model.StockRecommendation
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedGlow
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenGlow
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.HoldAmberGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RecommendationCard(
    recommendation: StockRecommendation,
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHindiExplanation by remember { mutableStateOf(true) }

    val signalColor = when (recommendation.signal) {
        SignalType.STRONG_BUY, SignalType.BUY -> BullishGreen
        SignalType.HOLD -> HoldAmber
        SignalType.SELL, SignalType.STRONG_SELL -> BearishRed
    }

    val glowColor = when (recommendation.signal) {
        SignalType.STRONG_BUY, SignalType.BUY -> BullishGreenGlow
        SignalType.HOLD -> HoldAmberGlow
        SignalType.SELL, SignalType.STRONG_SELL -> BearishRedGlow
    }

    CardContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Top Row: AI Badge + Language switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Powered",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI INSIGHT & RECOMMENDATION",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Toggle Hindi / English
                Row(
                    modifier = Modifier
                        .background(ChartCardBgElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, ChartBorder, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (showHindiExplanation) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { showHindiExplanation = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "सरल हिंदी",
                            fontSize = 11.sp,
                            fontWeight = if (showHindiExplanation) FontWeight.Bold else FontWeight.Normal,
                            color = if (showHindiExplanation) NeonCyan else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!showHindiExplanation) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { showHindiExplanation = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "English",
                            fontSize = 11.sp,
                            fontWeight = if (!showHindiExplanation) FontWeight.Bold else FontWeight.Normal,
                            color = if (!showHindiExplanation) NeonCyan else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Signal Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glowColor, RoundedCornerShape(12.dp))
                    .border(1.dp, signalColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI ACTION SIGNAL",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = recommendation.signal.label,
                        color = signalColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${recommendation.confidence}% Confidence",
                        color = signalColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(110.dp)) {
                        LinearProgressIndicator(
                            progress = { recommendation.confidence / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = signalColor,
                            trackColor = ChartBorder
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recommendation.riskLevel,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Trade Metrics: Target, Stop Loss, Entry, Risk:Reward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "ENTRY RANGE",
                    value = recommendation.entryRange,
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "TARGET",
                    value = "₹${recommendation.targetPrice}",
                    valueColor = BullishGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "STOP LOSS",
                    value = "₹${recommendation.stopLoss}",
                    valueColor = BearishRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation box (Beginner Hindi or Pro English)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChartCardBgElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, ChartBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                AnimatedContent(targetState = showHindiExplanation, label = "explanation_lang") { isHindi ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHindi) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "नए ट्रेडर्स के लिए सरल व्याख्या:" else "Technical Analysis Summary:",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) recommendation.fresherExplanationHindi else recommendation.summaryEnglish,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        // Key Technical Reasons
                        Spacer(modifier = Modifier.height(10.dp))
                        recommendation.keyReasons.take(3).forEach { reason ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(NeonCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reason,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Ask AI Assistant Button
            Button(
                onClick = onOpenAssistant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.15f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ask_ai_assistant_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI असिस्टेंट से पूछें: 'Hold karun ya sell?'",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
            .border(1.dp, ChartBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
