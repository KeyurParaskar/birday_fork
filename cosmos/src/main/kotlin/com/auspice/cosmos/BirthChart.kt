package com.auspice.cosmos

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/** A birth place: coordinates (east and north positive) and its IANA time zone, for historical offsets. */
data class BirthPlace(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
) {
    val label: String get() = "$name, $country"
}

/** The twelve sidereal signs of Indian astrology, in the same order as [WesternSign]. */
enum class Rashi(val english: String) {
    MESHA("Aries"), VRISHABHA("Taurus"), MITHUNA("Gemini"), KARKA("Cancer"),
    SIMHA("Leo"), KANYA("Virgo"), TULA("Libra"), VRISHCHIKA("Scorpio"),
    DHANU("Sagittarius"), MAKARA("Capricorn"), KUMBHA("Aquarius"), MEENA("Pisces");

    val displayName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** One of the 27 lunar mansions, each 13 degrees 20 minutes of the sidereal zodiac, split into four padas. */
data class Nakshatra(val index: Int, val pada: Int) {
    val name: String get() = NAMES[index]

    companion object {
        val NAMES = listOf(
            "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu", "Pushya", "Ashlesha",
            "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha",
            "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
            "Purva Bhadrapada", "Uttara Bhadrapada", "Revati",
        )
        const val SPAN = 360.0 / 27

        fun forSiderealLongitude(longitude: Double): Nakshatra {
            val l = Astronomy.normalize(longitude)
            val index = (l / SPAN).toInt().coerceIn(0, 26)
            val pada = ((l - index * SPAN) / (SPAN / 4)).toInt().coerceIn(0, 3) + 1
            return Nakshatra(index, pada)
        }
    }
}

/**
 * Sun, Moon and rising signs, tropical and sidereal. Without a birth time the whole local day is scanned and
 * every sign the Moon (or the Sun, on a cusp day) passes through is returned, so the app can say "Leo or Virgo"
 * instead of guessing. Without a place, the rising sign is unknown and [fallbackZone] sets the day's limits.
 */
data class BirthChart(
    val exactTime: Boolean,
    val sunSigns: List<WesternSign>,
    val moonSigns: List<WesternSign>,
    val risingSign: WesternSign?,
    val vedicSunSigns: List<Rashi>,
    val vedicMoonSigns: List<Rashi>,
    val nakshatras: List<Nakshatra>,
) {
    companion object {
        fun compute(date: LocalDate, time: LocalTime?, place: BirthPlace?, fallbackZone: ZoneId): BirthChart {
            val zone = place?.let { runCatching { ZoneId.of(it.zoneId) }.getOrNull() } ?: fallbackZone
            // One instant when the time is known, otherwise every hour of the local day plus its last minute
            val instants: List<Instant> = if (time != null) {
                listOf(date.atTime(time).atZone(zone).toInstant())
            } else {
                (0..23).map { date.atTime(it, 0).atZone(zone).toInstant() } +
                    date.atTime(23, 59).atZone(zone).toInstant()
            }
            val samples = instants.map { instant ->
                val jde = Astronomy.julianEphemerisDay(instant)
                val ayanamsa = Astronomy.lahiriAyanamsa(jde)
                val sun = Astronomy.sunApparentLongitude(jde)
                val moon = Astronomy.moonApparentLongitude(jde)
                Sample(sun, moon, Astronomy.normalize(sun - ayanamsa), Astronomy.normalize(moon - ayanamsa))
            }
            val rising = if (time != null && place != null && abs(place.latitude) < 66.0) {
                signOf(Astronomy.ascendant(instants.first(), place.latitude, place.longitude))
            } else null
            return BirthChart(
                exactTime = time != null,
                sunSigns = samples.map { signOf(it.sun) }.distinct(),
                moonSigns = samples.map { signOf(it.moon) }.distinct(),
                risingSign = rising,
                vedicSunSigns = samples.map { rashiOf(it.siderealSun) }.distinct(),
                vedicMoonSigns = samples.map { rashiOf(it.siderealMoon) }.distinct(),
                nakshatras = samples.map { Nakshatra.forSiderealLongitude(it.siderealMoon) }
                    .let { list -> if (time != null) list else list.map { Nakshatra(it.index, 0) } }
                    .distinct(),
            )
        }

        fun signOf(longitude: Double): WesternSign = WesternSign.entries[(Astronomy.normalize(longitude) / 30).toInt().coerceIn(0, 11)]
        fun rashiOf(longitude: Double): Rashi = Rashi.entries[(Astronomy.normalize(longitude) / 30).toInt().coerceIn(0, 11)]
    }

    private data class Sample(val sun: Double, val moon: Double, val siderealSun: Double, val siderealMoon: Double)
}
