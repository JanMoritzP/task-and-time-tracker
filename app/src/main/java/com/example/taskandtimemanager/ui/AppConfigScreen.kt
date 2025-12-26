package com.example.taskandtimemanager.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.AdminReceiver
import com.example.taskandtimemanager.model.AppData
import com.example.taskandtimemanager.data.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AppConfigScreen(
    dataStore: DataStore,
    scope: CoroutineScope,
    context: Context,
    devicePolicyManager: DevicePolicyManager
) {
    var apps by remember { mutableStateOf(emptyList<AppData>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var adminGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = dataStore.getApps()
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        adminGranted = devicePolicyManager.isAdminActive(adminComponent)
    }

    if (showAddDialog) {
        AddAppDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { app ->
                scope.launch {
                    dataStore.addApp(app)
                    apps = dataStore.getApps()
                    showAddDialog = false
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!adminGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ Admin Access Required", fontSize = 14.sp)
                    Button(
                        onClick = {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                            intent.putExtra(
                                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                ComponentName(context, AdminReceiver::class.java)
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Grant Access")
                    }
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("+ Add App")
        }

        LazyColumn {
            items(apps) { app ->
                Card(modifier = Modifier.padding(16.dp, 4.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(app.name, fontSize = 14.sp, fontWeight = androidx.compose.material3.LocalTextStyle.current.fontWeight)
                        Text("Package: ${app.packageName}", fontSize = 10.sp)
                        Text("Hard Cap: ${app.hardCap} min/day", fontSize = 10.sp)
                        Text("Cost: ${app.costPerMinute}/min", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAppDialog(onDismiss: () -> Unit, onAdd: (AppData) -> Unit) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var hardCap by remember { mutableStateOf("") }
    var costPerMinute by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("App Name") })
                TextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package Name") })
                TextField(value = hardCap, onValueChange = { hardCap = it }, label = { Text("Hard Cap (min)") })
                TextField(value = costPerMinute, onValueChange = { costPerMinute = it }, label = { Text("Cost/min") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val app = AppData(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    packageName = packageName,
                    hardCap = hardCap.toIntOrNull() ?: 0,
                    costPerMinute = costPerMinute.toFloatOrNull() ?: 0f
                )
                onAdd(app)
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