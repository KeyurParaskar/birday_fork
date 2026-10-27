package com.auspice.cosmos

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Personality types in the Myers-Briggs style: four letters (E/I, S/N, T/F, J/P) and an optional fifth,
 * A (assertive) or T (turbulent), written as in "INTJ-A".
 *
 * This is a self-reflection tool with original questions. It is not the official MBTI assessment;
 * MBTI and Myers-Briggs are trademarks of The Myers-Briggs Company. Auspice presents results as a mirror
 * for conversation, like the rest of its traditions, not as a scientific measurement.
 */
enum class Dimension(val first: Char, val second: Char, val firstName: String, val secondName: String) {
    ENERGY('E', 'I', "Extraversion", "Introversion"),
    INFORMATION('S', 'N', "Sensing", "Intuition"),
    DECISIONS('T', 'F', "Thinking", "Feeling"),
    STRUCTURE('J', 'P', "Judging", "Perceiving"),
    IDENTITY('A', 'T', "Assertive", "Turbulent");

    /** The letter chosen when scores are tied: I, N, F, P as in Myers-Briggs convention, and T. */
    val tieBreak: Char get() = second
}

/** A type such as INTJ or INTJ-A. [identity] is null when the fifth letter is unknown. */
data class PersonalityType(val letters: String, val identity: Char? = null) {
    init {
        require(letters.length == 4 && Dimension.entries.take(4).withIndex().all { (i, d) -> letters[i] == d.first || letters[i] == d.second }) {
            "Not a personality type: $letters"
        }
        require(identity == null || identity == 'A' || identity == 'T') { "Fifth letter must be A or T" }
    }

    val code: String get() = if (identity == null) letters else "$letters-$identity"
    val nickname: String get() = NICKNAMES.getValue(letters)
    val summary: String get() = SUMMARIES.getValue(letters)

    override fun toString() = code

    companion object {
        val ALL_LETTERS: List<String> = listOf(
            "ISTJ", "ISFJ", "INFJ", "INTJ", "ISTP", "ISFP", "INFP", "INTP",
            "ESTP", "ESFP", "ENFP", "ENTP", "ESTJ", "ESFJ", "ENFJ", "ENTJ",
        )

        /** Accepts "intj", "INTJ-A", "INTJ A", "INTJA" or "INTJ-t". Returns null for anything else. */
        fun parse(text: String?): PersonalityType? {
            if (text.isNullOrBlank()) return null
            val clean = text.uppercase().filter { it.isLetter() }
            if (clean.length !in 4..5) return null
            val identity = if (clean.length == 5) clean[4] else null
            return runCatching { PersonalityType(clean.take(4), identity) }.getOrNull()
        }

        // Original nicknames, one or two words each
        private val NICKNAMES = mapOf(
            "INTJ" to "Strategist", "INTP" to "Theorist", "ENTJ" to "Captain", "ENTP" to "Challenger",
            "INFJ" to "Counselor", "INFP" to "Idealist", "ENFJ" to "Mentor", "ENFP" to "Spark",
            "ISTJ" to "Steward", "ISFJ" to "Guardian", "ESTJ" to "Organizer", "ESFJ" to "Host",
            "ISTP" to "Craftsperson", "ISFP" to "Artist", "ESTP" to "Doer", "ESFP" to "Performer",
        )

        private val SUMMARIES = mapOf(
            "INTJ" to "Independent planner who builds long-range systems and trusts their own analysis.",
            "INTP" to "Curious analyst who loves models, puzzles and finding the flaw in an argument.",
            "ENTJ" to "Decisive leader who organises people and resources around ambitious goals.",
            "ENTP" to "Quick debater who generates ideas and enjoys testing them against others.",
            "INFJ" to "Quiet visionary who reads people well and works toward a meaningful purpose.",
            "INFP" to "Gentle, values-driven dreamer who seeks authenticity and personal meaning.",
            "ENFJ" to "Warm encourager who brings out the best in people and rallies groups.",
            "ENFP" to "Enthusiastic explorer of people and possibilities, full of new beginnings.",
            "ISTJ" to "Reliable, thorough and loyal to commitments, facts and proven methods.",
            "ISFJ" to "Caring protector who remembers details about people and quietly helps.",
            "ESTJ" to "Practical manager who values order, clear rules and getting things done.",
            "ESFJ" to "Sociable caretaker who keeps traditions alive and makes people feel welcome.",
            "ISTP" to "Calm troubleshooter who learns by taking things apart and fixing them.",
            "ISFP" to "Easygoing creator who lives in the moment and expresses themselves through craft.",
            "ESTP" to "Energetic realist who acts fast, takes risks and reads a room instantly.",
            "ESFP" to "Lively entertainer who brings fun, generosity and spontaneity everywhere.",
        )

        fun identityName(identity: Char): String = if (identity == 'A') "Assertive" else "Turbulent"

        fun identitySummary(identity: Char): String =
            if (identity == 'A') "Self-assured and even-tempered; setbacks rarely shake them for long."
            else "Self-conscious and striving; quick to notice problems and driven to improve."
    }
}

/**
 * One statement to rate from 1 (strongly disagree) to 5 (strongly agree). [towardsFirst] is true when agreeing
 * points to the dimension's first letter (E, S, T, J or A).
 */
data class Question(val text: String, val dimension: Dimension, val towardsFirst: Boolean)

/** A preference's direction and strength: 50 is balanced, 100 is as strong as the test can measure. */
data class Preference(val dimension: Dimension, val letter: Char, val percent: Int)

data class TestResult(val type: PersonalityType, val preferences: List<Preference>)

object PersonalityTest {

    private fun q(text: String, dimension: Dimension, towardsFirst: Boolean) = Question(text, dimension, towardsFirst)

    private val ENERGY = listOf(
        q("After a long week, a lively evening with friends recharges me.", Dimension.ENERGY, true),
        q("I usually figure out what I think by talking it through.", Dimension.ENERGY, true),
        q("I enjoy starting conversations with people I don't know.", Dimension.ENERGY, true),
        q("A busy, social workplace gives me energy.", Dimension.ENERGY, true),
        q("I'd rather join a group activity than spend an afternoon alone.", Dimension.ENERGY, true),
        q("I need quiet time alone to recover after socialising.", Dimension.ENERGY, false),
        q("I prefer to think things through before I say them.", Dimension.ENERGY, false),
        q("A deep talk with one person beats a party with many.", Dimension.ENERGY, false),
        q("A lot of interaction with people leaves me drained.", Dimension.ENERGY, false),
        q("I keep my thoughts to myself until I know someone well.", Dimension.ENERGY, false),
    )

    private val INFORMATION = listOf(
        q("I trust facts and direct experience more than hunches.", Dimension.INFORMATION, true),
        q("I like instructions that are concrete and step by step.", Dimension.INFORMATION, true),
        q("I notice practical details that others miss.", Dimension.INFORMATION, true),
        q("I'd rather improve what already works than try an untested idea.", Dimension.INFORMATION, true),
        q("I focus on what is happening now more than on what might happen.", Dimension.INFORMATION, true),
        q("I get absorbed in possibilities and what-ifs.", Dimension.INFORMATION, false),
        q("I enjoy abstract ideas even when they have no immediate use.", Dimension.INFORMATION, false),
        q("I read between the lines and look for hidden meanings.", Dimension.INFORMATION, false),
        q("Routine bores me; I like inventing new ways to do things.", Dimension.INFORMATION, false),
        q("I see the big picture before the details.", Dimension.INFORMATION, false),
    )

    private val DECISIONS = listOf(
        q("When deciding, logic matters more to me than how people will feel.", Dimension.DECISIONS, true),
        q("I'd rather be honest than tactful.", Dimension.DECISIONS, true),
        q("I can criticise an idea without it feeling personal.", Dimension.DECISIONS, true),
        q("Rules applied equally to everyone matter more than exceptions for individuals.", Dimension.DECISIONS, true),
        q("I find it easy to stay detached when analysing a problem.", Dimension.DECISIONS, true),
        q("Before deciding, I think about how it will affect everyone involved.", Dimension.DECISIONS, false),
        q("Keeping harmony in a group is very important to me.", Dimension.DECISIONS, false),
        q("I'm easily moved by other people's stories.", Dimension.DECISIONS, false),
        q("I'd bend a rule to help someone in a difficult situation.", Dimension.DECISIONS, false),
        q("My values guide my choices more than cool analysis does.", Dimension.DECISIONS, false),
    )

    private val STRUCTURE = listOf(
        q("I like to have plans settled well in advance.", Dimension.STRUCTURE, true),
        q("Leaving things undecided makes me uneasy.", Dimension.STRUCTURE, true),
        q("I finish tasks well before the deadline.", Dimension.STRUCTURE, true),
        q("I keep lists and schedules, and I actually follow them.", Dimension.STRUCTURE, true),
        q("I like my space to be orderly.", Dimension.STRUCTURE, true),
        q("I prefer to keep my options open and decide at the last moment.", Dimension.STRUCTURE, false),
        q("I work best in bursts close to a deadline.", Dimension.STRUCTURE, false),
        q("Spontaneous plans are more fun than ones made weeks ahead.", Dimension.STRUCTURE, false),
        q("Schedules and rules feel restrictive to me.", Dimension.STRUCTURE, false),
        q("I happily switch tasks when something more interesting comes up.", Dimension.STRUCTURE, false),
    )

    private val IDENTITY = listOf(
        q("I stay calm under pressure.", Dimension.IDENTITY, true),
        q("Once I've made a decision, I rarely second-guess it.", Dimension.IDENTITY, true),
        q("Most of the time I feel confident in my abilities.", Dimension.IDENTITY, true),
        q("Setbacks don't affect my mood for long.", Dimension.IDENTITY, true),
        q("I'm comfortable with who I am, even when others disagree.", Dimension.IDENTITY, true),
        q("I often worry that things will go wrong.", Dimension.IDENTITY, false),
        q("My mood changes a lot depending on how my day goes.", Dimension.IDENTITY, false),
        q("I'm hard on myself when I make a mistake.", Dimension.IDENTITY, false),
        q("I often wonder what others think of me.", Dimension.IDENTITY, false),
        q("I keep pushing to improve because I rarely feel good enough.", Dimension.IDENTITY, false),
    )

    /** The full test, 50 statements, interleaved so no dimension appears twice in a row. */
    val FULL: List<Question> = (0 until 10).flatMap { i -> listOf(ENERGY[i], INFORMATION[i], DECISIONS[i], STRUCTURE[i], IDENTITY[i]) }

    /** The short test for the fifth letter only, 10 statements, alternating direction. */
    val IDENTITY_ONLY: List<Question> = (0 until 5).flatMap { i -> listOf(IDENTITY[i], IDENTITY[i + 5]) }

    /**
     * Scores answers (1 to 5, one per question, same order). Dimensions with no questions are left out, so the
     * identity test can be scored on its own with [knownLetters] supplying the first four letters.
     */
    fun score(questions: List<Question>, answers: List<Int>, knownLetters: String? = null): TestResult {
        require(questions.size == answers.size) { "One answer per question" }
        require(answers.all { it in 1..5 }) { "Answers go from 1 to 5" }
        val preferences = Dimension.entries.mapNotNull { dimension ->
            val asked = questions.indices.filter { questions[it].dimension == dimension }
            if (asked.isEmpty()) return@mapNotNull null
            val sum = asked.sumOf { i -> (answers[i] - 3) * if (questions[i].towardsFirst) 1 else -1 }
            val max = asked.size * 2
            val letter = when {
                sum > 0 -> dimension.first
                sum < 0 -> dimension.second
                else -> dimension.tieBreak
            }
            Preference(dimension, letter, 50 + (50.0 * abs(sum) / max).roundToInt())
        }
        val byDimension = preferences.associateBy { it.dimension }
        val letters = if (Dimension.entries.take(4).all { it in byDimension }) {
            Dimension.entries.take(4).joinToString("") { byDimension.getValue(it).letter.toString() }
        } else {
            requireNotNull(knownLetters) { "The first four letters are needed when the test does not cover them" }
        }
        return TestResult(PersonalityType(letters, byDimension[Dimension.IDENTITY]?.letter), preferences)
    }
}
