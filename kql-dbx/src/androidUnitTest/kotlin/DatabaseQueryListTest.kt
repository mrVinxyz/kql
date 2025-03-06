import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseQueryListTest {
    private lateinit var database: Database
    private val dbName = "query_list_test.db"

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Database.create(
            context = appContext(),
            name = dbName,
            version = 1,
            onCreate = {
                execSQL(TestEntity.createTable())

                execSQL("""
                    INSERT INTO test_entity (
                        pkId, stringValue, intValue, longValue, floatValue, doubleValue, 
                        bigDecimalValue, shortValue, booleanValue, byteArrayValue, byteValue, nullValue
                    ) VALUES (
                        1, 'Entity One', 42, 1234567890, 3.14, 2.71828, 
                        '123.456', 100, 1, X'DEADBEEF', 7, NULL
                    )
                """)

                execSQL("""
                    INSERT INTO test_entity (
                        pkId, stringValue, intValue, longValue, floatValue, doubleValue, 
                        bigDecimalValue, shortValue, booleanValue, byteArrayValue, byteValue, nullValue
                    ) VALUES (
                        2, 'Entity Two', 84, 9876543210, 6.28, 3.14159, 
                        '456.789', 200, 0, X'CAFEBABE', 14, 'not null'
                    )
                """)

                execSQL("""
                    INSERT INTO test_entity (
                        pkId, stringValue, intValue, longValue, floatValue, doubleValue, 
                        bigDecimalValue, shortValue, booleanValue, byteArrayValue, byteValue, nullValue
                    ) VALUES (
                        3, 'Entity Three', 126, 555555555, 9.42, 1.61803, 
                        '789.123', 300, 1, X'BAADF00D', 21, NULL
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
    fun `test queryList returns all entities`() = runBlocking {
        val sql = "SELECT * FROM test_entity ORDER BY pkId"

        val result = database.queryList(sql) { row ->
            val id = row.get<Int>("pkId")
            println("ID $id")
            TestEntity(
                pkId = id!!,
                stringValue = row.get<String>("stringValue"),
                intValue = row.get<Int>("intValue"),
                longValue = row.get<Long>("longValue"),
                floatValue = row.get<Float>("floatValue"),
                doubleValue = row.get<Double>("doubleValue"),
                bigDecimalValue = row.get<BigDecimal>("bigDecimalValue"),
                shortValue = row.get<Short>("shortValue"),
                booleanValue = row.get<Boolean>("booleanValue"),
                byteArrayValue = row.get<ByteArray>("byteArrayValue"),
                byteValue = row.get<Byte>("byteValue"),
                nullValue = row.get<String>("nullValue")
            )
        }

        assertTrue(result is DatabaseResult.Success)
        val entities = result.getOrNull()

        assertEquals(3, entities?.size)

        assertEquals(1, entities?.get(0)?.pkId)
        assertEquals("Entity One", entities?.get(0)?.stringValue)
        assertEquals(42, entities?.get(0)?.intValue)

        assertEquals(2, entities?.get(1)?.pkId)
        assertEquals("Entity Two", entities?.get(1)?.stringValue)
        assertEquals(84, entities?.get(1)?.intValue)
        assertEquals("not null", entities?.get(1)?.nullValue)

        assertEquals(3, entities?.get(2)?.pkId)
        assertEquals("Entity Three", entities?.get(2)?.stringValue)
        assertEquals(126, entities?.get(2)?.intValue)
    }

    @Test
    fun `test queryList with no matching records returns null`() = runBlocking {
        val sql = "SELECT * FROM test_entity WHERE pkId > 100"

        val result = database.queryList(sql) { row ->
            TestEntity(pkId = row.get<Int>("pkId")!!)
        }

        assertTrue(result is DatabaseResult.Success)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test queryList with projection returns selected columns`() = runBlocking {
        val sql = "SELECT pkId, stringValue FROM test_entity ORDER BY pkId"

        val result = database.queryList(sql) { row ->
            TestEntity(
                pkId = row.get<Int>("pkId")!!,
                stringValue = row.get<String>("stringValue")
            )
        }

        assertTrue(result is DatabaseResult.Success)
        val entities = result.getOrNull()

        assertEquals(3, entities?.size)
        assertEquals(1, entities?.get(0)?.pkId)
        assertEquals("Entity One", entities?.get(0)?.stringValue)
        assertNull(entities?.get(0)?.intValue)
    }

    @Test
    fun `test queryList handles SQL syntax errors`() = runBlocking {
        val invalidSql = "SELECT * FROM non_existent_table"

        val result = database.queryList<TestEntity>(invalidSql) { row ->
            TestEntity(pkId = row.get<Int>("pkId")!!)
        }

        assertTrue(result is DatabaseResult.Failure)
        val error = result.getErrorOrNull()
        println(error)
        assertTrue(error is DatabaseError.QueryError)
    }

    @Test
    fun `test queryList with column index access`() = runBlocking {
        val sql = "SELECT pkId, stringValue, intValue, booleanValue FROM test_entity WHERE pkId = ?"

        val result = database.queryList(sql, 2) { row ->
            TestEntity(
                pkId = row.get<Int>(0)!!,
                stringValue = row.get<String>(1),
                intValue = row.get<Int>(2),
                booleanValue = row.get<Boolean>(3)
            )
        }

        assertTrue(result is DatabaseResult.Success)
        val entities = result.getOrNull()

        assertEquals(1, entities?.size)
        assertEquals(2, entities?.get(0)?.pkId)
        assertEquals("Entity Two", entities?.get(0)?.stringValue)
        assertEquals(84, entities?.get(0)?.intValue)
        assertEquals(false, entities?.get(0)?.booleanValue)
    }
}