package ua.x44to.shiftplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.DayEntry
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Store
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Нижня панель дня: вибір зміни і нотатка. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DaySheet(date: LocalDate, data: AppData, onDismiss: () -> Unit) {
    val entry = data.entry(date)
    var shiftId by remember(date) { mutableStateOf(entry.shiftId) }
    var note by remember(date) { mutableStateOf(entry.note) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun save() {
        Store.setDay(date, DayEntry(shiftId, note.trim()))
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = { save() }, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(L.dayTitle(date), style = MaterialTheme.typography.titleLarge)
            if (data.types.isEmpty()) {
                Text(L.noTypes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (t in data.types) {
                    FilterChip(
                        selected = shiftId == t.id,
                        onClick = { shiftId = if (shiftId == t.id) null else t.id },
                        label = { Text("${t.name} · ${t.timeRange()}") },
                        leadingIcon = {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(shiftColor(t.color)))
                        },
                    )
                }
                FilterChip(
                    selected = shiftId == null,
                    onClick = { shiftId = null },
                    label = { Text(L.dayOff) },
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(L.note) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    shiftId = null
                    note = ""
                    save()
                }) { Text(L.clearDay) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { save() }) { Text(L.save) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Заповнити календар циклом шаблону. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplyPatternDialog(data: AppData, defaultStart: LocalDate, onDismiss: () -> Unit) {
    var patternId by remember { mutableStateOf(data.patterns.firstOrNull()?.id) }
    var start by remember { mutableStateOf(defaultStart) }
    var months by remember { mutableIntStateOf(3) }
    var overwrite by remember { mutableStateOf(true) }
    var pickDate by remember { mutableStateOf(false) }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = start.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { start = it.toLocalDateUtc() }
                    pickDate = false
                }) { Text(L.ok) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(L.cancel) } },
        ) {
            DatePicker(state = state)
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L.applyPattern) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.patterns.isEmpty()) {
                    Text(L.noPatterns)
                }
                for (p in data.patterns) {
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = patternId == p.id, onClick = { patternId = p.id }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = patternId == p.id, onClick = { patternId = p.id })
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            PatternDots(p.steps, data)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(L.fromDate, style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { pickDate = true }) { Text(L.dayTitle(start)) }
                Text(L.forHowLong, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in listOf(1, 2, 3, 6, 12)) {
                        FilterChip(selected = months == n, onClick = { months = n }, label = { Text(L.months(n)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { overwrite = !overwrite }) {
                    Text(L.overwrite, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = patternId != null,
                onClick = {
                    val p = data.patterns.firstOrNull { it.id == patternId }
                    if (p != null) {
                        val days = java.time.temporal.ChronoUnit.DAYS.between(start, start.plusMonths(months.toLong())).toInt()
                        Store.applyPattern(p, start, days, overwrite)
                    }
                    onDismiss()
                },
            ) { Text(L.apply) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.cancel) } },
    )
}

/** Цикл шаблону маленькими кольоровими крапками (вихідний — порожнє коло). */
@Composable
fun PatternDots(steps: List<String?>, data: AppData) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        for (s in steps.take(21)) {
            val t = data.type(s)
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background(t?.let { shiftColor(it.color) } ?: MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
