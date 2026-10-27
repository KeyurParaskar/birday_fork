package com.auspice.cosmos

import java.time.LocalDate

/** Some traditions attach different meaning to an age depending on the person's sex; unknown is always allowed. */
enum class Sex { MALE, FEMALE, UNKNOWN }

/**
 * Ages and years that cultures around the world single out as lucky, unlucky or simply worth celebrating.
 * These feed the "this birthday" card: when someone is about to turn 42, a Japanese friend would recognise
 * a yakudoshi year, while a 60th birthday is kanreki in Japan, hwangap in Korea and a full zodiac cycle in China.
 *
 * Everything here is presented as cultural trivia, never as a prediction. Ages are the Western "completed years"
 * age; traditions that count age differently (kazoedoshi, Korean age) are noted in the text.
 */
object Milestones {

    fun forBirthday(birthDate: LocalDate, turningAge: Int, sex: Sex = Sex.UNKNOWN,
                    lunarNewYearOf: (Int) -> LocalDate? = { null }): List<Insight> {
        val out = mutableListOf<Insight>()
        val t = Tradition.MILESTONES_AND_SUPERSTITIONS

        // Golden birthday: turning the age that matches the day of the month you were born on.
        if (turningAge == birthDate.dayOfMonth) {
            out += Insight(t, "milestone.golden", "Golden birthday: turning $turningAge on the ${ordinal(turningAge)}",
                Sentiment.AUSPICIOUS, title = "Golden birthday")
        }

        // Ben Ming Nian: your Chinese zodiac animal returns every 12 years; tradition says to wear red for luck.
        if (turningAge > 0 && turningAge % 12 == 0) {
            val sign = ChineseSign.forDate(birthDate, lunarNewYearOf)
            out += Insight(Tradition.CHINESE_ASTROLOGY, "chinese.benmingnian",
                "Ben Ming Nian: the year of the ${sign.animal.name.lowercase().replaceFirstChar { it.uppercase() }} returns. Tradition says wear something red",
                Sentiment.CAUTIONARY, title = "Ben Ming Nian")
        }

        // Yakudoshi (Japan): calamity years. Men 25, 42, 61; women 19, 33, 37, 61. Counted in kazoedoshi, so the
        // Western age is one lower; we flag the Western age that falls inside the traditional year.
        val yakudoshi = when (sex) {
            Sex.MALE -> mapOf(24 to "honyaku", 41 to "taiyaku (the great calamity year)", 60 to "honyaku")
            Sex.FEMALE -> mapOf(18 to "honyaku", 32 to "taiyaku (the great calamity year)", 36 to "honyaku", 60 to "honyaku")
            Sex.UNKNOWN -> emptyMap()
        }
        yakudoshi[turningAge]?.let {
            out += Insight(t, "milestone.yakudoshi", "Yakudoshi (Japan): $it. Many visit a shrine for a purification rite",
                Sentiment.CAUTIONARY, title = "Yakudoshi year")
        }

        when (turningAge) {
            13 -> out += Insight(t, "milestone.thirteen", "13: considered unlucky in much of the West, lucky in Italy", Sentiment.NEUTRAL, title = "Thirteen")
            17 -> out += Insight(t, "milestone.seventeen", "17: the unlucky number in Italy (XVII rearranges to VIXI, \"I have lived\")", Sentiment.CAUTIONARY, title = "Seventeen")
            18 -> out += Insight(t, "milestone.chai", "18: chai, the Hebrew word for \"life\", a lucky number in Jewish tradition", Sentiment.AUSPICIOUS, title = "Chai")
            60 -> {
                out += Insight(t, "milestone.kanreki", "Kanreki (Japan) and hwangap (Korea): the sexagenary cycle completes, a rebirth celebrated in red", Sentiment.AUSPICIOUS, title = "Kanreki")
                out += Insight(t, "milestone.shashtipoorthi", "Shashtipoorthi (South India): the sixtieth year, often marked by renewing wedding vows", Sentiment.AUSPICIOUS, title = "Shashtipoorthi")
            }
            66 -> out += Insight(t, "milestone.sixtysix", "66: a lucky double-six age in Chinese tradition, celebrated with a big meal", Sentiment.AUSPICIOUS, title = "Lucky 66")
            70 -> out += Insight(t, "milestone.koki", "Koki (Japan): the rare age, celebrated in purple", Sentiment.AUSPICIOUS, title = "Koki")
            77 -> out += Insight(t, "milestone.kiju", "Kiju (Japan): the \"joyous age\", from how 喜 is written in cursive", Sentiment.AUSPICIOUS, title = "Kiju")
            80 -> out += Insight(t, "milestone.sanju", "Sanju (Japan): the \"umbrella age\", celebrated in gold", Sentiment.AUSPICIOUS, title = "Sanju")
            88 -> out += Insight(t, "milestone.beiju", "Beiju (Japan): the \"rice age\", because 八十八 resembles 米", Sentiment.AUSPICIOUS, title = "Beiju")
            90 -> out += Insight(t, "milestone.sotsuju", "Sotsuju (Japan): the \"graduation age\"", Sentiment.AUSPICIOUS, title = "Sotsuju")
            99 -> out += Insight(t, "milestone.hakuju", "Hakuju (Japan): the \"white age\", 百 minus one stroke", Sentiment.AUSPICIOUS, title = "Hakuju")
            100 -> out += Insight(t, "milestone.century", "A hundred years: a centenarian", Sentiment.AUSPICIOUS, title = "Centenarian")
        }

        // Numerology: a personal year 1 is the start of a new nine-year cycle, 9 is its completion.
        val personalYear = Numerology.personalYear(birthDate, birthDate.year + turningAge)
        if (personalYear == 1 || personalYear == 9) {
            out += Insight(Tradition.NUMEROLOGY, "numerology.personal_year.$personalYear",
                if (personalYear == 1) "Personal year 1: a new nine-year numerology cycle begins" else "Personal year 9: a nine-year numerology cycle completes",
                title = "Personal year $personalYear")
        }

        return out
    }

    /** English ordinal: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, 22nd... */
    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    /** Dates themselves that carry lore, regardless of age. */
    fun forDate(date: LocalDate): List<Insight> {
        val out = mutableListOf<Insight>()
        val t = Tradition.MILESTONES_AND_SUPERSTITIONS
        if (date.dayOfMonth == 13 && date.dayOfWeek == java.time.DayOfWeek.FRIDAY) {
            out += Insight(t, "date.friday13", "Born on a Friday the 13th", Sentiment.CAUTIONARY, title = "Friday the 13th")
        }
        if (date.dayOfMonth == 29 && date.monthValue == 2) {
            out += Insight(t, "date.leapling", "A leapling: born on February 29th, a true birthday only every four years", Sentiment.AUSPICIOUS, title = "Leapling")
        }
        if (date.dayOfMonth == 8 || date.dayOfMonth == 18 || date.dayOfMonth == 28) {
            out += Insight(t, "date.eight", "Born on a day with an 8, the luckiest number in Chinese culture", Sentiment.AUSPICIOUS, title = "Lucky eight")
        }
        if (date.dayOfMonth == 4 || date.dayOfMonth == 14 || date.dayOfMonth == 24) {
            out += Insight(t, "date.four", "Born on a day with a 4, avoided in East Asia because it sounds like \"death\"", Sentiment.CAUTIONARY, title = "Unlucky four")
        }
        return out
    }
}
