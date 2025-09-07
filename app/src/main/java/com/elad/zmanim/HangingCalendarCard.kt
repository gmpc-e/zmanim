package com.elad.zmanim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Minimal UI model mirroring the Figma widget content. */
data class DayUi(
    val hebrewDay: String,       // e.g., "יום שישי"
    val hebrewDate: String,      // e.g., "י\"ג באלול ה'תשפ\"ה"
    val gregorianDay: String,    // e.g., "Friday"
    val gregorianDate: String,   // e.g., "06/09/2025"
    val sunrise: String,         // "06:18"
    val sunset: String,          // "19:01"
    val occasions: String? = null,
    val isShabbat: Boolean = false
)

/** Figma-like hanging calendar card used in app mode. */
@Composable
fun HangingCalendarCard(
    day: DayUi,
    modifier: Modifier = Modifier
) {
    val strap = Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFFCFD8DC)))
    val headerGrad = Brush.verticalGradient(listOf(Color(0xFFFFF7E6), Color(0xFFFFECB3)))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth()) {
            // “Hanging strap“
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(strap)
            )

            // Header: Hebrew + Gregorian
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGrad)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    day.hebrewDay,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(day.hebrewDate, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${day.gregorianDay} • ${day.gregorianDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            // Rows: sunrise / sunset
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.sunrise, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.LightMode, contentDescription = "Sunrise")
                    }
                    Text("זריחה", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.sunset, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.DarkMode, contentDescription = "Sunset")
                    }
                    Text("שקיעה", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Occasions / notes
            day.occasions?.let { notes ->
                if (notes.isNotBlank()) {
                    Divider()
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // Shabbat badge
            if (day.isShabbat) {
                Box(
                    Modifier
                        .padding(bottom = 14.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("שבת קודש", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8D6E63))
                    }
                }
            }
        }
    }
}
