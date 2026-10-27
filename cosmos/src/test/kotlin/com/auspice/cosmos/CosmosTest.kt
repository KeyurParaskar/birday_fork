package com.auspice.cosmos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WesternZodiacTest {
    @Test fun cuspsAndWrapAround() {
        assertEquals(WesternSign.CAPRICORN, WesternSign.forDate(LocalDate.of(2000, 12, 22)))
        assertEquals(WesternSign.CAPRICORN, WesternSign.forDate(LocalDate.of(2000, 1, 1)))
        assertEquals(WesternSign.CAPRICORN, WesternSign.forDate(LocalDate.of(2000, 1, 19)))
        assertEquals(WesternSign.AQUARIUS, WesternSign.forDate(LocalDate.of(2000, 1, 20)))
        assertEquals(WesternSign.PISCES, WesternSign.forDate(LocalDate.of(2000, 3, 20)))
        assertEquals(WesternSign.ARIES, WesternSign.forDate(LocalDate.of(2000, 3, 21)))
        assertEquals(WesternSign.LEO, WesternSign.forDate(LocalDate.of(1990, 8, 10)))
        assertEquals(WesternSign.SAGITTARIUS, WesternSign.forDate(LocalDate.of(1990, 12, 21)))
    }

    @Test fun harmony() {
        assertTrue(WesternSign.LEO.isHarmoniousWith(WesternSign.GEMINI))   // fire + air
        assertTrue(WesternSign.TAURUS.isHarmoniousWith(WesternSign.CANCER)) // earth + water
        assertFalse(WesternSign.LEO.isHarmoniousWith(WesternSign.SCORPIO))  // fire + water
    }
}

class ChineseZodiacTest {
    @Test fun sexagenaryCycle() {
        val dragon2024 = ChineseSign.forChineseYear(2024)
        assertEquals(ChineseAnimal.DRAGON, dragon2024.animal)
        assertEquals(ChineseElement.WOOD, dragon2024.element)
        assertEquals(Polarity.YANG, dragon2024.polarity)

        val snake2025 = ChineseSign.forChineseYear(2025)
        assertEquals(ChineseAnimal.SNAKE, snake2025.animal)
        assertEquals(ChineseElement.WOOD, snake2025.element)
        assertEquals(Polarity.YIN, snake2025.polarity)

        assertEquals(ChineseAnimal.RAT, ChineseSign.forChineseYear(2020).animal)
        assertEquals(ChineseElement.METAL, ChineseSign.forChineseYear(2020).element)
        assertEquals(ChineseAnimal.PIG, ChineseSign.forChineseYear(1899).animal)
    }

    @Test fun lunarNewYearResolverShiftsEarlyDates() {
        val resolver: (Int) -> LocalDate? = { year -> if (year == 2020) LocalDate.of(2020, 1, 25) else null }
        assertEquals(ChineseAnimal.PIG, ChineseSign.forDate(LocalDate.of(2020, 1, 20), resolver).animal)
        assertEquals(ChineseAnimal.RAT, ChineseSign.forDate(LocalDate.of(2020, 1, 28), resolver).animal)
        // Without a resolver the Gregorian year is used (documented limitation)
        assertEquals(ChineseAnimal.RAT, ChineseSign.forDate(LocalDate.of(2020, 1, 20)).animal)
    }

    @Test fun relationships() {
        assertTrue(ChineseSign.forChineseYear(2020).clashesWith(ChineseAnimal.HORSE)) // Rat vs Horse
        assertTrue(ChineseSign.forChineseYear(2020).isInTrineWith(ChineseAnimal.DRAGON))
        assertTrue(ChineseSign.forChineseYear(2020).isInTrineWith(ChineseAnimal.MONKEY))
        assertFalse(ChineseSign.forChineseYear(2020).isInTrineWith(ChineseAnimal.OX))
    }
}

class NumerologyTest {
    @Test fun reduction() {
        assertEquals(7, Numerology.reduce(16))
        assertEquals(11, Numerology.reduce(11))
        assertEquals(2, Numerology.reduce(11, keepMasters = false))
        assertEquals(22, Numerology.reduce(1993))  // 1+9+9+3 = 22
        assertEquals(1, Numerology.reduce(10))
    }

    @Test fun lifePath() {
        // 1990-08-10: month 8, day 1, year 1+9+9+0=19 -> 1 ; 8+1+1 = 10 -> 1
        assertEquals(1, Numerology.lifePath(LocalDate.of(1990, 8, 10)))
        // 1993-11-29: month 11 (master), day 2+9=11 (master), year 22 (master); 11+11+22 = 44 -> 8
        assertEquals(8, Numerology.lifePath(LocalDate.of(1993, 11, 29)))
        // 2000-02-09: 2 + 9 + 2 = 13 -> 4
        assertEquals(4, Numerology.lifePath(LocalDate.of(2000, 2, 9)))
    }

    @Test fun birthdayAndPersonalYear() {
        assertEquals(11, Numerology.birthday(LocalDate.of(2000, 1, 29)))
        assertEquals(5, Numerology.birthday(LocalDate.of(2000, 1, 14)))
        // 08-10 in 2026: 8 + 1 + (2+0+2+6=10->1) = 10 -> 1
        assertEquals(1, Numerology.personalYear(LocalDate.of(1990, 8, 10), 2026))
    }

    @Test fun nameNumbers() {
        assertEquals(1, Numerology.value('A'))
        assertEquals(9, Numerology.value('I'))
        assertEquals(1, Numerology.value('J'))
        assertEquals(8, Numerology.value('Z'))
        // "John": J1 O6 H8 N5 = 20 -> 2 ; vowels O=6 ; consonants J+H+N = 14 -> 5
        assertEquals(2, Numerology.expression("John"))
        assertEquals(6, Numerology.soulUrge("John"))
        assertEquals(5, Numerology.personality("John"))
        // Accents and spaces are ignored: "José" -> J O S E = 1+6+1+5 = 13 -> 4
        assertEquals(4, Numerology.expression("José"))
        assertEquals(Numerology.expression("Jose"), Numerology.expression("José "))
    }
}

class MayanTest {
    @Test fun knownCorrelations() {
        // Baktun 13 completion, 21 December 2012, was 4 Ahau
        assertEquals(TzolkinDate(4, TzolkinSign.AHAU), TzolkinDate.forDate(LocalDate.of(2012, 12, 21)))
        // The next day advances both number and sign
        assertEquals(TzolkinDate(5, TzolkinSign.IMIX), TzolkinDate.forDate(LocalDate.of(2012, 12, 22)))
        // The cycle repeats every 260 days
        assertEquals(TzolkinDate.forDate(LocalDate.of(2012, 12, 21)), TzolkinDate.forDate(LocalDate.of(2012, 12, 21).plusDays(260)))
    }
}

class SymbolsAndLoreTest {
    @Test fun birthstones() {
        assertEquals(listOf("Ruby"), BirthSymbols.forMonth(java.time.Month.JULY).stones)
        assertTrue(BirthSymbols.forMonth(java.time.Month.JUNE).stones.contains("Pearl"))
    }

    @Test fun dayLore() {
        assertEquals("Kofi" to "Afua", DayLore.akanDayNames(DayOfWeek.FRIDAY))
        assertEquals("Red", DayLore.thaiColour(DayOfWeek.SUNDAY))
        assertTrue(DayLore.nurseryRhyme(DayOfWeek.MONDAY).contains("fair of face"))
    }

    @Test fun hijri() {
        // 1 January 2000 fell in Ramadan 1420 AH
        val h = OtherCalendars.hijri(LocalDate.of(2000, 1, 1))
        assertEquals(1420, h.get(java.time.temporal.ChronoField.YEAR))
        assertEquals(9, h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR))
    }
}

class MilestonesTest {
    private val birth = LocalDate.of(1990, 8, 10)

    @Test fun goldenBirthdayAndBenMingNian() {
        val keys = Milestones.forBirthday(birth, 10).map { it.key }
        assertTrue(keys.contains("milestone.golden"))
        val keys36 = Milestones.forBirthday(birth, 36).map { it.key }
        assertTrue(keys36.contains("chinese.benmingnian"))
        assertFalse(Milestones.forBirthday(birth, 35).map { it.key }.contains("chinese.benmingnian"))
    }

    @Test fun yakudoshiDependsOnSex() {
        assertTrue(Milestones.forBirthday(birth, 41, Sex.MALE).any { it.key == "milestone.yakudoshi" })
        assertFalse(Milestones.forBirthday(birth, 41, Sex.FEMALE).any { it.key == "milestone.yakudoshi" })
        assertFalse(Milestones.forBirthday(birth, 41).any { it.key == "milestone.yakudoshi" })
        assertTrue(Milestones.forBirthday(birth, 32, Sex.FEMALE).any { it.key == "milestone.yakudoshi" })
    }

    @Test fun sixtyIsCelebratedEverywhere() {
        val keys = Milestones.forBirthday(birth, 60).map { it.key }
        assertTrue(keys.contains("milestone.kanreki"))
        assertTrue(keys.contains("chinese.benmingnian"))
    }

    @Test fun dateLore() {
        val friday13 = LocalDate.of(2023, 10, 13)
        assertEquals(DayOfWeek.FRIDAY, friday13.dayOfWeek)
        assertTrue(Milestones.forDate(friday13).any { it.key == "date.friday13" })
        assertTrue(Milestones.forDate(LocalDate.of(2000, 2, 29)).any { it.key == "date.leapling" })
        assertTrue(Milestones.forDate(LocalDate.of(2000, 5, 8)).any { it.key == "date.eight" })
    }
}

class CosmicProfileTest {
    @Test fun aggregatesEverything() {
        val p = CosmicProfile.of(LocalDate.of(1990, 8, 10), "John")
        assertEquals(WesternSign.LEO, p.westernSign)
        assertEquals(ChineseAnimal.HORSE, p.chineseSign.animal)
        assertEquals(ChineseElement.METAL, p.chineseSign.element)
        assertEquals(1, p.lifePath)
        assertEquals(2, p.expression)
        assertEquals(listOf("Peridot", "Spinel", "Sardonyx"), p.symbols.stones)
    }

    @Test fun nameIsOptional() {
        val p = CosmicProfile.of(LocalDate.of(1990, 8, 10))
        assertNull(p.expression)
        assertNull(p.soulUrge)
    }
}
