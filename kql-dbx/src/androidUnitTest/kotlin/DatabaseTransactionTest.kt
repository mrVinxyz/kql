import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseTransactionTest {
    private lateinit var database: Database
    private val dbName = "transaction_test.db"

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Database.create(
            context = appContext(),
            name = dbName,
            version = 1,
            onCreate = {
                execSQL("""
                    CREATE TABLE test_transaction (
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
    fun `test successful transaction`() = runBlocking {
        val result = database.transaction { db ->
            db.execute("INSERT  INTO test_transaction (id, value) VALUES (?, ?)", 1, "First")
            db.execute("INSERT INTO test_transaction (id, value) VALUES (?, ?)", 2, "Second")
            "Transaction completed"
        }

        assertTrue(result is DatabaseResult.Success)
        assertEquals("Transaction completed", result.getOrNull())

        val countResult = database.querySingle("SELECT COUNT(*) as count FROM test_transaction") { row ->
            row.get<Long>("count")
        }

        assertTrue(countResult is DatabaseResult.Success)
        assertEquals(2L, countResult.getOrNull())
    }

    @Test
    fun `test transaction rollback on error`() = runBlocking {
        val result = database.transaction { db ->
            db.execute("INSERT INTO test_transaction (id, value) VALUES (?, ?)", 1, "First")
            error("Simulated transaction failure")
        }

        assertTrue(result is DatabaseResult.Failure)

        val countResult = database.querySingle("SELECT COUNT(*) as count FROM test_transaction") { row ->
            row.get<Long>("count")
        }

        assertTrue(countResult is DatabaseResult.Success)
        assertEquals(0L, countResult.getOrNull())
    }

    @Test
    fun `test transaction returns last expression value`() = runBlocking {
        val result = database.transaction { db ->
            db.execute("INSERT INTO test_transaction (id, value) VALUES (?, ?)", 1, "First")
            42
        }

        assertTrue(result is DatabaseResult.Success)
        assertEquals(42, result.getOrNull())
    }
}