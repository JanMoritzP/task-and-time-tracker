package com.example.taskandtimemanager

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.ui.AppConfigScreen
import com.example.taskandtimemanager.ui.CostsScreen
import com.example.taskandtimemanager.ui.DashboardScreen
import com.example.taskandtimemanager.ui.SettingsScreen
import com.example.taskandtimemanager.ui.TasksScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, AppBlockerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val dataStore = DataStore(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            var selectedTab by remember { mutableStateOf(0) }

            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("Dashboard") },
                                icon = { Text("📊") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("Tasks") },
                                icon = { Text("📋") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                label = { Text("Costs") },
                                icon = { Text("⚙️") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                label = { Text("App Blocker") },
                                icon = { Text("🚫") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                label = { Text("Settings") },
                                icon = { Text("⚙️") }
                            )
                        }
                    }
                ) { paddingValues ->
                    when (selectedTab) {
                        0 -> DashboardScreen(dataStore, scope)
                        1 -> TasksScreen(dataStore, scope, applicationContext)
                        2 -> CostsScreen(dataStore, scope)
                        3 -> AppConfigScreen(
                            dataStore = dataStore,
                            scope = scope,
                            context = this,
                            devicePolicyManager =
                            getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        )
                        4 -> SettingsScreen(dataStore, scope, applicationContext)
                    }
                }
            }
        }
    }
}
