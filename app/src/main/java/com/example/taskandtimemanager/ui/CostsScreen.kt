package com.example.taskandtimemanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import com.example.taskandtimemanager.ui.NeoButton
import com.example.taskandtimemanager.ui.NeoCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.AppUsageAggregate
import com.example.taskandtimemanager.model.AppUsagePurchase
import com.example.taskandtimemanager.model.TrackedApp
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "App costs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Configure time budgets and prices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Coin balance: $coinBalance", fontSize = 14.sp)
                }
            }

            NeoCard(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(apps) { app ->
                        val appAggregates = aggregates.filter { it.appId == app.id }
                        val minutesUsed = appAggregates.sumOf { it.usedMinutesAutomatic }
                        val appPurchases = purchases[app.id].orEmpty()
                        val minutesPurchased = appPurchases.sumOf { it.minutesPurchased }
                        val minutesRemaining = minutesPurchased - minutesUsed

                        var costPerMinuteInput by remember(app.id) {
                            mutableStateOf(
                                app.costPerMinute.takeIf { it > 0f }?.toString() ?: "",
                            )
                        }
                        var showBuyDialog by remember { mutableStateOf(false) }

                        NeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                                Text(
                                    "Purchased: $minutesPurchased min, Used today: $minutesUsed min, Remaining: ${minutesRemaining.coerceAtLeast(0)} min",
                                    fontSize = 12.sp,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = costPerMinuteInput,
                                        onValueChange = { input ->
                                            costPerMinuteInput =
                                                input.filter { ch -> ch.isDigit() || ch == '.' }
                                        },
                                        label = { Text("coins/min") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                        ),
                                    )
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val newCost = costPerMinuteInput.toFloatOrNull() ?: 0f
                                                dataStore.updateTrackedApp(app.copy(costPerMinute = newCost))
                                                val updatedApps = dataStore.getTrackedApps()
                                                apps = updatedApps
                                            }
                                        },
                                    ) {
                                        Text("Save")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Button(onClick = { showBuyDialog = true }) {
                                        Text("Buy time")
                                    }
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
                                                    val purchase =
                                                        dataStore.buyAppTimeIfEnoughCoins(
                                                            app.id,
                                                            minutesToBuy,
                                                        )
                                                    if (purchase != null) {
                                                        val updatedPurchases =
                                                            dataStore.getAppUsagePurchases(app.id)
                                                        purchases = purchases.toMutableMap().apply {
                                                            put(app.id, updatedPurchases)
                                                        }
                                                        coinBalance = dataStore.getCoinBalance()
                                                        showBuyDialog = false
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }
                            }
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
                    onValueChange = { input ->
                        minutesInput = input.filter { ch -> ch.isDigit() }
                    },
                    label = { Text("Minutes to buy") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutesToBuy = minutesInput.toLongOrNull() ?: 0L
                    val coinsToSpend = (minutesToBuy * app.costPerMinute).toInt()
                    onPurchase(minutesToBuy, coinsToSpend)
                },
            ) {
                Text("Buy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
