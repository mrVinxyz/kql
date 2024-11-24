package exec

import query.Insert
import query.InsertResult
import query.Query
import Select
import SelectResult
import Update
import UpdateResult
import query.BaseFilter
import query.Delete
import query.FilterCheck
import query.FilterResult
import java.sql.Connection

/**
 * Executes the SQL update query (e.g., INSERT, UPDATE, DELETE) using the given connection.
 *
 * @param conn The [Connection] to the database.
 * @return A [Result] of [Unit] indicating the success or failure of the query execution.
 */
fun Query.exec(conn: Connection): Result<Unit> = Executor.exec(conn, this)

/**
 * Executes the SQL query and returns the generated key as an [Int].
 *
 * This is useful for queries that generate a new key, such as `INSERT` statements with an auto-incremented key.
 *
 * @param conn The [Connection] to the database.
 * @return A [Result] of the generated key of type [Int].
 */
fun Query.execReturnKey(conn: Connection): Result<Int> = Executor.execReturnKey(conn, this)

/**
 * Executes a SQL query that returns a single row and maps the result to an object of type [R] using the provided mapper function.
 *
 * @param R The type to map the result to.
 * @param conn The [Connection] to the database.
 * @param mapper A function that takes a [Row] and returns a mapped result of type [R].
 * @return A [Result] of type [R].
 *
 * Example usage:
 * ```
 * val query = Query("SELECT id, name FROM users WHERE id = ?", 1)
 * val result: Result<User> = .execMapOne(conn) { row ->
 *     User(row.get<Int>("id"), row.get<String>("name"))
 * }
 * ```
 */
inline fun <reified R> Query.execMapOne(conn: Connection, mapper: (Row) -> R): Result<R> =
    Executor.execMapOne(conn, this, mapper)

/**
 * Executes a SQL query that returns multiple rows and maps each result to an object of type [R] using the provided mapper function.
 *
 * @param R The type to map the results to.
 * @param conn The [Connection] to the database.
 * @param mapper A function that takes a [Row] and returns a mapped result of type [R].
 * @return A [Result] of a list of objects of type [R].
 *
 * Example usage:
 * ```
 * val query = Query("SELECT id, name FROM users")
 * val result: Result<List<User>> = .execMapList(conn) { row ->
 *     User(row.get<Int>("id"), row.get<String>("name"))
 * }
 * ```
 */
inline fun <reified R> Query.execMapList(conn: Connection, mapper: (Row) -> R): Result<List<R>> =
    Executor.execMapList(conn, this, mapper)

fun <T> Insert.persist(conn: Connection): InsertResult<T> {
    filters()?.let { ft ->
        val result = ft.execute(conn)
        if (!result.ok) {
            return InsertResult.FilterFailure(result)
        }
    }

    return try {
        val result = sqlArgs().execReturnKey(conn)
        when {
            result.isSuccess -> {
                @Suppress("UNCHECKED_CAST")
                val id = result.getOrThrow() as? T
                InsertResult.Success(id)
            }

            else -> InsertResult.DatabaseError(result.exceptionOrNull() as java.lang.Exception)
        }
    } catch (e: Exception) {
        InsertResult.DatabaseError(e)
    }
}

inline fun <reified R> Select.mapOne(
    conn: Connection,
    mapper: (Row) -> R
): SelectResult<R> = try {
    val result = sqlArgs().execMapOne(conn, mapper)
    when {
        result.isSuccess -> SelectResult.Success(result.getOrThrow())
        else -> SelectResult.Empty()
    }
} catch (e: Exception) {
    SelectResult.DatabaseError(e)
}

inline fun <reified R> Select.mapList(
    conn: Connection,
    mapper: (Row) -> R
): SelectResult<List<R>> = try {
    val result = sqlArgs().execMapList(conn, mapper)
    when {
        result.isSuccess -> SelectResult.Success(result.getOrThrow())
        else -> SelectResult.Empty()
    }
} catch (e: Exception) {
    SelectResult.DatabaseError(e)
}

fun Update.persist(conn: Connection): UpdateResult {
    filters()?.let { ft ->
        val result = ft.execute(conn)
        if (!result.ok) {
            return UpdateResult.FilterFailure(result)
        }
    }

    return try {
        val result = sqlArgs().exec(conn)
        when {
            result.isSuccess -> UpdateResult.Success
            else -> UpdateResult.DatabaseError(result.exceptionOrNull() as Exception)
        }
    } catch (e: Exception) {
        UpdateResult.DatabaseError(e)
    }
}

fun Delete.persist(conn: Connection): Unit = sqlArgs().exec(conn).getOrThrow()

fun FilterCheck.execute(conn: Connection): FilterResult {
    val exists = select
        .sqlArgs()
        .execMapOne(conn) { it.get<Int>(1) }
        .getOrThrow() == 1

    return if (exists == errorCase) {
        FilterResult(false, msg, field)
    } else {
        FilterResult(true)
    }
}

fun BaseFilter.execute(conn: Connection): FilterResult {
    filters().forEach { check ->
        val result = check.execute(conn)
        if (!result.ok) {
            return result
        }
    }
    return FilterResult(true)
}