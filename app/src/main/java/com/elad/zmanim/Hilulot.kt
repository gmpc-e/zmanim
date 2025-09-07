package com.elad.zmanim

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

data class Hilula(
    val name: String,
    val month: Int,
    val day: Int,
    val notes: String? = null
)

/** Load hilulot from assets/hilulot.json (Hebrew date = { "month": Int, "day": Int }). */
fun loadHilulot(context: Context): List<Hilula> {
    return try {
        context.assets.open("hilulot.json")
            .bufferedReader(Charset.forName("UTF-8"))
            .use(BufferedReader::readText)
            .let { txt ->
                val arr = JSONArray(txt)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val hd = o.getJSONObject("hebrew_date")
                        add(
                            Hilula(
                                name = o.getString("name"),
                                month = hd.getInt("month"),
                                day = hd.getInt("day"),
                                notes = if (o.has("notes")) o.optString("notes", null) else null
                            )
                        )
                    }
                }
            }
    } catch (_: Throwable) {
        emptyList()
    }
}

/** Returns hilulot matching the given Gregorian date (converted to Hebrew date). */
fun hilulotOn(
    date: LocalDate,
    inIsrael: Boolean = true,
    list: List<Hilula>
): List<Hilula> {
    // Build a noon Gregorian calendar for stable Hebrew date conversion
    val cal: Calendar = GregorianCalendar().apply {
        set(Calendar.YEAR, date.year)
        set(Calendar.MONTH, date.monthValue - 1)
        set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // kosherjava package + explicit setters
    val jc = JewishCalendar().apply {
        setInIsrael(inIsrael)
        setDate(cal)
    }

    val month = jc.jewishMonth      // Kotlin property -> getJewishMonth()
    val day   = jc.jewishDayOfMonth // Kotlin property -> getJewishDayOfMonth()

    return list.filter { it.month == month && it.day == day }
}
