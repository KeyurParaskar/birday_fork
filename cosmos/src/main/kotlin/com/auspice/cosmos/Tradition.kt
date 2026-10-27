package com.auspice.cosmos

/**
 * The cultural or esoteric system an insight belongs to. Every insight Auspice shows is tagged with one of
 * these so the user can switch whole traditions on or off in the settings and so the UI can group them.
 */
enum class Tradition {
    PERSONALITY,
    WESTERN_ASTROLOGY,
    CHINESE_ASTROLOGY,
    VEDIC_ASTROLOGY,
    NUMEROLOGY,
    MAYAN,
    BIRTHSTONES_AND_FLOWERS,
    DAY_LORE,
    ISLAMIC_CALENDAR,
    HEBREW_CALENDAR,
    CELTIC_TREE,
    MILESTONES_AND_SUPERSTITIONS,
}

/** How a tradition regards the thing described: used to pick an icon/colour, never to alarm the user. */
enum class Sentiment { AUSPICIOUS, NEUTRAL, CAUTIONARY }

/**
 * A single, self-contained fact about a person or an upcoming birthday, ready to display.
 * [key] is a stable identifier (for example "milestone.golden") the app can map to localised strings and icons;
 * [fallbackText] is a full English sentence used when no localisation exists yet;
 * [title] is a two or three word English label short enough for a list row or a notification.
 */
data class Insight(
    val tradition: Tradition,
    val key: String,
    val fallbackText: String,
    val sentiment: Sentiment = Sentiment.NEUTRAL,
    val title: String = fallbackText,
)
