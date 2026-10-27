package com.auspice.cosmos

import java.time.LocalDate

/** One person as compatibility sees them. */
data class Person(val name: String, val birthDate: LocalDate, val yearKnown: Boolean)

data class CompatibilityResult(val items: List<Insight>) {
    /** Auspicious minus cautionary findings: above zero the traditions favour the pair, below they warn. */
    val score: Int get() = items.count { it.sentiment == Sentiment.AUSPICIOUS } - items.count { it.sentiment == Sentiment.CAUTIONARY }
}

/**
 * What traditions say about two people together, as trivia. Uses Western elements, the Chinese harmony
 * trines, secret friends and clashes, and numerology Life Path families.
 */
object Compatibility {

    fun between(
        a: Person,
        b: Person,
        enabled: Set<Tradition>,
        showCautionary: Boolean,
        lunarNewYearOf: (Int) -> LocalDate? = { null },
    ): CompatibilityResult {
        val items = mutableListOf<Insight>()

        if (Tradition.WESTERN_ASTROLOGY in enabled) {
            val sa = WesternSign.forDate(a.birthDate)
            val sb = WesternSign.forDate(b.birthDate)
            val ea = name(sa.element.name)
            val eb = name(sb.element.name)
            items += when {
                sa == sb -> Insight(Tradition.WESTERN_ASTROLOGY, "compat.western.same",
                    "Both are ${name(sa.name)}: the same sign, the same instincts", Sentiment.AUSPICIOUS, "Same sign")
                sa.element == sb.element -> Insight(Tradition.WESTERN_ASTROLOGY, "compat.western.element",
                    "${name(sa.name)} and ${name(sb.name)} are both $ea signs, which astrology calls kindred", Sentiment.AUSPICIOUS, "Same element")
                sa.isHarmoniousWith(sb) -> Insight(Tradition.WESTERN_ASTROLOGY, "compat.western.complementary",
                    "$ea and $eb are complementary elements in Western astrology", Sentiment.AUSPICIOUS, "Complementary elements")
                else -> Insight(Tradition.WESTERN_ASTROLOGY, "compat.western.different",
                    "$ea and $eb signs are said to need more effort to understand each other", Sentiment.NEUTRAL, "Different elements")
            }
        }

        if (Tradition.CHINESE_ASTROLOGY in enabled && a.yearKnown && b.yearKnown) {
            val ca = ChineseSign.forDate(a.birthDate, lunarNewYearOf).animal
            val cb = ChineseSign.forDate(b.birthDate, lunarNewYearOf).animal
            val na = name(ca.name)
            val nb = name(cb.name)
            items += when {
                ca == cb -> Insight(Tradition.CHINESE_ASTROLOGY, "compat.chinese.same",
                    "Both born in the year of the $na", Sentiment.NEUTRAL, "Same animal")
                (ca.ordinal + cb.ordinal) % 12 == 1 -> Insight(Tradition.CHINESE_ASTROLOGY, "compat.chinese.secret",
                    "$na and $nb are \"secret friends\" (liu he), the closest pairing in the Chinese zodiac", Sentiment.AUSPICIOUS, "Secret friends")
                ca.ordinal % 4 == cb.ordinal % 4 -> Insight(Tradition.CHINESE_ASTROLOGY, "compat.chinese.trine",
                    "$na and $nb share a harmony trine (san he)", Sentiment.AUSPICIOUS, "Harmony trine")
                (ca.ordinal + 6) % 12 == cb.ordinal -> Insight(Tradition.CHINESE_ASTROLOGY, "compat.chinese.clash",
                    "$na and $nb are opposite each other on the zodiac wheel, the traditional clash", Sentiment.CAUTIONARY, "Clashing animals")
                else -> Insight(Tradition.CHINESE_ASTROLOGY, "compat.chinese.neutral",
                    "$na and $nb have no special bond or conflict in the Chinese zodiac", Sentiment.NEUTRAL, "Neutral animals")
            }
        }

        if (Tradition.NUMEROLOGY in enabled && a.yearKnown && b.yearKnown) {
            val la = Numerology.lifePath(a.birthDate)
            val lb = Numerology.lifePath(b.birthDate)
            items += when {
                la == lb -> Insight(Tradition.NUMEROLOGY, "compat.numerology.same",
                    "Both walk Life Path $la", Sentiment.AUSPICIOUS, "Same Life Path")
                Numerology.lifePathFamily(la) == Numerology.lifePathFamily(lb) -> Insight(Tradition.NUMEROLOGY, "compat.numerology.family",
                    "Life Paths $la and $lb belong to the same numerology family", Sentiment.AUSPICIOUS, "Matching Life Paths")
                else -> Insight(Tradition.NUMEROLOGY, "compat.numerology.different",
                    "Life Paths $la and $lb approach life differently, numerologists say", Sentiment.NEUTRAL, "Different Life Paths")
            }
        }

        return CompatibilityResult(items.filter { showCautionary || it.sentiment != Sentiment.CAUTIONARY })
    }

    private fun name(s: String) = s.lowercase().replaceFirstChar { it.uppercase() }
}
