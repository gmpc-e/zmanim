package com.elad.zmanim

/** City/location model used across MainActivity, TopBar, and ShabbatRepository. */
data class City(
    val display: String,
    val lat: Double,
    val lon: Double,
    val tzid: String = "Asia/Jerusalem",
    val defaultCandleOffsetMin: Int = 18,
    val elevationMeters: Double = 0.0
)

/** Starter list — tweak/add cities as you like. */
object Cities {
    val all = listOf(
        City("הוד השרון", 32.1559, 34.8880, elevationMeters = 45.0),
        City("רעננה",     32.1848, 34.8713, elevationMeters = 55.0),
        City("תל אביב",   32.0853, 34.7818, elevationMeters = 15.0),
        City("ירושלים",   31.7683, 35.2137, defaultCandleOffsetMin = 40, elevationMeters = 800.0),
        City("חיפה",      32.7940, 34.9896, defaultCandleOffsetMin = 30, elevationMeters = 280.0),
        City("באר שבע",   31.2520, 34.7915, elevationMeters = 300.0),
        City("פתח תקווה", 32.0917, 34.8850, elevationMeters = 40.0),
        City("נתניה",     32.3215, 34.8532, elevationMeters = 20.0)
    )
}
