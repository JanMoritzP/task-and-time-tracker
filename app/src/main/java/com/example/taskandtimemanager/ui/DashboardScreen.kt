package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import com.example.taskandtimemanager.ui.NeoButton
import com.example.taskandtimemanager.ui.NeoCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import com.example.taskandtimemanager.model.TrackedApp
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

    // Tracked apps + usage overview for quick "buy 10 minutes" actions.
    var trackedApps by remember { mutableStateOf(emptyList<TrackedApp>()) }
    var todaysAggregates by remember { mutableStateOf(emptyList<AppUsageAggregate>()) }
    var purchasesByApp by remember { mutableStateOf<Map<String, List<AppUsagePurchase>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        taskDefinitions = dataStore.getTaskDefinitions()
        todaysExecutions = dataStore.getExecutionsForDate(today)

        // Coins are derived from all history, including app usage purchases.
        coinBalance = dataStore.getCoinBalance()

        // Load tracked apps and today's usage so we can show remaining minutes on the dashboard.
        val apps = dataStore.getTrackedApps()
        val aggregates = dataStore.getAppUsageAggregatesForDate(today)
        val purchases = apps.associate { app ->
            app.id to dataStore.getAppUsagePurchases(app.id)
        }
        trackedApps = apps
        todaysAggregates = aggregates
        purchasesByApp = purchases
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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with coin balance only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Overview of today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Coins: $coinBalance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Quick purchase options, small and directly under header
            if (trackedApps.isNotEmpty()) {
                NeoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Quick app time",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        ) {
                            items(trackedApps, key = { it.id }) { app ->
                                val appAggregates: List<AppUsageAggregate> =
                                    todaysAggregates.filter { it.appId == app.id }
                                val minutesUsed: Long =
                                    appAggregates.sumOf { it.usedMinutesAutomatic }
                                val appPurchases: List<AppUsagePurchase> =
                                    purchasesByApp[app.id].orEmpty()
                                val minutesPurchased: Long =
                                    appPurchases.sumOf { it.minutesPurchased }
                                val minutesRemaining: Long =
                                    (minutesPurchased - minutesUsed).coerceAtLeast(0)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            app.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = "Remaining today: ${minutesRemaining} min",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val purchase =
                                                    dataStore.buyAppTimeIfEnoughCoins(app.id, 10L)
                                                if (purchase != null) {
                                                    val updatedPurchases =
                                                        dataStore.getAppUsagePurchases(app.id)
                                                    purchasesByApp = purchasesByApp.toMutableMap().apply {
                                                        put(app.id, updatedPurchases)
                                                    }
                                                    coinBalance = dataStore.getCoinBalance()
                                                }
                                            }
                                        },
                                    ) {
                                        Text("+10 min", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending tasks section directly after purchases
            Text(
                "Pending tasks",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            NeoCard(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pendingTasks, key = { it.id }) { def ->
                        DashboardTaskCard(
                            definition = def,
                            executionsToday = executionsByTaskId[def.id].orEmpty(),
                            onComplete = {
                                scope.launch {
                                    dataStore.completeTaskNow(def.id, skipped = false)
                                    val today = LocalDate.now()
                                    todaysExecutions = dataStore.getExecutionsForDate(today)
                                    coinBalance = dataStore.getCoinBalance()
                                }
                            },
                            onSkip = {
                                scope.launch {
                                    dataStore.completeTaskNow(def.id, skipped = true)
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
    }
}

@Composable
private fun DashboardTaskCard(
    definition: TaskDefinition,
    executionsToday: List<TaskExecution>,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
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
                        text = "Mandatory",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                val recurrenceLabel =
                    when (definition.recurrenceType) {
                        "ONE_TIME" -> "One-time"
                        "DAILY" -> "Daily"
                        "UNLIMITED_PER_DAY" -> "Unlimited per day"
                        "LIMITED_PER_DAY" ->
                            "Up to ${definition.maxExecutionsPerDay ?: "?"} per day"
                        else -> definition.recurrenceType
                    }

                Text(
                    text = recurrenceLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )

                val doneCountToday = executionsToday.count { it.status == "DONE" }
                val skippedCountToday = executionsToday.count { it.status == "SKIPPED" }
                if (doneCountToday > 0 || skippedCountToday > 0) {
                    val statusText = buildString {
                        if (doneCountToday > 0) append("Done: $doneCountToday")
                        if (skippedCountToday > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("Skipped: $skippedCountToday")
                        }
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                    )
                }

                Text("${definition.rewardCoins} coins", fontSize = 11.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onComplete) {
                    Text("Complete")
                }
                if (definition.mandatory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}
