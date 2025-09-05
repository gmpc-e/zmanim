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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var offset by remember { mutableStateOf(0) }
        var selectedCity by remember { mutableStateOf(Cities.all.first()) }
        var settings by remember { mutableStateOf(AppSettings(board = BoardPreset.GRA)) }
        var selectedTab by remember { mutableStateOf(0) } // 0=זמני היום, 1=זמני שבת, 2=סיפור לשבת

        val tz = ZoneId.of(selectedCity.tzid)
        val date = LocalDate.now(tz).plusDays(offset.toLong())

        // Include board in the key so recomputation happens when changing method.
        val z = remember(date, selectedCity, settings.board) {
            ZmanimProvider.computeAll(
                date,
                selectedCity.lat,
                selectedCity.lon,
                tz,
                selectedCity.elevationMeters
            )
        }

        var shabbat by remember { mutableStateOf<ShabbatSummary?>(null) }
        LaunchedEffect(selectedCity, settings.candleOffsetMinutes) {
            shabbat = ShabbatRepository.fetchUpcoming(selectedCity, tz, settings.candleOffsetMinutes)
        }

        val nextShabbatDate = remember(tz) { nextShabbat(LocalDate.now(tz)) }
        val rt72FromZmanim: ZonedDateTime? = remember(selectedCity, nextShabbatDate) {
            runCatching {
                val r = ZmanimProvider.computeAll(nextShabbatDate, selectedCity.lat, selectedCity.lon, tz)
                r.tzeitRT72?.atDate(nextShabbatDate)?.atZone(tz)
            }.getOrNull()
        }

        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5)),
            startY = 0f, endY = 1200f
        )

        LaunchedEffect(selectedCity, date) {
            logMisheyakirFor(
                date = date,
                lat = selectedCity.lat,
                lon = selectedCity.lon,
                tz = ZoneId.of(selectedCity.tzid),
                elevationMeters = selectedCity.elevationMeters
            )
        }

        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // back (previous day)
                        IconButton(onClick = { offset-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "יום לפני")
                        }
                        // today (center)
                        FilledTonalButton(onClick = { offset = 0 }) {
                            Text("היום")
                        }
                        // forward (next day)
                        IconButton(onClick = { offset++ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "יום הבא")
                        }
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
                    ) { (d, city, board) ->
                        Column(Modifier.fillMaxSize().padding(12.dp)) {

                            CandleHeaderBox(
                                candleLighting = shabbat?.candleLighting,
                                tz = tz,
                                city = city

                            )

                            Spacer(Modifier.height(8.dp))

                            ControlsBox(
                                city = city,
                                onCityChange = { selectedCity = it },
                                board = board,
                                onBoardChange = { settings = settings.copy(board = it) }
                            )

                            Spacer(Modifier.height(10.dp))

                            // Tabs: Times of Day / Shabbat / Story
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("זמני היום") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("זמני שבת") }
                                )
                                Tab(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    text = { Text("סיפור לשבת") }
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            when (selectedTab) {
                                0 -> TimesOfDayCard(
                                    date = d,
                                    tz = tz,
                                    z = z,
                                    board = board,
                                    modifier = Modifier.weight(1f)
                                )
                                1 -> ShabbatCard(
                                    summary = shabbat,
                                    tz = tz,
                                    rt72FromZmanim = rt72FromZmanim
                                )
                                2 -> StoryCard(
                                    tz = tz
                                    // , remoteUrlTemplate = "https://example.com/stories/{key}.txt"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CandleHeaderBox(candleLighting: ZonedDateTime?, tz: ZoneId, city: City) {
    val headerGrad = Brush.horizontalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)))

    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(headerGrad)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "הדלקת נרות שבת קודש בעוד:",
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
                        "${city.display}, ${candleLighting.format(HHMM)}",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
fun ControlsBox(
    city: City,
    onCityChange: (City) -> Unit,
    board: BoardPreset,
    onBoardChange: (BoardPreset) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CityDropdown(city = city, onCityChange = onCityChange)
            BoardDropdown(board = board, onBoardChange = onBoardChange)
        }
    }
}

@Composable
fun TimesOfDayCard(
    date: LocalDate,
    tz: ZoneId,
    z: ZmanResults,
    board: BoardPreset,
    modifier: Modifier = Modifier
) {
    val hebText = remember(date) { hebrewDateFor(date, inIsrael = true) }
    val gregText = remember(date) { date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "זמני היום",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                hebText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                gregText,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ZRow("עלות השחר", z.alosHashachar)
            ZRow("זמן טלית ותפילין", z.misheyakir11_5)
            ZRow("זריחה מישורית", z.sunriseSeaLevel)
            ZRow("זריחה הנראית", z.sunriseVisible)

            Spacer(Modifier.height(6.dp))

            val isMGA = board == BoardPreset.MGA
            val isGRA = board == BoardPreset.GRA

            ZRow("סו\"ז ק\"ש מג\"א", z.sofZmanShmaMGA, selected = isMGA)
            ZRow("סו\"ז ק\"ש גרא", z.sofZmanShmaGRA, selected = isGRA)
            ZRow("סו\"ז תפילה מג\"א", z.sofZmanTfilaMGA, selected = isMGA)
            ZRow("סו\"ז תפילה גרא", z.sofZmanTfilaGRA, selected = isGRA)

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
}

@Composable
private fun ZRow(label: String, time: LocalTime?, selected: Boolean = false) {
    val timeText = time?.format(HHMM) ?: "--"
    val base = MaterialTheme.typography.bodyLarge
    val style = if (selected) base.copy(fontWeight = FontWeight.SemiBold) else base
    Text(
        text = "$label: $timeText",
        style = style,
        textAlign = TextAlign.Right,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
