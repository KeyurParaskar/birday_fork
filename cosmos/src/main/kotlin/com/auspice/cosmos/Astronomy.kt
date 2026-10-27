package com.auspice.cosmos

import java.time.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Positional astronomy for birth charts, after Jean Meeus, Astronomical Algorithms (2nd ed.).
 * Accuracy: Sun about 0.01 degree (chapter 25, low precision), Moon about 0.003 degree (chapter 47, full
 * longitude series), which is far finer than astrology needs: the Moon moves 0.5 degree an hour.
 * All angles are in degrees unless a name says otherwise.
 */
object Astronomy {
    private const val J2000 = 2451545.0

    fun normalize(degrees: Double): Double {
        val d = degrees % 360.0
        return if (d < 0) d + 360.0 else d
    }

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI

    /** Julian Day of an instant (UT). */
    fun julianDay(instant: Instant): Double =
        instant.epochSecond / 86400.0 + instant.nano / 86_400_000_000_000.0 + 2440587.5

    /**
     * Delta T (TT - UT) in seconds, interpolated from observed values 1900-2020 and extrapolated slowly after.
     * An error of a minute here moves the Moon by about 0.01 degree.
     */
    fun deltaTSeconds(year: Double): Double {
        val table = doubleArrayOf(-2.8, 10.4, 21.2, 24.0, 24.3, 29.1, 33.1, 40.2, 50.5, 56.9, 63.8, 66.1, 69.4)
        if (year <= 1900) return table[0]
        if (year >= 2020) return 69.4 + 0.1 * (year - 2020)
        val position = (year - 1900) / 10.0
        val i = floor(position).toInt()
        val f = position - i
        return table[i] + (table[i + 1] - table[i]) * f
    }

    /** Julian Ephemeris Day (TT) for an instant. */
    fun julianEphemerisDay(instant: Instant): Double {
        val jd = julianDay(instant)
        val year = 2000.0 + (jd - J2000) / 365.25
        return jd + deltaTSeconds(year) / 86400.0
    }

    private fun centuries(jde: Double) = (jde - J2000) / 36525.0

    /** Geometric (true) longitude of the Sun, Meeus 25.2 to 25.4. */
    fun sunTrueLongitude(jde: Double): Double {
        val t = centuries(jde)
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(rad(m)) +
            (0.019993 - 0.000101 * t) * sin(rad(2 * m)) +
            0.000289 * sin(rad(3 * m))
        return normalize(l0 + c)
    }

    /** Apparent longitude of the Sun (nutation and aberration applied), Meeus 25.8. */
    fun sunApparentLongitude(jde: Double): Double {
        val omega = 125.04 - 1934.136 * centuries(jde)
        return normalize(sunTrueLongitude(jde) - 0.00569 - 0.00478 * sin(rad(omega)))
    }

    // Periodic terms of the Moon's longitude, Meeus table 47.A: D, M, M', F and the coefficient in 1e-6 degree
    private val MOON_TERMS = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027), intArrayOf(2, 0, 0, 0, 658314),
        intArrayOf(0, 0, 2, 0, 213618), intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066), intArrayOf(2, 0, 1, 0, 53322),
        intArrayOf(2, -1, 0, 0, 45758), intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327), intArrayOf(0, 0, 1, 2, -12528),
        intArrayOf(0, 0, 1, -2, 10980), intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888), intArrayOf(2, 1, 0, 0, -6766),
        intArrayOf(1, 0, -1, 0, -5163), intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861), intArrayOf(2, 0, -3, 0, 3665),
        intArrayOf(0, 1, -2, 0, -2689), intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236), intArrayOf(0, 1, 2, 0, -2120),
        intArrayOf(0, 2, 0, 0, -2069), intArrayOf(2, -2, -1, 0, 2048), intArrayOf(2, 0, 1, -2, -1773),
        intArrayOf(2, 0, 0, 2, -1595), intArrayOf(4, -1, -1, 0, 1215), intArrayOf(0, 0, 2, 2, -1110),
        intArrayOf(3, 0, -1, 0, -892), intArrayOf(2, 1, 1, 0, -810), intArrayOf(4, -1, -2, 0, 759),
        intArrayOf(0, 2, -1, 0, -713), intArrayOf(2, 2, -1, 0, -700), intArrayOf(2, 1, -2, 0, 691),
        intArrayOf(2, -1, 0, -2, 596), intArrayOf(4, 0, 1, 0, 549), intArrayOf(0, 0, 4, 0, 537),
        intArrayOf(4, -1, 0, 0, 520), intArrayOf(1, 0, -2, 0, -487), intArrayOf(2, 1, 0, -2, -399),
        intArrayOf(0, 0, 2, -2, -381), intArrayOf(1, 1, 1, 0, 351), intArrayOf(3, 0, -2, 0, -340),
        intArrayOf(4, 0, -3, 0, 330), intArrayOf(2, -1, 2, 0, 327), intArrayOf(0, 2, 1, 0, -323),
        intArrayOf(1, 1, -1, 0, 299), intArrayOf(2, 0, 3, 0, 294),
    )

    /** Sum of the periodic longitude terms in 1e-6 degree (Meeus's "sigma l"), exposed for testing. */
    fun moonLongitudeTerms(jde: Double): Double {
        val t = centuries(jde)
        val lPrime = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841 - t * t * t * t / 65194000
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868 - t * t * t * t / 113065000
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000
        val mPrime = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699 - t * t * t * t / 14712000
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000 + t * t * t * t / 863310000
        val e = 1 - 0.002516 * t - 0.0000074 * t * t
        var sum = 0.0
        for (term in MOON_TERMS) {
            val argument = term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f
            val eccentricity = when (abs(term[1])) {
                1 -> e
                2 -> e * e
                else -> 1.0
            }
            sum += term[4] * eccentricity * sin(rad(argument))
        }
        val a1 = 119.75 + 131.849 * t
        val a2 = 53.09 + 479264.290 * t
        sum += 3958 * sin(rad(a1)) + 1962 * sin(rad(lPrime - f)) + 318 * sin(rad(a2))
        return sum
    }

    /** Geometric longitude of the Moon, Meeus chapter 47. */
    fun moonTrueLongitude(jde: Double): Double {
        val t = centuries(jde)
        val lPrime = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841 - t * t * t * t / 65194000
        return normalize(lPrime + moonLongitudeTerms(jde) / 1_000_000.0)
    }

    /** Nutation in longitude, Meeus 22 (low precision, 0.5 arcsecond), in degrees. */
    fun nutationInLongitude(jde: Double): Double {
        val t = centuries(jde)
        val omega = 125.04452 - 1934.136261 * t
        val l = 280.4665 + 36000.7698 * t
        val lMoon = 218.3165 + 481267.8813 * t
        val arcseconds = -17.20 * sin(rad(omega)) - 1.32 * sin(rad(2 * l)) - 0.23 * sin(rad(2 * lMoon)) + 0.21 * sin(rad(2 * omega))
        return arcseconds / 3600.0
    }

    fun moonApparentLongitude(jde: Double): Double = normalize(moonTrueLongitude(jde) + nutationInLongitude(jde))

    /** True obliquity of the ecliptic, Meeus 22.2 plus nutation in obliquity. */
    fun obliquity(jde: Double): Double {
        val t = centuries(jde)
        val mean = 23.0 + 26.0 / 60 + (21.448 - 46.8150 * t - 0.00059 * t * t + 0.001813 * t * t * t) / 3600.0
        val omega = 125.04452 - 1934.136261 * t
        val l = 280.4665 + 36000.7698 * t
        val lMoon = 218.3165 + 481267.8813 * t
        val deltaEpsilon = (9.20 * cos(rad(omega)) + 0.57 * cos(rad(2 * l)) + 0.10 * cos(rad(2 * lMoon)) - 0.09 * cos(rad(2 * omega))) / 3600.0
        return mean + deltaEpsilon
    }

    /** Apparent sidereal time at Greenwich, Meeus 12.4 plus the equation of the equinoxes. */
    fun greenwichSiderealTime(instant: Instant): Double {
        val jd = julianDay(instant)
        val t = (jd - J2000) / 36525.0
        val mean = 280.46061837 + 360.98564736629 * (jd - J2000) + 0.000387933 * t * t - t * t * t / 38710000
        val jde = julianEphemerisDay(instant)
        return normalize(mean + nutationInLongitude(jde) * cos(rad(obliquity(jde))))
    }

    /** Local sidereal time; [longitude] is east positive. */
    fun localSiderealTime(instant: Instant, longitude: Double): Double =
        normalize(greenwichSiderealTime(instant) + longitude)

    /**
     * The ecliptic longitude rising on the eastern horizon (the ascendant) for a place and instant.
     * Valid away from the polar circles, where the ascendant is undefined part of the time.
     */
    fun ascendant(instant: Instant, latitude: Double, longitude: Double): Double {
        val ramc = rad(localSiderealTime(instant, longitude))
        val epsilon = rad(obliquity(julianEphemerisDay(instant)))
        val phi = rad(latitude)
        return normalize(deg(atan2(cos(ramc), -(sin(ramc) * cos(epsilon) + tan(phi) * sin(epsilon)))))
    }

    /** Equatorial coordinates (right ascension, declination) of a point on the ecliptic. */
    fun eclipticToEquatorial(longitude: Double, obliquity: Double): Pair<Double, Double> {
        val l = rad(longitude)
        val e = rad(obliquity)
        val ra = normalize(deg(atan2(sin(l) * cos(e), cos(l))))
        val dec = deg(asin(sin(e) * sin(l)))
        return ra to dec
    }

    /**
     * Lahiri (Chitrapaksha) ayanamsa, the offset between tropical and sidereal zodiacs used by Indian
     * astrology: 23.857 degrees at J2000, growing with precession by about 50.3 arcseconds a year.
     */
    fun lahiriAyanamsa(jde: Double): Double = 23.857 + 1.3969 * centuries(jde)
}
