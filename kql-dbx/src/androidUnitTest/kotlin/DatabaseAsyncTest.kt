import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseAsyncTest {
    private lateinit var database: Database
    private val dbName = "async_test.db"

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Database.create(
            context = appContext(),
            name = dbName,
            version = 1,
            onCreate = {
                execSQL("""
                    CREATE TABLE test_async (
                        id INTEGER PRIMARY KEY,
                        value TEXT
                    )
                """)
            }
        )
    }

    @After
    fun tearDown() {
        database.db.close()
        appContext().deleteDatabase(dbName)
    }

    @Test
    fun `test async operation`() = runBlocking {
        val result = database.async {
            execute("INSERT INTO test_async (id, value) VALUES (?, ?)", 1, "Async Value")
            querySingle("SELECT value FROM test_async WHERE id = ?", 1) { row ->
                row.get<String>("value")
            }
        }

        assertTrue(result is DatabaseResult.Success)
        assertEquals("Async Value", result.getOrNull())
    }

    @Test
    fun `test async transaction operation`() = runBlocking {
        val result = database.async {
            transaction { db ->
                db.execute("INSERT INTO test_async (id, value) VALUES (?, ?)", 1, "Async Transaction Value")

                val value = db.querySingle("SELECT value FROM test_async WHERE id = ?", 1) { row ->
                    row.get<String>("value")
                }

                mapOf(
                    "insertedId" to 1,
                    "insertedValue" to value.getOrNull()
                )
            }
        }

        assertTrue(result is DatabaseResult.Success)

        val resultMap = result.getOrNull()
        assertNotNull(resultMap)
        assertEquals(1, resultMap["insertedId"])
        assertEquals("Async Transaction Value", resultMap["insertedValue"])

        val finalQueryResult = database.querySingle("SELECT value FROM test_async WHERE id = ?", 1) { row ->
            row.get<String>("value")
        }
        assertTrue(finalQueryResult is DatabaseResult.Success)
        assertEquals("Async Transaction Value", finalQueryResult.getOrNull())
    }

    @Test
    fun `test async error handling`() = runBlocking {
        val result = database.async {
            querySingle("SELECT * FROM non_existent_table") { row ->
                row.get<String>("value")
            }
        }

        assertTrue(result is DatabaseResult.Failure)

        val failure = result
        assertTrue(failure.error is DatabaseError.QueryError)

        val queryError = failure.error
        assertEquals("SELECT * FROM non_existent_table", queryError.sql)
        assertTrue(queryError.message.contains("no such table"))
    }

    @Test
    fun `test async transaction random error handling`() = runBlocking {
        val result = database.async {
            transaction {
                error("Simulated transaction error")
            }
        }

        println("RESULT $result")
        assertTrue(result is DatabaseResult.Failure)

        val failure = result
        assertTrue(failure.error is DatabaseError.TransactionError)
    }
}