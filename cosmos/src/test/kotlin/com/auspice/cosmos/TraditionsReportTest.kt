package com.auspice.cosmos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TraditionsReportTest {
    private val birth = LocalDate.of(1990, 8, 10)
    private val all = TraditionsReport.AVAILABLE.toSet()

    private fun labels(sections: List<ReportSection>, t: Tradition) =
        sections.firstOrNull { it.tradition == t }?.rows?.map { it.label }

    @Test fun fullReportWithKnownYear() {
        val s = TraditionsReport.build(birth, true, "John", LocalDate.of(2026, 8, 10), all, true)
        assertEquals(TraditionsReport.AVAILABLE, s.map { it.tradition })
        val western = s.first { it.tradition == Tradition.WESTERN_ASTROLOGY }.rows
        assertEquals("Fire", western[0].value)
        assertEquals("Fixed", western[1].value)
        assertEquals("Sun", western[2].value)
        val chinese = s.first { it.tradition == Tradition.CHINESE_ASTROLOGY }.rows
        assertEquals("Metal (yang)", chinese[0].value)
        assertEquals("Tiger, Dog", chinese[1].value)
        assertEquals("Rat", chinese[2].value)
        val numbers = s.first { it.tradition == Tradition.NUMEROLOGY }.rows.associate { it.label to it.value }
        assertEquals("1", numbers[ReportLabel.LIFE_PATH])
        assertEquals("1 (2026)", numbers[ReportLabel.PERSONAL_YEAR])
        assertEquals("2", numbers[ReportLabel.EXPRESSION])
        val day = s.first { it.tradition == Tradition.DAY_LORE }.rows.associate { it.label to it.value }
        assertEquals("Friday", day[ReportLabel.BORN_ON])
        assertEquals("Kofi / Afua", day[ReportLabel.AKAN_NAME])
    }

    @Test fun unknownYearDropsYearDependentTraditions() {
        val s = TraditionsReport.build(birth, false, null, LocalDate.of(2026, 8, 10), all, true)
        val shown = s.map { it.tradition }
        assertFalse(Tradition.CHINESE_ASTROLOGY in shown)
        assertFalse(Tradition.MAYAN in shown)
        assertFalse(Tradition.DAY_LORE in shown)
        assertFalse(Tradition.ISLAMIC_CALENDAR in shown)
        assertTrue(Tradition.WESTERN_ASTROLOGY in shown)
        val numerology = labels(s, Tradition.NUMEROLOGY)!!
        assertFalse(ReportLabel.LIFE_PATH in numerology)
        assertTrue(ReportLabel.BIRTHDAY_NUMBER in numerology)
        assertFalse(ReportLabel.EXPRESSION in numerology)
    }

    @Test fun disabledTraditionsAreSkipped() {
        val s = TraditionsReport.build(birth, true, "John", null, setOf(Tradition.NUMEROLOGY), true)
        assertEquals(listOf(Tradition.NUMEROLOGY), s.map { it.tradition })
        assertTrue(TraditionsReport.build(birth, true, "John", null, emptySet(), true).isEmpty())
    }

    @Test fun cautionaryLoreCanBeHidden() {
        val friday13 = LocalDate.of(1998, 11, 13)
        val withLore = TraditionsReport.build(friday13, true, null, null, all, true)
        val without = TraditionsReport.build(friday13, true, null, null, all, false)
        assertTrue(withLore.first { it.tradition == Tradition.MILESTONES_AND_SUPERSTITIONS }.rows.any { it.value.contains("Friday the 13th") })
        assertTrue(without.none { s -> s.rows.any { it.value.contains("Friday the 13th") } })
        // Friday the 13th needs the real year, so it disappears when the year is unknown
        val unknownYear = TraditionsReport.build(friday13, false, null, null, all, true)
        assertTrue(unknownYear.none { s -> s.rows.any { it.value.contains("Friday the 13th") } })
    }

    @Test fun highlightsForTheComingBirthday() {
        // Turning 10 on the 10th: golden birthday
        val golden = TraditionsReport.highlights(birth, true, LocalDate.of(2000, 8, 10), all, true)
        assertEquals("Golden birthday", golden.first().title)
        assertEquals("Golden birthday: turning 10 on the 10th", golden.first().fallbackText)
        // Turning 36: Ben Ming Nian, only while Chinese astrology is enabled
        val keys36 = TraditionsReport.highlights(birth, true, LocalDate.of(2026, 8, 10), all, true).map { it.key }
        assertTrue("chinese.benmingnian" in keys36)
        val noChinese = all - Tradition.CHINESE_ASTROLOGY
        assertFalse("chinese.benmingnian" in TraditionsReport.highlights(birth, true, LocalDate.of(2026, 8, 10), noChinese, true).map { it.key })
        // Nothing without a known year, without milestones, or for a newborn
        assertTrue(TraditionsReport.highlights(birth, false, LocalDate.of(2000, 8, 10), all, true).isEmpty())
        assertTrue(TraditionsReport.highlights(birth, true, LocalDate.of(2000, 8, 10), all - Tradition.MILESTONES_AND_SUPERSTITIONS, true).isEmpty())
        assertTrue(TraditionsReport.highlights(birth, true, birth, all, true).isEmpty())
    }

    @Test fun hijri() {
        assertEquals("24 Ramadan 1420 AH", TraditionsReport.hijriText(LocalDate.of(2000, 1, 1)))
        assertNull(TraditionsReport.hijriText(LocalDate.of(1800, 1, 1)))
    }

    @Test fun ordinals() {
        assertEquals("1st", Milestones.ordinal(1))
        assertEquals("2nd", Milestones.ordinal(2))
        assertEquals("3rd", Milestones.ordinal(3))
        assertEquals("11th", Milestones.ordinal(11))
        assertEquals("12th", Milestones.ordinal(12))
        assertEquals("13th", Milestones.ordinal(13))
        assertEquals("21st", Milestones.ordinal(21))
        assertEquals("22nd", Milestones.ordinal(22))
        assertEquals("31st", Milestones.ordinal(31))
    }
}
