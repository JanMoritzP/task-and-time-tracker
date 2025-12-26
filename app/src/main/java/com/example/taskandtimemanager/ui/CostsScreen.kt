package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.AppBlockerCommands
import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.TrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Composable
fun CostsScreen(
    dataStore: DataStore,
    scope: CoroutineScope,
) {
    var apps by remember { mutableStateOf<List<TrackedApp>>(emptyList()) }
    var aggregates by remember { mutableStateOf<List<AppUsageAggregate>>(emptyList()) }
    var purchases by remember { mutableStateOf<Map<String, List<AppUsagePurchase>>>(emptyMap()) }
    var coinBalance by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch {
            val today = LocalDate.now()
            val trackedApps = dataStore.getTrackedApps()
            val todaysAggregates = dataStore.getAppUsageAggregatesForDate(today)
            val purchasesByApp = trackedApps.associate { app ->
                app.id to dataStore.getAppUsagePurchases(app.id)
            }
            val balance = dataStore.getCoinBalance()

            apps = trackedApps
            aggregates = todaysAggregates
            purchases = purchasesByApp
            coinBalance = balance
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "⚙️ App Costs & Time Budget",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Text("Coin balance: $coinBalance", fontSize = 14.sp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps) { app ->
                val appAggregates = aggregates.filter { it.appId == app.id }
                val minutesUsed = appAggregates.sumOf { it.usedMinutesAutomatic }
                val appPurchases = purchases[app.id].orEmpty()
                val minutesPurchased = appPurchases.sumOf { it.minutesPurchased }
                val minutesRemaining = minutesPurchased - minutesUsed

                var costPerMinuteInput by remember(app.id) {
                    mutableStateOf(app.costPerMinute.takeIf { it > 0f }?.toString() ?: "")
                }
                var showBuyDialog by remember { mutableStateOf(false) }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Purchased: $minutesPurchased min, Used today: $minutesUsed min, Remaining: ${minutesRemaining.coerceAtLeast(0)} min",
                            fontSize = 12.sp,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = costPerMinuteInput,
                                onValueChange = { costPerMinuteInput = it },
                                label = { Text("coins/min") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Button(onClick = {
                                scope.launch {
                                    val newCost = costPerMinuteInput.toFloatOrNull() ?: 0f
                                    dataStore.updateTrackedApp(app.copy(costPerMinute = newCost))
                                    val updatedApps = dataStore.getTrackedApps()
                                    apps = updatedApps
                                }
                            }) {
                                Text("Save")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { showBuyDialog = true }) {
                            Text("Buy Time")
                        }

                        // Debug / entry point: allow manually opening the blocking overlay
                        // for this app to verify the UI even when time is not yet exhausted.
                        Spacer(modifier = Modifier.height(8.dp))
                        val context = LocalContext.current
                        OutlinedButton(onClick = {
                            // Use the shared helper to launch the overlay.
                            AppBlockerCommands.showBlockingOverlay(
                                context = context,
                                targetPackage = app.packageName,
                                targetAppName = app.name,
                            )
                        }) {
                            Text("Show Blocking Overlay for Debug")
                        }

                        if (showBuyDialog) {
                            BuyTimeDialog(
                                app = app,
                                coinBalance = coinBalance,
                                onDismiss = { showBuyDialog = false },
                                onPurchase = { minutesToBuy, coinsToSpend ->
                                    scope.launch {
                                        if (coinsToSpend <= coinBalance && minutesToBuy > 0) {
                                            // Delegate purchase and balance validation to DataStore / managers
                                            val purchase = dataStore.buyAppTimeIfEnoughCoins(app.id, minutesToBuy)
                                            if (purchase != null) {
                                                val updatedPurchases = dataStore.getAppUsagePurchases(app.id)
                                                purchases = purchases.toMutableMap().apply {
                                                    put(app.id, updatedPurchases)
                                                }
                                                coinBalance = dataStore.getCoinBalance()
                                                showBuyDialog = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyTimeDialog(
    app: TrackedApp,
    coinBalance: Int,
    onDismiss: () -> Unit,
    onPurchase: (minutesToBuy: Long, coinsToSpend: Int) -> Unit,
) {
    var minutesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buy time for ${app.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Coin balance: $coinBalance")
                Text("Cost per minute: ${app.costPerMinute} coins")
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { minutesInput = it },
                    label = { Text("Minutes to buy") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minutesToBuy = minutesInput.toLongOrNull() ?: 0L
                val coinsToSpend = (minutesToBuy * app.costPerMinute).toInt()
                onPurchase(minutesToBuy, coinsToSpend)
            }) {
                Text("Buy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
