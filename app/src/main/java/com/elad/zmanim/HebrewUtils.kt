package com.elad.zmanim

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

/** Returns Hebrew date string (e.g., י"ג באלול ה'תשפ"ה) for a Gregorian LocalDate */
fun hebrewDateFor(localDate: LocalDate, inIsrael: Boolean = true): String {
    val cal: Calendar = GregorianCalendar.of(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
    val jc = JewishCalendar().apply {
        this.inIsrael = inIsrael
        setDate(cal)
    }
    val fmt = HebrewDateFormatter().apply {
        isHebrewFormat = true
        isUseGershGershayim = true
    }
    return fmt.format(jc)
}

/** Finds the next Shabbat (Saturday) date on/after 'from'. */
fun nextShabbat(from: LocalDate = LocalDate.now()): LocalDate {
    var d = from
    while (d.dayOfWeek.value != 6 /* Saturday */) d = d.plusDays(1)
    return d
}

/** Safe Calendar.of for Java 8 */
private fun GregorianCalendar.Companion.of(year: Int, monthZeroBased: Int, day: Int): GregorianCalendar {
    val g = GregorianCalendar()
    g.set(year, monthZeroBased, day, 12, 0, 0)
    g.set(Calendar.MILLISECOND, 0)
    return g
}
