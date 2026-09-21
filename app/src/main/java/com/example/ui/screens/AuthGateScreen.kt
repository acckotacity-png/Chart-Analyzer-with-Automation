package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
fun AuthGateScreen(
    viewModel: MainViewModel,
    onApprovedAccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    val user = currentUser

    // If user is logged in and APPROVED, direct entry
    if (user != null && user.isApproved) {
        onApprovedAccess()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChartDarkBg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ChartCardBg)
                .border(1.dp, ChartBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ChartCardBgElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (user != null && !user.isApproved) Icons.Default.HourglassTop else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (user != null && !user.isApproved) HoldAmber else NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chart Analyzer Pro",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Supabase Access & Admin Authorization",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CASE 1: USER LOGGED IN BUT WAITING FOR APPROVAL
            if (user != null && !user.isApproved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HoldAmber.copy(alpha = 0.12f))
                        .border(1.dp, HoldAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Approval Pending / एडमिन अप्रूवल पेंडिंग",
                            color = HoldAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "नमस्ते ${user.full_name}, आपकी एक्सेस रिक्वेस्ट एडमिन के पास पहुँच गई है। एडमिन द्वारा Rights (Approval) देने के बाद ही आप ऐप और चार्ट देख सकेंगे।",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Email: ${user.email}\nMobile: ${user.mobile_number}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.refreshUserStatus() },
                        modifier = Modifier.weight(1f).testTag("auth_refresh_status_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Status", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.weight(1f).testTag("auth_logout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ChartCardBgElevated)
                    ) {
                        Text("Log Out", color = TextSecondary)
                    }
                }
            } else {
                // CASE 2: REGISTRATION OR LOGIN FORM
                if (authMessage != null) {
                    Text(
                        text = authMessage ?: "",
                        color = if (authMessage?.contains("Error") == true || authMessage?.contains("declined") == true) BearishRed else NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (isRegisterMode) {
                    // New User Registration Form
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name (नाम)", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ChartBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { mobileInput = it },
                        label = { Text("Mobile Number (मोबाइल नंबर)", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NeonCyan) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_mobile_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ChartBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email ID (ईमेल)", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Login, contentDescription = null, tint = NeonCyan) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ChartBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && mobileInput.isNotBlank() && emailInput.isNotBlank()) {
                                viewModel.requestUserAccess(nameInput, mobileInput, emailInput)
                            }
                        },
                        enabled = !authLoading && nameInput.isNotBlank() && mobileInput.isNotBlank() && emailInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reg_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Request Admin Access", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Already registered? Login with Email/Phone",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isRegisterMode = false }
                            .padding(8.dp)
                    )
                } else {
                    // Login / Status Check Mode
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email or Mobile Number", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Login, contentDescription = null, tint = NeonCyan) },
                        modifier = Modifier.fillMaxWidth().testTag("login_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ChartBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank()) {
                                viewModel.loginUser(emailInput)
                            }
                        },
                        enabled = !authLoading && emailInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Log In / Check Access", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "New User? Register with Name & Mobile",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isRegisterMode = true }
                            .padding(8.dp)
                    )
                }

                // Quick Admin demo button
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChartCardBgElevated)
                        .border(1.dp, ChartBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.loginUser("arjunmalviya166@gmail.com")
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log In as Admin (Arjun)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
