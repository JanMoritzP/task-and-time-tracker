package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CostsScreen(dataStore: DataStore, scope: CoroutineScope) {
    var apps by remember { mutableStateOf(emptyList<AppData>()) }

    LaunchedEffect(Unit) {
        apps = dataStore.getApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚙️ App Costs", fontSize = 18.sp, fontWeight = LocalTextStyle.current.fontWeight)
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps) { app ->
                var cost by remember { mutableStateOf(app.costPerMinute.toString()) }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(app.name, fontSize = 14.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = cost,
                                onValueChange = { cost = it },
                                label = { Text("coins/min") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                scope.launch {
                                    dataStore.updateApp(app.copy(costPerMinute = cost.toFloatOrNull() ?: 0f))
                                    apps = dataStore.getApps()
                                }
                            }) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}
