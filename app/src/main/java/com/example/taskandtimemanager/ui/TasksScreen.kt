package com.example.taskandtimemanager.ui

import android.content.Context
import androidx.compose.animation.animateContentSize
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
import java.util.UUID

@Composable
fun TasksScreen(dataStore: DataStore, scope: CoroutineScope, context: Context) {
    var tasks by remember { mutableStateOf(emptyList<Task>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(Unit) {
        tasks = dataStore.getTasks()
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
                    dataStore.addTask(task)
                    tasks = dataStore.getTasks()
                    showAddDialog = false
                    editingTask = null
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { 
                editingTask = null
                showAddDialog = true 
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("+ Add Task")
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val groupedTasks = tasks.groupBy { it.category }
            groupedTasks.forEach { (category, categoryTasks) ->
                item {
                    Text(
                        category,
                        fontSize = 14.sp,
                        fontWeight = LocalTextStyle.current.fontWeight,
                        modifier = Modifier.padding(16.dp, 8.dp)
                    )
                }
                
                items(categoryTasks) { task ->
                    TaskEditCard(
                        task = task,
                        onEdit = {
                            editingTask = task
                            showAddDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                dataStore.deleteTask(task.id)
                                tasks = dataStore.getTasks()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskEditCard(task: Task, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 4.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.name, fontSize = 14.sp, fontWeight = LocalTextStyle.current.fontWeight)
                    Text(task.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    if (task.infinite) {
                        Text("Repeats: ${task.infiniteCount}", fontSize = 12.sp)
                    }
                    if (task.mandatory) {
                        Text("⚠️ Mandatory", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                
                Button(onClick = onEdit, modifier = Modifier.width(70.dp).height(36.dp)) {
                    Text("Edit", fontSize = 10.sp)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDelete, modifier = Modifier.weight(1f).height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(task: Task?, onDismiss: () -> Unit, onAdd: (Task) -> Unit) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "") }
    var mandatory by remember { mutableStateOf(task?.mandatory ?: false) }
    var reward by remember { mutableStateOf(task?.reward?.toString() ?: "") }
    var infinite by remember { mutableStateOf(task?.infinite ?: false) }
    var infiniteName by remember { mutableStateOf(task?.infiniteName ?: "") }
    var infiniteReward by remember { mutableStateOf(task?.infiniteReward?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task != null) "Edit Task" else "Add Task") },
        text = {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                TextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = mandatory, onCheckedChange = { mandatory = it })
                    Text("Mandatory")
                }
                if (!infinite) {
                    TextField(value = reward, onValueChange = { reward = it }, label = { Text("Reward coins") }, singleLine = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = infinite, onCheckedChange = { infinite = it })
                    Text("Infinite")
                }
                if (infinite) {
                    TextField(value = infiniteName, onValueChange = { infiniteName = it }, label = { Text("Per-repeat name") }, singleLine = true)
                    TextField(value = infiniteReward, onValueChange = { infiniteReward = it }, label = { Text("Per-repeat reward") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newTask = Task(
                    id = task?.id ?: UUID.randomUUID().toString(),
                    name = name,
                    mandatory = mandatory,
                    reward = reward.toIntOrNull() ?: 0,
                    category = category,
                    infinite = infinite,
                    infiniteName = infiniteName,
                    infiniteReward = infiniteReward.toIntOrNull() ?: 1,
                    completed = task?.completed ?: false,
                    infiniteCount = task?.infiniteCount ?: 0
                )
                onAdd(newTask)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
