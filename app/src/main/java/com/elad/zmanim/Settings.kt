package com.elad.zmanim

/** Which calculation 'board' the user selected; shown in UI */
enum class BoardPreset(val display: String) {
    GRA("גר״א"),
    MGA("מג״א"),
    OR_HACHAIM("אור החיים"),
    RABEINU_TAM("רבנו תם"),
    CUSTOM("מותאם אישית")
}

data class AppSettings(
    val board: BoardPreset = BoardPreset.GRA,
    val candleOffsetMinutes: Int? = null // null => use city default
)
