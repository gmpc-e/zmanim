package com.elad.zmanim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.util.Calendar
import java.util.GregorianCalendar

/* =========================
   Tuning knobs (easy tweaks)
   ========================= */
object UiTuning {
    val GridGapDp = 6.dp * 0.80f // compact rhythm
    val TodayBg = Color(0xFF8EC3FF)
    val ShabbatBg = Color(0xFFE8F1FF)
    val HolidayChip = Color(0xFF6A1B9A)
    val HeaderPurple = Color(0xFF6A1B9A)
    val ShabbatPurple = Color(0xFF4A148C)
}

data class DayRowUi(
    val localDate: LocalDate,
    val hebrewDayName: String,
    val hebDayOfMonthHeb: String,
    val gregDayAbbrev: String,
    val gregDayOfMonth: String,
    val sunrise: String?,
    val sunset: String?,
    val parasha: String?,
    val holiday: String?,
    val isShabbat: Boolean,
    val isToday: Boolean
)

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val GREG_DAY_ABBR: DateTimeFormatter = DateTimeFormatter.ofPattern("E", Locale.ENGLISH)
private val GREG_DAY_NUM: DateTimeFormatter = DateTimeFormatter.ofPattern("d")

private val SECTION_GAP = 10.dp
private val SECTION_PAD_H = 10.dp

@Composable
fun WeeklyCalendarScreen(
    baseDate: LocalDate,
    city: City,
    tz: ZoneId,
    board: BoardPreset,
    candleOffsetMinutes: Int?,
    modifier: Modifier = Modifier,
    onCityClick: () -> Unit = {}
) {
    val candleOffset = candleOffsetMinutes ?: city.defaultCandleOffsetMin
    val today = remember(tz) { LocalDate.now(tz) }

    val week: List<DayRowUi> = remember(baseDate, city, tz) {
        (0..6).map { plus ->
            val d = baseDate.plusDays(plus.toLong())
            val z = ZmanimProvider.computeAll(d, city.lat, city.lon, tz, city.elevationMeters)
            DayRowUi(
                localDate = d,
                hebrewDayName = hebDayNameShort(d),
                hebDayOfMonthHeb = hebDayOfMonthHeb(d),
                gregDayAbbrev = d.format(GREG_DAY_ABBR) + ".",
                gregDayOfMonth = d.format(GREG_DAY_NUM),
                sunrise = z.sunriseSeaLevel?.format(HHMM),
                sunset = z.sunsetSeaLevel?.format(HHMM),
                parasha = parashaFor(d),
                holiday = holidayFor(d),
                isShabbat = d.dayOfWeek == DayOfWeek.SATURDAY,
                isToday = d == today
            )
        }
    }

    val gregSpan = remember(week) { gregorianMonthSpan(week.first().localDate, week.last().localDate) }
    val hebMonthYear = remember(baseDate) { hebrewMonthYear(baseDate) }

    val shabbatDate = remember(baseDate) { nextShabbat(baseDate) }
    val shabbatZ = remember(shabbatDate, city, tz) {
        ZmanimProvider.computeAll(shabbatDate, city.lat, city.lon, tz, city.elevationMeters)
    }

    val bgGrad = Brush.verticalGradient(listOf(Color(0xFFF7F9FD), Color(0xFFF2F5FB)))

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(bgGrad)
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderAndTable(
                hebMonthYear = hebMonthYear,
                cityName = city.display,
                gregSpan = gregSpan,
                week = week,
                onCityClick = onCityClick
            )

            Spacer(Modifier.height(SECTION_GAP))

            ShabbatSummaryRow(
                candle = shabbatZ.sunsetSeaLevel?.minusMinutes((18 + candleOffset).toLong())?.format(HHMM),
                havdalah = shabbatZ.tzeitStandard?.format(HHMM)
            )

            Spacer(Modifier.height(SECTION_GAP))
            Divider(color = Color(0xFFE3E7EF))
            Spacer(Modifier.height(SECTION_GAP))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = SECTION_PAD_H),
                horizontalArrangement = Arrangement.Center
            ) {
                // ↑ bigger title
                Text(
                    "זמני היום",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(UiTuning.GridGapDp))

            PrayerTimesList(anchor = week.first(), city = city, tz = tz, board = board)
        }
    }
}

/** Combined header + weekly table; right-side Hebrew cell now stacked (day↑, name↓). */
@Composable
private fun HeaderAndTable(
    hebMonthYear: String,
    cityName: String,
    gregSpan: String,
    week: List<DayRowUi>,
    onCityClick: () -> Unit
) {
    val uri = LocalUriHandler.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            // Force LTR so left/right are physical
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SECTION_PAD_H, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelSmall10 = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * 1.10f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTuning.HeaderPurple,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    Text(gregSpan, style = labelSmall10) // left (Gregorian)
                    Text(
                        cityName,
                        style = MaterialTheme.typography.titleMedium.copy( // ↑ bigger
                            fontWeight = FontWeight.SemiBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { onCityClick() }
                    )
                    Text(hebMonthYear, style = labelSmall10) // right (Hebrew)
                }
            }

            // Tiny icon header; nudge sunrise icon to visually align to time
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1.1f).fillMaxWidth()) { // ↓ slightly narrower left cluster
                        Box(Modifier.weight(1f))
                        Box(Modifier.weight(1f))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            Icon(
                                Icons.Filled.DarkMode,
                                contentDescription = "Sunset",
                                tint = Color(0xFF37474F),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Box(Modifier.weight(0.95f).height(16.dp)) { // ↑ a bit more middle space
                        Divider(
                            color = Color(0xFFDDDDDD),
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .align(Alignment.Center)
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(Modifier.weight(0.95f).fillMaxWidth()) { // ↓ slightly narrower right block
                            Box(Modifier.weight(1f))
                            Box(Modifier.weight(1f))
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                Icon(
                                    Icons.Filled.LightMode,
                                    contentDescription = "Sunrise",
                                    tint = Color(0xFFF9A825),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFFE3E7EF))

            // Body rows
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                week.forEach { d ->
                    val rowBg = when {
                        d.isToday -> UiTuning.TodayBg
                        d.isShabbat -> UiTuning.ShabbatBg
                        else -> Color.White
                    }

                    val base = MaterialTheme.typography.bodyLarge
                    val hebBig = base.copy( // bigger for hebrew day-of-month
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (base.fontSize.value * 0.75f).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    val hebSmall = base.copy( // smaller for hebrew day-name
                        fontWeight = FontWeight.Medium,
                        fontSize = (base.fontSize.value * 0.60f).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    val bodyLarge10 = base.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (base.fontSize.value * 0.9f).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                    val label10 = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * 0.9f).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        fontFeatureSettings = "tnum"
                    )
                    val holidayStyle = hebSmall

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT — Gregorian (abbr | day | sunset), no spacer, tighter
                        Row(
                            Modifier.weight(1.1f).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f)) { Text(d.gregDayAbbrev, style = label10) }
                            Box(Modifier.weight(1f)) { Text(d.gregDayOfMonth, style = bodyLarge10) }
                            Box(Modifier.weight(1f)) { Text(d.sunset ?: "--", style = label10) }
                        }

                        // MIDDLE — divider + Holiday/Parasha (more room now)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(IntrinsicSize.Min)
                        ) {
                            Divider(
                                color = Color(0xFFDDDDDD),
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.Center)
                            )

                            when {
                                !d.holiday.isNullOrBlank() -> {
                                    Surface(
                                        color = UiTuning.HolidayChip.copy(alpha = 0.10f),
                                        contentColor = UiTuning.HolidayChip,
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Text(
                                            d.holiday!!,
                                            style = holidayStyle,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                d.isShabbat && !d.parasha.isNullOrBlank() -> {
                                    val q = "פרשת השבוע ${d.parasha}"
                                    val url = "https://www.youtube.com/results?search_query=" +
                                            URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
                                    Text(
                                        d.parasha!!,
                                        style = holidayStyle,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 2.dp)
                                            .clickable { uri.openUri(url) }
                                    )
                                }
                            }
                        }

                        // RIGHT — Hebrew stacked cell (day-of-month ↑, day-name ↓) + sunrise cell
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Row(
                                Modifier.weight(0.8f).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stacked cell
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(d.hebDayOfMonthHeb, style = hebBig)
                                        Text(d.hebrewDayName, style = hebSmall)
                                    }
                                }
                                // Sunrise (icon + time) aligned together
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.LightMode,
                                            contentDescription = "Sunrise",
                                            tint = Color(0xFFF9A825),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(d.sunrise ?: "--", style = label10, color = Color(0xFF444444))
                                    }
                                }
                            }
                        }
                    }
                    Divider(color = Color(0xFFE3E7EF))
                }
            }
        }
    }
}

/** Shabbat summary (bigger): LEFT = havdalah, RIGHT = candle. */
@Composable
private fun ShabbatSummaryRow(candle: String?, havdalah: String?) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SECTION_PAD_H),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryBox(
                title = "צאת שבת",
                value = havdalah ?: "--",
                bg = UiTuning.ShabbatPurple.copy(alpha = 0.16f),
                contentColor = UiTuning.ShabbatPurple,
                modifier = Modifier.weight(1f)
            )
            SummaryBox(
                title = "כניסת שבת",
                value = candle ?: "--",
                bg = UiTuning.ShabbatPurple.copy(alpha = 0.16f),
                contentColor = UiTuning.ShabbatPurple,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryBox(
    title: String,
    value: String,
    bg: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 64.dp), // ↑ a bit taller
        shape = RoundedCornerShape(12.dp),
        color = bg,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(vertical = 10.dp), // ↑ padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) // ↑ size
        }
    }
}

/** Prayer times — clean list (no boxes). Label = right, time = left (flush). */
@Composable
private fun PrayerTimesList(anchor: DayRowUi, city: City, tz: ZoneId, board: BoardPreset) {
    val z = ZmanimProvider.computeAll(anchor.localDate, city.lat, city.lon, tz, city.elevationMeters)
    val isMGA = board == BoardPreset.MGA
    val isGRA = board == BoardPreset.GRA

    @Composable
    fun RowItem(label: String, time: LocalTime?, emphasize: Boolean = false) {
        val t = time?.format(HHMM) ?: "--"
        val style = if (emphasize)
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        else
            MaterialTheme.typography.labelSmall

        // Force LTR in row and lay out manually so "left/right" are physical
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = SECTION_PAD_H),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: time (flush left)
                Text(
                    t,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                // Right: label (flush right)
                Text(
                    label,
                    style = style,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Divider(color = Color(0xFFE6E9F0))
    }

    Column(Modifier.fillMaxWidth()) {
        RowItem("עלות השחר", z.alosHashachar)
        RowItem("זריחה", z.sunriseSeaLevel)
        RowItem("טלית ותפילין", z.misheyakir11_5)
        RowItem("חצות היום", z.chatzot)
        RowItem("מנחה גדולה", z.minchaGedola)
        RowItem("מנחה קטנה", z.minchaKetana)
        RowItem("פלג המנחה", z.plagHamincha)
        RowItem("שקיעה", z.sunsetSeaLevel)
        RowItem("צאת הכוכבים", z.tzeitStandard)
        RowItem("סו\"ז ק\"ש מג\"א", z.sofZmanShmaMGA, emphasize = isMGA)
        RowItem("סו\"ז ק\"ש גרא", z.sofZmanShmaGRA, emphasize = isGRA)
        RowItem("סו\"ז תפילה מג\"א", z.sofZmanTfilaMGA, emphasize = isMGA)
        RowItem("סו\"ז תפילה גרא", z.sofZmanTfilaGRA, emphasize = isGRA)
    }
}

/* ===== helpers ===== */

private fun hebDayNameShort(d: LocalDate): String = when (d.dayOfWeek) {
    DayOfWeek.SUNDAY -> "ראשון"
    DayOfWeek.MONDAY -> "שני"
    DayOfWeek.TUESDAY -> "שלישי"
    DayOfWeek.WEDNESDAY -> "רביעי"
    DayOfWeek.THURSDAY -> "חמישי"
    DayOfWeek.FRIDAY -> "שישי"
    DayOfWeek.SATURDAY -> "שבת"
}

private fun hebrewMonthYear(d: LocalDate): String =
    hebrewDateFor(d, inIsrael = true).split(' ').takeLast(2).joinToString(" ")

private fun gregorianMonthSpan(start: LocalDate, end: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    val a = start.withDayOfMonth(1).format(fmt)
    val b = end.withDayOfMonth(1).format(fmt)
    return if (a == b) a else "$a – $b"
}

private fun hebDayOfMonthHeb(date: LocalDate, inIsrael: Boolean = true): String {
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
    }
    val jc = JewishCalendar().apply { this.inIsrael = inIsrael; setDate(cal) }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    return fmt.formatHebrewNumber(jc.jewishDayOfMonth)
}

/** Holiday name (Hebrew) or null if not a holiday. */
private fun holidayFor(date: LocalDate, inIsrael: Boolean = true): String? {
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val jc = JewishCalendar().apply { this.inIsrael = inIsrael; setDate(cal) }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    val name = fmt.formatYomTov(jc) ?: return null
    return if (name.isBlank()) null else name
}

/** Parasha for a specific Gregorian date (Hebrew name if Shabbat; else null). */
private fun parashaFor(date: LocalDate, inIsrael: Boolean = true): String? {
    if (date.dayOfWeek != DayOfWeek.SATURDAY) return null
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val jc = JewishCalendar().apply { this.inIsrael = inIsrael; setDate(cal) }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    val name = fmt.formatParsha(jc) ?: return null
    return if (name.isBlank()) null else name
}
