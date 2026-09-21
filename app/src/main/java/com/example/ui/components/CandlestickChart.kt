package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit,
    isLiveUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    var isCandleMode by remember { mutableStateOf(true) }
    var showEmaOverlay by remember { mutableStateOf(true) }
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }

    val timeframes = listOf("1D", "1W", "1M", "1Y", "ALL")

    CardContainer(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Header Controls: Timeframe chips + Mode switch + Live pulse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        Box(
                            modifier = Modifier
                                .testTag("timeframe_$tf")
                                .background(
                                    if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .pointerInput(tf) {
                                    detectTapGestures { onTimeframeChange(tf) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                // Controls & Live Indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isLiveUpdating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(BullishGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BullishGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Chart type toggle
                    IconButton(
                        onClick = { isCandleMode = !isCandleMode },
                        modifier = Modifier.size(32.dp).testTag("toggle_chart_type")
                    ) {
                        Icon(
                            imageVector = if (isCandleMode) Icons.Default.AutoGraph else Icons.Default.BarChart,
                            contentDescription = "Switch Chart Style",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Indicators toggle
                    IconButton(
                        onClick = { showEmaOverlay = !showEmaOverlay },
                        modifier = Modifier.size(32.dp).testTag("toggle_ema_overlay")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle EMA Line",
                            tint = if (showEmaOverlay) HoldAmber else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Interactive inspection stats bar
            val activeCandle = selectedCandleIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()

            AnimatedVisibility(visible = activeCandle != null) {
                if (activeCandle != null) {
                    val isGreen = activeCandle.close >= activeCandle.open
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time: ${activeCandle.label}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "O: ${String.format("%.1f", activeCandle.open)}",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "H: ${String.format("%.1f", activeCandle.high)}",
                                color = BullishGreen,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "L: ${String.format("%.1f", activeCandle.low)}",
                                color = BearishRed,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "C: ${String.format("%.1f", activeCandle.close)}",
                                color = if (isGreen) BullishGreen else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .pointerInput(candles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val candleWidth = size.width / candles.size
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedCandleIndex = if (selectedCandleIndex == index) null else index
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val candleWidth = size.width / candles.size
                                val index = (change.position.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedCandleIndex = index
                            },
                            onDragEnd = {
                                // keep selected or reset
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("interactive_canvas_chart")) {
                    if (candles.isEmpty()) return@Canvas

                    val minPrice = candles.minOf { it.low } * 0.996f
                    val maxPrice = candles.maxOf { it.high } * 1.004f
                    val priceRange = if (maxPrice - minPrice <= 0f) 1f else (maxPrice - minPrice)

                    val maxVolume = candles.maxOfOrNull { it.volume } ?: 1f
                    val chartHeight = size.height * 0.76f
                    val volumeHeight = size.height * 0.22f
                    val volumeBaseY = size.height

                    val candleSpacing = size.width / candles.size
                    val candleBodyWidth = (candleSpacing * 0.65f).coerceIn(3f, 14f)

                    // Draw Horizontal Grid Lines
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = (chartHeight / gridSteps) * i
                        drawLine(
                            color = ChartBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }

                    // Draw Volume Bars at bottom
                    candles.forEachIndexed { i, candle ->
                        val x = i * candleSpacing + candleSpacing / 2
                        val isGreen = candle.close >= candle.open
                        val barHeight = (candle.volume / maxVolume) * volumeHeight
                        val topY = volumeBaseY - barHeight

                        drawRect(
                            color = if (isGreen) BullishGreen.copy(alpha = 0.28f) else BearishRed.copy(alpha = 0.28f),
                            topLeft = Offset(x - candleBodyWidth / 2, topY),
                            size = Size(candleBodyWidth, barHeight)
                        )
                    }

                    if (isCandleMode) {
                        // Draw Candlesticks (Wicks & Bodies)
                        candles.forEachIndexed { i, candle ->
                            val x = i * candleSpacing + candleSpacing / 2
                            val isGreen = candle.close >= candle.open
                            val candleColor = if (isGreen) BullishGreen else BearishRed

                            val openY = chartHeight - ((candle.open - minPrice) / priceRange) * chartHeight
                            val closeY = chartHeight - ((candle.close - minPrice) / priceRange) * chartHeight
                            val highY = chartHeight - ((candle.high - minPrice) / priceRange) * chartHeight
                            val lowY = chartHeight - ((candle.low - minPrice) / priceRange) * chartHeight

                            // Wick
                            drawLine(
                                color = candleColor,
                                start = Offset(x, highY),
                                end = Offset(x, lowY),
                                strokeWidth = 1.8f
                            )

                            // Body
                            val bodyTop = min(openY, closeY)
                            val bodyBottom = max(openY, closeY)
                            val bodyHeight = max(2f, bodyBottom - bodyTop)

                            drawRect(
                                color = candleColor,
                                topLeft = Offset(x - candleBodyWidth / 2, bodyTop),
                                size = Size(candleBodyWidth, bodyHeight)
                            )
                        }
                    } else {
                        // Draw Line / Area Chart
                        val linePath = Path()
                        val areaPath = Path()

                        candles.forEachIndexed { i, candle ->
                            val x = i * candleSpacing + candleSpacing / 2
                            val y = chartHeight - ((candle.close - minPrice) / priceRange) * chartHeight
                            if (i == 0) {
                                linePath.moveTo(x, y)
                                areaPath.moveTo(x, chartHeight)
                                areaPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                areaPath.lineTo(x, y)
                            }
                        }

                        val lastX = (candles.size - 1) * candleSpacing + candleSpacing / 2
                        areaPath.lineTo(lastX, chartHeight)
                        areaPath.close()

                        // Gradient Area Fill
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Top Stroke Line
                        drawPath(
                            path = linePath,
                            color = NeonCyan,
                            style = Stroke(width = 2.4f)
                        )
                    }

                    // EMA 20 Line Overlay
                    if (showEmaOverlay && candles.size > 2) {
                        val emaPath = Path()
                        var first = true
                        candles.forEachIndexed { i, candle ->
                            if (candle.ma20 > 0f) {
                                val x = i * candleSpacing + candleSpacing / 2
                                val y = chartHeight - ((candle.ma20 - minPrice) / priceRange) * chartHeight
                                if (first) {
                                    emaPath.moveTo(x, y)
                                    first = false
                                } else {
                                    emaPath.lineTo(x, y)
                                }
                            }
                        }
                        drawPath(
                            path = emaPath,
                            color = HoldAmber.copy(alpha = 0.9f),
                            style = Stroke(width = 1.8f)
                        )
                    }

                    // Crosshair on selected index
                    selectedCandleIndex?.let { idx ->
                        val x = idx * candleSpacing + candleSpacing / 2
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.8f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(BullishGreen, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bullish", color = TextSecondary, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(BearishRed, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bearish", color = TextSecondary, fontSize = 11.sp)
                    }
                    if (showEmaOverlay) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp, 2.dp).background(HoldAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("20 EMA", color = HoldAmber, fontSize = 11.sp)
                        }
                    }
                }

                Text(
                    text = "Tap & drag to inspect",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChartCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ChartBorder),
        tonalElevation = 2.dp
    ) {
        content()
    }
}
