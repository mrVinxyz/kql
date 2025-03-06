import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue
import java.math.BigDecimal


@RunWith(RobolectricTestRunner::class)
class DatabaseQuerySingleTest {
    private lateinit var database: Database
    private val dbName = "query_single_test.db"

    private fun appContext(): Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        database = Database.create(
            context = appContext(),
            name = dbName,
            version = 1,
            onCreate = {
                execSQL(TestEntity.createTable())

                execSQL(
                    """
                    INSERT INTO test_entity (
                        pkId, stringValue, intValue, longValue, floatValue, doubleValue, 
                        bigDecimalValue, shortValue, booleanValue, byteArrayValue, byteValue, nullValue
                    ) VALUES (
                        1, 'Test String', 42, 1234567890, 3.14, 2.71828, 
                        '123.456', 100, 1, X'DEADBEEF', 7, NULL
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
    fun `test querySingle returns correct entity`() = runBlocking {
        val sql = "SELECT * FROM test_entity WHERE pkId = ?"

        val result = database.querySingle(sql, 1) { row ->
            TestEntity(
                pkId = row.get<Int>("pkId")!!,
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
        val entity = result.getOrNull()

        assertEquals(1, entity?.pkId)
        assertEquals("Test String", entity?.stringValue)
        assertEquals(42, entity?.intValue)
        assertEquals(1234567890L, entity?.longValue)
        assertEquals(3.14f, entity?.floatValue)
        assertEquals(2.71828, entity?.doubleValue)
        assertEquals(BigDecimal("123.456"), entity?.bigDecimalValue)
        assertEquals(100.toShort(), entity?.shortValue)
        assertEquals(true, entity?.booleanValue)
        assertEquals(0xDE.toByte(), entity?.byteArrayValue?.get(0))
        assertEquals(7.toByte(), entity?.byteValue)
        assertNull(entity?.nullValue)
    }

    @Test
    fun `test querySingle returns null for non-existent entity`() = runBlocking {
        val sql = "SELECT * FROM test_entity WHERE pkId = ?"

        val result = database.querySingle<TestEntity>(sql, 999) { row ->
            TestEntity(
                pkId = row.get<Int>("pkId")!!,
                stringValue = row.get<String>("stringValue"),
                intValue = row.get<Int>("intValue"),
                longValue = row.get<Long>("longValue")
            )
        }

        assertTrue(result is DatabaseResult.Success)
        assertNull(result.getOrNull())
    }

    @Test
    fun `test querySingle handles SQL syntax errors`() = runBlocking {
        val invalidSql = "SELECT * FROM non_existent_table WHERE pkId = ?"

        val result = database.querySingle<TestEntity>(invalidSql, 1) { row ->
            TestEntity(pkId = row.get<Int>("pkId")!!)
        }

        assertTrue(result is DatabaseResult.Failure)
        val error = result.getErrorOrNull()
        assertTrue(error is DatabaseError.QueryError)
    }

    @Test
    fun `test querySingle with column index access`() = runBlocking {
        val sql = "SELECT pkId, stringValue, intValue, booleanValue FROM test_entity WHERE pkId = ?"

        val result = database.querySingle(sql, 1) { row ->
            TestEntity(
                pkId = row.get<Int>(0)!!,
                stringValue = row.get<String>(1),
                intValue = row.get<Int>(2),
                booleanValue = row.get<Boolean>(3)
            )
        }

        assertTrue(result is DatabaseResult.Success)
        val entity = result.getOrNull()

        assertEquals(1, entity?.pkId)
        assertEquals("Test String", entity?.stringValue)
        assertEquals(42, entity?.intValue)
        assertEquals(true, entity?.booleanValue)
    }
}