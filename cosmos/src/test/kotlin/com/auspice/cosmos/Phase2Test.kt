package com.auspice.cosmos

import com.ibm.icu.util.HebrewCalendar as IcuHebrew
import com.ibm.icu.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HebrewCalendarTest {
    @Test fun knownDates() {
        assertEquals(HebrewDate(5760, HebrewCalendar.TEVET, 23), HebrewCalendar.fromGregorian(LocalDate.of(2000, 1, 1)))
        assertEquals("23 Tevet 5760", HebrewCalendar.fromGregorian(LocalDate.of(2000, 1, 1)).toString())
        // Rosh Hashanah
        assertEquals(LocalDate.of(2023, 9, 16), HebrewCalendar.toGregorian(5784, HebrewCalendar.TISHRI, 1))
        assertEquals(LocalDate.of(2024, 10, 3), HebrewCalendar.toGregorian(5785, HebrewCalendar.TISHRI, 1))
        // First day of Passover and of Hanukkah
        assertEquals(LocalDate.of(2024, 4, 23), HebrewCalendar.toGregorian(5784, HebrewCalendar.NISAN, 15))
        // Candles were first lit on the evening of 25 December, when 25 Kislev began; its daytime is the 26th
        assertEquals(LocalDate.of(2024, 12, 26), HebrewCalendar.toGregorian(5785, HebrewCalendar.KISLEV, 25))
        assertTrue(HebrewCalendar.isLeapYear(5784))
        assertFalse(HebrewCalendar.isLeapYear(5785))
        assertEquals("Adar I", HebrewCalendar.monthName(HebrewCalendar.ADAR, 5784))
        assertEquals("Adar", HebrewCalendar.monthName(HebrewCalendar.ADAR, 5785))
    }

    /**
     * Every day from 1900 to 2100 must round-trip and match ICU's independent implementation, with one known
     * exception: ICU 77 puts Rosh Hashanah 5807 on Tuesday 2 October 2046. The molad of Tishri 5807 falls on a
     * Sunday at 11:04, so the lo ADU rule moves the new year to Monday 1 October; no rule moves it to Tuesday.
     * Both the Calendrical Calculations formulas and the classical postponement rules (see
     * matchesClassicalPostponementRules) agree on Monday, so only Hebrew years 5806 and 5807 may differ from ICU.
     */
    @Test fun matchesIcuForTwoCenturies() {
        val icu = IcuHebrew(TimeZone.GMT_ZONE)
        var date = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        val icuDisagreementWindow = LocalDate.of(2045, 9, 12)..LocalDate.of(2047, 9, 20)
        var checked = 0
        var icuDisagreements = 0
        while (!date.isAfter(end)) {
            icu.timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
            val year = icu.get(IcuHebrew.EXTENDED_YEAR)
            val ours = HebrewCalendar.fromGregorian(date)
            val expectedMonth = when (val m = icu.get(IcuHebrew.MONTH)) {
                IcuHebrew.TISHRI -> HebrewCalendar.TISHRI
                IcuHebrew.HESHVAN -> HebrewCalendar.MARHESHVAN
                IcuHebrew.KISLEV -> HebrewCalendar.KISLEV
                IcuHebrew.TEVET -> HebrewCalendar.TEVET
                IcuHebrew.SHEVAT -> HebrewCalendar.SHEVAT
                IcuHebrew.ADAR_1 -> HebrewCalendar.ADAR
                IcuHebrew.ADAR -> if (HebrewCalendar.isLeapYear(year)) HebrewCalendar.ADAR_II else HebrewCalendar.ADAR
                IcuHebrew.NISAN -> HebrewCalendar.NISAN
                IcuHebrew.IYAR -> HebrewCalendar.IYYAR
                IcuHebrew.SIVAN -> HebrewCalendar.SIVAN
                IcuHebrew.TAMUZ -> HebrewCalendar.TAMMUZ
                IcuHebrew.AV -> HebrewCalendar.AV
                IcuHebrew.ELUL -> HebrewCalendar.ELUL
                else -> error("Unexpected ICU month $m")
            }
            val icuDate = HebrewDate(year, expectedMonth, icu.get(IcuHebrew.DAY_OF_MONTH))
            if (date in icuDisagreementWindow) {
                if (icuDate != ours) icuDisagreements++
            } else {
                assertEquals("Mismatch on $date", icuDate, ours)
            }
            assertEquals("Round trip on $date", date, HebrewCalendar.toGregorian(ours))
            date = date.plusDays(1)
            checked++
        }
        assertTrue(checked > 73000)
        // The known disagreement exists, so this test will notice if a future ICU fixes it
        assertTrue(icuDisagreements > 0)
    }

    /** An independent implementation of the molad and the four dehiyyot, as taught in the classical sources. */
    private fun classicalRoshHashanahDayNumber(year: Int): Long {
        fun leap(y: Int) = Math.floorMod(7L * y + 1, 19L) < 7
        val months = Math.floorDiv(235L * year - 234, 19L)
        // Molad BaHaRaD (Monday, 5 hours, 204 parts) plus 29d 12h 793p per month, counted in parts
        val total = 2L * 25920 + 5 * 1080 + 204 + months * 765433L
        var day = total / 25920
        val parts = total % 25920
        val weekday = (day % 7).toInt() // 1 = Sunday ... 0 = Saturday, days begin at 6 pm
        if (parts >= 18 * 1080) day++                                                   // molad zaken
        else if (!leap(year) && weekday == 3 && parts >= 9 * 1080 + 204) day++           // GaTaRaD
        else if (leap(year - 1) && weekday == 2 && parts >= 15 * 1080 + 589) day++       // BeTUTaKPaT
        if ((day % 7).toInt() in setOf(1, 4, 6)) day++                                   // lo ADU Rosh
        return day
    }

    @Test fun matchesClassicalPostponementRules() {
        val reference = classicalRoshHashanahDayNumber(5785)
        val referenceDate = LocalDate.of(2024, 10, 3)
        for (year in 4000..7000) {
            val classical = referenceDate.plusDays(classicalRoshHashanahDayNumber(year) - reference)
            assertEquals("Rosh Hashanah $year", classical, HebrewCalendar.toGregorian(year, HebrewCalendar.TISHRI, 1))
        }
        assertEquals(LocalDate.of(2046, 10, 1), HebrewCalendar.toGregorian(5807, HebrewCalendar.TISHRI, 1))
    }

    @Test fun hebrewBirthdays() {
        // Born on 1 Adar 5783 (a common year): in the leap year 5784 the birthday is kept in Adar II
        val adarBirth = HebrewCalendar.toGregorian(5783, HebrewCalendar.ADAR, 1)
        val in5784 = HebrewCalendar.birthdayIn(HebrewCalendar.fromGregorian(adarBirth), 5784)
        assertEquals(HebrewDate(5784, HebrewCalendar.ADAR_II, 1), HebrewCalendar.fromGregorian(in5784))
        // The next Hebrew birthday is never in the past and lands on the same Hebrew day and month
        val birth = LocalDate.of(1990, 8, 10)
        val today = LocalDate.of(2026, 10, 27)
        val next = HebrewCalendar.nextBirthday(birth, today)
        assertFalse(next.isBefore(today))
        val b = HebrewCalendar.fromGregorian(birth)
        val n = HebrewCalendar.fromGregorian(next)
        assertEquals(b.month to b.day, n.month to n.day)
        assertTrue(next.isBefore(today.plusDays(400)))
    }
}

class ChaldeanAndCelticTest {
    @Test fun chaldean() {
        // J1 O7 H5 N5 = 18 -> 9
        assertEquals(18, Numerology.compoundExpression("John", NumerologySystem.CHALDEAN))
        assertEquals(9, Numerology.expression("John", NumerologySystem.CHALDEAN))
        // The Pythagorean default is unchanged
        assertEquals(2, Numerology.expression("John"))
        // Chaldean never assigns 9 to a letter
        assertTrue(('A'..'Z').none { Numerology.value(it, NumerologySystem.CHALDEAN) == 9 })
    }

    @Test fun celticTrees() {
        assertEquals(CelticTree.BIRCH, CelticTree.forDate(LocalDate.of(2000, 1, 1)))
        assertEquals(CelticTree.BIRCH, CelticTree.forDate(LocalDate.of(2000, 12, 24)))
        assertNull(CelticTree.forDate(LocalDate.of(2000, 12, 23)))
        assertEquals(CelticTree.ROWAN, CelticTree.forDate(LocalDate.of(2000, 1, 21)))
        assertEquals(CelticTree.VINE, CelticTree.forDate(LocalDate.of(2000, 9, 29)))
        assertEquals(CelticTree.IVY, CelticTree.forDate(LocalDate.of(2000, 9, 30)))
        assertEquals(CelticTree.REED, CelticTree.forDate(LocalDate.of(2000, 10, 31)))
        assertEquals(CelticTree.ELDER, CelticTree.forDate(LocalDate.of(2000, 12, 22)))
    }
}

class CompatibilityTest {
    private val all = TraditionsReport.AVAILABLE.toSet()
    private fun p(date: LocalDate) = Person("x", date, true)

    @Test fun chinesePairs() {
        val rat = p(LocalDate.of(2020, 6, 1))
        fun key(other: LocalDate) =
            Compatibility.between(rat, p(other), all, true).items.first { it.tradition == Tradition.CHINESE_ASTROLOGY }.key
        assertEquals("compat.chinese.secret", key(LocalDate.of(2021, 6, 1)))  // Ox
        assertEquals("compat.chinese.trine", key(LocalDate.of(2024, 6, 1)))   // Dragon
        assertEquals("compat.chinese.clash", key(LocalDate.of(2026, 6, 1)))   // Horse
        assertEquals("compat.chinese.same", key(LocalDate.of(2032, 6, 1)))    // Rat
    }

    @Test fun cautionaryAndUnknownYears() {
        val rat = p(LocalDate.of(2020, 6, 1))
        val horse = p(LocalDate.of(2026, 6, 1))
        assertTrue(Compatibility.between(rat, horse, all, false).items.none { it.sentiment == Sentiment.CAUTIONARY })
        val noYear = Person("y", LocalDate.of(2026, 6, 1), false)
        val items = Compatibility.between(rat, noYear, all, true).items
        assertEquals(listOf(Tradition.WESTERN_ASTROLOGY), items.map { it.tradition })
    }

    @Test fun westernAndNumerology() {
        // Two Leos born the same day share sign and Life Path
        val r = Compatibility.between(p(LocalDate.of(1990, 8, 10)), p(LocalDate.of(1990, 8, 10)), all, true)
        assertEquals(listOf("compat.western.same", "compat.numerology.same"), r.items.filter { it.tradition != Tradition.CHINESE_ASTROLOGY }.map { it.key })
        assertTrue(r.score > 0)
    }
}

class ReportPhase2Test {
    private val birth = LocalDate.of(1990, 8, 10)
    private val all = TraditionsReport.AVAILABLE.toSet()

    @Test fun hebrewAndCelticSections() {
        val s = TraditionsReport.build(birth, true, null, null, all, true, today = LocalDate.of(2026, 10, 27))
        val hebrew = s.first { it.tradition == Tradition.HEBREW_CALENDAR }.rows
        assertEquals(ReportLabel.HEBREW_DATE, hebrew[0].label)
        assertEquals(HebrewCalendar.fromGregorian(birth).toString(), hebrew[0].value)
        assertEquals("Hazel (Coll)", s.first { it.tradition == Tradition.CELTIC_TREE }.rows[0].value)
        // Unknown year: Celtic stays, Hebrew goes
        val noYear = TraditionsReport.build(birth, false, null, null, all, true).map { it.tradition }
        assertTrue(Tradition.CELTIC_TREE in noYear)
        assertFalse(Tradition.HEBREW_CALENDAR in noYear)
    }

    @Test fun chaldeanNameNumbers() {
        val rows = TraditionsReport.build(birth, true, "John", null, all, true, numerologySystem = NumerologySystem.CHALDEAN)
            .first { it.tradition == Tradition.NUMEROLOGY }.rows.associate { it.label to it.value }
        assertEquals("9 (18)", rows[ReportLabel.EXPRESSION])
        assertEquals("Chaldean", rows[ReportLabel.NUMEROLOGY_SYSTEM])
    }

    @Test fun yakudoshiNeedsSex() {
        val turning41 = LocalDate.of(2031, 8, 10)
        // Without a sex nothing applies at 41 for this date, so the milestones section is absent altogether
        fun milestoneText(sex: Sex) = TraditionsReport.build(birth, true, null, turning41, all, true, sex = sex)
            .firstOrNull { it.tradition == Tradition.MILESTONES_AND_SUPERSTITIONS }?.rows.orEmpty().map { it.value }
        assertTrue(milestoneText(Sex.MALE).any { it.startsWith("Yakudoshi") })
        assertFalse(milestoneText(Sex.UNKNOWN).any { it.startsWith("Yakudoshi") })
    }
}
