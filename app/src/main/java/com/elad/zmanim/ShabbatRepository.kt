package com.elad.zmanim

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class ShabbatSummary(
    val cityDisplay: String,
    val gregDate: LocalDate,        // Shabbat day (Saturday)
    val hebrewDate: String,         // formatted Hebrew date for Saturday
    val parashaHeb: String?,        // פרשת השבוע (Hebrew)
    val candleLighting: ZonedDateTime?,
    val havdalah: ZonedDateTime?,
    val havdalahRT72: ZonedDateTime? // computed from shkia? here we’ll derive from havdalah or via offset 72
)

object ShabbatRepository {
    private val client = OkHttpClient()

    /**
     * Fetches upcoming Shabbat info from Hebcal for the given city/offset.
     * - Uses 'M=on' (tzeit 8.5°) for Havdalah by default.
     * - 'b=' sets candle-lighting minutes before sunset (city default or user override).
     * Docs: https://www.hebcal.com/home/197/shabbat-times-rest-api (params: cfg, b, M/m, lg, latitude/longitude, tzid)
     */
    suspend fun fetchUpcoming(city: City, tz: ZoneId, candleOffsetMin: Int? = null): ShabbatSummary? =
        withContext(Dispatchers.IO) {
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
                            "candles" -> {
                                val dt = it.optString("date", null)
                                candle = parseZoned(dt, tz)
                            }
                            "havdalah" -> {
                                val dt = it.optString("date", null)
                                havdalah = parseZoned(dt, tz)
                            }
                            "parashat" -> {
                                parashaHeb = it.optString("hebrew", null)
                            }
                        }
                    }

                    // Shabbat day = upcoming Saturday from today:
                    val shabbatDate = nextShabbat(LocalDate.now(tz))
                    val heb = hebrewDateFor(shabbatDate, inIsrael = true)

                    // Optional RT72: if we have candle/sunset we could compute; for now derive via +72min from candle? Not correct.
                    // Better: If havdalah present (M=on by 8.5°), we still show a separate RT72 via +72m from sunset.
                    // We approximate RT72 as +72 minutes from candle-lighting + (b minutes) + (sunset to candle)??? Simpler: show +72 from sunset by recomputing via ZmanimProvider if needed.
                    val rt = havdalah?.let { null } // leave null here; app will compute RT from Zmanim if desired.

                    ShabbatSummary(
                        cityDisplay = city.display,
                        gregDate = shabbatDate,
                        hebrewDate = heb,
                        parashaHeb = parashaHeb,
                        candleLighting = candle,
                        havdalah = havdalah,
                        havdalahRT72 = rt
                    )
                }
            } catch (t: Throwable) {
                Log.e("Zmanim", "Shabbat API failure", t)
                null
            }
        }

    private fun parseZoned(iso: String?, tz: ZoneId): ZonedDateTime? {
        if (iso == null) return null
        // Hebcal sends ISO with zone offset; normalize to tz
        return try {
            val instant = Instant.parse(iso.replace(" ", "T").substringBeforeLast('+') + "Z")
            instant.atZone(tz)
        } catch (_: Throwable) {
            try { ZonedDateTime.parse(iso).withZoneSameInstant(tz) } catch (_: Throwable) { null }
        }
    }
}
