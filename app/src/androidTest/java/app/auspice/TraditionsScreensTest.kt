package app.auspice

import android.Manifest
import android.os.Build
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.auspice.activities.MainActivity
import app.auspice.fragments.DetailsFragmentArgs
import app.auspice.persistence.EventDatabase
import app.auspice.utilities.eventToResult
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the screens that phase 1 and 2 added and saves a screenshot of each.
 * Run on CI by the "Emulator tests" workflow; see docs/TESTING.md.
 */
@RunWith(AndroidJUnit4::class)
class TraditionsScreensTest {

    // Startup asks for these; granting them up front keeps system dialogs out of the way
    @get:Rule
    val permissions: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_CONTACTS
        ) else GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launch() {
        EventDatabase.destroyInstance()
        TestSupport.prepareApp()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario.close()
    }

    private fun openDetailsOfAda() {
        val ada = eventToResult(TestSupport.ada)
        scenario.onActivity {
            it.findNavController(R.id.navHostFragment)
                .navigate(R.id.detailsFragment, DetailsFragmentArgs(ada, 0).toBundle())
        }
    }

    @Test
    fun homeListShowsTheBirthdays() {
        onView(withText("Grace Hopper")).check(matches(isDisplayed()))
        TestSupport.screenshot("01_home")
    }

    @Test
    fun detailsShowTheTraditionsSection() {
        openDetailsOfAda()
        onView(withText("Western astrology")).perform(scrollTo()).check(matches(isDisplayed()))
        TestSupport.screenshot("02_details_top")
        onView(withText("Numerology")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Life Path:")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Birthstones and flowers")).perform(scrollTo()).check(matches(isDisplayed()))
        TestSupport.screenshot("03_details_traditions")
    }

    @Test
    fun compatibilityComparesTwoPeople() {
        openDetailsOfAda()
        onView(withId(R.id.detailsCompatibilityButton)).perform(scrollTo(), click())
        onView(withText("Grace Hopper")).inRoot(isDialog()).check(matches(isDisplayed()))
        TestSupport.screenshot("04_compatibility_pick")
        onView(withText("Grace Hopper")).inRoot(isDialog()).perform(click())
        onView(withText("Ada Lovelace and Grace Hopper")).inRoot(isDialog()).check(matches(isDisplayed()))
        TestSupport.screenshot("05_compatibility_result")
    }

    @Test
    fun settingsHaveTheTraditionsCategory() {
        onView(withId(R.id.navigationSettings)).perform(click())
        onView(withId(androidx.preference.R.id.recycler_view)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText("Traditions to show")))
        )
        onView(withText("Traditions to show")).check(matches(isDisplayed()))
        onView(withText("Numerology system")).check(matches(isDisplayed()))
        TestSupport.screenshot("06_settings_traditions")
    }

    @Test
    fun insertSheetOffersTheOptionalBirthdayFields() {
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.sexEventLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.birthTimeLayout)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.birthPlaceLayout)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        TestSupport.screenshot("07_insert_sheet")
    }

    private fun openPersonalityMenu() {
        openDetailsOfAda()
        onView(withId(R.id.detailsPersonalityButton)).perform(scrollTo(), click())
    }

    @Test
    fun personalityTypeCanBeEnteredDirectly() {
        openPersonalityMenu()
        TestSupport.screenshot("08_personality_menu")
        onView(withText("Enter the type")).inRoot(isDialog()).perform(click())
        onView(withText("ISTJ, the Steward")).inRoot(isDialog()).perform(click())
        onView(withText("Assertive (A)")).inRoot(isDialog()).perform(click())
        onView(withText("ISTJ-A, the Steward")).perform(scrollTo()).check(matches(isDisplayed()))
        TestSupport.screenshot("09_personality_entered")
    }

    @Test
    fun fullTestGivesTheTieBreakTypeForNeutralAnswers() {
        openPersonalityMenu()
        onView(withText("Take the full test (50 statements)")).inRoot(isDialog()).perform(click())
        TestSupport.screenshot("10_personality_test_question")
        repeat(50) {
            onView(withId(R.id.testAnswer3)).perform(click())
            onView(withId(R.id.testNext)).perform(click())
        }
        // Neutral everywhere ties every dimension, which resolves to I, N, F, P and T
        onView(withId(R.id.testResultType)).check(matches(withText("INFP-T")))
        TestSupport.screenshot("11_personality_test_result")
        onView(withId(R.id.testSave)).perform(click())
        onView(withText("INFP-T, the Idealist")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun fifthLetterTestCompletesAKnownType() {
        openPersonalityMenu()
        onView(withText("Find only the fifth letter (10 statements)")).inRoot(isDialog()).perform(click())
        // Statements alternate: assertive-keyed first, then turbulent-keyed
        repeat(10) { i ->
            onView(withId(if (i % 2 == 0) R.id.testAnswer5 else R.id.testAnswer1)).perform(click())
            onView(withId(R.id.testNext)).perform(click())
        }
        onView(withId(R.id.testResultType)).check(matches(withText("INTJ-A")))
        TestSupport.screenshot("12_fifth_letter_result")
        onView(withId(R.id.testSave)).perform(click())
        onView(withText("INTJ-A, the Strategist")).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
