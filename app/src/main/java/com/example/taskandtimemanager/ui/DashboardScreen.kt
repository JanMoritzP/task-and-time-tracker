package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Dashboard screen backed by TaskDefinition / TaskExecution and derived coin balance.
 */
@Composable
fun DashboardScreen(dataStore: DataStore, scope: CoroutineScope) {
    var taskDefinitions by remember { mutableStateOf(emptyList<TaskDefinition>()) }
    var todaysExecutions by remember { mutableStateOf(emptyList<TaskExecution>()) }
    var coinBalance by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        taskDefinitions = dataStore.getTaskDefinitions()
        todaysExecutions = dataStore.getExecutionsForDate(today)
        coinBalance = dataStore.getCoinBalance()
    }

    // Recompute stats derived from definitions + executions
    val activeTaskDefinitions = taskDefinitions.filter { !it.archived }
    val executionsByTaskId = todaysExecutions.groupBy { it.taskDefinitionId }

    val tasksDoneCount = activeTaskDefinitions.count { def ->
        executionsByTaskId[def.id]?.any { it.status == "DONE" } == true
    }
    val totalTasksCount = activeTaskDefinitions.size
    val totalExecutionsToday = todaysExecutions.size

    // Determine which tasks are still relevant/pending for today
    val pendingTasks = activeTaskDefinitions.filter { def ->
        val executionsForTask = executionsByTaskId[def.id].orEmpty()
        when (def.recurrenceType) {
            "ONE_TIME" -> executionsForTask.none { it.status == "DONE" }
            "DAILY" -> executionsForTask.none { it.status == "DONE" }
            "UNLIMITED_PER_DAY" -> true // always show; user can complete multiple times
            "LIMITED_PER_DAY" -> {
                val doneCount = executionsForTask.count { it.status == "DONE" }
                val maxPerDay = def.maxExecutionsPerDay ?: Int.MAX_VALUE
                doneCount < maxPerDay
            }
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("📊 Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$tasksDoneCount/$totalTasksCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Tasks Done", fontSize = 12.sp)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$totalExecutionsToday",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Executions Today", fontSize = 12.sp)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Coins: $coinBalance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Balance", fontSize = 12.sp)
                }
            }
        }

        Text("Pending Tasks", fontSize = 14.sp, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(pendingTasks, key = { it.id }) { def ->
                DashboardTaskCard(
                    definition = def,
                    executionsToday = executionsByTaskId[def.id].orEmpty(),
                    onComplete = {
                        scope.launch {
                            // Create a DONE execution for now and refresh state
                            dataStore.completeTaskNow(def.id)
                            val today = LocalDate.now()
                            todaysExecutions = dataStore.getExecutionsForDate(today)
                            coinBalance = dataStore.getCoinBalance()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DashboardTaskCard(
    definition: TaskDefinition,
    executionsToday: List<TaskExecution>,
    onComplete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(definition.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (definition.mandatory) {
                    Text(
                        text = "⚠️ Mandatory",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                val recurrenceLabel = when (definition.recurrenceType) {
                    "ONE_TIME" -> "One-time"
                    "DAILY" -> "Daily"
                    "UNLIMITED_PER_DAY" -> "Unlimited per day"
                    "LIMITED_PER_DAY" -> "Up to ${definition.maxExecutionsPerDay ?: "?"} per day"
                    else -> definition.recurrenceType
                }

                Text(
                    text = recurrenceLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )

                val doneCountToday = executionsToday.count { it.status == "DONE" }
                if (doneCountToday > 0) {
                    Text(
                        text = "Done today: $doneCountToday",
                        fontSize = 11.sp,
                    )
                }

                Text("${definition.rewardCoins} coins", fontSize = 11.sp)
            }

            Button(onClick = onComplete) {
                Text("Complete")
            }
        }
    }
}
