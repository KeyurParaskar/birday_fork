package com.auspice.cosmos

import java.text.Normalizer
import java.time.LocalDate

/**
 * How letters become numbers. Pythagorean is the common Western grid (A=1 ... I=9, then repeat).
 * Chaldean is the older Babylonian-attributed table: values 1 to 8 assigned by sound, 9 never used.
 */
enum class NumerologySystem { PYTHAGOREAN, CHALDEAN }

/**
 * Numerology. Numbers are reduced to a single digit except the master numbers 11, 22 and 33, which are kept.
 * Date numbers are the same in both systems; name numbers depend on the [NumerologySystem].
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

    /** Expression (Destiny, or Name number in Chaldean) from the full name: all letters. */
    fun expression(fullName: String, system: NumerologySystem = NumerologySystem.PYTHAGOREAN): Int =
        reduce(compoundExpression(fullName, system))

    /** The unreduced letter sum. Chaldean readings give meaning to these "compound" numbers too. */
    fun compoundExpression(fullName: String, system: NumerologySystem = NumerologySystem.PYTHAGOREAN): Int =
        letters(fullName).sumOf { value(it, system) }

    /** Soul Urge (Heart's Desire): vowels only. */
    fun soulUrge(fullName: String, system: NumerologySystem = NumerologySystem.PYTHAGOREAN): Int =
        reduce(letters(fullName).filter(::isVowel).sumOf { value(it, system) })

    /** Personality number: consonants only. */
    fun personality(fullName: String, system: NumerologySystem = NumerologySystem.PYTHAGOREAN): Int =
        reduce(letters(fullName).filterNot(::isVowel).sumOf { value(it, system) })

    /** Pythagorean grid: A=1 ... I=9, J=1 ... R=9, S=1 ... Z=8. */
    fun value(letter: Char): Int = ((letter.uppercaseChar() - 'A') % 9) + 1

    // Chaldean table, A to Z
    private val CHALDEAN = intArrayOf(1, 2, 3, 4, 5, 8, 3, 5, 1, 1, 2, 3, 4, 5, 7, 8, 1, 2, 3, 4, 6, 6, 6, 5, 1, 7)

    fun value(letter: Char, system: NumerologySystem): Int = when (system) {
        NumerologySystem.PYTHAGOREAN -> value(letter)
        NumerologySystem.CHALDEAN -> CHALDEAN[letter.uppercaseChar() - 'A']
    }

    /** Life Path numbers that numerologists usually group as natural matches. Masters count as their root. */
    fun lifePathFamily(n: Int): Int = when (reduce(n, keepMasters = false)) {
        1, 5, 7 -> 0
        2, 4, 8 -> 1
        else -> 2
    }

    // Y is treated as a consonant, the most common convention. Accents are stripped so "José" works.
    private fun isVowel(c: Char) = c.uppercaseChar() in "AEIOU"

    private fun letters(name: String): List<Char> =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .filter { it in 'A'..'Z' || it in 'a'..'z' }
            .toList()
}
