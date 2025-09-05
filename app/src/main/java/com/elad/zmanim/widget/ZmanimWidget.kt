package com.elad.zmanim.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.elad.zmanim.Cities
import com.elad.zmanim.ZmanimProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ZmanimWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val city = Cities.all.first()
        val tz = ZoneId.of(city.tzid)
        val date = LocalDate.now(tz)
        val z = ZmanimProvider.computeAll(date, city.lat, city.lon, tz, city.elevationMeters)
        val hhmm = DateTimeFormatter.ofPattern("HH:mm")

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .padding(12.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.End
        ) {
            Text(
                "זמני היום – ${city.display}",
                style = TextStyle(color = ColorProvider(Color(0xFF111111)))
            )
            Spacer(modifier = GlanceModifier.size(8.dp))
            Text("זריחה: ${z.sunriseVisible?.format(hhmm) ?: "--"}")
            Text("שקיעה: ${z.sunsetVisible?.format(hhmm) ?: "--"}")
            Text("צאת:   ${z.tzeitStandard?.format(hhmm) ?: "--"}")
            Spacer(modifier = GlanceModifier.size(8.dp))
            Text(
                "הקש/י לרענון",
                style = TextStyle(color = ColorProvider(Color(0xFF777777)))
            )
        }
    }
}

class ZmanimWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ZmanimWidget()
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { /* trigger recomposition */ }
        ZmanimWidget().update(context, glanceId)
    }
}
