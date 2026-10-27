package com.auspice.cosmos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AstronomyTest {
    private fun angleDiff(a: Double, b: Double) = abs(((a - b + 540) % 360) - 180)

    @Test fun sunMatchesMeeusExample25a() {
        val jde = 2448908.5 // 1992 October 13.0 TD
        assertEquals(199.90988, Astronomy.sunTrueLongitude(jde), 0.0005)
        assertEquals(199.90895, Astronomy.sunApparentLongitude(jde), 0.0005)
    }

    @Test fun moonMatchesMeeusExample47a() {
        val jde = 2448724.5 // 1992 April 12.0 TD
        assertEquals(-1127527.0, Astronomy.moonLongitudeTerms(jde), 3.0)
        assertEquals(133.162655, Astronomy.moonTrueLongitude(jde), 0.0001)
        assertEquals(133.167265, Astronomy.moonApparentLongitude(jde), 0.0005)
    }

    @Test fun siderealTimeMatchesMeeusExample12a() {
        // 1987 April 10, 0h UT: apparent sidereal time 13h 10m 46.1351s
        val instant = Instant.parse("1987-04-10T00:00:00Z")
        assertEquals((13 + 10 / 60.0 + 46.1351 / 3600.0) * 15, Astronomy.greenwichSiderealTime(instant), 0.001)
    }

    @Test fun newAndFullMoonsLineUp() {
        // New moon 2000 January 6, 18:14 UT; full moon (total lunar eclipse) 2000 January 21, 04:40 UT
        val newMoon = Astronomy.julianEphemerisDay(Instant.parse("2000-01-06T18:14:00Z"))
        assertTrue(angleDiff(Astronomy.moonApparentLongitude(newMoon), Astronomy.sunApparentLongitude(newMoon)) < 0.1)
        val fullMoon = Astronomy.julianEphemerisDay(Instant.parse("2000-01-21T04:40:00Z"))
        assertTrue(angleDiff(Astronomy.moonApparentLongitude(fullMoon), Astronomy.sunApparentLongitude(fullMoon) + 180) < 0.1)
    }

    @Test fun ascendantIsOnTheEasternHorizon() {
        val random = Random(42)
        repeat(500) {
            val instant = Instant.ofEpochSecond(random.nextLong(-631_152_000L, 2_524_608_000L)) // 1950 to 2050
            val latitude = random.nextDouble(-60.0, 60.0)
            val longitude = random.nextDouble(-180.0, 180.0)
            val asc = Astronomy.ascendant(instant, latitude, longitude)
            // Independently place that ecliptic point in the local sky
            val epsilon = Astronomy.obliquity(Astronomy.julianEphemerisDay(instant))
            val (ra, dec) = Astronomy.eclipticToEquatorial(asc, epsilon)
            val hourAngle = Math.toRadians(Astronomy.localSiderealTime(instant, longitude) - ra)
            val phi = Math.toRadians(latitude)
            val d = Math.toRadians(dec)
            val altitude = Math.toDegrees(asin(sin(phi) * sin(d) + cos(phi) * cos(d) * cos(hourAngle)))
            assertEquals("altitude at $instant $latitude $longitude", 0.0, altitude, 1e-6)
            assertTrue("rising, not setting, at $instant $latitude $longitude", sin(hourAngle) < 0)
        }
    }

    @Test fun lahiriAyanamsa() {
        assertEquals(23.857, Astronomy.lahiriAyanamsa(2451545.0), 1e-9)
        // About 50 arcseconds a year
        assertEquals(50.3 / 3600, Astronomy.lahiriAyanamsa(2451545.0 + 365.25) - Astronomy.lahiriAyanamsa(2451545.0), 0.001)
    }
}

class BirthChartTest {
    private val mumbai = Places.byLabel("Mumbai, India")!!

    @Test fun withoutTimeTheChartListsEveryPossibleSign() {
        var day = LocalDate.of(2001, 3, 1)
        var sawIngressDay = false
        repeat(60) {
            val unknown = BirthChart.compute(day, null, mumbai, ZoneOffset.UTC)
            val noon = BirthChart.compute(day, LocalTime.NOON, mumbai, ZoneOffset.UTC)
            assertTrue(unknown.moonSigns.size in 1..2)
            assertTrue(unknown.nakshatras.size in 1..3)
            assertTrue(noon.moonSigns.single() in unknown.moonSigns)
            assertTrue(noon.nakshatras.single().index in unknown.nakshatras.map { it.index })
            assertNull(unknown.risingSign)
            assertNotNull(noon.risingSign)
            if (unknown.moonSigns.size == 2) sawIngressDay = true
            day = day.plusDays(1)
        }
        // The Moon changes sign every two and a half days, so some days must be ambiguous
        assertTrue(sawIngressDay)
    }

    @Test fun sunSignFromThePositionAndRashiOffset() {
        val chart = BirthChart.compute(LocalDate.of(1990, 8, 10), LocalTime.of(10, 30), mumbai, ZoneOffset.UTC)
        assertEquals(listOf(WesternSign.LEO), chart.sunSigns)
        // Sidereal is about 24 degrees behind tropical in 1990: the Sun at about 17 Leo is about 23 Karka
        assertEquals(listOf(Rashi.KARKA), chart.vedicSunSigns)
        assertTrue(chart.nakshatras.single().pada in 1..4)
    }

    @Test fun nakshatraBoundaries() {
        assertEquals("Ashwini", Nakshatra.forSiderealLongitude(0.0).name)
        assertEquals(1, Nakshatra.forSiderealLongitude(0.0).pada)
        assertEquals(4, Nakshatra.forSiderealLongitude(13.0).pada)
        assertEquals("Bharani", Nakshatra.forSiderealLongitude(13.34).name)
        assertEquals("Revati", Nakshatra.forSiderealLongitude(359.9).name)
    }

    @Test fun placesAreValid() {
        assertTrue(Places.ALL.size > 100)
        assertEquals(Places.ALL.size, Places.ALL.map { it.label }.toSet().size)
        Places.ALL.forEach {
            ZoneId.of(it.zoneId)
            assertTrue(it.label, it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0)
        }
        // Historical daylight saving comes from the zone: New York was on EDT in July 1990
        val ny = Places.byLabel("New York, United States")!!
        assertEquals(-4 * 3600, ZoneId.of(ny.zoneId).rules.getOffset(Instant.parse("1990-07-01T12:00:00Z")).totalSeconds)
    }
}

class PersonalityEngineTest {
    @Test fun parsing() {
        assertEquals("INTJ-A", PersonalityType.parse("intj-a")?.code)
        assertEquals("INTJ-T", PersonalityType.parse("INTJ T")?.code)
        assertEquals("ENFP", PersonalityType.parse("enfp")?.code)
        assertEquals("ENFP-A", PersonalityType.parse("ENFPA")?.code)
        assertNull(PersonalityType.parse("ABCD"))
        assertNull(PersonalityType.parse("INTJ-X"))
        assertNull(PersonalityType.parse(""))
        assertEquals(16, PersonalityType.ALL_LETTERS.toSet().size)
        PersonalityType.ALL_LETTERS.forEach { assertTrue(PersonalityType(it).nickname.isNotBlank()) }
    }

    @Test fun questionBankIsBalanced() {
        assertEquals(50, PersonalityTest.FULL.size)
        assertEquals(50, PersonalityTest.FULL.map { it.text }.toSet().size)
        Dimension.entries.forEach { d ->
            val qs = PersonalityTest.FULL.filter { it.dimension == d }
            assertEquals(10, qs.size)
            assertEquals(5, qs.count { it.towardsFirst })
        }
        assertEquals(10, PersonalityTest.IDENTITY_ONLY.size)
        assertTrue(PersonalityTest.IDENTITY_ONLY.all { it.dimension == Dimension.IDENTITY })
    }

    @Test fun scoring() {
        val all = PersonalityTest.FULL
        // Agreeing with every first-letter statement and disagreeing with the rest
        val strong = PersonalityTest.score(all, all.map { if (it.towardsFirst) 5 else 1 })
        assertEquals("ESTJ-A", strong.type.code)
        assertTrue(strong.preferences.all { it.percent == 100 })
        // Neutral everywhere falls back to the tie-break letters
        assertEquals("INFP-T", PersonalityTest.score(all, all.map { 3 }).type.code)
        // Agreeing with everything cancels out, because each dimension is keyed both ways
        assertEquals("INFP-T", PersonalityTest.score(all, all.map { 5 }).type.code)
    }

    @Test fun fifthLetterOnly() {
        val qs = PersonalityTest.IDENTITY_ONLY
        val assertive = PersonalityTest.score(qs, qs.map { if (it.towardsFirst) 4 else 2 }, knownLetters = "INTJ")
        assertEquals("INTJ-A", assertive.type.code)
        assertEquals(listOf(Dimension.IDENTITY), assertive.preferences.map { it.dimension })
        val turbulent = PersonalityTest.score(qs, qs.map { if (it.towardsFirst) 2 else 5 }, knownLetters = "INTJ")
        assertEquals("INTJ-T", turbulent.type.code)
    }
}

class ReportPhase3Test {
    private val all = TraditionsReport.AVAILABLE.toSet()
    private val birth = LocalDate.of(1990, 8, 10)

    @Test fun personalitySection() {
        val s = TraditionsReport.build(birth, true, null, null, all, true, personality = "INTJ-A")
        val rows = s.first().rows
        assertEquals(Tradition.PERSONALITY, s.first().tradition)
        assertEquals("INTJ-A, the Strategist", rows.first { it.label == ReportLabel.PERSONALITY_TYPE }.value)
        assertEquals("Introversion, Intuition, Thinking, Judging", rows.first { it.label == ReportLabel.PERSONALITY_PREFERENCES }.value)
        assertTrue(rows.first { it.label == ReportLabel.PERSONALITY_IDENTITY }.value.startsWith("Assertive"))
        // Garbage is ignored rather than shown
        assertTrue(TraditionsReport.build(birth, true, null, null, all, true, personality = "XYZ").none { it.tradition == Tradition.PERSONALITY })
    }

    @Test fun skyRowsNeedTheYearAndRisingNeedsTimeAndPlace() {
        val mumbai = Places.byLabel("Mumbai, India")!!
        fun labels(yearKnown: Boolean, time: LocalTime?, place: BirthPlace?) =
            TraditionsReport.build(birth, yearKnown, null, null, all, true, birthTime = time, birthPlace = place)
                .flatMap { it.rows }.mapNotNull { it.label }
        assertTrue(ReportLabel.MOON_SIGN in labels(true, null, null))
        assertTrue(ReportLabel.NAKSHATRA in labels(true, null, null))
        assertTrue(ReportLabel.RISING_SIGN !in labels(true, null, mumbai))
        assertTrue(ReportLabel.RISING_SIGN in labels(true, LocalTime.of(6, 0), mumbai))
        assertTrue(ReportLabel.MOON_SIGN !in labels(false, null, null))
        val vedic = TraditionsReport.build(birth, true, null, null, all, true, birthTime = LocalTime.of(6, 0), birthPlace = mumbai)
            .first { it.tradition == Tradition.VEDIC_ASTROLOGY }.rows
        assertTrue(vedic.first { it.label == ReportLabel.NAKSHATRA }.value.contains(", pada "))
    }
}
