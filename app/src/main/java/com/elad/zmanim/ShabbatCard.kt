package com.elad.zmanim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart // fallback if missing: use CalendarToday
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ShabbatCard(
    summary: ShabbatSummary?,
    tz: ZoneId,
    rt72FromZmanim: ZonedDateTime? = null // if you compute RT via your ZmanimProvider for accuracy, pass here
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "זמני שבת קודש",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.CalendarToday, contentDescription = null)
            }

            Spacer(Modifier.height(8.dp))

            if (summary == null) {
                Text("…טוען נתוני שבת", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                return
            }

            // Hebrew + Gregorian date for the Shabbat day (Saturday)
            Text(
                "${summary.hebrewDate} • ${summary.gregDate.format(DDMMYYYY)}",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Parasha
            summary.parashaHeb?.let {
                Spacer(Modifier.height(4.dp))
                Text("פרשת השבוע: $it", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }

            // (Optional) Pirkei Avot and Haftarah lines – can be filled if/when you source them
            // Text("פרקי אבות: —", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            // Text("(הפטרה): —", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))

            // Times (candle-lighting, havdalah, RT)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("הדלקת נרות", fontWeight = FontWeight.SemiBold)
                    Text(summary.candleLighting?.format(HHMM) ?: "--")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("צאת השבת", fontWeight = FontWeight.SemiBold)
                    Text(summary.havdalah?.format(HHMM) ?: "--")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("צאת ר״ת", fontWeight = FontWeight.SemiBold)
                    Text(rt72FromZmanim?.format(HHMM) ?: "--")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Countdown chip to next candle-lighting
            val nowZ = remember { mutableStateOf(ZonedDateTime.now(tz)) }
            LaunchedEffect(summary.candleLighting?.toInstant(), tz) {
                while (true) {
                    nowZ.value = ZonedDateTime.now(tz)
                    kotlinx.coroutines.delay(1000)
                }
            }
            val remaining = summary.candleLighting?.let { Duration.between(nowZ.value, it) }

            AnimatedVisibility(
                visible = remaining != null && !remaining.isNegative,
                enter = fadeIn(), exit = fadeOut()
            ) {
                remaining?.let { dur ->
                    val (d, h, m, s) = breakdown(dur)
                    Text(
                        text = "זמן להדלקת נרות: ${if (d > 0) "$d ימים " else ""}%02d:%02d:%02d".format(h, m, s),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun breakdown(d: Duration): Quad<Int, Int, Int, Int> {
    var secs = d.seconds
    val days = (secs / 86_400).toInt()
    secs %= 86_400
    val hours = (secs / 3600).toInt()
    secs %= 3600
    val minutes = (secs / 60).toInt()
    val seconds = (secs % 60).toInt()
    return Quad(days, hours, minutes, seconds)
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D) {
    operator fun component1() = a
    operator fun component2() = b
    operator fun component3() = c
    operator fun component4() = d
}
