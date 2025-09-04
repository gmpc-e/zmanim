package com.elad.zmanim

data class City(val display: String, val lat: Double, val lon: Double, val tzid: String = "Asia/Jerusalem", val defaultCandleOffsetMin: Int = 18)

object Cities {
    val all = listOf(
        City("הוד השרון", 32.1559, 34.8880, defaultCandleOffsetMin = 18),
        City("רעננה",     32.1848, 34.8713, defaultCandleOffsetMin = 18),
        City("תל אביב",   32.0853, 34.7818, defaultCandleOffsetMin = 18),
        City("ירושלים",   31.7683, 35.2137, defaultCandleOffsetMin = 40), // ירושלים 40 דק׳
        City("חיפה",      32.7940, 34.9896, defaultCandleOffsetMin = 30), // חיפה 30 דק׳
        City("באר שבע",   31.2520, 34.7915, defaultCandleOffsetMin = 18),
        City("פתח תקווה", 32.0917, 34.8850, defaultCandleOffsetMin = 18),
        City("נתניה",     32.3215, 34.8532, defaultCandleOffsetMin = 18)
    )
}
