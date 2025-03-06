import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseExecuteTests {
    private lateinit var database: Database
    private val dbName = "execute_test.db"

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Database.create(
            context = appContext(),
            name = dbName,
            version = 1,
            onCreate = {
                execSQL(
                    """
                    CREATE TABLE test_table (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        value INTEGER
                    )
                    """
                )
            }
        )
    }

    @After
    fun tearDown() {
        database.db.close()
        appContext().deleteDatabase(dbName)
    }

    @Test
    fun `test execute successfully inserts data`() = runBlocking {
        val sql = "INSERT INTO test_table (id, name, value) VALUES (?, ?, ?)"
        val id = 1
        val name = "Test Name"
        val value = 42

        val result = database.execute(sql, id, name, value)
        assertTrue(result is DatabaseResult.Success)

        val cursor = database.query(
            "SELECT id, name, value FROM test_table WHERE id = ?",
            id
        )

        cursor.use {
            assertTrue(it.moveToFirst())

            val actualId = it.getInt(it.getColumnIndexOrThrow("id"))
            val actualName = it.getString(it.getColumnIndexOrThrow("name"))
            val actualValue = it.getInt(it.getColumnIndexOrThrow("value"))

            assertEquals(id, actualId)
            assertEquals(name, actualName)
            assertEquals(value, actualValue)
        }
    }

    @Test
    fun `test execute handles SQL syntax errors`() = runBlocking {
        val invalidSql = "INSERT INTO non_existent_table (id) VALUES (?)"

        val result = database.execute(invalidSql, 1)
        assertTrue(result is DatabaseResult.Failure)

        val error = result.getErrorOrNull()
        assertTrue(error is DatabaseError.ExecutionError)
        println("RESULT: $result")
        println("ERROR: $error")

    }

    @Test
    fun `test execute handles binding parameter errors`() = runBlocking {
        val sql = "INSERT INTO test_table (id, name, value) VALUES (?, ?, ?)"

        val result = database.execute(sql, "not-an-int", "Test Name", 42)
        assertTrue(result is DatabaseResult.Failure)

        val error = result.getErrorOrNull()
        assertTrue(error is DatabaseError.ExecutionError)
    }
}