package exec

import java.sql.Connection
import java.sql.Statement
import query.Query

/**
 * The `Executor` object provides a set of utility functions to execute SQL queries and map their results.
 * It supports executing updates, returning generated keys, mapping a single result, or mapping multiple rows to a list.
 * Each function wraps the execution in `runCatching` to handle any exceptions and return a `Result`.
 */
object Executor {
    /**
     * Executes an update SQL query (e.g., INSERT, UPDATE, DELETE) on the provided connection and query.
     *
     * The query parameters are set using the [Query.args] list.
     *
     * @param conn The [Connection] to the database.
     * @param query The [Query] object containing the SQL and arguments.
     * @return A [Result] of [Unit] indicating success or failure.
     *
     * Example usage:
     * ```
     * val query = Query("UPDATE users SET name = ? WHERE id = ?", listOf("John Doe", 1))
     * val nothing: Result<Unit> = Executor.exec(conn, query)
     * ```
     */
    fun exec(conn: Connection, query: Query): Result<Unit> =
        runCatching {
            conn.prepareStatement(query.sql).use { stmt ->
                stmt.setParameters(query.args)
                stmt.executeUpdate()
                Unit
            }
        }.onFailure {
            error(
                "An error occurred while executing:\n[SQL] ${query.sql}\n[ARGS] ${query.args}\n${it.message};"
            )
        }


    /**
     * Executes an update SQL query and returns the generated key(s).
     *
     * This function is used for queries like INSERT that generate keys (e.g., auto-incremented IDs).
     *
     * @param conn The [Connection] to the database.
     * @param query The [Query] object containing the SQL and arguments.
     * @return A [Result] of the generated key of type [T].
     *
     * Example usage:
     * ```
     * val query = Query("INSERT INTO users (name) VALUES (?)", listOf("John Doe"))
     * val id: Result<Int> = Executor.execReturn(conn, query)
     * ```
     */
    inline fun <reified T : Any> execReturnKey(conn: Connection, query: Query): Result<T> =
        runCatching {
            conn.prepareStatement(query.sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setParameters(query.args)
                stmt.executeUpdate()

                stmt.generatedKeys.use { rs ->
                    Row(rs).get<T>(1)
                }
            }
        }.onFailure {
            error(
                "An error occurred while executing and returning:\n[SQL] ${query.sql}\n[ARGS] ${query.args}\n${it.message};"
            )
        }

    /**
     * Executes a SQL query and maps the first result row using the provided mapping function.
     *
     * This function is typically used when a query is expected to return only one row, such as `SELECT` with a unique constraint.
     *
     * @param conn The [Connection] to the database.
     * @param query The [Query] object containing the SQL and arguments.
     * @param mapper A function that maps a [Row] to a result of type [R].
     * @return A [Result] of the mapped object of type [R].
     *
     * Example usage:
     * ```
     * val query = Query("SELECT id, name FROM users WHERE id = ?", listOf(1))
     * val user: Result<User> = Executor.execMapOne(conn, query) { row ->
     *     User(row.get<Int>("id"), row.get<String>("name"))
     * }
     * ```
     */
    inline fun <reified R> execMapOne(conn: Connection, query: Query, mapper: (Row) -> R): Result<R> =
        runCatching {
            conn.prepareStatement(query.sql).use { stmt ->
                stmt.setParameters(query.args)
                val rs = stmt.executeQuery()
                val row = Row(rs)
                mapper(row)
            }
        }.onFailure {
            error(
                "An error occurred while executing and mapping one:\n[SQL] ${query.sql}\n[ARGS] ${query.args}\n${it.message};",
            )
        }

    /**
     * Executes a SQL query and maps all result rows to a list using the provided mapping function.
     *
     * This function is typically used for queries that return multiple rows, such as `SELECT` without a unique constraint.
     *
     * @param conn The [Connection] to the database.
     * @param query The [Query] object containing the SQL and arguments.
     * @param mapper A function that maps a [Row] to a result of type [R].
     * @return A [Result] of a list of mapped objects of type [R].
     *
     * Example usage:
     * ```
     * val query = Query("SELECT id, name FROM users")
     * val users: Result<List<User>> = Executor.execMapList(conn, query) { row ->
     *     User(row.get<Int>("id"), row.get<String>("name"))
     * }
     * ```
     */
    inline fun <reified R> execMapList(conn: Connection, query: Query, mapper: (Row) -> R): Result<List<R>> =
        runCatching {
            conn.prepareStatement(query.sql).use { stmt ->
                stmt.setParameters(query.args)

                val rs = stmt.executeQuery()
                val rows = Rows(rs).iterator()

                val resultList = mutableListOf<R>()
                while (rows.hasNext()) {
                    val row = rows.next()
                    resultList.add(mapper(row))
                }

                resultList
            }
        }.onFailure {
            error(
                "An error occurred while executing and mapping list:\n[SQL] ${query.sql}\n[ARGS] ${query.args}\n${it.message};"
            )
        }
}