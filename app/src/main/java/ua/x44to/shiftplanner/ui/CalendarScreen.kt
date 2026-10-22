package ua.x44to.shiftplanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Store
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val CENTER = 1200

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val data by Store.data.collectAsStateWithLifecycle()
    val base = remember { YearMonth.now() }
    val pager = rememberPagerState(initialPage = CENTER, pageCount = { CENTER * 2 })
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var patternDialog by remember { mutableStateOf(false) }
    val month = base.plusMonths((pager.currentPage - CENTER).toLong())

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // заголовок місяця
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Text(
                    L.monthTitle(month),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(CENTER) } }) {
                    Icon(Icons.Filled.Today, contentDescription = L.today)
                }
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            WeekHeader(data.settings.weekStartsMonday)
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
                MonthGrid(
                    month = base.plusMonths((page - CENTER).toLong()),
                    data = data,
                    mondayFirst = data.settings.weekStartsMonday,
                    onDay = { selected = it },
                )
            }
            FilledTonalButton(
                onClick = { patternDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).height(52.dp),
            ) {
                Icon(Icons.Filled.Repeat, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(L.applyPattern, style = MaterialTheme.typography.titleSmall)
            }
            TodayCard(data)
            MonthSummary(month, data)
            Spacer(Modifier.height(16.dp))
        }
    }

    selected?.let { date ->
        DaySheet(date = date, data = data, onDismiss = { selected = null })
    }
    if (patternDialog) {
        ApplyPatternDialog(
            data = data,
            defaultStart = if (month == YearMonth.now()) LocalDate.now() else month.atDay(1),
            onDismiss = { patternDialog = false },
        )
    }
}

private fun orderedWeek(mondayFirst: Boolean): List<DayOfWeek> {
    val first = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    return (0L until 7L).map { first.plus(it) }
}

@Composable
private fun WeekHeader(mondayFirst: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        for (d in orderedWeek(mondayFirst)) {
            val weekend = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY
            Text(
                L.weekday(d),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (weekend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, data: AppData, mondayFirst: Boolean, onDay: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val weekStart = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val lead = ((first.dayOfWeek.value - weekStart.value) + 7) % 7
    val gridStart = first.minusDays(lead.toLong())
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        for (w in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val date = gridStart.plusDays((w * 7 + i).toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        data = data,
                        modifier = Modifier.weight(1f),
                        onClick = { onDay(date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate, inMonth: Boolean, isToday: Boolean, data: AppData,
    modifier: Modifier, onClick: () -> Unit,
) {
    val entry = data.entry(date)
    val type = data.type(entry.shiftId)
    val cs = MaterialTheme.colorScheme
    val bg = type?.let { shiftColor(it.color) }
    val alpha = if (inMonth) 1f else 0.35f
    Box(
        modifier
            .padding(2.dp)
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg?.copy(alpha = 0.9f * alpha) ?: cs.surfaceContainerLow.copy(alpha = alpha))
            .then(if (isToday) Modifier.border(2.dp, cs.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        val fg = if (bg != null) onShiftColor(bg) else cs.onSurface
        Text(
            date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = fg.copy(alpha = if (inMonth) 1f else 0.6f),
        )
        if (type != null) {
            Text(
                type.short,
                modifier = Modifier.align(Alignment.Center).padding(top = 10.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
            )
        }
        if (entry.note.isNotBlank()) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(5.dp).size(6.dp).clip(CircleShape)
                    .background(if (bg != null) fg else cs.tertiary),
            )
        }
    }
}

@Composable
private fun TodayCard(data: AppData) {
    val today = LocalDate.now()
    val t = data.shiftOn(today)
    val next = data.nextShift(today.plusDays(1))
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(L.today, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            if (t != null) {
                ShiftLine(t.name, t.timeRange() + " · " + L.hours(t.hours()), shiftColor(t.color))
            } else {
                Text(L.dayOff, style = MaterialTheme.typography.titleMedium)
            }
            val note = data.entry(today).note
            if (note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                Text(L.next, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                val (d, nt) = next
                val inDays = ChronoUnit.DAYS.between(today, d)
                ShiftLine(nt.name, L.dayShort(d) + " · " + nt.timeRange() + if (inDays > 1) " · +$inDays" else "",
                    shiftColor(nt.color))
            }
        }
    }
}

@Composable
fun ShiftLine(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthSummary(month: YearMonth, data: AppData) {
    val counts = linkedMapOf<String, Int>()
    var hours = 0.0
    val notes = mutableListOf<Pair<LocalDate, String>>()
    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        val e = data.entry(date)
        data.type(e.shiftId)?.let {
            counts[it.id] = (counts[it.id] ?: 0) + 1
            hours += it.hours()
        }
        if (e.note.isNotBlank()) notes += date to e.note
    }
    if (counts.isEmpty() && notes.isEmpty()) return
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(L.monthTitle(month), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            for ((id, n) in counts) {
                val t = data.type(id) ?: continue
                ShiftLine("${t.name} × $n", L.hours(t.hours() * n), shiftColor(t.color))
            }
            if (counts.isNotEmpty()) {
                Text("${L.totalHours}: ${L.hours(hours)}", style = MaterialTheme.typography.titleSmall)
            }
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(L.notesThisMonth, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                for ((d, n) in notes) {
                    Text("${d.dayOfMonth} — $n", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
