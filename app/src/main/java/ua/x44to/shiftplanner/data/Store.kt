package ua.x44to.shiftplanner.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ua.x44to.shiftplanner.widget.NextShiftWidget
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Усі дані — один JSON-файл у пам'яті застосунку. Невеликий, тож читаємо й пишемо цілком. */
object Store {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data
    private var file: File? = null
    private lateinit var app: Context

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        app = context.applicationContext
        val f = File(app.filesDir, "data.json")
        file = f
        _data.value = try {
            if (f.exists()) json.decodeFromString(AppData.serializer(), f.readText()) else defaults()
        } catch (e: Exception) {
            defaults()
        }
        if (!f.exists()) save(_data.value)
    }

    fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        _data.value = next
        save(next)
        scope.launch {
            try {
                NextShiftWidget().updateAll(app)
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    private fun save(d: AppData) {
        val f = file ?: return
        try {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), d))
            if (!tmp.renameTo(f)) {
                f.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }

    // ───────── зміни даних ─────────
    fun setDay(date: LocalDate, entry: DayEntry) = update { d ->
        val key = date.toString()
        d.copy(days = if (entry.isEmpty()) d.days - key else d.days + (key to entry))
    }

    fun upsertType(t: ShiftType) = update { d ->
        val exists = d.types.any { it.id == t.id }
        d.copy(types = if (exists) d.types.map { if (it.id == t.id) t else it } else d.types + t)
    }

    /** Видаляє тип зміни: із днів (нотатки лишаються) і з кроків шаблонів (крок стає вихідним). */
    fun deleteType(id: String) = update { d ->
        d.copy(
            types = d.types.filter { it.id != id },
            days = d.days.mapValues { (_, e) -> if (e.shiftId == id) e.copy(shiftId = null) else e }
                .filterValues { !it.isEmpty() },
            patterns = d.patterns.map { p -> p.copy(steps = p.steps.map { if (it == id) null else it }) },
        )
    }

    fun upsertPattern(p: Pattern) = update { d ->
        val exists = d.patterns.any { it.id == p.id }
        d.copy(patterns = if (exists) d.patterns.map { if (it.id == p.id) p else it } else d.patterns + p)
    }

    fun deletePattern(id: String) = update { d -> d.copy(patterns = d.patterns.filter { it.id != id }) }

    /** Розкладає цикл шаблону по днях від [from] на [days] днів. Нотатки не чіпає. */
    fun applyPattern(p: Pattern, from: LocalDate, days: Int, overwrite: Boolean) = update { d ->
        if (p.steps.isEmpty()) return@update d
        val map = d.days.toMutableMap()
        for (i in 0 until days) {
            val date = from.plusDays(i.toLong())
            val key = date.toString()
            val cur = map[key] ?: DayEntry()
            if (!overwrite && cur.shiftId != null) continue
            val next = cur.copy(shiftId = p.steps[i % p.steps.size])
            if (next.isEmpty()) map.remove(key) else map[key] = next
        }
        d.copy(days = map)
    }

    fun setSettings(s: Settings) = update { d -> d.copy(settings = s) }

    fun newId(): String = UUID.randomUUID().toString().take(8)

    private fun defaults(): AppData {
        val day = ShiftType(newId(), L.defDay, L.defDayShort, 0xFFF59E0B, "08:00", "20:00")
        val night = ShiftType(newId(), L.defNight, L.defNightShort, 0xFF5E35B1, "20:00", "08:00")
        val morning = ShiftType(newId(), L.defMorning, L.defMorningShort, 0xFF00897B, "07:00", "15:00")
        return AppData(
            types = listOf(day, night, morning),
            patterns = listOf(
                Pattern(newId(), L.defPattern22, listOf(day.id, day.id, null, null)),
                Pattern(newId(), L.defPatternDN, listOf(day.id, night.id, null, null)),
            ),
        )
    }
}
