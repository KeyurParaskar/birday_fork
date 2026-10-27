package com.auspice.cosmos

import java.text.Normalizer
import java.time.LocalDate

/**
 * Pythagorean (Western) numerology. Numbers are reduced to a single digit except the master numbers
 * 11, 22 and 33, which are kept as they are. A Chaldean variant is planned (see docs/PLAN.md).
 */
object Numerology {
    val MASTER_NUMBERS = setOf(11, 22, 33)

    /** Reduces [n] digit by digit until a single digit or a master number remains. */
    fun reduce(n: Int, keepMasters: Boolean = true): Int {
        var value = Math.abs(n)
        while (value > 9 && !(keepMasters && value in MASTER_NUMBERS)) {
            value = value.toString().sumOf { it - '0' }
        }
        return value
    }

    /**
     * Life Path: month, day and year are each reduced first, then summed and reduced. This is the method
     * that preserves master numbers correctly (reducing the raw digit string can lose them).
     */
    fun lifePath(date: LocalDate): Int =
        reduce(reduce(date.monthValue) + reduce(date.dayOfMonth) + reduce(date.year))

    /** Birthday number: the day of the month, reduced. 11, 22 stay master numbers. */
    fun birthday(date: LocalDate): Int = reduce(date.dayOfMonth)

    /** Attitude (or Sun) number: month plus day, reduced. */
    fun attitude(date: LocalDate): Int = reduce(reduce(date.monthValue) + reduce(date.dayOfMonth))

    /** Personal year for [inYear]: month plus day plus the year in question, reduced. */
    fun personalYear(date: LocalDate, inYear: Int): Int =
        reduce(reduce(date.monthValue) + reduce(date.dayOfMonth) + reduce(inYear), keepMasters = false)

    /** Expression (Destiny) number from the full name: all letters. */
    fun expression(fullName: String): Int = reduce(letters(fullName).sumOf(::value))

    /** Soul Urge (Heart's Desire): vowels only. */
    fun soulUrge(fullName: String): Int = reduce(letters(fullName).filter(::isVowel).sumOf(::value))

    /** Personality number: consonants only. */
    fun personality(fullName: String): Int = reduce(letters(fullName).filterNot(::isVowel).sumOf(::value))

    /** Pythagorean grid: A=1 ... I=9, J=1 ... R=9, S=1 ... Z=8. */
    fun value(letter: Char): Int = ((letter.uppercaseChar() - 'A') % 9) + 1

    // Y is treated as a consonant, the most common convention. Accents are stripped so "José" works.
    private fun isVowel(c: Char) = c.uppercaseChar() in "AEIOU"

    private fun letters(name: String): List<Char> =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .filter { it in 'A'..'Z' || it in 'a'..'z' }
            .toList()
}
