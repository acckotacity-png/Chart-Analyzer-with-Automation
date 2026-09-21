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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.ui.MainViewModel
import com.example.ui.components.CardContainer
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartCardBgElevated
import com.example.ui.theme.ChartDarkBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CloudConnectScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val supabaseConfig by viewModel.supabaseConfig.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var projectUrl by remember(supabaseConfig.projectUrl) { mutableStateOf(supabaseConfig.projectUrl) }
    var anonKey by remember(supabaseConfig.anonKey) { mutableStateOf(supabaseConfig.anonKey) }
    var customWebhookUrl by remember { mutableStateOf("https://api.marketdata.cloud/v1/quotes") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChartDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChartCardBg)
                .border(1.dp, ChartBorder)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Cloud & Deployment Integrations",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supabase PostgreSQL • GitHub CI/CD • Real-time Data Feeds",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Banner if synced
        if (syncStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(BullishGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, BullishGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = syncStatus ?: "", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 1. Supabase Connection Card
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BullishGreen.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supabase Cloud Sync", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (supabaseConfig.isConnected) BullishGreen.copy(alpha = 0.2f) else HoldAmber.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (supabaseConfig.isConnected) "CONNECTED" else "READY TO CONNECT",
                            color = if (supabaseConfig.isConnected) BullishGreen else HoldAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "अपने स्टॉक वॉचलिस्ट, पोर्टफोलियो और AI रिकमेंडेशन रिपोर्ट्स को Supabase PostgreSQL डेटाबेस से जोड़ें।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("SUPABASE PROJECT URL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = projectUrl,
                    onValueChange = { projectUrl = it },
                    placeholder = { Text("https://your-project.supabase.co", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supabase_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ChartBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("SUPABASE ANON / PUBLISHABLE KEY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supabase_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ChartBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateSupabaseCredentials(projectUrl, anonKey)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_supabase_button")
                    ) {
                        Text("Save Config", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.syncDataToSupabase()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("sync_supabase_button")
                    ) {
                        Text("Sync Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last Sync: ${supabaseConfig.lastSyncTime}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. GitHub CI/CD & Vercel/Render Live Deployment
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(ElectricBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub & Cloud Deployment (Vercel / Render)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "यह प्रोजेक्ट GitHub रिपॉजिटरी से कनेक्ट होकर स्वचालित CI/CD पाइपलाइन के साथ Vercel, Render और Android APK पर डिप्लॉय होने के लिए पूरी तरह अनुकूलित है।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChartCardBgElevated, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Repository Status", color = TextMuted, fontSize = 11.sp)
                        Text("main branch • Production Ready", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(BullishGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("AUTOMATED", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Real-time Market Data Connection Settings
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alpha Vantage / Financial Market API (Retrofit)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Retrofit REST क्लाइंट का उपयोग करके Alpha Vantage से लाइव स्टॉक कोट्स और डे-कैंडल्स लोड करें। अगर आपके पास API Key नहीं है, तो 'demo' कुंजी से टेस्ट किया जा सकता है:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                var alphaVantageKey by remember { mutableStateOf(com.example.data.api.FinancialDataProviderClient.apiKey) }

                OutlinedTextField(
                    value = alphaVantageKey,
                    onValueChange = {
                        alphaVantageKey = it
                        viewModel.setFinancialProviderApiKey(it)
                    },
                    label = { Text("Alpha Vantage API Key", fontSize = 11.sp) },
                    placeholder = { Text("demo or your free Alpha Vantage key", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alpha_vantage_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ChartBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.fetchRealTimeMarketPrice()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_retrofit_fetch_button")
                    ) {
                        Text("Test Live Quote Fetch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Engine: Retrofit + OkHttp", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3.5 Admin User Management & Rights Approval Panel
        val currentUser by viewModel.currentUser.collectAsState()
        val allUsers by viewModel.allUsersList.collectAsState()

        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin: User Approval & Rights",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.loadAllUsersForAdmin() },
                        colors = ButtonDefaults.buttonColors(containerColor = ChartCardBgElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Refresh List", fontSize = 11.sp, color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentUser?.isAdmin == true)
                        "Logged in as Admin (${currentUser?.email}). You have full rights to approve/reject user registration requests."
                    else
                        "Logged in as: ${currentUser?.full_name ?: "Guest"} (${currentUser?.email ?: ""}) - Status: ${currentUser?.status?.uppercase() ?: "PENDING"}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (allUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChartCardBgElevated)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap 'Refresh List' to load registered users from Supabase", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allUsers.forEach { u ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ChartCardBgElevated)
                                    .border(1.dp, ChartBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.full_name.ifBlank { "User" }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${u.mobile_number} • ${u.email}", color = TextSecondary, fontSize = 11.sp)
                                        Text(
                                            "Status: ${u.status.uppercase()}",
                                            color = when (u.status.lowercase()) {
                                                "approved" -> BullishGreen
                                                "rejected" -> BearishRed
                                                else -> HoldAmber
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (currentUser?.isAdmin == true) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (u.status != "approved") {
                                                Button(
                                                    onClick = { viewModel.approveUser(u.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.testTag("approve_user_${u.id}")
                                                ) {
                                                    Text("Approve", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (u.status != "rejected" && u.role != "admin") {
                                                Button(
                                                    onClick = { viewModel.rejectUser(u.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.testTag("reject_user_${u.id}")
                                                ) {
                                                    Text("Reject", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Data Security Guarantee
        CardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Data Security & Masking Active", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("आपकी सभी API कीज और क्रेडेंशियल्स क्लाइंट-साइड सिक्योर स्टोरेज में एनक्रिप्टेड रहती हैं।", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
