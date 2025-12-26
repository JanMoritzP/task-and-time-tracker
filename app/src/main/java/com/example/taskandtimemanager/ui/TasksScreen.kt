package com.example.taskandtimemanager.ui

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import com.example.taskandtimemanager.ui.NeoButton
import com.example.taskandtimemanager.ui.NeoCard
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskandtimemanager.data.DataStore
import com.example.taskandtimemanager.model.TaskCategoryDefaults
import com.example.taskandtimemanager.model.TaskDefinition
import com.example.taskandtimemanager.model.TaskExecution
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(dataStore: DataStore, scope: CoroutineScope, context: Context) {

    var tasks by remember { mutableStateOf(emptyList<TaskDefinition>()) }
    var executionsToday by remember { mutableStateOf(emptyMap<String, List<TaskExecution>>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDefinition?>(null) }

    LaunchedEffect(Unit) {
        tasks = dataStore.getTaskDefinitions().filter { !it.archived }
        val today = LocalDate.now()
        val todaysExecutions =
            dataStore.getExecutionsForDate(today)
                .groupBy { it.taskDefinitionId }
        executionsToday = todaysExecutions
    }

    if (showAddDialog) {
        AddTaskDialog(
            task = editingTask,
            onDismiss = {
                showAddDialog = false
                editingTask = null
            },
            onAdd = { task ->
                scope.launch {
                    if (editingTask == null) {
                        dataStore.addTaskDefinition(task)
                    } else {
                        dataStore.updateTaskDefinition(task)
                    }
                    tasks = dataStore.getTaskDefinitions().filter { !it.archived }
                    showAddDialog = false
                    editingTask = null
                }
            },
        )
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
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Manage task definitions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        editingTask = null
                        showAddDialog = true
                    },
                ) {
                    Text("Add task")
                }
            }

            NeoCard(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    // Category-based grouping intentionally removed; all tasks shown in a flat list.
                    items(tasks) { task ->
                        val taskExecutionsToday = executionsToday[task.id].orEmpty()
                        TaskEditCard(
                            task = task,
                            executionsToday = taskExecutionsToday,
                            onEdit = {
                                editingTask = task
                                showAddDialog = true
                            },
                            onArchive = {
                                scope.launch {
                                    dataStore.archiveTaskDefinition(task.id)
                                    tasks = dataStore.getTaskDefinitions().filter { !it.archived }
                                }
                            },
                            onDeletePermanently = {
                                scope.launch {
                                    // TODO: add explicit permanent delete support to DataStore if still required
                                    // For now, archive only to keep history.
                                    dataStore.archiveTaskDefinition(task.id)
                                    tasks = dataStore.getTaskDefinitions().filter { !it.archived }
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
fun TaskEditCard(
    task: TaskDefinition,
    executionsToday: List<TaskExecution>,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDeletePermanently: () -> Unit,
) {
    val executionsCount = executionsToday.size
    val coinsEarnedToday = executionsToday.sumOf { it.coinsAwarded }

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.name,
                        fontSize = 14.sp,
                        fontWeight = LocalTextStyle.current.fontWeight,
                    )
                    Text(
                        text =
                        "Reward: ${task.rewardCoins} coins · Recurrence: ${task.recurrenceType}" +
                            (task.maxExecutionsPerDay?.let { " · Max/day: $it" } ?: ""),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Today: $executionsCount executions · $coinsEarnedToday coins",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (task.mandatory) {
                        Text(
                            "Mandatory",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Button(
                    onClick = onEdit,
                ) {
                    Text("Edit", fontSize = 10.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onArchive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("Archive", fontSize = 10.sp)
                }
                Button(
                    onClick = onDeletePermanently,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(task: TaskDefinition?, onDismiss: () -> Unit, onAdd: (TaskDefinition) -> Unit) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var mandatory by remember { mutableStateOf(task?.mandatory ?: false) }
    // For new tasks, default the base reward field to "0" instead of "null".
    var rewardCoins by remember {
        mutableStateOf(
            if (task == null) {
                "0"
            } else {
                task.rewardCoins.toString()
            },
        )
    }
    var recurringRewardCoins by remember { mutableStateOf(task?.recurringRewardCoins?.toString() ?: "") }
    var recurrenceType by remember { mutableStateOf(task?.recurrenceType ?: "DAILY") }
    var maxExecutionsPerDay by remember { mutableStateOf(task?.maxExecutionsPerDay?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task != null) "Edit task" else "Add task") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = mandatory,
                        onCheckedChange = { mandatory = it },
                    )
                    Text("Mandatory")
                }
                TextField(
                    value = rewardCoins,
                    onValueChange = { rewardCoins = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Base reward coins (first completion per day)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                TextField(
                    value = recurringRewardCoins,
                    onValueChange = { recurringRewardCoins = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Recurring reward coins (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                // Simple text-based recurrence type for now to avoid heavy UI changes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = recurrenceType == "UNLIMITED_PER_DAY",
                        onCheckedChange = { checked ->
                            recurrenceType = if (checked) "UNLIMITED_PER_DAY" else "DAILY"
                            if (!checked) {
                                maxExecutionsPerDay = ""
                            }
                        },
                    )
                    Text("Infinite executions per day")
                }
                if (recurrenceType == "LIMITED_PER_DAY") {
                    TextField(
                        value = maxExecutionsPerDay,
                        onValueChange = { maxExecutionsPerDay = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max executions per day") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newTask = TaskDefinition(
                        id = task?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        // Category is intentionally hard-coded to LEGACY and not shown in the UI.
                        category = TaskCategoryDefaults.DEFAULT_TASK_CATEGORY,
                        mandatory = mandatory,
                        rewardCoins = rewardCoins.toIntOrNull() ?: 0,
                        recurringRewardCoins = recurringRewardCoins.toIntOrNull(),
                        recurrenceType = recurrenceType,
                        maxExecutionsPerDay = maxExecutionsPerDay.toIntOrNull(),
                        archived = task?.archived ?: false,
                    )
                    onAdd(newTask)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
