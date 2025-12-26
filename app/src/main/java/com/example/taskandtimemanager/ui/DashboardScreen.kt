package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(dataStore: DataStore, scope: CoroutineScope) {
    var tasks by remember { mutableStateOf(emptyList<Task>()) }

    LaunchedEffect(Unit) {
        tasks = dataStore.getTasks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📊 Dashboard", fontSize = 20.sp, fontWeight = LocalTextStyle.current.fontWeight)

        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.completed }
        val infiniteTasks = tasks.filter { it.infinite }

        // Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$completedTasks/$totalTasks", fontSize = 24.sp, fontWeight = LocalTextStyle.current.fontWeight)
                    Text("Tasks Done", fontSize = 12.sp)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${infiniteTasks.sumOf { it.infiniteCount }}", fontSize = 24.sp, fontWeight = LocalTextStyle.current.fontWeight)
                    Text("Infinite Done", fontSize = 12.sp)
                }
            }
        }

        Text("Pending Tasks", fontSize = 14.sp, fontWeight = LocalTextStyle.current.fontWeight)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks.filter { !it.completed }) { task ->
                DashboardTaskCard(task, onComplete = {
                    scope.launch {
                        dataStore.completeTask(task.id, task.reward)
                        tasks = dataStore.getTasks()
                    }
                }, onIncrement = {
                    scope.launch {
                        dataStore.incrementInfiniteTask(task.id)
                        tasks = dataStore.getTasks()
                    }
                })
            }
        }
    }
}

@Composable
fun DashboardTaskCard(task: Task, onComplete: () -> Unit, onIncrement: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontSize = 14.sp, fontWeight = LocalTextStyle.current.fontWeight)
                if (task.mandatory) {
                    Text("⚠️ Mandatory", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
                if (task.infinite) {
                    Text("Repeats: ${task.infiniteCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("${task.reward} coins", fontSize = 11.sp)
                }
            }

            if (task.infinite) {
                Button(onClick = onIncrement) {
                    Text("✓")
                }
            } else {
                Button(onClick = onComplete) {
                    Text("Complete")
                }
            }
        }
    }
}
