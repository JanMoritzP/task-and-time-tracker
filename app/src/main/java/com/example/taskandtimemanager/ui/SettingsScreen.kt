package com.example.taskandtimemanager.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import com.example.taskandtimemanager.ui.NeoButton
import com.example.taskandtimemanager.ui.NeoCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.RewardDefinition
import com.example.taskandtimemanager.model.RewardRedemption
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    dataStore: DataStore,
    scope: CoroutineScope,
    context: Context,
) {
    var rewardDefinitions by remember { mutableStateOf(emptyList<RewardDefinition>()) }
    var rewardRedemptions by remember { mutableStateOf(emptyList<RewardRedemption>()) }
    var coinBalance by remember { mutableStateOf(0) }
    var showAddRewardDialog by remember { mutableStateOf(false) }
    var showSingleUseRewardDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }
    var exportImportStatus by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        rewardDefinitions = dataStore.getRewardDefinitions()
        rewardRedemptions = dataStore.getRewardRedemptions()
        coinBalance = dataStore.getCoinBalance()
    }

    if (showAddRewardDialog) {
        AddRewardDefinitionDialog(
            onDismiss = { showAddRewardDialog = false },
            onAdd = { reward ->
                scope.launch {
                    dataStore.addRewardDefinition(reward)
                    rewardDefinitions = dataStore.getRewardDefinitions()
                    showAddRewardDialog = false
                }
            },
        )
    }

    if (showSingleUseRewardDialog) {
        SingleUseRewardDialog(
            onDismiss = { showSingleUseRewardDialog = false },
            onConfirm = { name, coins ->
                scope.launch {
                    if (coins > 0) {
                        dataStore.grantSingleUseRewardFromSettings(name, coins)
                        coinBalance = dataStore.getCoinBalance()
                        // Also reflect in executions‑based history by refetching redemptions if needed
                        rewardRedemptions = dataStore.getRewardRedemptions()
                    }
                    showSingleUseRewardDialog = false
                }
            },
        )
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        val json = dataStore.exportState()
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(json)
                                writer.flush()
                            }
                        } ?: error("Unable to open output stream")
                    }.onSuccess {
                        exportImportStatus = "Export successful"
                    }.onFailure { e ->
                        exportImportStatus = "Export failed: ${e.message}"
                    }
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                val content = reader.readText()
                                dataStore.importState(content)
                            }
                        } ?: error("Unable to open input stream")
                    }.onSuccess {
                        exportImportStatus = "Import successful"
                        rewardDefinitions = dataStore.getRewardDefinitions()
                        rewardRedemptions = dataStore.getRewardRedemptions()
                        coinBalance = dataStore.getCoinBalance()
                    }.onFailure { e ->
                        exportImportStatus = "Import failed: ${e.message}"
                    }
                }
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
                Text(text = "Settings", fontSize = 20.sp)
                Text(text = "Rewards, data export, and app blocking", fontSize = 12.sp)
            }

            TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Rewards") },
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Export / Import") },
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("App blocking") },
                )
            }

            when (activeTab) {
                0 ->
                    RewardsTabContent(
                        rewardDefinitions = rewardDefinitions,
                        rewardRedemptions = rewardRedemptions,
                        coinBalanceState = remember { mutableStateOf(coinBalance) },
                        onRedeem = { rewardId ->
                            scope.launch {
                                val redemption = dataStore.redeemReward(rewardId)
                                if (redemption != null) {
                                    rewardRedemptions = dataStore.getRewardRedemptions()
                                    coinBalance = dataStore.getCoinBalance()
                                }
                            }
                        },
                        onAddRewardClicked = { showAddRewardDialog = true },
                        onSingleUseRewardClicked = { showSingleUseRewardDialog = true },
                    )

                1 ->
                    ExportImportTabContent(
                        onExportClick = {
                            exportLauncher.launch("task_time_manager_export.json")
                        },
                        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                        statusText = exportImportStatus,
                    )

                2 -> AppBlockingSettingsTab()
            }
        }
    }
}

@Composable
private fun RewardsTabContent(
    rewardDefinitions: List<RewardDefinition>,
    rewardRedemptions: List<RewardRedemption>,
    coinBalanceState: MutableState<Int>,
    onRedeem: (String) -> Unit,
    onAddRewardClicked: () -> Unit,
    onSingleUseRewardClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Coin balance: ${coinBalanceState.value}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddRewardClicked) {
                        Text("Add reward")
                    }
                    Button(onClick = onSingleUseRewardClicked) {
                        Text("Single use reward")
                    }
                }
            }
        }

        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Available", fontSize = 14.sp)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(rewardDefinitions.filter { !it.archived }) { reward ->
                        RewardDefinitionItem(
                            reward = reward,
                            onRedeem = { onRedeem(reward.id) },
                        )
                    }
                }
            }
        }

        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "History", fontSize = 14.sp)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(rewardRedemptions) { redemption ->
                        RewardRedemptionItem(
                            redemption = redemption,
                            rewardDefinitions = rewardDefinitions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardDefinitionItem(
    reward: RewardDefinition,
    onRedeem: () -> Unit,
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reward.name, fontSize = 12.sp)
                Text(reward.description, fontSize = 10.sp)
                Text("Cost: ${reward.coinCost} coins", fontSize = 11.sp)
            }
            Button(onClick = onRedeem, modifier = Modifier.padding(start = 8.dp)) {
                Text("Redeem")
            }
        }
    }
}

@Composable
private fun RewardRedemptionItem(
    redemption: RewardRedemption,
    rewardDefinitions: List<RewardDefinition>,
) {
    val rewardName =
        rewardDefinitions.find { it.id == redemption.rewardDefinitionId }?.name
            ?: "Unknown reward"

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(rewardName, fontSize = 12.sp)
            Text("Coins spent: ${redemption.coinsSpent}", fontSize = 11.sp)
            Text("When: ${redemption.redemptionDateTime}", fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRewardDefinitionDialog(
    onDismiss: () -> Unit,
    onAdd: (RewardDefinition) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coinCost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reward") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reward name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = coinCost,
                    onValueChange = { coinCost = it },
                    label = { Text("Coin cost") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = coinCost.toIntOrNull() ?: 0
                    if (name.isNotBlank() && cost > 0) {
                        val reward =
                            RewardDefinition(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                coinCost = cost,
                            )
                        onAdd(reward)
                    }
                },
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleUseRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var coins by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Single use reward") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reward name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = coins,
                    onValueChange = { coins = it },
                    label = { Text("Coins to grant") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = coins.toIntOrNull() ?: 0
                    if (amount > 0) {
                        onConfirm(name, amount)
                    }
                },
            ) {
                Text("Grant")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ExportImportTabContent(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    statusText: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export data")
                }
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import data")
                }
                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBlockingSettingsTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "App blocking configuration",
                    fontSize = 14.sp,
                )
                Text(
                    text = "Tracked apps and limits are configured in the App Config tab.",
                    fontSize = 12.sp,
                )
                Text(
                    text = "Global caps are not available yet; each app is configured individually.",
                    fontSize = 12.sp,
                )
            }
        }
    }
}
