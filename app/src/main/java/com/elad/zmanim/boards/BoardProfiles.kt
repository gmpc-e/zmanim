package com.elad.zmanim.boards

import com.elad.zmanim.ZmanResults
import java.time.Duration
import java.time.LocalTime

enum class SofZmanRef { MGA, GRA }
enum class TzeitMode { STANDARD, RABEINU_TAM_72 }

data class BoardProfile(
    val name: String,
    val sofZmanRef: SofZmanRef,
    val misheyakirOffsetMin: Int = 30,
    val tzeitPrimaryMode: TzeitMode = TzeitMode.STANDARD,
    val showSecondaryTzeit: Boolean = false,
    val explanations: Map<String, String> = emptyMap()
)

data class SelectedZmanim(
    val misheyakir: LocalTime?,
    val sunrise: LocalTime?,
    val chatzot: LocalTime?,
    val minchaGedola: LocalTime?,
    val minchaKetana: LocalTime?,
    val plag: LocalTime?,
    val sunset: LocalTime?,
    val tzeitPrimary: LocalTime?,
    val tzeitSecondary: LocalTime?,
    val sofZmanShmaMga: LocalTime?,
    val sofZmanShmaGra: LocalTime?,
    val sofZmanTfilaMga: LocalTime?,
    val sofZmanTfilaGra: LocalTime?
)

enum class BoardPreset { GRA, MGA, OR_HA_CHAIM, RABEINU_TAM }

val BOARD_PROFILES: Map<BoardPreset, BoardProfile> = mapOf(
    BoardPreset.GRA to BoardProfile(
        name = "גר\"א",
        sofZmanRef = SofZmanRef.GRA,
        misheyakirOffsetMin = 30,
        tzeitPrimaryMode = TzeitMode.STANDARD,
        showSecondaryTzeit = true,
        explanations = mapOf(
            "alos" to "עלות השחר: תחילת היום ההלכתי. הערך משתנה לפי שיטות; כאן ברירת מחדל.",
            "misheyakir" to "משיכיר: זמן הנחת תפילין כאשר ניתן להכיר את חברו ממרחק קצר.",
            "sunrise" to "זריחה: נראית הנצילות הראשונה של השמש מעל האופק (חישוב).",
            "chatzot" to "חצות היום: אמצע היום ההלכתי, בין הנץ לשקיעה.",
            "minchaGedola" to "מנחה גדולה: חצות + חצי שעה זמנית.",
            "minchaKetana" to "מנחה קטנה: 9.5 שעות זמניות מתחילת היום.",
            "plag" to "פלג המנחה: שעה ורבע זמנית לפני שקיעה.",
            "sunset" to "שקיעה: סוף היום האסטרונומי.",
            "tzeit" to "צאת הכוכבים: סוף היום ההלכתי, לפי שיטות שונות.",
            "sofZmanShma" to "סוף זמן ק\"ש: לפי מג\"א או גר\"א (שעות זמניות)."
        )
    ),
    BoardPreset.MGA to BoardProfile(
        name = "מג\"א",
        sofZmanRef = SofZmanRef.MGA,
        misheyakirOffsetMin = 35,
        tzeitPrimaryMode = TzeitMode.STANDARD,
        showSecondaryTzeit = true
    ),
    BoardPreset.OR_HA_CHAIM to BoardProfile(
        name = "אור החיים",
        sofZmanRef = SofZmanRef.MGA,
        misheyakirOffsetMin = 30,
        tzeitPrimaryMode = TzeitMode.STANDARD,
        showSecondaryTzeit = true
    ),
    BoardPreset.RABEINU_TAM to BoardProfile(
        name = "רבינו תם",
        sofZmanRef = SofZmanRef.GRA,
        misheyakirOffsetMin = 30,
        tzeitPrimaryMode = TzeitMode.RABEINU_TAM_72,
        showSecondaryTzeit = true
    )
)

// Central default
val DEFAULT_PROFILE: BoardProfile = BOARD_PROFILES[BoardPreset.GRA]!!

// Pre-resolved constants to avoid Map lookups inside Composables
val PROFILE_GRA: BoardProfile = BOARD_PROFILES[BoardPreset.GRA]!!
val PROFILE_MGA: BoardProfile = BOARD_PROFILES[BoardPreset.MGA]!!
val PROFILE_OHC: BoardProfile = BOARD_PROFILES[BoardPreset.OR_HA_CHAIM]!!
val PROFILE_RT:  BoardProfile = BOARD_PROFILES[BoardPreset.RABEINU_TAM]!!

/** Safe helper used by UI: uses the boards enum (no cross-package enum mixing). */
fun profileFor(board: BoardPreset): BoardProfile = when (board) {
    BoardPreset.GRA -> PROFILE_GRA
    BoardPreset.MGA -> PROFILE_MGA
    BoardPreset.OR_HA_CHAIM -> PROFILE_OHC
    BoardPreset.RABEINU_TAM -> PROFILE_RT
}

private fun midPoint(a: LocalTime?, b: LocalTime?): LocalTime? {
    if (a == null || b == null) return null
    val aSec = a.toSecondOfDay()
    val bSec = b.toSecondOfDay()
    val mid = ((aSec + bSec) / 2)
    return LocalTime.ofSecondOfDay(mid.toLong())
}

private fun minutesBetween(a: LocalTime?, b: LocalTime?): Long? {
    if (a == null || b == null) return null
    return Duration.between(a, b).toMinutes()
}

/**
 * Select which times to surface for a given board profile.
 * GRA hour = (sunrise..sunset)/12
 * MGA hour = (alos..tzeitStandard)/12
 */
fun selectByProfile(z: com.elad.zmanim.ZmanResults, profile: BoardProfile): SelectedZmanim {
    val sunrise = z.sunriseSeaLevel
    val sunset = z.sunsetSeaLevel
    val alos = z.alosHashachar
    val tzeitStd = z.tzeitStandard
    val tzeitRT = z.tzeitRT72

    val misheyakir = sunrise?.minusMinutes(profile.misheyakirOffsetMin.toLong())
    val chatzot = z.chatzot ?: midPoint(sunrise, sunset)

    val graHourMin = minutesBetween(sunrise, sunset)?.let { it / 12.0 }
    val mgaHourMin = minutesBetween(alos, tzeitStd)?.let { it / 12.0 }

    fun addMinutesD(base: LocalTime?, minutesD: Double?): LocalTime? {
        if (base == null || minutesD == null) return null
        return base.plusMinutes(minutesD.toLong())
    }

    val sofShmaGra   = addMinutesD(sunrise, graHourMin?.times(3.0))
    val sofTfilaGra  = addMinutesD(sunrise, graHourMin?.times(4.0))
    val sofShmaMga   = addMinutesD(alos,    mgaHourMin?.times(3.0))
    val sofTfilaMga  = addMinutesD(alos,    mgaHourMin?.times(4.0))

    val minchaGedola = z.minchaGedola ?: addMinutesD(chatzot, graHourMin?.times(0.5))
    val minchaKetana = z.minchaKetana ?: addMinutesD(sunrise, graHourMin?.times(9.5))

    // Always derive Plag (GRA basis)
    val plag =
        if (sunset == null || graHourMin == null) null
        else sunset.minusMinutes(graHourMin.times(1.25).toLong())

    val tzeitPrimary = when (profile.tzeitPrimaryMode) {
        TzeitMode.STANDARD -> tzeitStd
        TzeitMode.RABEINU_TAM_72 -> tzeitRT
    }
    val tzeitSecondary =
        if (profile.showSecondaryTzeit) {
            when (profile.tzeitPrimaryMode) {
                TzeitMode.STANDARD -> tzeitRT
                TzeitMode.RABEINU_TAM_72 -> tzeitStd
            }
        } else null

    return SelectedZmanim(
        misheyakir = misheyakir,
        sunrise = sunrise,
        chatzot = chatzot,
        minchaGedola = minchaGedola,
        minchaKetana = minchaKetana,
        plag = plag,
        sunset = sunset,
        tzeitPrimary = tzeitPrimary,
        tzeitSecondary = tzeitSecondary,
        sofZmanShmaMga = sofShmaMga,
        sofZmanShmaGra = sofShmaGra,
        sofZmanTfilaMga = sofTfilaMga,
        sofZmanTfilaGra = sofTfilaGra
    )
}
