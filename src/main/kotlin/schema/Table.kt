package schema

import dialect.SQLiteDialect
import expr.SqlRenderer
import query.Query
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
 * It is designed to be extended by specific table implementations.
 *
 * @param tableName The name of the table in the database.
 * @param usePrefix A flag to determine whether the table name should be prefixed to column names (default is `true`).
 */
abstract class Table(
    val tableName: String,
    val dialect: SqlRenderer,
    protected val usePrefix: Boolean = false,
    private val useAlias: String? = null
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
     * @return The created [Column] with the inferred type [T].
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
                else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
            }

        val parsedKey = if (usePrefix) tableName.plus("_").plus(key) else key
        val column = Column<T>(parsedKey, columnType, this)
        columns.add(column)

        return column
    }

    // Convenience functions for different column types
    protected fun text(key: String): Column<String> = column(key)
    protected fun integer(key: String): Column<Int> = column(key)
    protected fun long(key: String): Column<Long> = column(key)
    protected fun float(key: String): Column<Float> = column(key)
    protected fun double(key: String): Column<Double> = column(key)
    protected fun decimal(key: String): Column<BigDecimal> = column(key)
    protected fun boolean(key: String): Column<Boolean> = column(key)
    protected fun timestamp(key: String): Column<Long> = column(key)
    protected fun dateText(key: String): Column<String> = column(key)

    /**
     * Marks the current column as the primary key.
     *
     * @return The current column with the type [T] as the primary key.
     */
    protected fun <T : Any> Column<T>.setPrimaryKey(): Column<T> {
        primaryKey = this
        return this
    }

    /** Returns a list of all columns in the table. */
    fun getColumnsList(): List<Column<*>> = columns

    /**
     * Returns the primary key column.
     * If not primary key has been set, it will set the first column as primary key.
     *
     * @return The primary key [Column] of the table.
     */
    fun <T : Any> primaryKey(): Column<T> {
        if (primaryKey == null) primaryKey = columns.first()
        @Suppress("UNCHECKED_CAST") return primaryKey as Column<T>
    }

    fun alias(): String = useAlias ?: tableName.split("_").map { it.first() }.joinToString("")
}

abstract class LiteTable(
    tableName: String,
    usePrefix: Boolean = false,
    useAlias: String? = null
) : Table(
    tableName = tableName,
    dialect = SQLiteDialect(),
    usePrefix = usePrefix,
    useAlias = useAlias
)

/**
 * Generates an SQL `CREATE TABLE` query for the current table, based on its defined columns.
 *
 * The generated query ensures that the table is created if it does not already exist. The first column in the table,
 * or the explicitly set primary key, is marked as the `PRIMARY KEY`.
 *
 * @return A [Query] object containing the generated SQL string.
 *
 * Example usage:
 * ```
 * val userTable = object : Table("users") {
 *     val id = integer("id").setPrimaryKey()
 *     val name = text("name")
 *     val age = integer("age")
 * }
 * val createTableQuery = userTable.createTable()
 * ```
 */
fun Table.createTable(): Query {
    val sql = StringBuilder("CREATE TABLE IF NOT EXISTS ${this.tableName} (")

    this.getColumnsList().forEachIndexed { index, column ->
        index.takeIf { it == 0 }?.let {
            sql.append("${this.primaryKey<Int>().key()} ${column.type().sqlTypeStr()}")
            sql.append(" PRIMARY KEY, ")
            return@forEachIndexed
        }

        sql.append("${column.key()} ${column.type().sqlTypeStr()}")
        sql.append(", ")
    }
    sql.setLength(sql.length - 2)
    sql.append(");")

    return Query(sql.toString())
}