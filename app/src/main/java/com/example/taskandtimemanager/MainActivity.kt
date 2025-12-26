package com.example.taskandtimemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.ui.DashboardScreen
import com.example.taskandtimemanager.ui.TasksScreen
import com.example.taskandtimemanager.ui.CostsScreen
import com.example.taskandtimemanager.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                        3 -> SettingsScreen(dataStore, scope, applicationContext)
                    }
                }
            }
        }
    }
}
