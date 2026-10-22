package ua.x44to.shiftplanner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ua.x44to.shiftplanner.MainActivity
import ua.x44to.shiftplanner.data.AppData
import ua.x44to.shiftplanner.data.L
import ua.x44to.shiftplanner.data.Store
import java.time.LocalDate

/** Віджет: сьогоднішня зміна, наступна і найближчі 7 днів кольоровою смужкою. */
class NextShiftWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context.applicationContext)
        provideContent {
            GlanceTheme {
                WidgetBody(Store.data.value)
            }
        }
    }
}

class NextShiftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextShiftWidget()
}

@Composable
private fun WidgetBody(data: AppData) {
    val today = LocalDate.now()
    val t = data.shiftOn(today)
    val next = data.nextShift(today.plusDays(1))
    val onSurface = GlanceTheme.colors.onSurface
    val muted = GlanceTheme.colors.onSurfaceVariant
    Column(
        GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp)
            .padding(14.dp).clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(L.today, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        if (t != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(10.dp).cornerRadius(5.dp).background(ColorProvider(Color(t.color.toInt())))) {}
                Spacer(GlanceModifier.width(6.dp))
                Text(t.name, style = TextStyle(color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            Text(t.timeRange(), style = TextStyle(color = muted, fontSize = 13.sp))
        } else {
            Text(L.dayOff, style = TextStyle(color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.height(6.dp))
        if (next != null) {
            val (d, nt) = next
            Text(
                "${L.next}: ${L.dayShort(d)} · ${nt.name} ${nt.timeRange()}",
                style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 2,
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val d = today.plusDays(i.toLong())
                val dt = data.shiftOn(d)
                Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(L.weekday(d.dayOfWeek), style = TextStyle(color = muted, fontSize = 10.sp))
                    Box(
                        GlanceModifier.size(22.dp).cornerRadius(6.dp).background(
                            if (dt != null) ColorProvider(Color(dt.color.toInt())) else GlanceTheme.colors.surfaceVariant,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dt?.short ?: d.dayOfMonth.toString(),
                            style = TextStyle(
                                color = if (dt != null) ColorProvider(Color.White) else muted,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
