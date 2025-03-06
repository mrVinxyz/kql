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
class DatabaseExecuteBatchTest {
    private lateinit var database: Database
    private val dbName = "execute_batch_test.db"

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
                    CREATE TABLE test_batch_table (
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
    fun `test executeBatch successfully inserts multiple rows`() = runBlocking {
        val sql = "INSERT INTO test_batch_table (id, name, value) VALUES (?, ?, ?)"
        val batchArgs = listOf(
            arrayOf(1, "Name 1", 10),
            arrayOf(2, "Name 2", 20),
            arrayOf(3, "Name 3", 30)
        )

        val result = database.executeBatch(sql, batchArgs)
        assertTrue(result is DatabaseResult.Success)

        val cursor = database.query(
            "SELECT id, name, value FROM test_batch_table ORDER BY id"
        )

        cursor.use {
            val results = mutableListOf<Triple<Int, String, Int>>()
            
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val value = it.getInt(it.getColumnIndexOrThrow("value"))
                results.add(Triple(id, name, value))
            }

            assertEquals(3, results.size)
            assertEquals(Triple(1, "Name 1", 10), results[0])
            assertEquals(Triple(2, "Name 2", 20), results[1])
            assertEquals(Triple(3, "Name 3", 30), results[2])
        }
    }

    @Test
    fun `test executeBatch handles SQL syntax errors`() = runBlocking {
        val invalidSql = "INSERT INTO non_existent_table (id, name, value) VALUES (?, ?, ?)"
        val batchArgs = listOf(
            arrayOf(1, "Name 1", 10),
            arrayOf(2, "Name 2", 20)
        )

        val result = database.executeBatch(invalidSql, batchArgs)
        assertTrue(result is DatabaseResult.Failure)

        val error = result.getErrorOrNull()
        assertTrue(error is DatabaseError.BatchExecutionError)
    }

    @Test
    fun `test executeBatch handles binding parameter errors`() = runBlocking {
        val sql = "INSERT INTO test_batch_table (id, name, value) VALUES (?, ?, ?)"
        val batchArgs = listOf(
            arrayOf(1, "Name 1", 10),
            arrayOf("not-an-int", "Name 2", 20)
        )

        val result = database.executeBatch(sql, batchArgs)
        assertTrue(result is DatabaseResult.Failure)

        val error = result.getErrorOrNull()
        assertTrue(error is DatabaseError.BatchExecutionError)
    }

    @Test
    fun `test executeBatch with empty batch args does nothing`() = runBlocking {
        val sql = "INSERT INTO test_batch_table (id, name, value) VALUES (?, ?, ?)"
        val batchArgs = emptyList<Array<out Any?>>()

        val result = database.executeBatch(sql, batchArgs)
        assertTrue(result is DatabaseResult.Success)

        // Verify no data was inserted
        val cursor = database.query("SELECT COUNT(*) FROM test_batch_table")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun `test executeBatch rolls back on failure`() = runBlocking {
        database.execute(
            "INSERT INTO test_batch_table (id, name, value) VALUES (?, ?, ?)",
            0, "Initial Row", 100
        )

        val sql = "INSERT INTO test_batch_table (id, name, value) VALUES (?, ?, ?)"
        val batchArgs = listOf(
            arrayOf(1, "Name 1", 10),
            arrayOf(0, "Duplicate PK", 20),
            arrayOf(3, "Name 3", 30)
        )

        val result = database.executeBatch(sql, batchArgs)
        assertTrue(result is DatabaseResult.Failure)

        val cursor = database.query("SELECT COUNT(*) FROM test_batch_table")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
    }
}