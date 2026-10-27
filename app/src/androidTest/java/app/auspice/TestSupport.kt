package app.auspice

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.auspice.model.Event
import app.auspice.persistence.EventDatabase
import java.time.LocalDate

/** Shared setup for the emulator tests: a predictable app state and screenshots that survive uninstall. */
object TestSupport {
    val context: Context get() = ApplicationProvider.getApplicationContext()

    // Two birthdays a month away, so no confetti or "today" notification interferes with the screens
    private val inAMonth: LocalDate = LocalDate.now().plusDays(30)
    val ada = Event(
        id = 1,
        name = "Ada",
        surname = "Lovelace",
        originalDate = inAMonth.withYear(1990),
        yearMatter = true,
        sex = "FEMALE",
    )
    val grace = Event(
        id = 2,
        name = "Grace",
        surname = "Hopper",
        originalDate = inAMonth.plusDays(3).withYear(1986),
        yearMatter = true,
    )

    /** Skip the welcome screen and the rating prompt, stop looping animations, and reset the data. */
    fun prepareApp(events: List<Event> = listOf(ada, grace)) {
        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            clear()
            putBoolean("first", false)
            putBoolean("loop_avd", false)
            putBoolean("vibration", false)
        }
        context.getSharedPreferences("app_rating", 0).edit(commit = true) {
            putBoolean("do_not_show_again", true)
        }
        val dao = EventDatabase.getBirdayDatabase(context).eventDao()
        dao.deleteAllEvent(dao.getOrderedEventsStatic().map { e ->
            Event(id = e.id, name = e.name, surname = e.surname, originalDate = e.originalDate)
        })
        dao.insertAllEventReplace(events)
    }

    /**
     * Saves a screenshot to Pictures/AuspiceTests through MediaStore. Files there are kept when the test run
     * uninstalls the app, so the CI workflow can pull them afterwards. Never fails a test.
     */
    fun screenshot(name: String) {
        try {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.waitForIdleSync()
            Thread.sleep(400)
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AuspiceTests")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            Log.w("AuspiceTests", "Screenshot $name failed", e)
        }
    }
}
