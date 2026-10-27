package com.auspice.cosmos

import java.time.LocalDate
import java.time.MonthDay

/**
 * The "Celtic tree calendar" of thirteen lunar months, each tied to a letter of the Ogham alphabet.
 * It is a modern construction popularised by Robert Graves in The White Goddess (1948), not an attested
 * ancient calendar, and Auspice labels it that way. The dates are the widely published ones.
 */
enum class CelticTree(val ogham: String, private val startMonth: Int, private val startDay: Int) {
    BIRCH("Beith", 12, 24),
    ROWAN("Luis", 1, 21),
    ASH("Nion", 2, 18),
    ALDER("Fearn", 3, 18),
    WILLOW("Saille", 4, 15),
    HAWTHORN("Huath", 5, 13),
    OAK("Duir", 6, 10),
    HOLLY("Tinne", 7, 8),
    HAZEL("Coll", 8, 5),
    VINE("Muin", 9, 2),
    IVY("Gort", 9, 30),
    REED("Ngetal", 10, 28),
    ELDER("Ruis", 11, 25);

    val start: MonthDay get() = MonthDay.of(startMonth, startDay)

    companion object {
        /** 23 December falls outside the thirteen months: the "nameless day". Returns null for it. */
        fun forDate(date: LocalDate): CelticTree? {
            val md = MonthDay.from(date)
            if (md == MonthDay.of(12, 23)) return null
            // Birch wraps the year end, from 24 December to 20 January
            if (md >= BIRCH.start || md < ROWAN.start) return BIRCH
            return entries.filter { it != BIRCH && md >= it.start }.maxBy { it.start }
        }
    }
}
