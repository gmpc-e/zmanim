package com.elad.zmanim

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ShabbatSummary(
    val cityDisplay: String,
    val gregDate: LocalDate,        // Shabbat day (Saturday)
    val hebrewDate: String,         // formatted Hebrew date for Saturday
    val parashaHeb: String?,        // פרשת השבוע (Hebrew)
    val candleLighting: ZonedDateTime?,
    val havdalah: ZonedDateTime?,
    val havdalahRT72: ZonedDateTime? // optional; can be computed via local zmanim if desired
)

object ShabbatRepository {
    private val client = OkHttpClient()
    private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * Fetch upcoming Shabbat info from Hebcal for the given city/offset.
     * Params: cfg=json, lg=he, M=on (8.5° Havdalah), b=<minutes>, lat/lon/tzid
     */
    suspend fun fetchUpcoming(
        city: City,
        tz: ZoneId,
        candleOffsetMin: Int? = null
    ): ShabbatSummary? = withContext(Dispatchers.IO) {
        try {
            val b = (candleOffsetMin ?: city.defaultCandleOffsetMin).coerceAtLeast(0)
            val url = "https://www.hebcal.com/shabbat?cfg=json&lg=he&M=on&b=$b" +
                    "&latitude=${city.lat}&longitude=${city.lon}&tzid=${city.tzid}"

            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("Zmanim", "Shabbat API HTTP ${resp.code}")
                    return@withContext null
                }
                val root = JSONObject(resp.body?.string() ?: return@withContext null)
                val items = root.optJSONArray("items") ?: return@withContext null

                var candle: ZonedDateTime? = null
                var havdalah: ZonedDateTime? = null
                var parashaHeb: String? = null

                for (i in 0 until items.length()) {
                    val it = items.getJSONObject(i)
                    when (it.optString("category")) {
                        "candles"  -> candle   = parseZoned(it.optString("date", null), tz)
                        "havdalah" -> havdalah = parseZoned(it.optString("date", null), tz)
                        "parashat" -> parashaHeb = it.optString("hebrew", null)
                    }
                }

                val shabbatDate = nextShabbat(LocalDate.now(tz))
                val heb = hebrewDateFor(shabbatDate, inIsrael = true)

                ShabbatSummary(
                    cityDisplay = city.display,
                    gregDate = shabbatDate,
                    hebrewDate = heb,
                    parashaHeb = parashaHeb,
                    candleLighting = candle,
                    havdalah = havdalah,
                    havdalahRT72 = null // (optional) compute locally from zmanim if you want exact RT72
                )
            }
        } catch (t: Throwable) {
            Log.e("Zmanim", "Shabbat API failure", t)
            null
        }
    }

    private fun parseZoned(iso: String?, tz: ZoneId): ZonedDateTime? {
        if (iso.isNullOrBlank()) return null
        return try {
            // Hebcal example: 2025-09-05T18:40:00+03:00
            ZonedDateTime.parse(iso, ISO_OFFSET).withZoneSameInstant(tz)
        } catch (_: Throwable) {
            null
        }
    }
}
