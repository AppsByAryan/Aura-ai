package com.example.aura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aura.ui.screens.AuditLogScreen
import com.example.aura.ui.screens.DevicesScreen
import com.example.aura.ui.screens.HomeScreen
import com.example.aura.ui.screens.SecuritySettingsScreen
import com.example.aura.ui.screens.SystemDashboardScreen
import com.example.ui.theme.AuraBlack
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraCyanContainer
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary

enum class AuraTab(val label: String, val icon: ImageVector, val tag: String) {
    ASSISTANT("Assistant", Icons.Default.GraphicEq, "tab_assistant"),
    SYSTEM("System", Icons.Default.Speed, "tab_system"),
    DEVICES("Devices", Icons.Default.Computer, "tab_devices"),
    AUDIT("Audit Log", Icons.Default.History, "tab_audit"),
    SECURITY("Security", Icons.Default.Security, "tab_security")
}

@Composable
fun AuraApp(
    viewModel: AuraViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentTab by rememberSaveable { mutableStateOf(AuraTab.ASSISTANT) }

    // When on secondary tab, back press returns to ASSISTANT
    if (currentTab != AuraTab.ASSISTANT) {
        BackHandler {
            currentTab = AuraTab.ASSISTANT
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuraBlack,
        bottomBar = {
            NavigationBar(
                containerColor = AuraDarkSurface,
                tonalElevation = 8.dp
            ) {
                AuraTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AuraCyan,
                            selectedTextColor = AuraCyan,
                            unselectedIconColor = AuraTextMuted,
                            unselectedTextColor = AuraTextMuted,
                            indicatorColor = AuraCyanContainer
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AuraTab.ASSISTANT -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateSecurity = { currentTab = AuraTab.SECURITY }
                )
                AuraTab.SYSTEM -> SystemDashboardScreen(
                    viewModel = viewModel
                )
                AuraTab.DEVICES -> DevicesScreen(
                    viewModel = viewModel
                )
                AuraTab.AUDIT -> AuditLogScreen(
                    viewModel = viewModel
                )
                AuraTab.SECURITY -> SecuritySettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
