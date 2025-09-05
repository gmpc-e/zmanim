package com.elad.zmanim

import android.util.Log
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Minimal logger to inspect KosherJava Misheyakir variants for a given city/date.
 * Works with com.kosherjava.zmanim.*
 */
fun logMisheyakirFor(
    date: LocalDate,
    lat: Double,
    lon: Double,
    tz: ZoneId,
    elevationMeters: Double = 0.0,
    tag: String = "MisheyakirProbe"
) {
    // IMPORTANT: com.kosherjava.zmanim.util.GeoLocation expects a TimeZone, not a tz ID string.
    val tzOld: TimeZone = TimeZone.getTimeZone(tz)
    val geo = GeoLocation("probe", lat, lon, elevationMeters, tzOld)
    val czc = ComplexZmanimCalendar(geo)

    // Set calendar to the given local date (midnight)
    val cal = GregorianCalendar(tzOld)
    cal.time = Date.from(date.atStartOfDay(tz).toInstant())
    czc.calendar = cal

    fun fmt(d: Date?): String =
        d?.toInstant()?.atZone(tz)?.toLocalTime()?.format(HHMM) ?: "--"

    Log.d(tag, "---- $date @ ($lat,$lon) tz=${tz.id} elev=${elevationMeters}m ----")
    Log.d(tag, "misheyakir11.5  : ${fmt(czc.getMisheyakir11Point5Degrees())}")
    Log.d(tag, "misheyakir10.2  : ${fmt(czc.getMisheyakir10Point2Degrees())}")
    Log.d(tag, "misheyakir9.5   : ${fmt(czc.getMisheyakir9Point5Degrees())}")
    Log.d(tag, "misheyakir7.65  : ${fmt(czc.getMisheyakir7Point65Degrees())}")
    // Optional context
    Log.d(tag, "alos             : ${fmt(czc.alosHashachar)}")
    Log.d(tag, "sunrise (visible): ${fmt(czc.sunrise)}")
}
