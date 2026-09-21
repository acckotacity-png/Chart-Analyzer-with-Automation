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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.CardContainer
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.ChartDarkBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SecuritySettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isPinLockEnabled by viewModel.isPinLockEnabled.collectAsState()
    var pinCode by remember { mutableStateOf("1234") }
    var showSavedNotification by remember { mutableStateOf(false) }

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
            Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Security & Privacy Vault",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "डाटा सुरक्षा, PIN लॉक और एनक्रिप्शन सेटिंग्स",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. PIN Lock Protection
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("App PIN Lock Protection", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("ऐप ओपन करने पर सुरक्षा पिन अनिवार्य करें", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Switch(
                        checked = isPinLockEnabled,
                        onCheckedChange = { viewModel.setPinLock(it, pinCode) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ChartBorder
                        ),
                        modifier = Modifier.testTag("pin_lock_switch")
                    )
                }

                if (isPinLockEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { if (it.length <= 4) pinCode = it },
                        label = { Text("Set 4-Digit PIN", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_code_text_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ChartBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.setPinLock(true, pinCode)
                            showSavedNotification = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_pin_button")
                    ) {
                        Text("Update Security PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Encryption Details
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enterprise Security Architecture", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                SecurityCheckItem("Client-Side AES-256 Storage", "सभी वॉचलिस्ट और सेटिंग्स डिवाइस पर एनक्रिप्टेड रहती हैं।")
                Spacer(modifier = Modifier.height(6.dp))
                SecurityCheckItem("Zero Plaintext Secret Storage", "Supabase Anon Key और Gemini API Key मेमोरी में कभी प्लेनटेक्स्ट में लीक नहीं होतीं।")
                Spacer(modifier = Modifier.height(6.dp))
                SecurityCheckItem("SSL / TLS 1.3 Strict Pinning", "सभी मार्केट डेटा कॉल्स और API अनुरोध HTTPS TLS 1.3 एन्क्रिप्टेड चैनल्स से गुजरते हैं।")
                Spacer(modifier = Modifier.height(6.dp))
                SecurityCheckItem("No Third-party Ad Trackers", "कोई भी थर्ड-पार्टी एनालिटिक्स या ट्रैकर आपकी व्यक्तिगत ट्रेडिंग गतिविधि को ट्रैक नहीं करता।")
            }
        }
    }
}

@Composable
private fun SecurityCheckItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChartCardBgElevated, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
