package com.auspice.cosmos

import java.time.LocalDate

/**
 * A date in the Hebrew calendar. Months use the biblical numbering from Calendrical Calculations
 * (Dershowitz and Reingold): Nisan = 1 ... Elul = 6, Tishri = 7 ... Adar = 12, Adar II = 13 (leap years only).
 * The civil year starts at Tishri, so Tishri comes before Nisan within one year number.
 */
data class HebrewDate(val year: Int, val month: Int, val day: Int) {

    fun monthName(): String = HebrewCalendar.monthName(month, year)

    /** "23 Tevet 5760" */
    override fun toString(): String = "$day ${monthName()} $year"
}

/**
 * Arithmetic Hebrew calendar, ported from Calendrical Calculations (3rd ed.), chapter 8.
 * Dates are exchanged as "fixed" day numbers (rata die, RD 1 = 1 January 1 CE, proleptic Gregorian).
 */
object HebrewCalendar {
    const val NISAN = 1
    const val IYYAR = 2
    const val SIVAN = 3
    const val TAMMUZ = 4
    const val AV = 5
    const val ELUL = 6
    const val TISHRI = 7
    const val MARHESHVAN = 8
    const val KISLEV = 9
    const val TEVET = 10
    const val SHEVAT = 11
    const val ADAR = 12
    const val ADAR_II = 13

    /** RD of 1 Tishri AM 1 (7 October 3761 BCE, Julian) */
    private const val EPOCH = -1373427L

    /** RD of 1970-01-01, so that RD = epochDay + this */
    private const val RD_UNIX_EPOCH = 719163L

    private val NAMES = mapOf(
        NISAN to "Nisan", IYYAR to "Iyar", SIVAN to "Sivan", TAMMUZ to "Tammuz", AV to "Av", ELUL to "Elul",
        TISHRI to "Tishrei", MARHESHVAN to "Cheshvan", KISLEV to "Kislev", TEVET to "Tevet", SHEVAT to "Shevat",
    )

    fun monthName(month: Int, year: Int): String = when {
        month == ADAR -> if (isLeapYear(year)) "Adar I" else "Adar"
        month == ADAR_II -> "Adar II"
        else -> NAMES.getValue(month)
    }

    fun isLeapYear(year: Int): Boolean = Math.floorMod(7L * year + 1, 19L) < 7

    fun lastMonthOfYear(year: Int): Int = if (isLeapYear(year)) ADAR_II else ADAR

    /** Days from the epoch to the molad of Tishri of [year], with the first set of postponements. */
    private fun elapsedDays(year: Int): Long {
        val monthsElapsed = Math.floorDiv(235L * year - 234, 19L)
        val partsElapsed = 12084L + 13753L * monthsElapsed
        val days = 29L * monthsElapsed + Math.floorDiv(partsElapsed, 25920L)
        return if (Math.floorMod(3 * (days + 1), 7L) < 3) days + 1 else days
    }

    /** The remaining postponements, which keep years within the legal lengths. */
    private fun yearLengthCorrection(year: Int): Long {
        val ny0 = elapsedDays(year - 1)
        val ny1 = elapsedDays(year)
        val ny2 = elapsedDays(year + 1)
        return when {
            ny2 - ny1 == 356L -> 2
            ny1 - ny0 == 382L -> 1
            else -> 0
        }
    }

    /** RD of 1 Tishri (Rosh Hashanah) of [year]. */
    fun newYear(year: Int): Long = EPOCH + elapsedDays(year) + yearLengthCorrection(year)

    fun daysInYear(year: Int): Int = (newYear(year + 1) - newYear(year)).toInt()

    private fun isLongMarheshvan(year: Int) = daysInYear(year) % 10 == 5
    private fun isShortKislev(year: Int) = daysInYear(year) % 10 == 3

    fun daysInMonth(month: Int, year: Int): Int = when {
        month == IYYAR || month == TAMMUZ || month == ELUL || month == TEVET || month == ADAR_II -> 29
        month == ADAR && !isLeapYear(year) -> 29
        month == MARHESHVAN && !isLongMarheshvan(year) -> 29
        month == KISLEV && isShortKislev(year) -> 29
        else -> 30
    }

    /** RD of the given Hebrew date. */
    fun toFixed(year: Int, month: Int, day: Int): Long {
        var days = newYear(year) + day - 1
        if (month < TISHRI) {
            for (m in TISHRI..lastMonthOfYear(year)) days += daysInMonth(m, year)
            for (m in NISAN until month) days += daysInMonth(m, year)
        } else {
            for (m in TISHRI until month) days += daysInMonth(m, year)
        }
        return days
    }

    fun fromFixed(fixed: Long): HebrewDate {
        // Mean year length is 35975351 / 98496 days
        val approx = (Math.floorDiv((fixed - EPOCH) * 98496L, 35975351L) + 1).toInt()
        var year = approx - 1
        while (newYear(year + 1) <= fixed) year++
        val start = if (fixed < toFixed(year, NISAN, 1)) TISHRI else NISAN
        var month = start
        while (fixed > toFixed(year, month, daysInMonth(month, year))) month++
        val day = (fixed - toFixed(year, month, 1) + 1).toInt()
        return HebrewDate(year, month, day)
    }

    fun fromGregorian(date: LocalDate): HebrewDate = fromFixed(date.toEpochDay() + RD_UNIX_EPOCH)

    fun toGregorian(date: HebrewDate): LocalDate = toGregorian(date.year, date.month, date.day)

    fun toGregorian(year: Int, month: Int, day: Int): LocalDate =
        LocalDate.ofEpochDay(toFixed(year, month, day) - RD_UNIX_EPOCH)

    /**
     * Where the Hebrew birthday of someone born on [birth] falls in Hebrew year [inYear].
     * Someone born in the last month of the year (Adar, or Adar II in a leap year) celebrates in the last month
     * of the target year; a 30th that does not exist in the target month rolls into the next day.
     */
    fun birthdayIn(birth: HebrewDate, inYear: Int): LocalDate {
        val fixed = if (birth.month == lastMonthOfYear(birth.year)) {
            toFixed(inYear, lastMonthOfYear(inYear), birth.day)
        } else {
            toFixed(inYear, birth.month, 1) + birth.day - 1
        }
        return LocalDate.ofEpochDay(fixed - RD_UNIX_EPOCH)
    }

    /** The first Gregorian date on or after [from] that is the Hebrew birthday of someone born on [birthDate]. */
    fun nextBirthday(birthDate: LocalDate, from: LocalDate): LocalDate {
        val birth = fromGregorian(birthDate)
        val currentYear = fromGregorian(from).year
        return (currentYear..currentYear + 1)
            .map { birthdayIn(birth, it) }
            .first { !it.isBefore(from) }
    }
}
