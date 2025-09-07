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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ Let us handle insets inside Compose
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { ZmanimApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ZmanimApp() {
    var selectedCity by remember { mutableStateOf(Cities.all.first()) }
    var settings by remember { mutableStateOf(AppSettings(board = BoardPreset.GRA)) }
    var showAbout by remember { mutableStateOf(false) }

    val tz = ZoneId.of(selectedCity.tzid)

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2)),
        startY = 0f, endY = 900f
    )

    // Root container: pad by system bars so nothing draws under status bar/notch
    Box(
        Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(WindowInsets.systemBars.asPaddingValues()) // ✅ reliable inset padding
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface // (no alpha; avoids white veil under status bar)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Compact, centered header
                Surface(
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "זמנים - לוח שנה יהודי",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showAbout = true }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Outlined.Info, contentDescription = "About")
                            }
                            BoardDropdown(
                                board = settings.board,
                                onBoardChange = { settings = settings.copy(board = it) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Weekly swipe (page = week). RTL interaction: swipe RIGHT → NEXT week.
                val today = remember(tz) { LocalDate.now(tz) }
                val thisWeekStart = remember(today) { sundayOfWeek(today) }
                val initial = 78
                val pager = rememberPagerState(initialPage = initial, pageCount = { 156 })

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AnimatedContent(
                        targetState = Triple(pager.currentPage, selectedCity, settings.board),
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) togetherWith
                                    fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)))
                        },
                        label = "week-city-board"
                    ) { (page, city, board) ->
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.weight(1f)
                        ) { idx ->
                            val weeksDelta = (initial - idx) * -1 // RIGHT → future weeks
                            val weekStart = thisWeekStart.plusDays((weeksDelta * 7L))
                            WeeklyCalendarScreen(
                                baseDate = weekStart,
                                city = city,
                                tz = tz,
                                board = board,
                                candleOffsetMinutes = settings.candleOffsetMinutes,
                                onCityClick = { /* city picker later */ },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}

private fun sundayOfWeek(d: LocalDate): LocalDate {
    val dow = d.dayOfWeek
    val move = if (dow == DayOfWeek.SUNDAY) 0 else dow.value
    return d.minusDays(move.toLong())
}
