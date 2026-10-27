package com.auspice.cosmos

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.chrono.HijrahDate

/** Modern (Jewelers of America, 1912 with later additions) birthstones and traditional birth flowers. */
object BirthSymbols {
    data class MonthSymbols(val stones: List<String>, val flowers: List<String>)

    private val byMonth = mapOf(
        Month.JANUARY to MonthSymbols(listOf("Garnet"), listOf("Carnation", "Snowdrop")),
        Month.FEBRUARY to MonthSymbols(listOf("Amethyst"), listOf("Violet", "Primrose")),
        Month.MARCH to MonthSymbols(listOf("Aquamarine", "Bloodstone"), listOf("Daffodil", "Jonquil")),
        Month.APRIL to MonthSymbols(listOf("Diamond"), listOf("Daisy", "Sweet Pea")),
        Month.MAY to MonthSymbols(listOf("Emerald"), listOf("Lily of the Valley", "Hawthorn")),
        Month.JUNE to MonthSymbols(listOf("Pearl", "Alexandrite", "Moonstone"), listOf("Rose", "Honeysuckle")),
        Month.JULY to MonthSymbols(listOf("Ruby"), listOf("Larkspur", "Water Lily")),
        Month.AUGUST to MonthSymbols(listOf("Peridot", "Spinel", "Sardonyx"), listOf("Gladiolus", "Poppy")),
        Month.SEPTEMBER to MonthSymbols(listOf("Sapphire"), listOf("Aster", "Morning Glory")),
        Month.OCTOBER to MonthSymbols(listOf("Opal", "Tourmaline"), listOf("Marigold", "Cosmos")),
        Month.NOVEMBER to MonthSymbols(listOf("Topaz", "Citrine"), listOf("Chrysanthemum")),
        Month.DECEMBER to MonthSymbols(listOf("Turquoise", "Tanzanite", "Zircon"), listOf("Narcissus", "Holly")),
    )

    fun forMonth(month: Month): MonthSymbols = byMonth.getValue(month)
}

/** Things different cultures read into the day of the week someone was born on. */
object DayLore {
    /** The English nursery rhyme "Monday's child is fair of face". */
    fun nurseryRhyme(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Monday's child is fair of face"
        DayOfWeek.TUESDAY -> "Tuesday's child is full of grace"
        DayOfWeek.WEDNESDAY -> "Wednesday's child is full of woe"
        DayOfWeek.THURSDAY -> "Thursday's child has far to go"
        DayOfWeek.FRIDAY -> "Friday's child is loving and giving"
        DayOfWeek.SATURDAY -> "Saturday's child works hard for a living"
        DayOfWeek.SUNDAY -> "The child born on the Sabbath day is bonny and blithe and good and gay"
    }

    /** Thai tradition assigns each weekday a colour (and a Buddha posture) considered lucky for that person. */
    fun thaiColour(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Yellow"
        DayOfWeek.TUESDAY -> "Pink"
        DayOfWeek.WEDNESDAY -> "Green"
        DayOfWeek.THURSDAY -> "Orange"
        DayOfWeek.FRIDAY -> "Light blue"
        DayOfWeek.SATURDAY -> "Purple"
        DayOfWeek.SUNDAY -> "Red"
    }

    /** Akan (Ghana) day names, given to children after the weekday of their birth. Returns masculine to feminine. */
    fun akanDayNames(day: DayOfWeek): Pair<String, String> = when (day) {
        DayOfWeek.MONDAY -> "Kwadwo" to "Adwoa"
        DayOfWeek.TUESDAY -> "Kwabena" to "Abena"
        DayOfWeek.WEDNESDAY -> "Kwaku" to "Akua"
        DayOfWeek.THURSDAY -> "Yaw" to "Yaa"
        DayOfWeek.FRIDAY -> "Kofi" to "Afua"
        DayOfWeek.SATURDAY -> "Kwame" to "Ama"
        DayOfWeek.SUNDAY -> "Kwasi" to "Akosua"
    }
}

/** Birth dates in other religious calendars that the JDK can compute without extra data. */
object OtherCalendars {
    /** Islamic (Hijri, Umm al-Qura tables in the JDK) rendering of a Gregorian date. */
    fun hijri(date: LocalDate): HijrahDate = HijrahDate.from(date)
}
