package ua.x44to.shiftplanner

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.DayEntry
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Pattern
import ua.x44to.shiftplanner.data.Settings
import ua.x44to.shiftplanner.data.ShiftType
import ua.x44to.shiftplanner.data.Store
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth

/**
 * Не перевірка, а фотосесія: проходить екрани з демо-даними й зберігає скриншоти в files/screens
 * (звідти їх забирає .github/screenshots.sh). Українською та англійською, у світлій і темній темі.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx get() = inst.targetContext

    @Test
    fun screenshots() {
        for (lang in listOf("uk", "en")) {
            L.override = lang
            shell("cmd uimode night no")
            Store.init(ctx)
            Store.update { demo() }
            ActivityScenario.launch(MainActivity::class.java).use {
                settle()
                shot("$lang-light-calendar")
                tap("18")
                settle()
                shot("$lang-light-day")
                tap(L.save)
                settle()
                tap(L.applyPattern)
                settle()
                shot("$lang-light-template")
                tap(L.cancel)
                settle()
                tap(L.stats)
                settle()
                shot("$lang-light-stats")
                tap(L.shifts)
                settle()
                shot("$lang-light-shifts")
                compose.onAllNodesWithText(L.defDay, useUnmergedTree = true)[0].performClick()
                settle()
                shot("$lang-light-shift-edit")
                tap(L.cancel)
                settle()
            }
            shell("cmd uimode night yes")
            ActivityScenario.launch(MainActivity::class.java).use {
                settle()
                shot("$lang-dark-calendar")
                tap(L.stats)
                settle()
                shot("$lang-dark-stats")
                tap(L.shifts)
                settle()
                shot("$lang-dark-shifts")
            }
        }
        shell("cmd uimode night no")
        L.override = null
    }

    /** Натиснути на текст: шукаємо в повному дереві (у FAB текст не зливається з кнопкою), тап — у центр тексту. */
    private fun tap(text: String) {
        compose.onAllNodesWithText(text, useUnmergedTree = true)[0].performClick()
    }

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(1500)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        val bmp: Bitmap = inst.uiAutomation.takeScreenshot() ?: error("no screenshot for $name")
        val dir = File(ctx.filesDir, "screens").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shell(cmd: String) {
        inst.uiAutomation.executeShellCommand(cmd).use { pfd ->
            java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
        }
    }

    /** Місяць, схожий на справжній: цикл день-ніч-два вихідні, кілька ранкових, нотатки. */
    private fun demo(): AppData {
        val uk = L.override == "uk"
        val day = ShiftType("d", L.defDay, L.defDayShort, 0xFFF59E0B, "08:00", "20:00")
        val night = ShiftType("n", L.defNight, L.defNightShort, 0xFF5E35B1, "20:00", "08:00")
        val morning = ShiftType("m", L.defMorning, L.defMorningShort, 0xFF00897B, "07:00", "15:00", 30)
        val cycle = listOf("d", "n", null, null)
        val month = YearMonth.now()
        val start = month.atDay(1).minusDays(10)
        val days = mutableMapOf<String, DayEntry>()
        for (i in 0 until 60) {
            val date = start.plusDays(i.toLong())
            val id = cycle[i % cycle.size] ?: continue
            days[date.toString()] = DayEntry(id)
        }
        for (d in listOf(9, 10)) days[month.atDay(d).toString()] = DayEntry("m")
        fun note(d: Int, text: String) {
            val key = month.atDay(d).toString()
            days[key] = (days[key] ?: DayEntry()).copy(note = text)
        }
        note(18, if (uk) "Обмінятися змінами з Олею" else "Swap shifts with Anna")
        note(25, if (uk) "Медогляд о 10:00" else "Medical check at 10:00")
        note(LocalDate.now().dayOfMonth.coerceIn(1, month.lengthOfMonth()), if (uk) "Взяти ключі від складу" else "Pick up the warehouse keys")
        return AppData(
            types = listOf(day, night, morning),
            days = days,
            patterns = listOf(
                Pattern("p1", L.defPatternDN, cycle),
                Pattern("p2", L.defPattern22, listOf("d", "d", null, null)),
                Pattern("p3", if (uk) "Ранкові 5/2" else "Mornings 5/2", listOf("m", "m", "m", "m", "m", null, null)),
            ),
            settings = if (uk) Settings(hourlyRate = 150.0, currency = "₴") else Settings(hourlyRate = 18.0, currency = "$"),
        )
    }
}
