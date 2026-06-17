package com.example.taskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ─── Data ────────────────────────────────────────────────────────────────────

enum class Priority(val label: String, val color: Color) {
    LOW("Low", Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFF9800)),
    HIGH("High", Color(0xFFF44336))
}

data class Task(
    val id: Int,
    val text: String,
    val note: String = "",
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false
)

// Theme

private val BackgroundDark = Color(0xFF0F1117)
private val SurfaceDark    = Color(0xFF1A1D26)
private val CardDark       = Color(0xFF222535)
private val AccentViolet   = Color(0xFF7C6AF7)
private val AccentBlue     = Color(0xFF4F9CF9)
private val OnSurfaceLight = Color(0xFFE8EAF6)
private val SubtleGray     = Color(0xFF6B7280)

private val AppColorScheme = darkColorScheme(
    background        = BackgroundDark,
    surface           = SurfaceDark,
    surfaceVariant    = CardDark,
    primary           = AccentViolet,
    onPrimary         = Color.White,
    onBackground      = OnSurfaceLight,
    onSurface         = OnSurfaceLight,
    onSurfaceVariant  = SubtleGray,
    error             = Color(0xFFEF5350),
)

// ─── Activity ────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskManagerApp()
                }
            }
        }
    }
}

// ─── Root Composable ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp() {
    val taskList = remember { mutableStateListOf<Task>() }
    var idCounter by remember { mutableIntStateOf(0) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask   by remember { mutableStateOf<Task?>(null) }
    var deleteTarget  by remember { mutableStateOf<Task?>(null) }

    // Filter
    var activeFilter by remember { mutableStateOf<Boolean?>(null) } // null=all, true=done, false=pending

    val filteredList = when (activeFilter) {
        true  -> taskList.filter { it.done }
        false -> taskList.filter { !it.done }
        else  -> taskList.toList()
    }

    // Dialogs
    if (showAddDialog) {
        TaskDialog(
            task = null,
            onDismiss = { showAddDialog = false },
            onSave = { text, note, priority ->
                taskList.add(Task(idCounter++, text, note, priority))
                showAddDialog = false
            }
        )
    }

    editingTask?.let { task ->
        TaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { text, note, priority ->
                val idx = taskList.indexOfFirst { it.id == task.id }
                if (idx >= 0) taskList[idx] = task.copy(text = text, note = note, priority = priority)
                editingTask = null
            }
        )
    }

    deleteTarget?.let { task ->
        ConfirmDeleteDialog(
            taskText = task.text,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                taskList.remove(task)
                deleteTarget = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header ──
            AppHeader(
                total   = taskList.size,
                done    = taskList.count { it.done },
                pending = taskList.count { !it.done }
            )

            // ── Filter chips ──
            FilterRow(
                activeFilter = activeFilter,
                onFilterChange = { activeFilter = it }
            )

            // ── Task list ──
            if (filteredList.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    isFiltered = activeFilter != null
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredList, key = { _, task -> task.id }) { _, task ->
                        TaskCard(
                            task     = task,
                            onToggle = {
                                val idx = taskList.indexOfFirst { it.id == task.id }
                                if (idx >= 0) taskList[idx] = task.copy(done = !task.done)
                            },
                            onEdit   = { editingTask = task },
                            onDelete = { deleteTarget = task }
                        )
                    }
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
fun AppHeader(total: Int, done: Int, pending: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AccentViolet.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text = "My Tasks",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceLight,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (total == 0) "Nothing here yet — tap + to begin"
                else "$pending pending · $done done",
                fontSize = 13.sp,
                color = SubtleGray
            )
            if (total > 0) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = AccentViolet,
                    trackColor = CardDark
                )
            }
        }
    }
}

// Filter Row

@Composable
fun FilterRow(activeFilter: Boolean?, onFilterChange: (Boolean?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("All",     null,  Icons.AutoMirrored.Outlined.List),
            Triple("Pending", false, Icons.Outlined.RadioButtonUnchecked),
            Triple("Done",    true,  Icons.Outlined.CheckCircle)
        ).forEach { (label, filter, icon) ->
            val selected = activeFilter == filter
            FilterChip(
                selected = selected,
                onClick  = { onFilterChange(filter) },
                label    = { Text(label, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor      = AccentViolet.copy(alpha = 0.25f),
                    selectedLabelColor          = AccentViolet,
                    selectedLeadingIconColor    = AccentViolet,
                    containerColor              = CardDark,
                    labelColor                  = SubtleGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled         = true,
                    selected        = selected,
                    selectedBorderColor = AccentViolet.copy(alpha = 0.6f),
                    borderColor         = Color(0xFF2E3245),
                    borderWidth         = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

// ─── Task Card ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true }
            else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFB71C1C))
                        )
                    )
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority stripe
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(task.priority.color.copy(alpha = if (task.done) 0.3f else 1f))
                )

                Spacer(Modifier.width(12.dp))

                // Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (task.done) AccentViolet else SubtleGray,
                            shape = CircleShape
                        )
                        .background(if (task.done) AccentViolet else Color.Transparent)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = task.done,
                        enter   = scaleIn(tween(150)) + fadeIn(tween(150)),
                        exit    = scaleOut(tween(100)) + fadeOut(tween(100))
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint   = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.done) SubtleGray else OnSurfaceLight,
                        textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.note.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = task.note,
                            fontSize = 12.sp,
                            color = SubtleGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    PriorityBadge(priority = task.priority, dimmed = task.done)
                }

                Spacer(Modifier.width(8.dp))

                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = SubtleGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority, dimmed: Boolean) {
    val color = priority.color.copy(alpha = if (dimmed) 0.4f else 1f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Task Dialog (Add / Edit) ─────────────────────────────────────────────────

@Composable
fun TaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (String, String, Priority) -> Unit
) {
    var text     by remember { mutableStateOf(task?.text     ?: "") }
    var note     by remember { mutableStateOf(task?.note     ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    val isEdit = task != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEdit) "Edit Task" else "New Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SubtleGray)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Task input
                DialogTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Task *",
                    placeholder = "What needs to be done?",
                    maxLines = 3
                )

                Spacer(Modifier.height(12.dp))

                // Note input
                DialogTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (optional)",
                    placeholder = "Add a note…",
                    maxLines = 2
                )

                Spacer(Modifier.height(20.dp))

                // Priority
                Text("Priority", fontSize = 13.sp, color = SubtleGray, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        PriorityButton(
                            priority = p,
                            selected = priority == p,
                            onClick  = { priority = p },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = {
                        if (text.trim().isNotEmpty()) onSave(text.trim(), note.trim(), priority)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    enabled = text.trim().isNotEmpty()
                ) {
                    Icon(
                        if (isEdit) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEdit) "Save Changes" else "Add Task",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    maxLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AccentViolet,
            unfocusedBorderColor = Color(0xFF2E3245),
            focusedTextColor     = OnSurfaceLight,
            unfocusedTextColor   = OnSurfaceLight,
            cursorColor          = AccentViolet,
            focusedLabelColor    = AccentViolet,
            unfocusedLabelColor  = SubtleGray,
            focusedContainerColor   = CardDark,
            unfocusedContainerColor = CardDark,
            focusedPlaceholderColor = SubtleGray.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = SubtleGray.copy(alpha = 0.4f)
        )
    )
}

@Composable
fun PriorityButton(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) priority.color.copy(alpha = 0.2f) else CardDark
            )
            .border(
                width = 1.5.dp,
                color = if (selected) priority.color else Color(0xFF2E3245),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) priority.color else SubtleGray
        )
    }
}

// ─── Confirm Delete Dialog ────────────────────────────────────────────────────

@Composable
fun ConfirmDeleteDialog(
    taskText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Delete task?", fontWeight = FontWeight.Bold, color = OnSurfaceLight, fontSize = 18.sp)
        },
        text = {
            Text(
                "\"$taskText\" will be permanently removed.",
                color = SubtleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SubtleGray)
            }
        }
    )
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
fun EmptyState(modifier: Modifier = Modifier, isFiltered: Boolean) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AccentViolet.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Outlined.FilterList else Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = AccentViolet.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (isFiltered) "No tasks here" else "All clear!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceLight
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isFiltered) "No tasks match this filter."
            else "Tap + to add your first task.",
            fontSize = 14.sp,
            color = SubtleGray,
            textAlign = TextAlign.Center
        )
    }
}