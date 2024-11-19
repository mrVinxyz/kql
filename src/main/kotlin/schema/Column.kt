package schema

/** The `ColumnType` represents the supported data types for database columns. */
enum class ColumnType {
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    DECIMAL,
    BOOLEAN,
    DATE_TEXT,
    DATE_TIMESTAMP
}

/**
 * Converts a [ColumnType] to its corresponding SQL type as a string.
 *
 * This function maps each `ColumnType` to a standard SQL data type, which is used in table creation
 * or query generation when defining columns in the database schema.

 * @return The SQL type as a string, corresponding to the [ColumnType].
 *
 * Example:
 * ```
 * val sqlType: String = ColumnType.INT.sqlTypeStr()  // Returns "INTEGER"
 * ```
 */
fun ColumnType.sqlTypeStr(): String {
    return when (this) {
        ColumnType.STRING -> "TEXT"
        ColumnType.INT -> "INTEGER"
        ColumnType.LONG -> "INTEGER"
        ColumnType.FLOAT -> "REAL"
        ColumnType.DOUBLE -> "REAL"
        ColumnType.DECIMAL -> "NUMERIC"
        ColumnType.BOOLEAN -> "INTEGER"
        ColumnType. DATE_TEXT -> "TEXT"
        ColumnType.DATE_TIMESTAMP -> "INTEGER"
    }
}

/**
 * The `Column` class represents a database column, which belongs to a specific table and has a data type.
 *
 * The class encapsulates the column's key (name), its type (as a [ColumnType]), and the table it belongs to.
 * The [ColumnType] determines the column's data type, which can be converted to a corresponding SQL type using the [sqlArgsType] function.
 *
 * @param key The name of the column in the table.
 * @param type The [ColumnType] of the column, representing the type of data it holds.
 * @param table The [Table] object representing the table to which this column belongs.
 *
 * Example:
 * ```
 * val idColumn = Column<Int>("id", ColumnType.INT, usersTable)
 * idColumn.key() // Returns "id"
 * idColumn.type() // Returns Column.INT
 * idColumn.table() // returns UsersTable
 * ```
 */
open class Column<T : Any>(
    private val key: String,
    private val type: ColumnType,
    private val table: Table,
) {
    /** Returns the name of the column. */
    fun key(): String = key

    /** Returns the [ColumnType] of the column, which represents the type of data it stores. */
    fun type(): ColumnType = type

    /** Returns the [Table] to which this column belongs. */
    fun table(): Table = table

    /** Returns the Column key name formatted with table name alias. */
    fun keyWithTableAlias(): String {
        return table.alias().plus(".").plus(key)
    }
}
