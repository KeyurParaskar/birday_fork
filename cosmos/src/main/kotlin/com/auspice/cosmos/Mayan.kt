package com.auspice.cosmos

import java.time.LocalDate
import java.time.temporal.JulianFields

/** The twenty Tzolk'in day signs in order, Yucatec spelling. */
enum class TzolkinSign(val meaning: String) {
    IMIX("crocodile"), IK("wind"), AKBAL("night"), KAN("seed"), CHICCHAN("serpent"),
    CIMI("death"), MANIK("deer"), LAMAT("star"), MULUC("water"), OC("dog"),
    CHUEN("monkey"), EB("road"), BEN("reed"), IX("jaguar"), MEN("eagle"),
    CIB("owl"), CABAN("earth"), ETZNAB("flint"), CAUAC("storm"), AHAU("sun");

    val key: String get() = "mayan.sign." + name.lowercase()
}

/** A Tzolk'in date: a number 1..13 and one of twenty day signs, repeating every 260 days. */
data class TzolkinDate(val number: Int, val sign: TzolkinSign) {
    companion object {
        /** The GMT correlation: Julian Day 584283 corresponds to 4 Ahau 8 Cumku. */
        private const val GMT_CORRELATION = 584283L

        fun forDate(date: LocalDate): TzolkinDate {
            val days = date.getLong(JulianFields.JULIAN_DAY) - GMT_CORRELATION
            val number = Math.floorMod(days + 3, 13L).toInt() + 1
            val sign = TzolkinSign.entries[Math.floorMod(days + 19, 20L).toInt()]
            return TzolkinDate(number, sign)
        }
    }
}
