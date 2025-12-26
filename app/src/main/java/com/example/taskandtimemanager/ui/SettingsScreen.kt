package com.example.taskandtimemanager.ui

import android.content.Context
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
import com.example.taskandtimemanager.model.OneTimeReward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID

@Composable
fun SettingsScreen(dataStore: DataStore, scope: CoroutineScope, context: Context) {
    var rewards by remember { mutableStateOf(emptyList<OneTimeReward>()) }
    var showAddRewardDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        rewards = dataStore.getOneTimeRewards()
    }

    if (showAddRewardDialog) {
        AddOneTimeRewardDialog(
            onDismiss = { showAddRewardDialog = false },
            onAdd = { reward ->
                scope.launch {
                    dataStore.addOneTimeReward(reward)
                    rewards = dataStore.getOneTimeRewards()
                    showAddRewardDialog = false
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth()) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Rewards") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Export") }
            )
        }

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showAddRewardDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("+ Add Reward")
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text("Unclaimed", fontSize = 14.sp, fontWeight = LocalTextStyle.current.fontWeight)
                        }
                        items(rewards.filter { it.claimedDate.isEmpty() }) { reward ->
                            RewardItem(reward, onClaim = {
                                scope.launch {
                                    dataStore.claimOneTimeReward(reward.id)
                                    rewards = dataStore.getOneTimeRewards()
                                }
                            })
                        }

                        item {
                            Text("Claimed", fontSize = 14.sp, fontWeight = LocalTextStyle.current.fontWeight, modifier = Modifier.padding(top = 16.dp))
                        }
                        items(rewards.filter { it.claimedDate.isNotEmpty() }) { reward ->
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(reward.name, fontSize = 12.sp)
                                    Text("${reward.coins} coins - ${reward.claimedDate} ${reward.claimedTime}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val csv = dataStore.exportToCSV()
                                val downloadsDir = context.getExternalFilesDir(null)
                                val file = File(
                                    downloadsDir,
                                    "TaskTimeManager_${LocalDate.now()}.csv"
                                )
                                file.writeText(csv)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📊 Export to CSV")
                    }
                    Text("Data exported to app files", fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
fun RewardItem(reward: OneTimeReward, onClaim: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reward.name, fontSize = 12.sp)
                Text(reward.description, fontSize = 10.sp)
                Text("${reward.coins} coins", fontSize = 11.sp, fontWeight = LocalTextStyle.current.fontWeight)
            }
            Button(onClick = onClaim, modifier = Modifier.height(36.dp)) {
                Text("Claim")
            }
        }
    }
}

@Composable
fun AddOneTimeRewardDialog(onDismiss: () -> Unit, onAdd: (OneTimeReward) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coins by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reward") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Reward Name") })
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                TextField(value = coins, onValueChange = { coins = it }, label = { Text("Coins") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val reward = OneTimeReward(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    coins = coins.toIntOrNull() ?: 0
                )
                onAdd(reward)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
