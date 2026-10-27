package com.auspice.cosmos

import java.time.LocalDate
import java.time.MonthDay

enum class Element { FIRE, EARTH, AIR, WATER }
enum class Modality { CARDINAL, FIXED, MUTABLE }

/**
 * The twelve tropical (Western) sun signs with the commonly published cusp dates.
 * Real cusps shift by a day depending on the year and time zone; a later phase can refine this with
 * an actual solar-longitude computation. The ordering starts at Aries, the traditional first sign.
 */
enum class WesternSign(
    val symbol: String,
    val element: Element,
    val modality: Modality,
    val rulingPlanet: String,
    private val startMonth: Int,
    private val startDay: Int,
) {
    ARIES("♈", Element.FIRE, Modality.CARDINAL, "Mars", 3, 21),
    TAURUS("♉", Element.EARTH, Modality.FIXED, "Venus", 4, 20),
    GEMINI("♊", Element.AIR, Modality.MUTABLE, "Mercury", 5, 21),
    CANCER("♋", Element.WATER, Modality.CARDINAL, "Moon", 6, 21),
    LEO("♌", Element.FIRE, Modality.FIXED, "Sun", 7, 23),
    VIRGO("♍", Element.EARTH, Modality.MUTABLE, "Mercury", 8, 23),
    LIBRA("♎", Element.AIR, Modality.CARDINAL, "Venus", 9, 23),
    SCORPIO("♏", Element.WATER, Modality.FIXED, "Pluto", 10, 23),
    SAGITTARIUS("♐", Element.FIRE, Modality.MUTABLE, "Jupiter", 11, 22),
    CAPRICORN("♑", Element.EARTH, Modality.CARDINAL, "Saturn", 12, 22),
    AQUARIUS("♒", Element.AIR, Modality.FIXED, "Uranus", 1, 20),
    PISCES("♓", Element.WATER, Modality.MUTABLE, "Neptune", 2, 19);

    val start: MonthDay get() = MonthDay.of(startMonth, startDay)

    /** Stable key for localisation, e.g. "western.sign.leo". */
    val key: String get() = "western.sign." + name.lowercase()

    /** Signs are considered compatible when they share an element or are the "complementary" elements. */
    fun isHarmoniousWith(other: WesternSign): Boolean {
        if (element == other.element) return true
        return when (element) {
            Element.FIRE -> other.element == Element.AIR
            Element.AIR -> other.element == Element.FIRE
            Element.EARTH -> other.element == Element.WATER
            Element.WATER -> other.element == Element.EARTH
        }
    }

    companion object {
        fun forDate(date: LocalDate): WesternSign {
            val md = MonthDay.from(date)
            // Capricorn wraps the year end: it runs from Dec 22 to Jan 19.
            if (md >= CAPRICORN.start || md < AQUARIUS.start) return CAPRICORN
            return entries.filter { it != CAPRICORN && md >= it.start }.maxBy { it.start }
        }
    }
}
