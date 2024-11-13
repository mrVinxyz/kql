package query

import exec.Executor
import exec.Row
import java.sql.Connection

/**
 * The `Query` class encapsulates an SQL query and its associated parameters.
 * It allows the execution of SQL queries through the `Executor` object and provides utility functions for mapping results.
 *
 * @param sql The SQL query string.
 * @param args A variable number of arguments to be used as parameters in the SQL query.
 *
 * The `args` list can handle both individual parameters and lists of parameters.
 *
 * ### Example:
 * ```
 * val query = Query("SELECT * FROM users WHERE id = ?", 1)
 * val result: Result<User> = query.execMapOne(conn) { row ->
 *     User(row.get<Int>("id"), row.get<String>("name"))
 * }
 * ```
 */
open class Query(val sql: String, vararg args: Any?) {
    /**
     * A flattened list of SQL query parameters.
     * If a parameter is a list, it is flattened into individual elements.
     */
    val args = args.flatMap {
        it as? List<*> ?: listOf(it)
    }

    /**
     * Returns a pair consisting of the SQL string and its associated parameters.
     */
    fun sqlArgs(): Pair<String, List<Any?>> = Pair(sql, args)

    override fun toString(): String = "[SQL = $sql]\n [ARGS = $args]"

    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Query

        if (sql != other.sql) return false
        if (args != other.args) return false

        return true
    }
}

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
 * val result: Result<User> = query.execMapOne(conn) { row ->
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
 * val result: Result<List<User>> = query.execMapList(conn) { row ->
 *     User(row.get<Int>("id"), row.get<String>("name"))
 * }
 * ```
 */
inline fun <reified R> Query.execMapList(conn: Connection, mapper: (Row) -> R): Result<List<R>> =
    Executor.execMapList(conn, this, mapper)