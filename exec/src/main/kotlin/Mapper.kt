package exec

import schema.Column
import schema.ColumnType
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * The `Row` class provides a way to retrieve data from a single row of a [ResultSet].
 * It supports retrieving values based on column metadata through the [Column] class,
 * or directly via the column index or name.
 *
 * It supports the following types:
 * - [String]
 * - [Int]
 * - [Long]
 * - [Float]
 * - [Double]
 * - [BigDecimal]
 * - [Boolean]
 *
 * For nullable types such as [String] and [BigDecimal], if the result is `null`, a default value (`""` for [String] and `BigDecimal.ZERO` for [BigDecimal]) is returned.
 *
 * ### Example Usage with `Column`:
 * ```
 * val nameColumn = Column<String>("name", ColumnType.STRING)
 * val name: String = row.get(nameColumn)
 * ```
 *
 * ### Example Usage with Index or Name:
 * ```
 * val name: String = row.get<String>("name")
 * val age: Int = row.get<Int>(1)
 * ```
 *
 * @property resultSet The current row's [ResultSet] used to retrieve data.
 */
class Row(val resultSet: ResultSet) {
    /**
     * Retrieves a value of type [T] from the current row of the [ResultSet] based on the [Column] metadata.
     *
     * @param column The [Column] metadata, including its type and key (name or index).
     * @return The value of type [T] corresponding to the column.
     * Example:
     * ```
     * val nameColumn = Column<String>("name", ColumnType.STRING)
     * val name: String = row.get(nameColumn)
     * ```
     */
    inline operator fun <reified T : Any> get(column: Column<T>): T =
        when (column.type) {
            ColumnType.INT -> resultSet.getInt(column.key) as T
            ColumnType.STRING -> {
                val value = resultSet.getString(column.key)
                (value ?: "") as T
            }

            ColumnType.LONG -> resultSet.getLong(column.key) as T
            ColumnType.FLOAT -> resultSet.getFloat(column.key) as T
            ColumnType.DOUBLE -> resultSet.getDouble(column.key) as T
            ColumnType.DECIMAL -> {
                val value = resultSet.getBigDecimal(column.key)
                (value ?: BigDecimal.ZERO) as T
            }

            ColumnType.BOOLEAN -> resultSet.getBoolean(column.key) as T
        }

    /**
     * Retrieves a value of type [T] from the current row of the [ResultSet], using either the column index or column name specified by [arg].
     *
     * Supported types: [String], [Int], [Long], [Float], [Double], [BigDecimal], and [Boolean].
     * For [String] and [BigDecimal], if the result is `null`, a default value (`""` for [String], `BigDecimal.ZERO` for [BigDecimal]) is returned.
     *
     * @param T The type of the value to retrieve.
     * @param arg The column index ([Int]) or name ([String]).
     * @return The value from the specified column as type [T].
     * @throws IllegalStateException if [T] is unsupported or [arg] is not a valid column identifier.
     *
     * Example:
     * ```
     * val name: String = row.get<String>("name")
     * val age: Int = row.get<Int>(1)
     * ```
     */
    inline operator fun <reified T : Any> get(arg: Any): T =
        when (T::class) {
            String::class -> when (arg) {
                is Int -> resultSet.getString(arg) ?: ""
                is String -> resultSet.getString(arg) ?: ""
                else -> error("Invalid type: $arg")
            } as T

            Int::class -> when (arg) {
                is Int -> resultSet.getInt(arg)
                is String -> resultSet.getInt(arg)
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")
            } as T

            Long::class -> when (arg) {
                is Int -> resultSet.getLong(arg)
                is String -> resultSet.getLong(arg)
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")

            } as T

            Float::class -> when (arg) {
                is Int -> resultSet.getFloat(arg)
                is String -> resultSet.getFloat(arg)
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")

            } as T

            Double::class -> when (arg) {
                is Int -> resultSet.getDouble(arg)
                is String -> resultSet.getDouble(arg)
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")

            } as T

            BigDecimal::class -> when (arg) {
                is Int -> resultSet.getBigDecimal(arg) ?: BigDecimal.ZERO
                is String -> resultSet.getBigDecimal(arg) ?: BigDecimal.ZERO
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")

            } as T

            Boolean::class -> when (arg) {
                is Int -> resultSet.getBoolean(arg)
                is String -> resultSet.getBoolean(arg)
                else -> error("Invalid type: $arg; Arg must be position as Int or column name as String")

            } as T

            else -> error("Unsupported type: ${T::class}")
        }
}

/**
 * The `Rows` class provides an iterable interface for iterating through multiple rows in a [ResultSet].
 * Each iteration returns a `Row` object.
 *
 * The class implements [Iterable] and provides an iterator for navigating through the `ResultSet`.
 *
 * **Example Usage**:
 * ```
 * val rows = Rows(resultSet)
 * for (row in rows) {
 *     val id: Int = row[Table.ColumnId]
 *     val name: String = row.get<String>("name")
 *     val age: Int = row.get<Int>(1)
 * }
 * ```
 */
class Rows(private val resultSet: ResultSet) : Iterable<Row> {
    override fun iterator(): Iterator<Row> = resultSet.iterator()
}

fun ResultSet.iterator(): Iterator<Row> =
    object : Iterator<Row> {
        var isHasNext: Boolean? = null

        override fun hasNext() = isHasNext ?: this@iterator.next().also { isHasNext = it }

        override fun next(): Row {
            val isHasNext = isHasNext?.also { isHasNext = null } ?: this@iterator.next()
            if (!isHasNext) throw NoSuchElementException("No more rows in ResultSet.")
            return Row(this@iterator)
        }
    }

/**
 * Sets parameters on a [PreparedStatement] for the provided list of parameters.
 *
 * This function dynamically assigns the correct setter method on the [PreparedStatement] based on the type of each parameter.
 * It supports the following types:
 * - [String]
 * - [Int]
 * - [Long]
 * - [Float]
 * - [Double]
 * - [BigDecimal]
 * - [Boolean]
 *
 * If a type is not specifically handled, it defaults to using [java.sql.PreparedStatement.setObject].
 *
 * ### Example:
 * ```
 * val query = "INSERT INTO users (name, age) VALUES (?, ?)"
 * val stmt = connection.prepareStatement(query)
 * stmt.setParameters(listOf("John Doe", 30))
 * stmt.executeUpdate()
 * ```
 *
 * @param params The list of parameters to set on the [PreparedStatement].
 */
fun PreparedStatement.setParameters(params: List<Any?>) =
    params.forEachIndexed { index, param ->
        when (param) {
            is String -> this.setString(index + 1, param)
            is Int -> this.setInt(index + 1, param)
            is Long -> this.setLong(index + 1, param)
            is Float -> this.setFloat(index + 1, param)
            is Double -> this.setDouble(index + 1, param)
            is BigDecimal -> this.setBigDecimal(index + 1, param)
            is Boolean -> this.setBoolean(index + 1, param)
            else -> this.setObject(index + 1, param)
        }
    }
