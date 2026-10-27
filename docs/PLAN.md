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
   Chinese zodiac can switch off everything else. Defaults: Western, Chinese, numerology and birthstones
   on; the rest off until the user explores them.
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
free of Android means they can be unit tested on the JVM in seconds (already done: 19 tests), reused by
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
- [ ] Rename application id and package from `com.minar.birday` to `app.auspice`, new icon, new name,
      new splash. Keep the GPL notice and a "Based on Birday" credit in About.
- [ ] Replace screenshots and Fastlane metadata.

### Phase 1: show it in the app

- [ ] "Traditions" section on the details screen rendering `CosmicProfile`, grouped by tradition, each
      group collapsible with a one-line origin note.
- [ ] "This year" note on the upcoming-birthday cards and in the notification text, from `Milestones`.
- [ ] Settings: tradition toggles and the cautionary switch.
- [ ] Localised strings for every `Insight.key` (English first; Crowdin project for the rest).
- [ ] Extend statistics with the new tradition stats.
- [ ] Sources screen in About listing where each tradition's rules come from.

### Phase 2: depth

- [ ] Hebrew calendar (port of a known algorithm; unit tested against published dates).
- [ ] Chaldean numerology as an alternative system.
- [ ] Compatibility view for two people.
- [ ] Weekly "did you know" notification.
- [ ] Optional `sex` field on the contact (yakudoshi becomes visible).
- [ ] Celtic tree calendar, Native American "medicine wheel" animals and similar modern systems, clearly
      labelled as modern inventions.

### Phase 3: astronomy

- [ ] Optional birth time and place on the contact.
- [ ] Solar longitude for exact Western cusps; Moon longitude (Meeus) for Moon sign; sidereal (Lahiri)
      conversion for Vedic rashi and nakshatra; rising sign from local sidereal time.
- [ ] Precision is documented honestly: without a birth time, Moon sign is shown as "likely X (or Y)" when
      the Moon changed sign that day.

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
