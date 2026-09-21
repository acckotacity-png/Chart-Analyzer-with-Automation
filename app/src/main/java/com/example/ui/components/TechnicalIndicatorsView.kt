package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TechnicalIndicators
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TechnicalIndicatorsView(
    indicators: TechnicalIndicators,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    CardContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Indicators",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TECHNICAL HEALTH METRICS",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (indicators.trend.contains("Uptrend")) BullishGreen.copy(alpha = 0.15f)
                            else if (indicators.trend.contains("Downtrend")) BearishRed.copy(alpha = 0.15f)
                            else HoldAmber.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = indicators.trend,
                        color = if (indicators.trend.contains("Uptrend")) BullishGreen
                        else if (indicators.trend.contains("Downtrend")) BearishRed
                        else HoldAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RSI Gauge Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChartCardBgElevated, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Relative Strength Index (RSI 14)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${indicators.rsi}",
                        color = when {
                            indicators.rsi >= 70.0 -> BearishRed
                            indicators.rsi <= 30.0 -> BullishGreen
                            else -> NeonCyan
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Multi-zone RSI progress bar
                LinearProgressIndicator(
                    progress = { (indicators.rsi / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = when {
                        indicators.rsi >= 70.0 -> BearishRed
                        indicators.rsi <= 30.0 -> BullishGreen
                        else -> NeonCyan
                    },
                    trackColor = ChartBorder
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Oversold (<30)", color = BullishGreen, fontSize = 10.sp)
                    Text("Neutral (30-70)", color = TextMuted, fontSize = 10.sp)
                    Text("Overbought (>70)", color = BearishRed, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Grid of Indicators: MACD, 20 EMA, Support, Resistance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndicatorItem(
                    title = "MACD (12, 26, 9)",
                    value = "${indicators.macd}",
                    subtext = indicators.macdCross,
                    statusColor = if (indicators.macdCross.contains("Bullish")) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )
                IndicatorItem(
                    title = "20 EMA DYNAMIC",
                    value = "₹${indicators.ema20}",
                    subtext = if (currentPrice >= indicators.ema20) "Bullish (Above EMA)" else "Bearish (Below EMA)",
                    statusColor = if (currentPrice >= indicators.ema20) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndicatorItem(
                    title = "MAJOR SUPPORT",
                    value = "₹${indicators.supportPrice}",
                    subtext = "High Demand Zone",
                    statusColor = BullishGreen,
                    modifier = Modifier.weight(1f)
                )
                IndicatorItem(
                    title = "MAJOR RESISTANCE",
                    value = "₹${indicators.resistancePrice}",
                    subtext = "Supply Pressure Zone",
                    statusColor = BearishRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IndicatorItem(
    title: String,
    value: String,
    subtext: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ChartCardBgElevated, RoundedCornerShape(10.dp))
            .border(1.dp, ChartBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
