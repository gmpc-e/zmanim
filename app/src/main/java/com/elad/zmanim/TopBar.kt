package com.elad.zmanim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ShabbatTopBar(
    board: BoardPreset,
    onBoardChange: (BoardPreset) -> Unit,
    city: City,
    onCityChange: (City) -> Unit,
    date: LocalDate,
    tz: ZoneId,
    candleLighting: ZonedDateTime?,
    onAbout: () -> Unit = {}
) {
    val headerGrad = Brush.verticalGradient(listOf(Color(0xFFFFF7E6), Color(0xFFFFECB3)))

    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(headerGrad)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFE082), Color(0xFFFFD54F))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                AnimatedCandles(modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(6.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth() // ✅ fix: stretch column
                ) {
                    Text(
                        "זמן עד הדלקת נרות",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val line = countdownLine(tz, candleLighting)
                    Text(
                        line,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (candleLighting != null && !isNegative(tz, candleLighting)) {
                        Text(
                            "(${candleLighting.format(DateTimeFormatter.ofPattern("HH:mm"))})",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth().alpha(0.85f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Row 2: controls + dates + about
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        BoardDropdown(board = board, onBoardChange = onBoardChange)
                        CityDropdown(city = city, onCityChange = onCityChange)
                    }
                }

                val hebText = remember(date) { hebrewDateFor(date, inIsrael = true) }
                val gregText = remember(date) { date.format(DDMMYYYY) }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(hebText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(gregText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.alpha(0.9f))
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onAbout,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("About") }
                }
            }
        }
    }
}

@Composable
fun BoardDropdown(
    board: BoardPreset,
    onBoardChange: (BoardPreset) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) { Text("שיטה: ${board.display}") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        BoardPreset.values().forEach { b ->
            DropdownMenuItem(text = { Text(b.display) }, onClick = { onBoardChange(b); open = false })
        }
    }
}

@Composable
fun CityDropdown(
    city: City,
    onCityChange: (City) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) { Text("עיר: ${city.display}") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Cities.all.forEach { c ->
            DropdownMenuItem(text = { Text(c.display) }, onClick = { onCityChange(c); open = false })
        }
    }
}
