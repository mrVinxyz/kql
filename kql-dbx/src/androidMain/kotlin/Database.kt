import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Database private constructor(
    context: Context,
    private val name: String,
    private val version: Int,
    private val onCreate: SQLiteDatabase.() -> Unit,
    private val onUpgrade: SQLiteDatabase.() -> Unit
) : SQLiteOpenHelper(context, name, null, version) {

    val db: SQLiteDatabase by lazy { writableDatabase }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.onCreate()
        } catch (e: Exception) {
            Log.e("Database", "Failed to create database $name", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.onUpgrade()
        } catch (e: Exception) {
            Log.e("Database", "Failed to upgrade database $name from $oldVersion to $newVersion", e)
            throw e
        }
    }

    suspend inline fun <T> transaction(
        crossinline block: suspend (Database) -> T
    ): DatabaseResult<T> {
        db.beginTransaction()
        try {
            val result = block(this@Database)
            db.setTransactionSuccessful()
            return DatabaseResult.Success(result)
        } finally {
            db.endTransaction()
        }
    }

    suspend fun execute(
        sql: String,
        vararg args: Any?
    ): DatabaseResult<Unit> =
        withContext(Dispatchers.IO) {
            DatabaseResult.runCatching({ e ->
                DatabaseError.ExecutionError(
                    sql = sql,
                    args = args,
                    message = "Execution failed: ${e.message}"
                )
            }) {
                db.compileStatement(sql).use { stmt ->
                    stmt.bindParameters(args)
                    stmt.execute()
                }
            }
        }

    suspend fun executeBatch(
        sql: String,
        batchArgs: List<Array<Any?>>
    ): DatabaseResult<Unit> =
        withContext(Dispatchers.IO) {
            DatabaseResult.runCatching({ e ->
                DatabaseError.BatchExecutionError(
                    sql = sql,
                    message = "Batch execution failed: ${e.message}"
                )
            }) {
                db.beginTransaction()
                try {
                    db.compileStatement(sql).use { stmt ->
                        for (args in batchArgs) {
                            stmt.clearBindings()
                            stmt.bindParameters(args)
                            if (stmt.executeUpdateDelete() < 0) {
                                error("Batch operation failed for args: ${args.contentToString()}")
                            }
                        }
                        db.setTransactionSuccessful()
                    }
                } finally {
                    db.endTransaction()
                }
            }
        }

    suspend inline fun <reified R> querySingle(
        sql: String,
        vararg args: Any?,
        crossinline mapper: (Row) -> R
    ): DatabaseResult<R?> =
        withContext(Dispatchers.IO) {
            DatabaseResult.runCatching({ e ->
                DatabaseError.QueryError(
                    sql = sql,
                    args = args,
                    message = "Query failed: ${e.message}"
                )
            }) {
                val stringArgs = args.map { it?.toString() }.toTypedArray()
                db.rawQuery(sql, stringArgs).use { cursor ->
                    Rows(cursor).use { rows ->
                        rows.iterator().let { iterator ->
                            if (iterator.hasNext()) mapper(iterator.next()) else null
                        }
                    }
                }
            }
        }

    suspend inline fun <reified R> queryList(
        sql: String,
        vararg args: Any?,
        crossinline mapper: (Row) -> R
    ): DatabaseResult<List<R>?> =
        withContext(Dispatchers.IO) {
            DatabaseResult.runCatching({ e ->
                DatabaseError.QueryError(
                    sql = sql,
                    args = args,
                    message = "Query failed: ${e.message}"
                )
            }) {
                val stringArgs = args.map { it?.toString() }.toTypedArray()
                db.rawQuery(sql, stringArgs).use { cursor ->
                    Rows(cursor).use { rows ->
                        val iterator = rows.iterator()
                        if (!iterator.hasNext()) return@runCatching null

                        val resultList = mutableListOf<R>().apply {
                            while (iterator.hasNext()) {
                                val row = iterator.next()
                                add(mapper(row))
                            }
                        }
                        resultList
                    }
                }
            }
        }

    suspend inline fun <reified R> querySequence(
        sql: String,
        vararg args: Any?,
        crossinline mapper: (Row) -> R
    ): DatabaseResult<Sequence<R>?> =
        withContext(Dispatchers.IO) {
            DatabaseResult.runCatching({ e ->
                DatabaseError.QueryError(
                    sql = sql,
                    args = args,
                    message = "Query failed: ${e.message}"
                )
            }) {
                val stringArgs = args.map { it?.toString() }.toTypedArray()
                val cursor = db.rawQuery(sql, stringArgs)

                val rows = Rows(cursor).iterator()
                if (!rows.hasNext()) {
                    cursor.close()
                    return@runCatching null
                }

                sequence {
                    cursor.use {
                        while (rows.hasNext()) {
                            yield(mapper(rows.next()))
                        }
                    }
                }
            }
        }

    fun query(sql: String, vararg args: Any?): Cursor =
        db.rawQuery(sql, args.map { it.toString() }.toTypedArray())

    companion object {
        fun create(
            context: Context,
            name: String,
            version: Int,
            onCreate: SQLiteDatabase.() -> Unit = {},
            onUpgrade: SQLiteDatabase.() -> Unit = { }
        ) = Database(context, name, version, onCreate, onUpgrade)
    }
}

