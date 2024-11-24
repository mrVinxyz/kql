package query

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
 * val result: Result<User> = .execMapOne(conn) { row ->
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
