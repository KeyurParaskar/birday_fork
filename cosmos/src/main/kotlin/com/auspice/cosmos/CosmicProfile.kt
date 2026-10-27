package com.auspice.cosmos

import java.time.LocalDate

/**
 * Everything Auspice can say about a person from their birth date alone (plus name for numerology).
 * The app renders this on the details screen; traditions the user has disabled are simply skipped there.
 */
data class CosmicProfile(
    val birthDate: LocalDate,
    val westernSign: WesternSign,
    val chineseSign: ChineseSign,
    val tzolkin: TzolkinDate,
    val lifePath: Int,
    val birthdayNumber: Int,
    val expression: Int?,
    val soulUrge: Int?,
    val personality: Int?,
    val symbols: BirthSymbols.MonthSymbols,
    val dateLore: List<Insight>,
) {
    companion object {
        fun of(birthDate: LocalDate, fullName: String? = null,
               lunarNewYearOf: (Int) -> LocalDate? = { null }): CosmicProfile {
            val name = fullName?.takeIf { it.any(Char::isLetter) }
            return CosmicProfile(
                birthDate = birthDate,
                westernSign = WesternSign.forDate(birthDate),
                chineseSign = ChineseSign.forDate(birthDate, lunarNewYearOf),
                tzolkin = TzolkinDate.forDate(birthDate),
                lifePath = Numerology.lifePath(birthDate),
                birthdayNumber = Numerology.birthday(birthDate),
                expression = name?.let(Numerology::expression),
                soulUrge = name?.let(Numerology::soulUrge),
                personality = name?.let(Numerology::personality),
                symbols = BirthSymbols.forMonth(birthDate.month),
                dateLore = Milestones.forDate(birthDate),
            )
        }
    }
}
