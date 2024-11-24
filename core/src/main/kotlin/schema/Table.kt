package schema

import expr.SqlRenderer
import java.math.BigDecimal
import schema.ColumnType.BOOLEAN
import schema.ColumnType.DECIMAL
import schema.ColumnType.DOUBLE
import schema.ColumnType.FLOAT
import schema.ColumnType.INT
import schema.ColumnType.LONG
import schema.ColumnType.STRING

/**
 * The `Table` class represents the structure of a database table.
 *
 * @param tableName The name of the table in the database.
 * @param dialect The [SqlRenderer] dialect used for rendering SQL queries.
 * @param usePrefix A flag to determine whether the table name should be prefixed to column names (defaults to `false`).
 *                  When true, each column name will be automatically prefixed with the table name and an underscore
 *                  (e.g., for a table "users", a column "id" becomes "users_id").
 * @param alias Optional table alias. Defaults to acronym of table name plus '_' (e.g. 'table_name' -> 'tn_')

 * Example:
 * ```
 * object UsersTable : Table("users", dialect) {
 *     val id = integer("id").setPrimaryKey()
 *     val name = text("name")
 *     val age = integer("age")
 * }
 * ```
 */
abstract class Table(
    val tableName: String,
    val dialect: SqlRenderer,
    protected val usePrefix: Boolean = false,
    private val alias: String? = null
) {
    protected val columns = mutableListOf<Column<*>>()
    private var primaryKey: Column<*>? = null

    /**
     * Dynamically creates a column for the given key (name) and inferred type [T].
     *
     * The column is prefixed with the table name if [usePrefix] is set to `true`.
     * The function also maps the type [T] to a corresponding [ColumnType].
     *
     * @param key The name of the column (without the table prefix).
     * @throws error if the type [T] is not supported
     */
    protected inline fun <reified T : Any> column(key: String): Column<T> {
        val columnType =
            when (T::class) {
                String::class -> STRING
                Int::class -> INT
                Long::class -> LONG
                Float::class -> FLOAT
                Double::class -> DOUBLE
                BigDecimal::class -> DECIMAL
                Boolean::class -> BOOLEAN
                else -> throw error("Unsupported type: ${T::class}")
            }

        val parsedKey = if (usePrefix) tableName.plus("_").plus(key) else key
        val column = Column<T>(parsedKey, columnType, this)
        columns.add(column)

        return column
    }

    /** Creates a text column. */
    fun text(key: String): Column<String> = column(key)

    /** Creates an integer column. */
    fun integer(key: String): Column<Int> = column(key)

    /** Creates a long integer column. */
    fun long(key: String): Column<Long> = column(key)

    /** Creates a floating-point column. */
    fun float(key: String): Column<Float> = column(key)

    /** Creates a double precision floating-point column. */
    fun double(key: String): Column<Double> = column(key)

    /** Creates a decimal column. */
    fun decimal(key: String): Column<BigDecimal> = column(key)

    /** Creates a boolean column. */
    fun boolean(key: String): Column<Boolean> = column(key)

    /** Creates a timestamp column (stored as milliseconds since epoch). */
    fun timestamp(key: String): Column<Long> = column(key)

    /**
     * Marks the current column as the primary key.
     *
     * @return The current column as the primary key.
     */
    fun <T : Any> Column<T>.setPrimaryKey(): Column<T> {
        primaryKey = this
        return this
    }

    /** Returns a list of all columns in the table. */
    fun columnsList(): List<Column<*>> = columns

    /**
     * Returns the primary key column of the table.
     * @return The primary key [Column] of the table, or null if no primary key has been set
     *
     * @return The primary key [Column] of the table.
     */
    fun <T : Any> primaryKey(): Column<T>? {
        @Suppress("UNCHECKED_CAST")
        return primaryKey as Column<T>
    }

    /**
     * Returns the table's alias.
     * If no alias was provided, generates one from the table name by taking the first letter
     * of each word (e.g. 'table_name' -> 'tn').
     */
    fun alias(): String = alias ?: tableName.split("_").map { it.first() }.joinToString("")
}
