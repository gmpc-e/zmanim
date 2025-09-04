package com.elad.zmanim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZmanimApp() }
    }
}

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ZmanimApp() {
    // RTL UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var offset by remember { mutableStateOf(0) }
        var selectedCity by remember { mutableStateOf(Cities.all.first()) }
        var settings by remember { mutableStateOf(AppSettings(board = BoardPreset.GRA)) }

        val date = LocalDate.now().plusDays(offset.toLong())
        val tz = ZoneId.of(selectedCity.tzid)

        // Daily zmanim
        val z = remember(date, selectedCity) {
            ZmanimProvider.computeAll(date, selectedCity.lat, selectedCity.lon, tz)
        }

        // Shabbat card data (+ countdown)
        var shabbat by remember { mutableStateOf<ShabbatSummary?>(null) }
        LaunchedEffect(selectedCity, settings.candleOffsetMinutes) {
            shabbat = ShabbatRepository.fetchUpcoming(selectedCity, tz, settings.candleOffsetMinutes)
        }

        // If you want RT72 Havdalah precisely, compute via Zmanim for next Shabbat:
        val nextShabbatDate = remember { nextShabbat(LocalDate.now(tz)) }
        val rt72FromZmanim: ZonedDateTime? = remember(selectedCity, nextShabbatDate) {
            try {
                val r = ZmanimProvider.computeAll(nextShabbatDate, selectedCity.lat, selectedCity.lon, tz)
                // Compose Zoned from LocalTime + date+tz (note: this is approximate if DST flips nearby)
                r.tzeitRT72?.atDate(nextShabbatDate)?.atZone(tz)
            } catch (_: Throwable) { null }
        }

        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5)),
            startY = 0f, endY = 1000f
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("זמני היום – $date", textAlign = TextAlign.Right)
                        }
                    },
                    actions = {
                        // Board/preset shown for transparency
                        AssistChip(
                            onClick = {},
                            label = { Text("שיטה: ${settings.board.display}") }
                        )
                        Spacer(Modifier.width(8.dp))

                        // City selector
                        var cityMenu by remember { mutableStateOf(false) }
                        TextButton(onClick = { cityMenu = true }) {
                            Icon(Icons.Default.LocationCity, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(selectedCity.display)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }) {
                            Cities.all.forEach { c ->
                                DropdownMenuItem(text = { Text(c.display) }, onClick = {
                                    selectedCity = c
                                    cityMenu = false
                                })
                            }
                        }

                        // Quick settings (board + candle offset)
                        var showSettings by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                        if (showSettings) {
                            AlertDialog(
                                onDismissRequest = { showSettings = false },
                                confirmButton = {
                                    TextButton(onClick = { showSettings = false }) { Text("סגור") }
                                },
                                title = { Text("הגדרות חישוב / מנהג") },
                                text = {
                                    Column {
                                        Text("שיטה ('Board'):", fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            BoardPreset.values().forEach { b ->
                                                FilterChip(
                                                    selected = settings.board == b,
                                                    onClick = { settings = settings.copy(board = b) },
                                                    label = { Text(b.display) }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text("הדלקת נרות (דקות לפני שקיעה):", fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(18, 30, 40).forEach { opt ->
                                                FilterChip(
                                                    selected = (settings.candleOffsetMinutes ?: selectedCity.defaultCandleOffsetMin) == opt,
                                                    onClick = { settings = settings.copy(candleOffsetMinutes = opt) },
                                                    label = { Text("$opt") }
                                                )
                                            }
                                            FilterChip(
                                                selected = settings.candleOffsetMinutes == null,
                                                onClick = { settings = settings.copy(candleOffsetMinutes = null) },
                                                label = { Text("ברירת מחדל עיר") }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilledTonalButton(onClick = { offset-- }) { Text("אתמול") }
                        FilledTonalButton(onClick = { offset = 0 }) { Text("היום") }
                        FilledTonalButton(onClick = { offset++ }) { Text("מחר") }
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(gradient)
                    .padding(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    AnimatedContent(
                        targetState = Triple(date, selectedCity, settings.board),
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) togetherWith
                                    fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)))
                        },
                        label = "date-city-board"
                    ) { _ ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Board name
                            item {
                                Text(
                                    "שיטה נבחרת: ${settings.board.display}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Section: Times of the day
                            item {
                                SectionCard(title = "זמני היום") {
                                    ZRow("עלות השחר", z.alosHashachar)
                                    ZRow("זמן טלית ותפילין", z.misheyakir11_5)
                                    ZRow("זריחה מישורית", z.sunriseSeaLevel)
                                    ZRow("זריחה הנראית", z.sunriseVisible)
                                    Spacer(Modifier.height(6.dp))
                                    ZRow("סו\"ז ק\"ש מג\"א", z.sofZmanShmaMGA)
                                    ZRow("סו\"ז ק\"ש גרא", z.sofZmanShmaGRA)
                                    ZRow("סו\"ז תפילה מג\"א", z.sofZmanTfilaMGA)
                                    ZRow("סו\"ז תפילה גרא", z.sofZmanTfilaGRA)
                                    Spacer(Modifier.height(6.dp))
                                    ZRow("חצות היום", z.chatzot)
                                    ZRow("מנחה גדולה", z.minchaGedola)
                                    ZRow("מנחה קטנה", z.minchaKetana)
                                    ZRow("פלג המנחה", z.plagHamincha)
                                    Spacer(Modifier.height(6.dp))
                                    ZRow("שקיעה מישורית", z.sunsetSeaLevel)
                                    ZRow("שקיעה הנראית", z.sunsetVisible)
                                    ZRow("צאת הכוכבים", z.tzeitStandard)
                                    ZRow("צאת הכוכבים לרבנו תם", z.tzeitRT72)
                                }
                            }

                            // Section: Shabbat Kodesh
                            item {
                                ShabbatCard(summary = shabbat, tz = tz, rt72FromZmanim = rt72FromZmanim)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ZRow(label: String, time: LocalTime?) {
    val timeText = time?.format(HHMM) ?: "--"
    Text(
        text = "$label: $timeText",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Right,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}
