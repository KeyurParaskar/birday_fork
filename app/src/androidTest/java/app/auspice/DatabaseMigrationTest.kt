package app.auspice

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.auspice.persistence.EventDatabase
import app.auspice.utilities.resultToEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrading from Birday's database (version 11) to Auspice's current one must keep every event and add the
 * optional columns (sex in 12; birth time, place and personality in 13). The version 11 schema below is exactly what Room generated for Birday's Event entity.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context get() = TestSupport.context
    private val dbName = "BirdayDB"

    @Before
    fun clean() {
        EventDatabase.destroyInstance()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        EventDatabase.destroyInstance()
        context.deleteDatabase(dbName)
    }

    private fun createVersion11Database() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `Event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`type` TEXT, `name` TEXT NOT NULL, `surname` TEXT, `favorite` INTEGER, " +
                            "`yearMatter` INTEGER, `originalDate` TEXT NOT NULL, `notes` TEXT DEFAULT '', `image` BLOB)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_Event_name_surname_originalDate` " +
                            "ON `Event` (`name`, `surname`, `originalDate`)"
                    )
                    db.execSQL(
                        "INSERT INTO Event (type, name, surname, favorite, yearMatter, originalDate, notes) " +
                            "VALUES ('BIRTHDAY', 'Ada', 'Lovelace', 1, 1, '1990-08-10', 'Likes engines')"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).apply {
            writableDatabase.close()
            close()
        }
    }

    @Test
    fun version11UpgradesWithEveryEventIntact() {
        createVersion11Database()

        val dao = EventDatabase.getBirdayDatabase(context).eventDao()
        val events = dao.getOrderedEventsStatic()

        assertEquals(1, events.size)
        val ada = events[0]
        assertEquals("Ada", ada.name)
        assertEquals("Lovelace", ada.surname)
        assertEquals("Likes engines", ada.notes)
        assertEquals(true, ada.favorite)
        // Columns added in versions 12 and 13 start empty
        assertNull(ada.sex)
        assertNull(ada.birthTime)
        assertNull(ada.birthPlace)
        assertNull(ada.birthLatitude)
        assertNull(ada.birthZone)
        assertNull(ada.personality)

        // and can be written and read back
        dao.updateEventReplace(
            resultToEvent(ada).copy(
                sex = "FEMALE",
                birthTime = "06:30",
                birthPlace = "London, United Kingdom",
                birthLatitude = 51.51,
                birthLongitude = -0.13,
                birthZone = "Europe/London",
                personality = "INTJ-A",
            )
        )
        val updated = dao.getOrderedEventsStatic()[0]
        assertEquals("FEMALE", updated.sex)
        assertEquals("06:30", updated.birthTime)
        assertEquals(51.51, updated.birthLatitude!!, 1e-9)
        assertEquals("Europe/London", updated.birthZone)
        assertEquals("INTJ-A", updated.personality)
        assertEquals("Likes engines", updated.notes)
    }
}
