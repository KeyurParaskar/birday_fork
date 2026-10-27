package app.auspice.utilities

import android.content.Context
import android.content.SharedPreferences
import app.auspice.R
import app.auspice.model.EventCode
import app.auspice.model.EventResult
import com.auspice.cosmos.Insight
import com.auspice.cosmos.ReportLabel
import com.auspice.cosmos.ReportSection
import com.auspice.cosmos.Tradition
import com.auspice.cosmos.TraditionsReport
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Bridge between the pure-Kotlin traditions engine (cosmos) and the Android app

const val PREF_TRADITIONS = "traditions_enabled"
const val PREF_TRADITIONS_CAUTIONARY = "traditions_show_cautionary"
const val PREF_DISABLE_ASTROLOGY = "disable_astrology"

/**
 * Gregorian date of the Chinese New Year falling in the given Gregorian year, found with the same
 * ICU-based animal lookup Birday already uses. Results are cached; null if ICU cannot answer.
 */
object LunarNewYear {
    private val cache = ConcurrentHashMap<Int, LocalDate>()

    fun of(year: Int): LocalDate? = cache[year] ?: try {
        compute(year)?.also { cache[year] = it }
    } catch (_: Exception) {
        null
    }

    // The new year always falls between January 21 and February 20
    private fun compute(year: Int): LocalDate? {
        val previousAnimal = chineseAnimal(LocalDate.of(year, 1, 1))
        var day = LocalDate.of(year, 1, 20)
        val last = LocalDate.of(year, 2, 21)
        while (!day.isAfter(last)) {
            if (chineseAnimal(day) != previousAnimal) return day
            day = day.plusDays(1)
        }
        return null
    }
}

// The traditions the user wants to see. The legacy "disable astrology" switch hides all of them
fun enabledTraditions(sharedPrefs: SharedPreferences): Set<Tradition> {
    if (sharedPrefs.getBoolean(PREF_DISABLE_ASTROLOGY, false)) return emptySet()
    val names = sharedPrefs.getStringSet(PREF_TRADITIONS, null) ?: return TraditionsReport.DEFAULT_ENABLED
    return names.mapNotNull { name -> Tradition.entries.firstOrNull { it.name == name } }.toSet()
}

fun showCautionaryLore(sharedPrefs: SharedPreferences) =
    sharedPrefs.getBoolean(PREF_TRADITIONS_CAUTIONARY, true)

// The full traditions report for the details screen, empty for anything that isn't a birthday
fun eventTraditionsReport(sharedPrefs: SharedPreferences, event: EventResult): List<ReportSection> {
    if (event.type != EventCode.BIRTHDAY.name) return emptyList()
    val fullName = listOfNotNull(event.name, event.surname).joinToString(" ").trim()
    return TraditionsReport.build(
        birthDate = event.originalDate,
        yearKnown = event.yearMatter == true,
        fullName = fullName,
        nextBirthday = event.nextDate,
        enabled = enabledTraditions(sharedPrefs),
        showCautionary = showCautionaryLore(sharedPrefs),
        locale = Locale.getDefault(),
        lunarNewYearOf = LunarNewYear::of,
    )
}

// What is special about the coming birthday, for the event list and the notifications
fun eventHighlights(sharedPrefs: SharedPreferences, event: EventResult): List<Insight> {
    if (event.type != EventCode.BIRTHDAY.name) return emptyList()
    return try {
        TraditionsReport.highlights(
            birthDate = event.originalDate,
            yearKnown = event.yearMatter == true,
            nextBirthday = event.nextDate,
            enabled = enabledTraditions(sharedPrefs),
            showCautionary = showCautionaryLore(sharedPrefs),
            lunarNewYearOf = LunarNewYear::of,
        )
    } catch (_: Exception) {
        emptyList()
    }
}

fun traditionTitle(context: Context, tradition: Tradition): String = context.getString(
    when (tradition) {
        Tradition.WESTERN_ASTROLOGY -> R.string.tradition_western
        Tradition.CHINESE_ASTROLOGY -> R.string.tradition_chinese
        Tradition.VEDIC_ASTROLOGY -> R.string.tradition_vedic
        Tradition.NUMEROLOGY -> R.string.tradition_numerology
        Tradition.MAYAN -> R.string.tradition_mayan
        Tradition.BIRTHSTONES_AND_FLOWERS -> R.string.tradition_birthstones
        Tradition.DAY_LORE -> R.string.tradition_day_lore
        Tradition.ISLAMIC_CALENDAR -> R.string.tradition_islamic
        Tradition.MILESTONES_AND_SUPERSTITIONS -> R.string.tradition_milestones
    }
)

fun reportLabelText(context: Context, label: ReportLabel): String = context.getString(
    when (label) {
        ReportLabel.ELEMENT -> R.string.label_element
        ReportLabel.MODALITY -> R.string.label_modality
        ReportLabel.RULING_PLANET -> R.string.label_ruling_planet
        ReportLabel.BEST_MATCHES -> R.string.label_best_matches
        ReportLabel.CLASHES_WITH -> R.string.label_clashes_with
        ReportLabel.LIFE_PATH -> R.string.label_life_path
        ReportLabel.BIRTHDAY_NUMBER -> R.string.label_birthday_number
        ReportLabel.ATTITUDE_NUMBER -> R.string.label_attitude_number
        ReportLabel.PERSONAL_YEAR -> R.string.label_personal_year
        ReportLabel.EXPRESSION -> R.string.label_expression
        ReportLabel.SOUL_URGE -> R.string.label_soul_urge
        ReportLabel.PERSONALITY -> R.string.label_personality
        ReportLabel.TZOLKIN -> R.string.label_tzolkin
        ReportLabel.BIRTHSTONE -> R.string.label_birthstone
        ReportLabel.BIRTH_FLOWER -> R.string.label_birth_flower
        ReportLabel.BORN_ON -> R.string.label_born_on
        ReportLabel.NURSERY_RHYME -> R.string.label_nursery_rhyme
        ReportLabel.THAI_COLOUR -> R.string.label_thai_colour
        ReportLabel.AKAN_NAME -> R.string.label_akan_name
        ReportLabel.HIJRI_DATE -> R.string.label_hijri_date
    }
)
