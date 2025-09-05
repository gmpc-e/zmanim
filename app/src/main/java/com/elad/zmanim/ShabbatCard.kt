package com.elad.zmanim

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ShabbatCard(
    summary: ShabbatSummary?,
    tz: ZoneId,
    rt72FromZmanim: ZonedDateTime? = null
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
            } else {
                Text(
                    "${summary.hebrewDate} • ${summary.gregDate.format(DDMMYYYY)}",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                summary.parashaHeb?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("פרשת השבוע: $it", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(12.dp))

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
            }
        }
    }
}
