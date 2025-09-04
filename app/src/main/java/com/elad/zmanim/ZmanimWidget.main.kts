#!/usr/bin/env kotlin

package com.elad.koshercalendar

import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.*

class ZmanimWidget : GlanceAppWidget() {
    override suspend fun Content() {
        val now = LocalDate.now()
        val zman = ZmanimProvider.compute(
            now,
            32.1559, // Hod Hasharon lat
            34.8880, // Hod Hasharon lon
            ZoneId.of("Asia/Jerusalem")
        )

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("היום • ${now}", style = TextStyle(color = ColorProvider(R.color.black)))
            Spacer(8.dp)
            Text("נץ: ${zman.netz ?: "--"}")
            Text("חצות: ${zman.chatzot ?: "--"}")
            Text("מנחה: ${zman.minchaKetana ?: "--"}")
            Text("שקיעה: ${zman.shkia ?: "--"}")
            Text("צאת: ${zman.tzeit ?: "--"}")
        }
    }
}

class ZmanimWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZmanimWidget()
}
