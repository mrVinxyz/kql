import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabasePositiveTests {
    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `test onCreate is called and creates database schema correctly`() {
        val database = Database.create(
            context = appContext(),
            name = "onCreate_test.db",
            version = 1,
            onCreate = {
                execSQL(
                    """
                    CREATE TABLE test_table (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """
                )
            }
        )

        val db = database.db

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='test_table'",
            null
        )
        val tableExists = cursor.use { it.moveToFirst() }
        assertTrue(tableExists, "Table 'test_table' should exist")

        db.close()
        appContext().deleteDatabase("onCreate_test.db")
    }

    @Test
    fun `test onUpgrade is called when database version increases`() {
        val database1 = Database.create(
            context = appContext(),
            name = "onUpgrade_test.db",
            version = 1,
            onCreate = {
                execSQL(
                    """
                    CREATE TABLE test_table (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """
                )
            }
        )

        database1.db.execSQL("INSERT INTO test_table (id, name) VALUES (1, 'Initial data')")
        database1.db.close()

        val database2 = Database.create(
            context = appContext(),
            name = "onUpgrade_test.db",
            version = 2,
            onCreate = {
                println("is this running?")
                execSQL(
                    """
                    CREATE TABLE test_table (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """
                )
            },
            onUpgrade = {
                execSQL("ALTER TABLE test_table ADD COLUMN value INTEGER DEFAULT 0")
            }
        )

        val columnCursor = database2.db.rawQuery("PRAGMA table_info(test_table)", null)
        val columns = mutableListOf<String>()
        columnCursor.use {
            while (it.moveToNext()) {
                columns.add(it.getString(it.getColumnIndexOrThrow("name")))
            }
        }

        assertTrue(columns.contains("value"), "Column 'value' should be added during upgrade")
        database2.db.close()
        appContext().deleteDatabase("onUpgrade_test.db")
    }
}
