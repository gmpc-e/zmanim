package com.elad.zmanim

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar

/** Returns Hebrew date string (e.g., י"ג באלול ה'תשפ"ה) for a Gregorian LocalDate */
fun hebrewDateFor(localDate: LocalDate, inIsrael: Boolean = true): String {
    val cal: Calendar = newGregorian(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
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
    while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plusDays(1)
    return d
}

/** Simple helper to make a GregorianCalendar without extension hacks. */
private fun newGregorian(year: Int, monthZeroBased: Int, day: Int): GregorianCalendar {
    return GregorianCalendar().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)     // 0-based
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)           // noon avoids DST edge effects
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
