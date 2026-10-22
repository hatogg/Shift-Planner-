package ua.x44to.shiftplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Store
import java.time.YearMonth

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val st = remember(data, month) { monthStats(data, month) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                L.monthTitle(month), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(L.totalShifts, st.shifts.toString(), Modifier.weight(1f))
            StatTile(L.totalHours, L.hours(st.hours), Modifier.weight(1f))
            StatTile(L.daysOff, st.daysOff.toString(), Modifier.weight(1f))
        }
        if (data.settings.hourlyRate > 0) {
            Spacer(Modifier.height(8.dp))
            StatTile(
                L.earnings,
                "%,.0f %s".format(L.locale, st.hours * data.settings.hourlyRate, data.settings.currency),
                Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (st.byType.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val max = st.byType.maxOf { it.second }
                    for ((t, n) in st.byType) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ShortBadge(t)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.name, style = MaterialTheme.typography.titleSmall)
                                    Text("$n × ${t.timeRange()} · ${L.hours(t.hours() * n)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp)).background(shiftColor(t.color)),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(L.nothingPlanned, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(20.dp))
        SettingsCard(data)
        Spacer(Modifier.height(24.dp))
    }
}

private class MonthStats(
    val shifts: Int,
    val hours: Double,
    val daysOff: Int,
    val byType: List<Pair<ua.x44to.shiftplanner.data.ShiftType, Int>>,
)

private fun monthStats(data: AppData, month: YearMonth): MonthStats {
    val counts = linkedMapOf<String, Int>()
    var hours = 0.0
    var off = 0
    for (d in 1..month.lengthOfMonth()) {
        val t = data.shiftOn(month.atDay(d))
        if (t == null) {
            off++
        } else {
            counts[t.id] = (counts[t.id] ?: 0) + 1
            hours += t.hours()
        }
    }
    val byType = data.types.mapNotNull { t -> counts[t.id]?.let { t to it } }
    return MonthStats(counts.values.sum(), hours, off, byType)
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsCard(data: AppData) {
    val s = data.settings
    var rate by remember(s.hourlyRate) { mutableStateOf(if (s.hourlyRate > 0) s.hourlyRate.toString().removeSuffix(".0") else "") }
    var currency by remember(s.currency) { mutableStateOf(s.currency) }
    SectionTitle(L.settings)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rate,
                    onValueChange = { v ->
                        rate = v.replace(',', '.').filter { it.isDigit() || it == '.' }.take(9)
                        Store.setSettings(s.copy(hourlyRate = rate.toDoubleOrNull() ?: 0.0))
                    },
                    label = { Text(L.hourlyRate) }, singleLine = true, modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { v ->
                        currency = v.take(4)
                        Store.setSettings(s.copy(currency = currency))
                    },
                    label = { Text(L.currency) }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    Store.setSettings(s.copy(weekStartsMonday = !s.weekStartsMonday))
                },
            ) {
                Text(L.weekMonday, modifier = Modifier.weight(1f))
                Switch(checked = s.weekStartsMonday, onCheckedChange = { Store.setSettings(s.copy(weekStartsMonday = it)) })
            }
        }
    }
}
