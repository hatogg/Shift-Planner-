package ua.x44to.shiftplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Pattern
import ua.x44to.shiftplanner.data.SHIFT_COLORS
import ua.x44to.shiftplanner.data.ShiftType
import ua.x44to.shiftplanner.data.Store
import ua.x44to.shiftplanner.data.parseTime

@Composable
fun ShiftsScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    var editType by remember { mutableStateOf<ShiftType?>(null) }
    var editPattern by remember { mutableStateOf<Pattern?>(null) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        item { SectionTitle(L.shiftTypes) }
        items(data.types, key = { "t" + it.id }) { t ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editType = t }) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { ShortBadge(t) },
                    headlineContent = { Text(t.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(t.timeRange() + " · " + L.hours(t.hours()) +
                            if (t.breakMin > 0) " (−${t.breakMin} ${L.minShort})" else "")
                    },
                )
            }
        }
        item {
            FilledTonalButton(
                onClick = { editType = ShiftType(Store.newId(), "", "", SHIFT_COLORS[(data.types.size * 3) % SHIFT_COLORS.size]) },
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(L.addShift)
            }
        }
        item { Spacer(Modifier.height(12.dp)); SectionTitle(L.patterns) }
        items(data.patterns, key = { "p" + it.id }) { p ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editPattern = p }) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(p.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Column {
                            Text(L.stepsCount(p.steps.size))
                            PatternDots(p.steps, data)
                        }
                    },
                )
            }
        }
        item {
            FilledTonalButton(
                onClick = { editPattern = Pattern(Store.newId(), "", emptyList()) },
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(L.addPattern)
            }
        }
    }

    editType?.let { t ->
        ShiftTypeDialog(t, isNew = data.types.none { it.id == t.id }, onDismiss = { editType = null })
    }
    editPattern?.let { p ->
        PatternDialog(p, data, isNew = data.patterns.none { it.id == p.id }, onDismiss = { editPattern = null })
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun ShortBadge(t: ShiftType) {
    val c = shiftColor(t.color)
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c), contentAlignment = Alignment.Center) {
        Text(t.short, color = onShiftColor(c), fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ShiftTypeDialog(initial: ShiftType, isNew: Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var short by remember { mutableStateOf(initial.short) }
    var color by remember { mutableStateOf(initial.color) }
    var start by remember { mutableStateOf(initial.start) }
    var end by remember { mutableStateOf(initial.end) }
    var breakMin by remember { mutableStateOf(if (initial.breakMin > 0) initial.breakMin.toString() else "") }
    var picking by remember { mutableStateOf<String?>(null) }  // "start" / "end"
    var confirmDelete by remember { mutableStateOf(false) }

    picking?.let { which ->
        val cur = parseTime(if (which == "start") start else end)
        val state = rememberTimePickerState(initialHour = cur?.hour ?: 8, initialMinute = cur?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val v = "%02d:%02d".format(state.hour, state.minute)
                    if (which == "start") start = v else end = v
                    picking = null
                }) { Text(L.ok) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(L.cancel) } },
            text = { TimePicker(state = state) },
        )
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(L.deleteTypeQ) },
            confirmButton = {
                TextButton(onClick = {
                    Store.deleteType(initial.id)
                    onDismiss()
                }) { Text(L.delete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(L.cancel) } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L.editShift) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (short.isBlank() || short == initial.short && isNew) short = it.take(1).uppercase()
                    },
                    label = { Text(L.name) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = short, onValueChange = { short = it.take(3) },
                    label = { Text(L.shortLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text(L.color, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in SHIFT_COLORS) {
                        val col = shiftColor(c)
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(col)
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == color) Icon(Icons.Filled.Check, contentDescription = null, tint = onShiftColor(col))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = "start" }, modifier = Modifier.weight(1f)) {
                        Text("${L.start}: $start")
                    }
                    OutlinedButton(onClick = { picking = "end" }, modifier = Modifier.weight(1f)) {
                        Text("${L.end}: $end")
                    }
                }
                OutlinedTextField(
                    value = breakMin, onValueChange = { v -> breakMin = v.filter { it.isDigit() }.take(3) },
                    label = { Text(L.breakMin) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                val preview = ShiftType(initial.id, name, short, color, start, end, breakMin.toIntOrNull() ?: 0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShortBadge(preview)
                    Spacer(Modifier.width(12.dp))
                    Text(preview.timeRange() + " · " + L.hours(preview.hours()))
                }
                if (!isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(L.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    Store.upsertType(
                        ShiftType(initial.id, name.trim(), short.trim().ifBlank { name.trim().take(1) }, color, start, end,
                            breakMin.toIntOrNull() ?: 0),
                    )
                    onDismiss()
                },
            ) { Text(L.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.cancel) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatternDialog(initial: Pattern, data: AppData, isNew: Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    val steps = remember { mutableStateListOf<String?>().apply { addAll(initial.steps) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L.editPattern) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(L.name) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text("${L.steps} · ${L.stepsCount(steps.size)}", style = MaterialTheme.typography.labelLarge)
                if (steps.isNotEmpty()) {
                    Text(L.stepsHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.forEachIndexed { i, s ->
                        val t = data.type(s)
                        InputChip(
                            selected = false,
                            onClick = { steps.removeAt(i) },
                            label = { Text("${i + 1}. ${t?.short ?: L.dayOff}") },
                            leadingIcon = {
                                Box(
                                    Modifier.size(10.dp).clip(CircleShape)
                                        .background(t?.let { shiftColor(it.color) } ?: MaterialTheme.colorScheme.outlineVariant),
                                )
                            },
                        )
                    }
                }
                Text(L.addStep, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (t in data.types) {
                        AssistChip(
                            onClick = { steps.add(t.id) },
                            label = { Text(t.name) },
                            leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(shiftColor(t.color))) },
                        )
                    }
                    AssistChip(onClick = { steps.add(null) }, label = { Text(L.dayOff) })
                }
                if (!isNew) {
                    TextButton(onClick = {
                        Store.deletePattern(initial.id)
                        onDismiss()
                    }) { Text(L.delete, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && steps.isNotEmpty(),
                onClick = {
                    Store.upsertPattern(Pattern(initial.id, name.trim(), steps.toList()))
                    onDismiss()
                },
            ) { Text(L.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.cancel) } },
    )
}
