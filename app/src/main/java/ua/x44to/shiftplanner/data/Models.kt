package ua.x44to.shiftplanner.data

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Тип зміни: «Денна 08:00–20:00» тощо. color — ARGB. */
@Serializable
data class ShiftType(
    val id: String,
    val name: String,
    val short: String,
    val color: Long,
    val start: String = "08:00",
    val end: String = "20:00",
    val breakMin: Int = 0,
) {
    /** Тривалість у годинах; зміна, що закінчується наступного дня (20:00–08:00), рахується правильно. */
    fun hours(): Double {
        val s = parseTime(start) ?: return 0.0
        val e = parseTime(end) ?: return 0.0
        var minutes = Duration.between(s, e).toMinutes()
        if (minutes <= 0) minutes += 24 * 60
        return (minutes - breakMin).coerceAtLeast(0) / 60.0
    }

    fun timeRange(): String = "$start–$end"
}

/** Що стоїть у дні: зміна (або нічого — вихідний) і нотатка. */
@Serializable
data class DayEntry(
    val shiftId: String? = null,
    val note: String = "",
) {
    fun isEmpty() = shiftId == null && note.isBlank()
}

/** Шаблон графіка: цикл кроків, null — вихідний. Напр. [Д, Д, Н, Н, null, null]. */
@Serializable
data class Pattern(
    val id: String,
    val name: String,
    val steps: List<String?>,
)

@Serializable
data class Settings(
    val hourlyRate: Double = 0.0,
    val currency: String = "₴",
    val weekStartsMonday: Boolean = true,
)

@Serializable
data class AppData(
    val types: List<ShiftType> = emptyList(),
    val days: Map<String, DayEntry> = emptyMap(),
    val patterns: List<Pattern> = emptyList(),
    val settings: Settings = Settings(),
    val version: Int = 1,
) {
    fun type(id: String?): ShiftType? = if (id == null) null else types.firstOrNull { it.id == id }

    fun entry(date: LocalDate): DayEntry = days[date.toString()] ?: DayEntry()

    fun shiftOn(date: LocalDate): ShiftType? = type(entry(date).shiftId)

    /** Найближчий день, починаючи з [from], у якому стоїть зміна (до року вперед). */
    fun nextShift(from: LocalDate): Pair<LocalDate, ShiftType>? {
        for (i in 0..366) {
            val d = from.plusDays(i.toLong())
            val t = shiftOn(d)
            if (t != null) return d to t
        }
        return null
    }
}

fun parseTime(s: String): LocalTime? = try {
    LocalTime.parse(if (s.length == 4) "0$s" else s)
} catch (e: Exception) {
    null
}

/** Кольори для змін — помітні й у світлій, і в темній темі. */
val SHIFT_COLORS: List<Long> = listOf(
    0xFFF59E0B, 0xFFEF6C00, 0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1,
    0xFF3949AB, 0xFF1E88E5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342,
    0xFF6D4C41, 0xFF546E7A,
)
