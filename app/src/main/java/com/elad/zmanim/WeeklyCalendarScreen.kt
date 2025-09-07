package com.elad.zmanim

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.elad.zmanim.boards.BoardPreset as BoardsPreset
import com.elad.zmanim.boards.BoardProfile
import com.elad.zmanim.boards.profileFor
import com.elad.zmanim.boards.selectByProfile
import com.elad.zmanim.boards.SofZmanRef
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

/* ===== Adapter: map legacy/root enum -> boards enum safely by name ===== */
private fun com.elad.zmanim.BoardPreset.toBoards(): BoardsPreset = when (this.name) {
    "GRA" -> BoardsPreset.GRA
    "MGA" -> BoardsPreset.MGA
    "OR_HA_CHAIM", "OR_HACHAIM" -> BoardsPreset.OR_HA_CHAIM
    "RABEINU_TAM" -> BoardsPreset.RABEINU_TAM
    else -> BoardsPreset.GRA // fallback for CUSTOM or unknowns
}

/* ===== models for the weekly UI ===== */

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

/* ===== formatters ===== */

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val GREG_DAY_ABBR: DateTimeFormatter = DateTimeFormatter.ofPattern("E", java.util.Locale.ENGLISH)
private val GREG_DAY_NUM: DateTimeFormatter = DateTimeFormatter.ofPattern("d")

/* ===== theme tweaks used here ===== */

private object UiTuning {
    val SectionPadH = 12.dp
    val SectionPadV = 10.dp
    val HolidayChip = Color(0xFF6A5ACD)
    val ShabbatPurple = Color(0xFF5B3BAE)
    val TodayBg = Color(0xFFD9ECFF)
    val ShabbatBg = Color(0xFFF1EAFE)
}

/* ===== main screen ===== */

@Composable
fun WeeklyCalendarScreen(
    baseDate: LocalDate,
    city: City,
    tz: ZoneId,
    board: com.elad.zmanim.BoardPreset,          // root enum
    candleOffsetMinutes: Int?,
    modifier: Modifier = Modifier,
    onCityClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val candleOffset = candleOffsetMinutes ?: city.defaultCandleOffsetMin
    val today = remember(tz) { LocalDate.now(tz) }

    // Load hilulot list once
    val hilulot = remember { loadHilulot(context) }

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

    val (gregTop, gregBottom) = remember(week) { gregSpanSplit(week.first().localDate, week.last().localDate) }
    val (hebTop, hebBottom)   = remember(baseDate) { hebMonthYearSplit(baseDate) }

    val shabbatDate = remember(baseDate) { nextShabbatFrom(baseDate) }
    val shabbatZ: ZmanResults = remember(shabbatDate, city, tz) {
        ZmanimProvider.computeAll(shabbatDate, city.lat, city.lon, tz, city.elevationMeters)
    }

    // Candle lighting = Friday sunset - candleOffset
    val erevShabbat = remember(shabbatDate) { shabbatDate.minusDays(1) }
    val erevZ: ZmanResults = remember(erevShabbat, city, tz) {
        ZmanimProvider.computeAll(erevShabbat, city.lat, city.lon, tz, city.elevationMeters)
    }
    val candleText = erevZ.sunsetSeaLevel
        ?.minusMinutes(candleOffset.toLong())
        ?.format(HHMM)
        ?.let { "$it (${candleOffset}-)" }

    val bgGrad = Brush.verticalGradient(listOf(Color(0xFFF7F9FD), Color(0xFFF2F5FB)))

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .background(bgGrad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiTuning.SectionPadH, vertical = UiTuning.SectionPadV)
                .fillMaxSize()
        ) {
            /* ===== header (months/years + city) ===== */
            HeaderRow(
                gregTop = gregTop, gregBottom = gregBottom,
                hebTop = hebTop, hebBottom = hebBottom,
                cityLabel = city.toString(),
                onCityClick = onCityClick
            )

            Spacer(Modifier.height(8.dp))

            /* ===== table (7 rows) ===== */
            week.forEach { d ->
                DayRow(
                    d = d,
                    city = city,
                    tz = tz,
                    board = board,          // still pass root enum to row (adapt inside)
                    hilulot = hilulot
                )
                Divider(color = Color(0xFFE3E7EF))
            }

            /* ===== Shabbat summary cards ===== */
            ShabbatSummaryRow(
                candle = candleText,
                havdalah = shabbatZ.tzeitRT72?.format(HHMM) ?: shabbatZ.tzeitStandard?.format(HHMM)
            )

            Spacer(Modifier.height(12.dp))

            /* ===== Prayer times list (board-aware) ===== */
            PrayerTimesList(anchor = week.first(), city = city, tz = tz, board = board)
        }
    }
}

/* ===== header composable ===== */

@Composable
private fun HeaderRow(
    gregTop: String, gregBottom: String,
    hebTop: String, hebBottom: String,
    cityLabel: String,
    onCityClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(gregTop, color = Color(0xFF6A5ACD), fontWeight = FontWeight.SemiBold)
            Text(gregBottom, color = Color(0xFF6A5ACD), fontWeight = FontWeight.SemiBold)
        }
        Text(
            cityLabel,
            modifier = Modifier.clickable { onCityClick() },
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF333333)
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(hebTop, color = Color(0xFF6A5ACD), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
            Text(hebBottom, color = Color(0xFF6A5ACD), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
    }
    Spacer(Modifier.height(6.dp))
}

/* ===== one day row ===== */

@Composable
private fun DayRow(
    d: DayRowUi,
    city: City,
    tz: ZoneId,
    board: com.elad.zmanim.BoardPreset,  // root enum
    hilulot: List<Hilula>
) {
    val rowBg = when {
        d.isToday -> UiTuning.TodayBg
        d.isShabbat -> UiTuning.ShabbatBg
        else -> Color.White
    }
    val base = MaterialTheme.typography.bodyLarge
    val gregTopStyle = base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = (base.fontSize.value * 0.90f).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val gregBottomStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * 0.90f).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val hebTopStyle = base.copy(
        fontWeight = FontWeight.SemiBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeight = 0.95.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.Both
        )
    )
    val hebBottomStyle = MaterialTheme.typography.labelSmall.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeight = 0.90.em,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.Both
        )
    )
    val timeStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
    val ctx = LocalContext.current

    Surface(color = rowBg) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                /* LEFT — Gregorian STACKED + sunset */
                Row(
                    Modifier
                        .weight(1.1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // stacked greg
                    Box(Modifier.wrapContentWidth(Alignment.Start)) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(d.gregDayOfMonth, style = gregTopStyle)
                            Text(d.gregDayAbbrev, style = gregBottomStyle)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    // sunset time
                    Box(Modifier.wrapContentWidth(Alignment.Start)) {
                        Text(d.sunset ?: "--", style = timeStyle)
                    }
                }

                /* MIDDLE — divider + Holiday/Parasha + Hilula chips */
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(IntrinsicSize.Min)
                ) {
                    Divider(
                        color = Color(0xFFDDDDDD),
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .align(Alignment.Center)
                    )

                    // holiday or parasha (center)
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
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 2.dp)
                                    .clickable {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse(url)
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ctx.startActivity(intent)
                                    }
                            )
                        }
                        else -> Unit
                    }

                    // Hilula chips (if any)
                    val todaysHilulot = hilulotOn(d.localDate, true, hilulot)
                    if (todaysHilulot.isNotEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            todaysHilulot.forEach { h ->
                                Surface(
                                    color = UiTuning.HolidayChip.copy(alpha = 0.10f),
                                    contentColor = UiTuning.HolidayChip,
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        "הילולה: " + h.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                /* RIGHT — Hebrew STACKED + sunrise */
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        Modifier
                            .weight(0.80f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // stacked hebrew (tight)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(d.hebDayOfMonthHeb, style = hebTopStyle)
                                Text(d.hebrewDayName, style = hebBottomStyle)
                            }
                        }

                        // sunrise icon + time
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            Icon(
                                Icons.Filled.LightMode,
                                contentDescription = "Sunrise",
                                tint = Color(0xFFF9A825),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(d.sunrise ?: "--", style = timeStyle)
                        }
                    }
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
                .padding(horizontal = UiTuning.SectionPadH),
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
private fun SummaryBox(title: String, value: String, bg: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = bg,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/* ===== PrayerTimes (board-aware) ===== */

@Composable
private fun InfoIcon(text: String?) {
    if (text.isNullOrBlank()) return
    var open by remember { mutableStateOf(false) }
    Icon(
        imageVector = Icons.Filled.Info,
        contentDescription = "מידע",
        modifier = Modifier
            .size(16.dp)
            .clickable { open = true },
        tint = Color(0xFF6A5ACD)
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("סגור") } },
            title = { Text("הסבר קצר") },
            text = { Text(text) }
        )
    }
}

@Composable
private fun PrayerTimesList(
    anchor: DayRowUi,
    city: City,
    tz: ZoneId,
    board: com.elad.zmanim.BoardPreset      // root enum
) {
    val z = ZmanimProvider.computeAll(anchor.localDate, city.lat, city.lon, tz, city.elevationMeters)

    // Convert to boards enum at the usage site
    val profile: BoardProfile = profileFor(board.toBoards())

    val sel = selectByProfile(z, profile)

    val isMGA = profile.sofZmanRef == SofZmanRef.MGA
    val isGRA = profile.sofZmanRef == SofZmanRef.GRA
    val isRT  = board.name == "RABEINU_TAM"

    @Composable
    fun RowItem(label: String, time: LocalTime?, emphasize: Boolean = false, infoKey: String? = null) {
        val t = time?.format(HHMM) ?: "--"
        val style = if (emphasize)
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        else
            MaterialTheme.typography.labelSmall

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = style,
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.End),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Text(t, style = if (emphasize) style.copy(fontWeight = FontWeight.Bold) else style)
                if (infoKey != null) {
                    Spacer(Modifier.width(6.dp))
                    InfoIcon(profile.explanations[infoKey])
                }
            }
        }
        Divider(color = Color(0xFFE6E9F0))
    }

    Column(Modifier.fillMaxWidth()) {
        RowItem("עלות השחר", z.alosHashachar, infoKey = "alos")
        RowItem("טלית ותפילין", sel.misheyakir, infoKey = "misheyakir")
        RowItem("זריחה", sel.sunrise, infoKey = "sunrise")
        RowItem("חצות היום", sel.chatzot, infoKey = "chatzot")
        RowItem("מנחה גדולה", sel.minchaGedola, infoKey = "minchaGedola")
        RowItem("מנחה קטנה", sel.minchaKetana, infoKey = "minchaKetana")
        RowItem("פלג המנחה", sel.plag, infoKey = "plag")
        RowItem("שקיעה", sel.sunset, infoKey = "sunset")

        RowItem(
            if (isRT) "צאת הכוכבים (רבינו תם)" else "צאת הכוכבים",
            sel.tzeitPrimary,
            emphasize = isRT,
            infoKey = "tzeit"
        )
        sel.tzeitSecondary?.let { t ->
            RowItem(if (isRT) "צאת (סטנדרטי)" else "צאת (רבינו תם)", time = t)
        }

        RowItem("סו\"ז ק\"ש מג\"א", sel.sofZmanShmaMga, emphasize = isMGA, infoKey = "sofZmanShma")
        RowItem("סו\"ז ק\"ש גר\"א", sel.sofZmanShmaGra, emphasize = isGRA, infoKey = "sofZmanShma")
        RowItem("סו\"ז תפילה מג\"א", sel.sofZmanTfilaMga, emphasize = isMGA)
        RowItem("סו\"ז תפילה גר\"א", sel.sofZmanTfilaGra, emphasize = isGRA)
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

private fun hebDayOfMonthHeb(d: LocalDate, inIsrael: Boolean = true): String {
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, d.year)
        set(Calendar.MONTH, d.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, d.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val jc = JewishCalendar().apply {
        setInIsrael(inIsrael)
        setDate(cal)
    }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    return fmt.formatHebrewNumber(jc.jewishDayOfMonth)
}

private fun hebrewDateForLocal(d: LocalDate, inIsrael: Boolean = true): String {
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, d.year)
        set(Calendar.MONTH, d.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, d.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val jc = JewishCalendar().apply {
        setInIsrael(inIsrael)
        setDate(cal)
    }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    return fmt.format(jc)
}

private fun hebMonthYearSplit(d: LocalDate, inIsrael: Boolean = true): Pair<String,String> {
    val parts = hebrewDateForLocal(d, inIsrael).trim().split(' ')
    val last2 = parts.takeLast(2)
    val month = last2.getOrNull(0) ?: ""
    val year  = last2.getOrNull(1) ?: ""
    return month to year
}

/** Returns (top, bottom) where top is gregorian month or span, bottom is year or span. */
private fun gregSpanSplit(start: LocalDate, end: LocalDate): Pair<String,String> {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM", java.util.Locale.ENGLISH)
    val yearFmt  = DateTimeFormatter.ofPattern("yyyy")
    val sM = start.format(monthFmt)
    val eM = end.format(monthFmt)
    val sY = start.format(yearFmt)
    val eY = end.format(yearFmt)
    val top = if (sM == eM) sM else "$sM–$eM"
    val bottom = if (sY == eY) sY else "$sY–$eY"
    return top to bottom
}

private fun holidayFor(date: LocalDate, inIsrael: Boolean = true): String? {
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val jc = JewishCalendar().apply {
        setInIsrael(inIsrael)
        setDate(cal)
    }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    return fmt.formatYomTov(jc)?.takeIf { it.isNotBlank() }
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
    val jc = JewishCalendar().apply {
        setInIsrael(inIsrael)
        setDate(cal)
    }
    val fmt = HebrewDateFormatter().apply { isHebrewFormat = true; isUseGershGershayim = true }
    val name = fmt.formatParsha(jc) ?: return null
    return if (name.isBlank()) null else name
}

private fun nextShabbatFrom(from: LocalDate): LocalDate {
    var d = from
    while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
    return d
}
