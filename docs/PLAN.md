# Auspice: product and technical plan

Auspice is a birthday and event reminder that also tells you what the world's traditions say about each
person and each upcoming birthday: Western and Chinese astrology, Vedic astrology, numerology, Mayan day
signs, birthstones, weekday lore, other religious calendars, and the lucky and unlucky ages of many cultures.

It is derived from [Birday](https://github.com/m-i-n-a-r/birday) by m-i-n-a-r, which already does the
reminder half beautifully. Birday is GPL-3, so Auspice is GPL-3 too and keeps Birday's full git history and
credits. The goal is not a horoscope app with a reminder bolted on; it is Birday's clean, offline, no-account
philosophy, extended with a layer of cultural trivia that makes every birthday a little richer.

## 1. Product principles

1. **Offline and private, like Birday.** Everything is computed on the device from a date and a name. No
   accounts, no network, no analytics.
2. **Trivia, not fortune telling.** Every insight is presented as "what tradition X says", with a short
   origin note. Nothing is phrased as advice or prediction. Cautionary items (yakudoshi, Friday the 13th)
   get a neutral icon, never an alarming one.
3. **Every tradition is optional.** Each tradition is a toggle in settings. A user who only wants the
   Chinese zodiac can switch off everything else. Defaults: Western, Chinese, numerology, birthstones and
   birthday milestones on; the rest off until the user explores them.
4. **Respectful breadth.** Religious calendars (Islamic, Hebrew, Hindu) and cultural practices are
   described in their own terms, with sources in the About screen. Where a practice is sex-specific
   (yakudoshi), the app asks for nothing it does not already know: it shows the item only when the user
   has set that field on the contact.
5. **Keep Birday's feature set intact.** Import from contacts, backup, widgets, notifications and
   statistics all keep working. Auspice adds; it does not rearrange.

## 2. What the app shows

### Person profile (details screen, new "Traditions" section)

| Tradition | Shown from date alone | Needs more input |
|---|---|---|
| Western astrology | Sun sign, element, modality, ruling planet, symbol | Rising sign and Moon sign need birth time and place (phase 3) |
| Chinese astrology | Animal, element, yin/yang, trine and clash animals | none (uses the lunar new year via ICU, already in Birday) |
| Numerology | Life Path, Birthday, Attitude numbers | Expression, Soul Urge, Personality numbers from the full name (already in the contact) |
| Mayan | Tzolk'in day sign and number | none |
| Birthstones and flowers | Modern birthstone(s), birth flower(s) | none |
| Weekday lore | English rhyme, Thai lucky colour, Akan day name | none |
| Islamic calendar | Hijri birth date | none (JDK built in) |
| Hebrew calendar | Hebrew birth date | needs a Hebrew calendar implementation (phase 2) |
| Vedic astrology | Rashi (Moon sign), Nakshatra | Moon longitude calculation plus birth time; sidereal ayanamsa (phase 3) |
| Date lore | Friday the 13th, leapling, lucky 8 / unlucky 4 days | none |

### Upcoming birthday card (home list and notification)

For each upcoming birthday, a one-line "this year" note when something applies:

- Ben Ming Nian (your Chinese animal year returns, wear red), every 12 years
- Golden birthday (age equals the day of the month)
- Yakudoshi years (Japan), when sex is set
- Kanreki / hwangap / shashtipoorthi at 60, kiju at 77, beiju at 88 and the other Japanese longevity ages
- Numerology personal year 1 (new cycle) and 9 (cycle completes)
- 13, 17, 18 and 66 with their regional meanings

### Compatibility (phase 2)

Tap two people: Western element harmony, Chinese trine/clash, Life Path pairing. Shown as "traditions say",
with the same trivia framing.

### Statistics (extend Birday's existing stats)

Birday already computes most common Western and Chinese sign. Add: most common element, Life Path spread,
how many friends share your Tzolk'in sign, who has a Ben Ming Nian this year.

### Daily or weekly "did you know" notification (phase 2, off by default)

One short tradition fact tied to a birthday in the next seven days.

## 3. Architecture

```
auspice/
├── app/      Birday's Android app (Kotlin, Room, WorkManager, Navigation, Material 3)
├── cosmos/   NEW pure-Kotlin JVM module: all tradition calculations, no Android imports
└── docs/     this plan, sources, screenshots
```

**Why a separate module.** The calculations are deterministic functions of a date and a name. Keeping them
free of Android means they can be unit tested on the JVM in seconds (54 tests so far), reused by
widgets and WorkManager workers without a Context, and later shared with a desktop or web build.

**How the app uses it.** `CosmicProfile.of(date, fullName, lunarNewYearOf)` returns everything for the
details screen. `Milestones.forBirthday(date, turningAge, sex)` returns the "this year" notes as a list of
`Insight` values. Each `Insight` carries a `Tradition` (for the settings toggles and grouping), a stable
`key` such as `western.sign.leo` that the app maps to localised strings and icons, and an English fallback.

**Lunar new year.** Birday resolves the Chinese year with `android.icu.util.ChineseCalendar` through its
`CalendarAdapter`. The app passes that as the `lunarNewYearOf` resolver; `cosmos` itself does not depend on
ICU, so it stays small and testable.

**Data model changes (Room migration).**

| Field on `Event` | Type | Why |
|---|---|---|
| `sex` | enum, nullable, default null | Yakudoshi and some Vedic rules differ by sex. Never required. |
| `birthTime` | LocalTime, nullable | Rising sign, Moon sign, Nakshatra (phase 3) |
| `birthPlace` | lat/lon, nullable | Same, plus time zone for the birth moment |

Backup JSON gains the same optional fields; old backups import unchanged.

**Settings.** One multi-select preference "Traditions to show", persisted as a set of `Tradition` names,
plus "Show cautionary items" (default on). Reuses Birday's existing `astrologyDisabled`-style flag pattern.

## 4. Roadmap

### Phase 0: rebrand and foundation (this commit and the next)

- [x] `cosmos` module with Western, Chinese (sexagenary), numerology (Pythagorean), Mayan Tzolk'in,
      birthstones and flowers, weekday lore, Hijri date, milestones and date lore. 19 unit tests.
- [x] Module wired into the Gradle build; CI's `./gradlew build` runs its tests.
- [x] Rename application id and package from `com.minar.birday` to `app.auspice`, new icon, new name,
      new splash. GPL notice kept; the About card credits Birday and links to it.
- [x] Fastlane title and descriptions rewritten; Birday's changelogs, Crowdin config and funding file removed.
- [ ] Replace the screenshots once the traditions UI exists (phase 1); until then they still show Birday's
      screens, which are unchanged apart from the icon.
- [ ] Set up an Auspice translation project; translated strings currently say "Auspice" wherever Birday
      said "Birday", and the translated developer bio was dropped in favour of the English credit.

### Phase 1: show it in the app

- [x] "Traditions" section on the details screen, built by `TraditionsReport` in cosmos and grouped by
      tradition. Year-dependent traditions are skipped when the birth year is unknown.
- [x] "This year" highlight on the event list cards and in single-event notifications (golden birthday,
      Ben Ming Nian, kanreki and so on), from `TraditionsReport.highlights`.
- [x] Settings: a Traditions category with the master switch, per-tradition toggles, a cautionary-lore
      switch, and a "Sources and notes" dialog. Defaults: Western, Chinese, numerology, birthstones and
      milestones on.
- [x] Statistics: most common zodiac element and most common Life Path number.
- [~] Localisation: section titles, row labels and settings are string resources, so they can be
      translated. The values themselves (element names, stones, milestone sentences) still come from cosmos
      in English; moving them to resources is part of the translation project.
- [ ] Replace the store screenshots with ones showing the traditions section.

### Phase 2: depth

- [x] Hebrew calendar: Hebrew birth date and next Hebrew birthday. Ported from Calendrical Calculations
      and verified day by day from 1900 to 2100 against ICU, and every Rosh Hashanah from year 4000 to 7000
      against an independent implementation of the classical postponement rules. ICU 77 differs on one
      year only (5807, in 2046); both of our implementations agree on the date and the test documents it.
- [x] Chaldean numerology as an alternative system, with compound name numbers. Chosen in Settings.
- [x] Compatibility view for two people, from the details screen: Western elements, Chinese trines,
      secret friends and clashes, and Life Path families.
- [x] Weekly "did you know" notification on Mondays, off by default.
- [x] Optional sex field on birthdays (database version 12, included in JSON backups), so yakudoshi shows.
- [x] Celtic tree calendar, labelled as a modern construction (Graves, 1948).
- [ ] ~~Native American "medicine wheel" animals~~: dropped. It is a 1980 invention by Sun Bear that many
      Native American nations regard as cultural appropriation, which conflicts with principle 4 above.
      It is easy to add later as one more cosmos calculator if wanted.

### Phase 3: astronomy (on branch `phase-3`, pending a local CI run)

- [x] Optional birth time and place on birthdays. Places come from 136 built-in cities with IANA time
      zones, so historical daylight saving is right and nothing needs the network.
- [x] Sun and Moon positions (Meeus, Astronomical Algorithms): the Moon sign, the exact Sun sign on cusp
      days, and the rising sign when both time and place are known. Checked against Meeus's worked examples,
      the January 2000 new and full moons, and 500 random ascendants that must lie on the eastern horizon.
- [x] Vedic astrology: sidereal Sun and Moon signs (rashi) with the Lahiri ayanamsa, and the nakshatra with
      its pada.
- [x] Honest precision: without a birth time the whole day is scanned and every possible sign is listed,
      as in "Leo or Virgo (depends on the birth time)".
- [ ] Custom coordinates for places not in the list.
- [ ] Planets beyond the Sun and Moon, if users ask for them.

### Personality types (added during phase 3)

- [x] Myers-Briggs-style four-letter types plus the Assertive/Turbulent fifth letter ("INTJ-A"), stored per
      person (database version 13).
- [x] Three ways to set it from the details screen: enter it directly, take the full 50-statement test, or
      take the 10-statement test for the fifth letter only.
- [x] Original questions, balanced so that agreeing with everything does not bias the result; ties follow the
      classic I/N/F/P convention. Labelled clearly as not the official MBTI assessment.
- [ ] Personality in the compatibility view.

### Phase 4: polish and release

- [ ] F-Droid and Play listings, privacy policy (trivially "no data leaves the device").
- [ ] Accessibility pass on the new screens; symbols always have text alternatives.

## 5. Risks and decisions

- **Licence.** Birday is GPL-3. Auspice must stay GPL-3, keep the copyright notices, and credit Birday.
  Done by keeping the full history and LICENSE.md; the About screen credit is in phase 0.
- **Accuracy claims.** Chinese year is exact (ICU). Western cusps are the conventional dates and can be a
  day off; phase 3 fixes that. Vedic without birth time is inherently approximate and will say so.
- **Cultural sensitivity.** Each tradition is attributed and described, never ranked or mocked. Sex-specific
  rules only appear when the user has entered that field. No health, fertility or financial framing.
- **Scope creep.** The traditions list is long on purpose, but each is a self-contained calculator with
  tests. Anything that needs network data or a large dataset is out of scope.
- **Name.** "Auspice" (a sign, an omen) is a placeholder chosen to avoid the Birday brand. Rename freely
  before phase 0 finishes; nothing in the code depends on it yet except `rootProject.name`.

## 6. Sources to cite in the app

Western sign dates: conventional tropical cusps. Chinese sexagenary cycle: standard stem-branch mapping
(year 4 CE = Yang Wood Rat). Pythagorean numerology: standard letter grid, master numbers 11/22/33.
Mayan Tzolk'in: GMT correlation (JDN 584283 = 4 Ahau 8 Cumku). Birthstones: Jewelers of America modern
list. Akan day names, Thai weekday colours, Japanese yakudoshi and longevity celebrations, Korean hwangap,
Tamil shashtipoorthi: as commonly documented; add encyclopedic references in the About screen.
