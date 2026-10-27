package com.auspice.cosmos

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

/** The label of one row in a traditions section. The app maps each to a localised string. */
enum class ReportLabel {
    ELEMENT, MODALITY, RULING_PLANET,
    BEST_MATCHES, CLASHES_WITH,
    LIFE_PATH, BIRTHDAY_NUMBER, ATTITUDE_NUMBER, PERSONAL_YEAR, EXPRESSION, SOUL_URGE, PERSONALITY, NUMEROLOGY_SYSTEM,
    TZOLKIN,
    BIRTHSTONE, BIRTH_FLOWER,
    BORN_ON, NURSERY_RHYME, THAI_COLOUR, AKAN_NAME,
    HIJRI_DATE,
    HEBREW_DATE, HEBREW_BIRTHDAY,
    CELTIC_TREE,
}

/**
 * One line in a section. A row with a [label] is shown as "label: value"; a row without one is a full
 * sentence (a milestone or a piece of date lore) and carries the sentiment of the insight it came from.
 */
data class ReportRow(val label: ReportLabel?, val value: String, val sentiment: Sentiment = Sentiment.NEUTRAL)

data class ReportSection(val tradition: Tradition, val rows: List<ReportRow>)

/**
 * Turns a person's dates and name into the sections the details screen shows, and the short highlights the
 * event list and notifications show. All gating lives here so it can be unit tested without Android:
 * which traditions are enabled, whether the birth year is known, and whether cautionary lore is wanted.
 */
object TraditionsReport {

    /** Traditions the app can show today, in display order. Vedic astrology arrives in phase 3. */
    val AVAILABLE = listOf(
        Tradition.WESTERN_ASTROLOGY,
        Tradition.CHINESE_ASTROLOGY,
        Tradition.NUMEROLOGY,
        Tradition.MAYAN,
        Tradition.BIRTHSTONES_AND_FLOWERS,
        Tradition.DAY_LORE,
        Tradition.ISLAMIC_CALENDAR,
        Tradition.HEBREW_CALENDAR,
        Tradition.CELTIC_TREE,
        Tradition.MILESTONES_AND_SUPERSTITIONS,
    )

    /** What a new user sees before touching the settings. */
    val DEFAULT_ENABLED = setOf(
        Tradition.WESTERN_ASTROLOGY,
        Tradition.CHINESE_ASTROLOGY,
        Tradition.NUMEROLOGY,
        Tradition.BIRTHSTONES_AND_FLOWERS,
        Tradition.MILESTONES_AND_SUPERSTITIONS,
    )

    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani", "Jumada al-awwal", "Jumada al-thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah",
    )

    /**
     * @param birthDate the stored birth date; when [yearKnown] is false only its month and day are trusted
     * @param fullName name and surname, used for the name numbers; null or blank skips them
     * @param nextBirthday the date of the next (or today's) occurrence, which sets the age being turned
     * @param today used to find the next Hebrew birthday
     */
    fun build(
        birthDate: LocalDate,
        yearKnown: Boolean,
        fullName: String?,
        nextBirthday: LocalDate?,
        enabled: Set<Tradition>,
        showCautionary: Boolean,
        locale: Locale = Locale.ENGLISH,
        lunarNewYearOf: (Int) -> LocalDate? = { null },
        numerologySystem: NumerologySystem = NumerologySystem.PYTHAGOREAN,
        sex: Sex = Sex.UNKNOWN,
        today: LocalDate = LocalDate.now(),
    ): List<ReportSection> {
        val sections = mutableListOf<ReportSection>()
        fun add(tradition: Tradition, rows: List<ReportRow>) {
            if (tradition in enabled && rows.isNotEmpty()) sections += ReportSection(tradition, rows)
        }

        // Western astrology needs only month and day
        val western = WesternSign.forDate(birthDate)
        add(Tradition.WESTERN_ASTROLOGY, listOf(
            ReportRow(ReportLabel.ELEMENT, titleCase(western.element.name)),
            ReportRow(ReportLabel.MODALITY, titleCase(western.modality.name)),
            ReportRow(ReportLabel.RULING_PLANET, western.rulingPlanet),
        ))

        // Chinese astrology needs the year
        if (yearKnown) {
            val chinese = ChineseSign.forDate(birthDate, lunarNewYearOf)
            val trine = ChineseAnimal.entries.filter { it != chinese.animal && chinese.isInTrineWith(it) }
            val clash = ChineseAnimal.entries.first { chinese.clashesWith(it) }
            add(Tradition.CHINESE_ASTROLOGY, listOf(
                ReportRow(ReportLabel.ELEMENT, "${titleCase(chinese.element.name)} (${chinese.polarity.name.lowercase()})"),
                ReportRow(ReportLabel.BEST_MATCHES, trine.joinToString(", ") { titleCase(it.name) }),
                ReportRow(ReportLabel.CLASHES_WITH, titleCase(clash.name)),
            ))
        }

        // Numerology: the date numbers that need no year are always available
        val numerology = mutableListOf<ReportRow>()
        if (yearKnown) numerology += ReportRow(ReportLabel.LIFE_PATH, Numerology.lifePath(birthDate).toString())
        numerology += ReportRow(ReportLabel.BIRTHDAY_NUMBER, Numerology.birthday(birthDate).toString())
        numerology += ReportRow(ReportLabel.ATTITUDE_NUMBER, Numerology.attitude(birthDate).toString())
        if (nextBirthday != null) {
            val year = nextBirthday.year
            numerology += ReportRow(ReportLabel.PERSONAL_YEAR, "${Numerology.personalYear(birthDate, year)} ($year)")
        }
        val name = fullName?.takeIf { it.any(Char::isLetter) }
        if (name != null) {
            val expression = Numerology.expression(name, numerologySystem)
            val compound = Numerology.compoundExpression(name, numerologySystem)
            // Chaldean readings also look at the unreduced "compound" number
            val expressionText =
                if (numerologySystem == NumerologySystem.CHALDEAN && compound != expression) "$expression ($compound)"
                else expression.toString()
            if (expression > 0) numerology += ReportRow(ReportLabel.EXPRESSION, expressionText)
            listOf(
                ReportLabel.SOUL_URGE to Numerology.soulUrge(name, numerologySystem),
                ReportLabel.PERSONALITY to Numerology.personality(name, numerologySystem),
            ).filter { it.second > 0 }.forEach { numerology += ReportRow(it.first, it.second.toString()) }
            numerology += ReportRow(ReportLabel.NUMEROLOGY_SYSTEM, titleCase(numerologySystem.name))
        }
        add(Tradition.NUMEROLOGY, numerology)

        if (yearKnown) {
            val tzolkin = TzolkinDate.forDate(birthDate)
            add(Tradition.MAYAN, listOf(
                ReportRow(ReportLabel.TZOLKIN, "${tzolkin.number} ${titleCase(tzolkin.sign.name)} (${tzolkin.sign.meaning})"),
            ))
        }

        val symbols = BirthSymbols.forMonth(birthDate.month)
        add(Tradition.BIRTHSTONES_AND_FLOWERS, listOf(
            ReportRow(ReportLabel.BIRTHSTONE, symbols.stones.joinToString(", ")),
            ReportRow(ReportLabel.BIRTH_FLOWER, symbols.flowers.joinToString(", ")),
        ))

        if (yearKnown) {
            val day = birthDate.dayOfWeek
            val akan = DayLore.akanDayNames(day)
            add(Tradition.DAY_LORE, listOf(
                ReportRow(ReportLabel.BORN_ON, day.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }),
                ReportRow(ReportLabel.NURSERY_RHYME, DayLore.nurseryRhyme(day)),
                ReportRow(ReportLabel.THAI_COLOUR, DayLore.thaiColour(day)),
                ReportRow(ReportLabel.AKAN_NAME, "${akan.first} / ${akan.second}"),
            ))
        }

        if (yearKnown) {
            hijriText(birthDate)?.let { add(Tradition.ISLAMIC_CALENDAR, listOf(ReportRow(ReportLabel.HIJRI_DATE, it))) }
        }

        // The Hebrew date depends on the year; the day starts at sunset, which a date alone cannot tell
        if (yearKnown && Tradition.HEBREW_CALENDAR in enabled) {
            runCatching {
                val hebrew = HebrewCalendar.fromGregorian(birthDate)
                val next = HebrewCalendar.nextBirthday(birthDate, today)
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                listOf(
                    ReportRow(ReportLabel.HEBREW_DATE, hebrew.toString()),
                    ReportRow(ReportLabel.HEBREW_BIRTHDAY, next.format(formatter)),
                )
            }.getOrNull()?.let { add(Tradition.HEBREW_CALENDAR, it) }
        }

        // Celtic tree calendar needs only month and day
        val tree = CelticTree.forDate(birthDate)
        add(Tradition.CELTIC_TREE, listOf(
            ReportRow(ReportLabel.CELTIC_TREE, if (tree == null) "The nameless day" else "${titleCase(tree.name)} (${tree.ogham})"),
        ))

        // Date lore and this birthday's milestones, each also gated by its own tradition
        if (Tradition.MILESTONES_AND_SUPERSTITIONS in enabled) {
            val lore = Milestones.forDate(birthDate)
                .filter { yearKnown || it.key != "date.friday13" }
            val insights = filterInsights(lore, enabled, showCautionary) +
                highlights(birthDate, yearKnown, nextBirthday, enabled, showCautionary, lunarNewYearOf, sex)
            add(Tradition.MILESTONES_AND_SUPERSTITIONS, insights.map { ReportRow(null, it.fallbackText, it.sentiment) })
        }

        return sections
    }

    /**
     * What is special about the birthday coming up: golden birthdays, Ben Ming Nian, yakudoshi, longevity
     * celebrations and numerology cycles. Empty when the year is unknown or milestones are switched off.
     */
    fun highlights(
        birthDate: LocalDate,
        yearKnown: Boolean,
        nextBirthday: LocalDate?,
        enabled: Set<Tradition>,
        showCautionary: Boolean,
        lunarNewYearOf: (Int) -> LocalDate? = { null },
        sex: Sex = Sex.UNKNOWN,
    ): List<Insight> {
        if (!yearKnown || nextBirthday == null) return emptyList()
        if (Tradition.MILESTONES_AND_SUPERSTITIONS !in enabled) return emptyList()
        val turningAge = nextBirthday.year - birthDate.year
        if (turningAge <= 0) return emptyList()
        return filterInsights(Milestones.forBirthday(birthDate, turningAge, sex, lunarNewYearOf), enabled, showCautionary)
    }

    private fun filterInsights(insights: List<Insight>, enabled: Set<Tradition>, showCautionary: Boolean) =
        insights.filter { it.tradition in enabled && (showCautionary || it.sentiment != Sentiment.CAUTIONARY) }

    /** "19 Ramadan 1420 AH", or null where the platform's Hijri calendar cannot represent the date. */
    fun hijriText(date: LocalDate): String? = runCatching {
        val h = HijrahDate.from(date)
        val month = HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1]
        "${h.get(ChronoField.DAY_OF_MONTH)} $month ${h.get(ChronoField.YEAR)} AH"
    }.getOrNull()

    private fun titleCase(s: String) = s.lowercase().replaceFirstChar { it.uppercase() }
}
