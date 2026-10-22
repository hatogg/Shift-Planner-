package ua.x44to.shiftplanner.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Тексти інтерфейсу: українська (також для ru/be) або англійська — за мовою телефона. */
object L {
    private val ukr = Locale.getDefault().language in setOf("uk", "ru", "be")
    val locale: Locale = if (ukr) Locale.forLanguageTag("uk") else Locale.ENGLISH

    private fun t(uk: String, en: String) = if (ukr) uk else en

    val calendar get() = t("Календар", "Calendar")
    val stats get() = t("Статистика", "Statistics")
    val shifts get() = t("Зміни", "Shifts")
    val today get() = t("Сьогодні", "Today")
    val dayOff get() = t("Вихідний", "Day off")
    val note get() = t("Нотатка", "Note")
    val save get() = t("Зберегти", "Save")
    val cancel get() = t("Скасувати", "Cancel")
    val delete get() = t("Видалити", "Delete")
    val clearDay get() = t("Очистити день", "Clear day")
    val applyPattern get() = t("Заповнити шаблоном", "Fill from template")
    val apply get() = t("Застосувати", "Apply")
    val shiftTypes get() = t("Типи змін", "Shift types")
    val patterns get() = t("Шаблони графіка", "Schedule templates")
    val addShift get() = t("Додати зміну", "Add shift")
    val addPattern get() = t("Додати шаблон", "Add template")
    val editShift get() = t("Зміна", "Shift")
    val editPattern get() = t("Шаблон", "Template")
    val name get() = t("Назва", "Name")
    val shortLabel get() = t("Коротко (до 3 символів)", "Short (up to 3 chars)")
    val color get() = t("Колір", "Color")
    val start get() = t("Початок", "Start")
    val end get() = t("Кінець", "End")
    val breakMin get() = t("Перерва, хв", "Break, min")
    val steps get() = t("Кроки циклу", "Cycle steps")
    val addStep get() = t("Додати крок:", "Add step:")
    val stepsHint get() = t("Натисніть на крок, щоб прибрати його", "Tap a step to remove it")
    val fromDate get() = t("Починаючи з", "Starting")
    val forHowLong get() = t("На скільки", "For how long")
    val overwrite get() = t("Перезаписати вже заплановані дні", "Overwrite days already planned")
    val noPatterns get() = t("Спершу створіть шаблон у розділі «Зміни».", "Create a template under Shifts first.")
    val noTypes get() = t("Додайте хоча б одну зміну в розділі «Зміни».", "Add at least one shift under Shifts.")
    val totalShifts get() = t("Змін", "Shifts")
    val totalHours get() = t("Годин", "Hours")
    val earnings get() = t("Заробіток", "Earnings")
    val daysOff get() = t("Вихідних", "Days off")
    val settings get() = t("Налаштування", "Settings")
    val hourlyRate get() = t("Ставка за годину", "Hourly rate")
    val currency get() = t("Валюта", "Currency")
    val weekMonday get() = t("Тиждень починається з понеділка", "Week starts on Monday")
    val deleteTypeQ get() = t("Видалити зміну? Вона зникне з усіх днів календаря.",
        "Delete this shift? It will be removed from every day in the calendar.")
    val nothingPlanned get() = t("Нічого не заплановано", "Nothing planned")
    val next get() = t("Наступна", "Next")
    val widgetTitle get() = "Shift Planner"
    val ok get() = t("Гаразд", "OK")
    val hoursShort get() = t("год", "h")
    val minShort get() = t("хв", "min")
    val notesThisMonth get() = t("Нотатки цього місяця", "Notes this month")

    fun months(n: Int): String {
        if (!ukr) return if (n == 1) "1 month" else "$n months"
        return when (n) {
            1 -> "1 місяць"
            2, 3, 4 -> "$n місяці"
            else -> "$n місяців"
        }
    }

    fun stepsCount(n: Int) = t("$n днів у циклі", "$n days in the cycle")

    fun monthTitle(m: YearMonth): String {
        val name = m.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        return name.replaceFirstChar { it.titlecase(locale) } + " " + m.year
    }

    fun weekday(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT_STANDALONE, locale)

    fun dayTitle(d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }

    fun dayShort(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))

    fun hours(h: Double): String = if (h % 1.0 == 0.0) "${h.toLong()} $hoursShort" else "%.1f $hoursShort".format(locale, h)

    // стартові дані
    val defDay get() = t("Денна", "Day")
    val defDayShort get() = t("Д", "D")
    val defNight get() = t("Нічна", "Night")
    val defNightShort get() = t("Н", "N")
    val defMorning get() = t("Ранкова", "Morning")
    val defMorningShort get() = t("Р", "M")
    val defPattern22 get() = t("2/2 денні", "2/2 days")
    val defPatternDN get() = t("День-ніч-два вихідні", "Day-night-two off")
}
