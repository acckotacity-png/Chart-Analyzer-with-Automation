package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CloudConnectScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FresherHubScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.ChartBorder
import com.example.ui.theme.ChartCardBg
import com.example.ui.theme.ChartDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

sealed class Screen(val title: String, val icon: ImageVector, val tag: String) {
    object Dashboard : Screen("Charts", Icons.Default.ShowChart, "nav_dashboard")
    object Assistant : Screen("AI Mentor", Icons.Default.AutoAwesome, "nav_assistant")
    object Watchlist : Screen("Watchlist", Icons.Default.Bookmark, "nav_watchlist")
    object Guide : Screen("Fresher Hub", Icons.Default.Lightbulb, "nav_guide")
    object Cloud : Screen("Cloud/SaaS", Icons.Default.CloudSync, "nav_cloud")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }

    val screens = listOf(
        Screen.Dashboard,
        Screen.Assistant,
        Screen.Watchlist,
        Screen.Guide,
        Screen.Cloud
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().background(ChartDarkBg),
        bottomBar = {
            NavigationBar(
                containerColor = ChartCardBg,
                contentColor = NeonCyan,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = ChartBorder)
                    .testTag("main_bottom_nav_bar")
            ) {
                screens.forEachIndexed { index, screen ->
                    val isSelected = selectedScreenIndex == index
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = { selectedScreenIndex = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedScreenIndex) {
            0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToAssistant = { selectedScreenIndex = 1 },
                onNavigateToCloud = { selectedScreenIndex = 4 },
                modifier = modifier
            )
            1 -> AssistantScreen(viewModel = viewModel, modifier = modifier)
            2 -> WatchlistScreen(
                viewModel = viewModel,
                onStockSelected = { selectedScreenIndex = 0 },
                modifier = modifier
            )
            3 -> FresherHubScreen(modifier = modifier)
            4 -> CloudConnectScreen(viewModel = viewModel, modifier = modifier)
        }
    }
}
