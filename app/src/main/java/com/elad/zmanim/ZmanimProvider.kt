package com.elad.zmanim

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone

data class ZmanResults(
    val date: LocalDate,

    // Morning
    val alosHashachar: LocalTime?,       // עלות השחר
    val misheyakir11_5: LocalTime?,      // משיכיר ~11.5°, "זמן טלית ותפילין"
    val misheyakir10_2: LocalTime?,      // משיכיר ~10.2°
    val misheyakir9_5: LocalTime?,       // משיכיר ~9.5°
    val misheyakir7_65: LocalTime?,      // משיכיר ~7.65°

    // Sunrise: sea-level vs visible
    val sunriseSeaLevel: LocalTime?,     // זריחה מישורית (גובה פני הים)
    val sunriseVisible: LocalTime?,      // זריחה הנראית (ברירת מחדל ספרייה)

    // Latest times (GRA/MGA)
    val sofZmanShmaMGA: LocalTime?,      // סוף זמן ק"ש מג"א
    val sofZmanShmaGRA: LocalTime?,      // סוף זמן ק"ש גר"א
    val sofZmanTfilaMGA: LocalTime?,     // סוף זמן תפילה מג"א
    val sofZmanTfilaGRA: LocalTime?,     // סוף זמן תפילה גר"א

    // Midday / afternoon
    val chatzot: LocalTime?,             // חצות היום
    val minchaGedola: LocalTime?,        // תחילת זמן מנחה גדולה
    val minchaKetana: LocalTime?,        // מנחה קטנה
    val plagHamincha: LocalTime?,        // פלג המנחה

    // Sunset: sea-level vs visible
    val sunsetSeaLevel: LocalTime?,      // שקיעה מישורית
    val sunsetVisible: LocalTime?,       // שקיעה הנראית

    // Nightfall
    val tzeitStandard: LocalTime?,       // צאת הכוכבים (סטנדרטי)
    val tzeitRT72: LocalTime?            // צאת הכוכבים לרבנו תם (~72 דק')
)

object ZmanimProvider {

    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    fun LocalTime?.fmt(): String = this?.format(HHMM) ?: "--"

    private fun toLocalTime(d: java.util.Date?, tz: ZoneId): LocalTime? =
        d?.toInstant()?.atZone(tz)?.toLocalTime()

    /** Build a ComplexZmanimCalendar for a given date/lat/lon/tz and elevation (meters). */
    private fun czcFor(
        date: LocalDate,
        lat: Double,
        lon: Double,
        tz: ZoneId,
        elevationMeters: Double
    ): ComplexZmanimCalendar {
        val tzOld: TimeZone = TimeZone.getTimeZone(tz)
        val geo = GeoLocation("Location", lat, lon, elevationMeters, tzOld)
        val czc = ComplexZmanimCalendar(geo)
        val cal: Calendar = Calendar.getInstance(tzOld).apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1) // Calendar is 0-based month
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        czc.calendar = cal
        return czc
    }

    /**
     * Compute a rich set of zmanim:
     * - uses "visible" sunrise/sunset from the default calendar (observer at given elevation; we’ll pass 0 in the app)
     * - computes "sea-level" sunrise/sunset by re-evaluating with elevation=0
     * - Misheyakir variants (11.5°, 10.2°, 9.5°, 7.65°)
     * - GRA/MGA cutoffs, chatzot, mincha times, plag, tzeit (standard) and RT (72)
     */
    fun computeAll(
        date: LocalDate,
        lat: Double,
        lon: Double,
        tz: ZoneId,
        observerElevationMeters: Double = 0.0 // keep 0 for most cities
    ): ZmanResults {
        val czcVisible = czcFor(date, lat, lon, tz, observerElevationMeters)
        val czcSea     = czcFor(date, lat, lon, tz, 0.0)

        DebugLog.d("ComputeAll $date @ ($lat,$lon) tz=$tz elev=$observerElevationMeters")

        // Morning
        val alos             = toLocalTime(czcVisible.alosHashachar, tz)
        val misheyakir11_5   = toLocalTime(czcVisible.getMisheyakir11Point5Degrees(), tz)
        val misheyakir10_2   = toLocalTime(czcVisible.getMisheyakir10Point2Degrees(), tz)
        val misheyakir9_5    = toLocalTime(czcVisible.getMisheyakir9Point5Degrees(), tz)
        val misheyakir7_65   = toLocalTime(czcVisible.getMisheyakir7Point65Degrees(), tz)

        // Sunrise (sea-level vs visible)
        val sunriseSea       = toLocalTime(czcSea.sunrise, tz)
        val sunriseVis       = toLocalTime(czcVisible.sunrise, tz)

        // Latest times (GRA/MGA)
        val shmaMGA          = toLocalTime(czcVisible.sofZmanShmaMGA, tz)
        val shmaGRA          = toLocalTime(czcVisible.sofZmanShmaGRA, tz)
        val tfilaMGA         = toLocalTime(czcVisible.sofZmanTfilaMGA, tz)
        val tfilaGRA         = toLocalTime(czcVisible.sofZmanTfilaGRA, tz)

        // Midday / afternoon
        val chatzot          = toLocalTime(czcVisible.chatzos, tz)
        val minGedola        = toLocalTime(czcVisible.minchaGedola, tz)
        val minKetana        = toLocalTime(czcVisible.minchaKetana, tz)
        val plag             = toLocalTime(czcVisible.plagHamincha, tz)

        // Sunset (sea-level vs visible)
        val sunsetSea        = toLocalTime(czcSea.sunset, tz)
        val sunsetVis        = toLocalTime(czcVisible.sunset, tz)

        // Nightfall
        val tzeitStd         = toLocalTime(czcVisible.tzais, tz)
        val tzeitRT72        = toLocalTime(czcVisible.tzais72, tz)

        return ZmanResults(
            date = date,
            alosHashachar = alos,
            misheyakir11_5 = misheyakir11_5,
            misheyakir10_2 = misheyakir10_2,
            misheyakir9_5 = misheyakir9_5,
            misheyakir7_65 = misheyakir7_65,
            sunriseSeaLevel = sunriseSea,
            sunriseVisible = sunriseVis,
            sofZmanShmaMGA = shmaMGA,
            sofZmanShmaGRA = shmaGRA,
            sofZmanTfilaMGA = tfilaMGA,
            sofZmanTfilaGRA = tfilaGRA,
            chatzot = chatzot,
            minchaGedola = minGedola,
            minchaKetana = minKetana,
            plagHamincha = plag,
            sunsetSeaLevel = sunsetSea,
            sunsetVisible = sunsetVis,
            tzeitStandard = tzeitStd,
            tzeitRT72 = tzeitRT72
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Optional: quick logger to inspect values in Logcat (no UI impact)
    // Call it from a LaunchedEffect(selectedCity,date) if you want to see values.
    fun logMisheyakirFor(
        date: LocalDate,
        lat: Double,
        lon: Double,
        tz: ZoneId,
        elevationMeters: Double = 0.0,
        tag: String = "MisheyakirProbe"
    ) {
        try {
            val czc = czcFor(date, lat, lon, tz, elevationMeters)
            fun fmt(d: java.util.Date?) =
                d?.toInstant()?.atZone(tz)?.toLocalTime()?.format(HHMM) ?: "--"

            android.util.Log.d(tag, "---- $date @ ($lat,$lon) tz=${tz.id} elev=${elevationMeters}m ----")
            android.util.Log.d(tag, "misheyakir11.5 : ${fmt(czc.getMisheyakir11Point5Degrees())}")
            android.util.Log.d(tag, "misheyakir10.2 : ${fmt(czc.getMisheyakir10Point2Degrees())}")
            android.util.Log.d(tag, "misheyakir9.5  : ${fmt(czc.getMisheyakir9Point5Degrees())}")
            android.util.Log.d(tag, "misheyakir7.65 : ${fmt(czc.getMisheyakir7Point65Degrees())}")
            android.util.Log.d(tag, "alos           : ${fmt(czc.alosHashachar)}")
            android.util.Log.d(tag, "sunrise(vis)   : ${fmt(czc.sunrise)}")
        } catch (t: Throwable) {
            android.util.Log.e(tag, "probe failed: ${t.message}", t)
        }
    }
}
