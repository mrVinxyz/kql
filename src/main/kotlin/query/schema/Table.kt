package query.schema

import query.Query
import query.expr.Delete
import query.expr.Insert
import query.expr.Select
import query.expr.Update
import query.expr.Where
import query.setParameters
import java.math.BigDecimal
import java.sql.Connection

/**
 * The `Table` class represents the structure of a database table.
 * It is designed to be extended by specific table implementations.
 *
 * @param tableName The name of the table in the database.
 * @param tablePrefix A flag to determine whether the table name should be prefixed to column names (default is `true`).
 */
abstract class Table(val tableName: String, protected val tablePrefix: Boolean = false) {
    protected val columns = mutableListOf<Column<*>>()
    private var primaryKey: Column<*>? = null

    /**
     * Dynamically creates a column for the given key (name) and inferred type [T].
     *
     * The column is prefixed with the table name if [tablePrefix] is set to `true`.
     * The function also maps the type [T] to a corresponding [ColumnType].
     *
     * @param key The name of the column (without the table prefix).
     * @return The created [Column] with the inferred type [T].
     */
    protected inline fun <reified T : Any> column(key: String): Column<T> {
        val columnType =
            when (T::class) {
                String::class -> ColumnType.STRING
                Int::class -> ColumnType.INT
                Long::class -> ColumnType.LONG
                Float::class -> ColumnType.FLOAT
                Double::class -> ColumnType.DOUBLE
                BigDecimal::class -> ColumnType.DECIMAL
                Boolean::class -> ColumnType.BOOLEAN
                else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
            }

        val parsedKey = if (tablePrefix) tableName.plus("_").plus(key) else key
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

    fun shortName(): String = tableName.split("_").map { it.first() }.joinToString("")
}

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


fun Table.insert(init: Insert.() -> Unit): Insert = Insert(this).apply(init)

fun Table.insert(vararg columns: Column<*>): Insert = Insert(this).insert(*columns)

fun Table.select(vararg columns: Column<*>): Select = Select(this).select(*columns)

fun Table.selectPrimary(value: Any, vararg columns: Column<*>): Select = Select(this).selectPrimary(value, *columns)

fun Table.update(block: (Update) -> Unit): Update = Update(this).apply(block)

fun Table.updatePrimary(value: Any, block: (Update) -> Unit) = Update(this).updatePrimary(value, block)

fun Table.deleteWhere(init: Where.() -> Unit): Delete = Delete(this).deleteWhere(init)

fun Table.deletePrimary(value: Any): Delete = Delete(this).deletePrimary(value)

fun Table.recordsCount(conn: Connection, block: Where.() -> Unit): Result<Int> {
    val sql = StringBuilder("SELECT COUNT(*) FROM ${this.tableName}")
    val where = Where()
    block(where)

    val clauses = where.whereClauses()
    sql.append(clauses.first)

    return runCatching {
        conn.prepareStatement(sql.toString()).use { stmt ->
            stmt.setParameters(clauses.second)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

inline fun <reified T> Table.recordExistsBy(
    conn: Connection,
    column: Column<*>,
    value: T,
): Result<Boolean> {
    val sql = "SELECT EXISTS(SELECT 1 FROM ${this.tableName} WHERE ${column.key()} = ?);"

    return runCatching {
        conn.prepareStatement(sql).use { stmt ->
            stmt.setParameters(listOf(value))
            stmt.executeQuery().use { rs ->
                rs.next() && rs.getBoolean(1)
            }
        }
    }
}

inline fun <reified T : Any> Table.recordExists(
    conn: Connection,
    value: T
): Result<Boolean> = recordExistsBy(conn, this.primaryKey<Int>(), value)
