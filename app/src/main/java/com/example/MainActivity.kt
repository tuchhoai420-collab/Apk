package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdbScreen
import com.example.ui.screens.AgentScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentLavenderContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnAccentLavender
import com.example.ui.theme.OnAccentLavenderContainer
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderSubtle
import com.example.ui.theme.SophisticatedNavBg
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentLavender),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = OnAccentLavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cometa OS",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentLavenderContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (activeEngine == com.example.agent.AIEngineType.LOCAL_LLAMA) "LLAMA.CPP" else "GEMINI AI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnAccentLavenderContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (deviceState.isConnected) EmeraldGreen else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (deviceState.isConnected) "ADB Conectado" else "Modo Local",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                },

                actions = {
                    Surface(
                        color = SophisticatedSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SophisticatedBorderSubtle),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("top_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (deviceState.isConnected) EmeraldGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (deviceState.isConnected) "ADB: OK" else "ADB: OFF",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (deviceState.isConnected) EmeraldGreen else TextMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SophisticatedBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SophisticatedNavBg,
                contentColor = TextPrimary,
                modifier = Modifier
                    .height(72.dp)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Consola NL",
                            tint = if (currentTab == 0) OnAccentLavenderContainer else TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "Consola NL",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (currentTab == 0) TextPrimary else TextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AccentLavenderContainer,
                        selectedIconColor = OnAccentLavenderContainer,
                        selectedTextColor = TextPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_agent")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Modelos GGUF",
                            tint = if (currentTab == 1) OnAccentLavenderContainer else TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "Modelos",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (currentTab == 1) TextPrimary else TextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AccentLavenderContainer,
                        selectedIconColor = OnAccentLavenderContainer,
                        selectedTextColor = TextPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_models")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Depuración ADB",
                            tint = if (currentTab == 2) OnAccentLavenderContainer else TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "Depuración ADB",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == 2) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (currentTab == 2) TextPrimary else TextMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AccentLavenderContainer,
                        selectedIconColor = OnAccentLavenderContainer,
                        selectedTextColor = TextPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_adb")
                )
            }
        },
        containerColor = SophisticatedBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tabIndex ->
                when (tabIndex) {
                    0 -> AgentScreen(viewModel = viewModel)
                    1 -> ModelsScreen(viewModel = viewModel)
                    2 -> AdbScreen(viewModel = viewModel)
                }
            }
        }
    }
}

