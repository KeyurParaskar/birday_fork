package com.auspice.cosmos

import java.time.LocalDate

enum class ChineseAnimal(val hanzi: String) {
    RAT("鼠"), OX("牛"), TIGER("虎"), RABBIT("兔"), DRAGON("龍"), SNAKE("蛇"),
    HORSE("馬"), GOAT("羊"), MONKEY("猴"), ROOSTER("雞"), DOG("狗"), PIG("豬");

    val key: String get() = "chinese.animal." + name.lowercase()
}

enum class ChineseElement(val hanzi: String) {
    WOOD("木"), FIRE("火"), EARTH("土"), METAL("金"), WATER("水");

    val key: String get() = "chinese.element." + name.lowercase()
}

enum class Polarity { YANG, YIN }

/**
 * A full sexagenary description of a Chinese year: animal (earthly branch), element and polarity (heavenly stem).
 * Example: 2024 is the year of the Wood Dragon (yang).
 */
data class ChineseSign(val animal: ChineseAnimal, val element: ChineseElement, val polarity: Polarity, val chineseYear: Int) {
    /** Animals six positions apart are traditionally the "clashing" pairs (e.g. Rat and Horse). */
    fun clashesWith(other: ChineseAnimal): Boolean = (animal.ordinal + 6) % 12 == other.ordinal

    /** The three "harmony" trines: Rat-Dragon-Monkey, Ox-Snake-Rooster, Tiger-Horse-Dog, Rabbit-Goat-Pig. */
    fun isInTrineWith(other: ChineseAnimal): Boolean = animal.ordinal % 4 == other.ordinal % 4

    companion object {
        /** The Chinese year 4 CE (and every 60th after it) begins a cycle with Yang Wood Rat. */
        fun forChineseYear(year: Int): ChineseSign {
            val offset = Math.floorMod(year - 4, 60)
            val animal = ChineseAnimal.entries[offset % 12]
            val element = ChineseElement.entries[(offset % 10) / 2]
            val polarity = if (offset % 2 == 0) Polarity.YANG else Polarity.YIN
            return ChineseSign(animal, element, polarity, year)
        }

        /**
         * Resolves the sign for a Gregorian date. The Chinese year starts at Lunar New Year (late January to
         * mid February), so [lunarNewYearOf] must return the Gregorian date on which the Chinese year of the
         * given Gregorian year begins. On Android pass a resolver backed by android.icu ChineseCalendar
         * (Birday already ships one); the default falls back to the Gregorian year, which is wrong for
         * roughly the first five weeks of each year and is only meant for previews and tests.
         */
        fun forDate(date: LocalDate, lunarNewYearOf: (Int) -> LocalDate? = { null }): ChineseSign {
            val newYear = lunarNewYearOf(date.year)
            val chineseYear = if (newYear != null && date.isBefore(newYear)) date.year - 1 else date.year
            return forChineseYear(chineseYear)
        }
    }
}
