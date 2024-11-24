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
}

/**
 * The `Column` class represents a database column, which belongs to a specific table and has a data type.
 *
 * The class encapsulates the column's key (name), its type (as a [ColumnType]), and the table it belongs to.
 *
 * @param key The name of the column in the table.
 * @param type The [ColumnType] of the column, representing the type of data it holds.
 * @param table The [Table] object representing the table to which this column belongs.
 *
 * Example:
 * ```
 * val idColumn = Column<Int>("id", ColumnType.INT, usersTable)
 * idcolumn.key // Returns "id"
 * idColumn.type() // Returns Column.INT
 * idColumn.table() // returns UsersTable
 * ```
 */
open class Column<T : Any>(
    val key: String,
    val type: ColumnType,
    val table: Table,
)
