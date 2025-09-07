package com.elad.zmanim.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elad.zmanim.Cities
import com.elad.zmanim.ZmanimProvider
import com.elad.zmanim.hebrewDateFor
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ZmanimWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }
}

class ZmanimWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZmanimWidget()
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ZmanimWidget().update(context, glanceId)
    }
}

@Composable
private fun WidgetContent() {
    val city = Cities.all.first()
    val tz: ZoneId = ZoneId.of(city.tzid)
    val date = LocalDate.now(tz)
    val z = ZmanimProvider.computeAll(date, city.lat, city.lon, tz, city.elevationMeters)
    val hhmm = DateTimeFormatter.ofPattern("HH:mm")
    val hebDate = hebrewDateFor(date, inIsrael = true)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.End
    ) {
        Text(
            "לוח היום – ${city.display}",
            style = TextStyle(color = GlanceTheme.colors.onSurface)
        )
        Text(
            hebDate,
            style = TextStyle(color = GlanceTheme.colors.secondary)
        )
        Spacer(GlanceModifier.size(8.dp))
        Text("זריחה: ${z.sunriseSeaLevel?.format(hhmm) ?: "--"}")
        Text("שקיעה: ${z.sunsetSeaLevel?.format(hhmm) ?: "--"}")
        Text("צאת:   ${z.tzeitStandard?.format(hhmm) ?: "--"}")
        Spacer(GlanceModifier.size(6.dp))
        Text(
            "הקש/י לרענון",
            style = TextStyle(color = GlanceTheme.colors.onSecondary)
        )
    }
}
