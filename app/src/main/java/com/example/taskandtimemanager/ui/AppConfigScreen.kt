package com.example.taskandtimemanager.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.AdminReceiver
import com.example.taskandtimemanager.AppBlockerService
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.TrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AppConfigScreen(
    dataStore: DataStore,
    scope: CoroutineScope,
    context: Context,
    devicePolicyManager: DevicePolicyManager
) {
    var apps by remember { mutableStateOf(emptyList<TrackedApp>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var adminGranted by remember { mutableStateOf(false) }
 
    LaunchedEffect(Unit) {
        apps = dataStore.getTrackedApps()
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        adminGranted = devicePolicyManager.isAdminActive(adminComponent)
    }

    if (showAddDialog) {
        AddAppDialog(
            context = context,
            onDismiss = { showAddDialog = false },
            onAdd = { app ->
                scope.launch {
                    val tracked = TrackedApp(
                        id = app.id,
                        name = app.name,
                        packageName = app.packageName,
                        costPerMinute = app.costPerMinute,
                    )
                    dataStore.addTrackedApp(tracked)
                    apps = dataStore.getTrackedApps()
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
                            val adminComponent = ComponentName(context, AdminReceiver::class.java)
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "This app needs device admin to be able to block distracting apps."
                                )
                            }
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
                Card(
                    modifier = Modifier
                        .padding(16.dp, 4.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(app.name, fontSize = 14.sp, fontWeight = androidx.compose.material3.LocalTextStyle.current.fontWeight)
                        Text("Package: ${app.packageName}", fontSize = 10.sp)
                        Text("Cost: ${app.costPerMinute}/min", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private data class InstalledAppOption(
    val label: String,
    val packageName: String
)

@Composable
fun AddAppDialog(
    context: Context,
    onDismiss: () -> Unit,
    onAdd: (TrackedApp) -> Unit
) {
    val pm = context.packageManager
    val installedOptions = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo -> pm.getLaunchIntentForPackage(appInfo.packageName) != null }
            .sortedBy { appInfo -> pm.getApplicationLabel(appInfo).toString() }
            .map { appInfo ->
                InstalledAppOption(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName
                )
            }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<InstalledAppOption?>(null) }
    var costPerMinute by remember { mutableStateOf("1.0") }
    var hardCapMinutes by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = selectedOption?.label ?: "Select app",
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    label = { Text("App") }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    installedOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                selectedOption = option
                                expanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = costPerMinute,
                    onValueChange = { costPerMinute = it },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    label = { Text("Cost/min") }
                )

                OutlinedTextField(
                    value = if (hardCapMinutes == 0L) "" else hardCapMinutes.toString(),
                    onValueChange = { value ->
                        hardCapMinutes = value.toLongOrNull() ?: 0L
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    label = { Text("Hard cap (minutes, 0 = no cap)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val option = selectedOption ?: return@Button
                    val app = TrackedApp(
                        id = UUID.randomUUID().toString(),
                        name = option.label,
                        packageName = option.packageName,
                        costPerMinute = costPerMinute.toFloatOrNull() ?: 0f,
                        purchasedMinutesTotal = hardCapMinutes,
                        isBlocked = hardCapMinutes == 0L && (costPerMinute.toFloatOrNull() ?: 0f) <= 0f,
                    )
                    onAdd(app)
                },
                enabled = selectedOption != null
            ) {
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
