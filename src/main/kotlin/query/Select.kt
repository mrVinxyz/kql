package query

import exec.Row
import expr.ColumnsSelectExpr
import expr.CountSelectExpr
import expr.ExistsSelectExpr
import expr.JoinExpression
import expr.LimitExpr
import expr.OffsetExpr
import expr.OrderByExpr
import expr.OrderByExpression
import expr.OrderDirection
import expr.SelectClauseExpression
import java.sql.Connection
import schema.Column
import schema.Table

class Select(private val table: Table) {
    private val selectColumns = mutableListOf<Column<*>>()
    private var whereClause: Where? = null
    private val joinExpressions = mutableListOf<JoinExpression>()
    private var limitExpr: LimitExpr? = null
    private var offsetExpr: OffsetExpr? = null
    private var orderByExpr: OrderByExpression? = null
    private var selectExpr: SelectClauseExpression = ColumnsSelectExpr(table.getColumnsList())

    fun count(block: (Select.() -> Unit)? = null): Select {
        selectExpr = CountSelectExpr()
        block?.let { this.it() }
        return this
    }

    fun exists(block: Where.() -> Unit): Select {
        selectExpr = ExistsSelectExpr()
        where(block)
        return this
    }

    fun select(vararg columns: Column<*>): Select {
        require(columns.isNotEmpty()) { "select columns can not be empty" }

        selectColumns.addAll(columns)
        selectExpr = ColumnsSelectExpr(selectColumns)

        return this
    }

    fun selectAll(vararg except: Column<*>): Select {
        val columns = table.getColumnsList()
        val expectColumns = columns.filter { it !in except }
        select(*expectColumns.toTypedArray())
        return this
    }

    fun <T : Any> selectPrimary(id: T, vararg columns: Column<*>): Select {
        val primaryKey = table.primaryKey<T>()
        select(*columns)
        where { primaryKey eq id }
        return this
    }

    fun where(init: Where.() -> Unit): Select {
        val newWhere = Where()
        init(newWhere)

        whereClause = newWhere
        return this
    }

    fun join(vararg columns: Column<*>, init: Join.() -> Unit): Select {
        val builder = Join(table, columns.toList())
        builder.init()
        builder.build()?.let { join ->
            joinExpressions.add(join)
        }
        return this
    }

    fun limit(value: Int?): Select {
        limitExpr = value?.let {
            require(it > 0) { "Limit must be greater than 0" }
            LimitExpr(it)
        }
        return this
    }

    fun offset(value: Int?): Select {
        offsetExpr = value?.let {
            require(it >= 0) { "Offset must be greater than or equal to 0" }
            OffsetExpr(it)
        }
        return this
    }

    fun pagination(page: Int?, size: Int?): Select {
        if (page != null && size != null) {
            require(page > 0) { "Page must be greater than 0" }
            require(size > 0) { "Page size must be greater than 0" }

            limitExpr = LimitExpr(size)
            offsetExpr = OffsetExpr((page - 1) * size)
        }
        return this
    }

    fun orderBy(vararg orders: Pair<Column<*>, OrderDirection>): Select {
        orderByExpr = OrderByExpr(orders.toList())
        return this
    }

    fun sqlArgs(): Query {
        val sql = StringBuilder().apply {
            if (selectExpr is ColumnsSelectExpr) {
                append("SELECT ")
                val mainTableColumns = selectColumns.map { column ->
                    "${table.alias()}.${column.key()}"
                }

                val joinedColumns = joinExpressions.flatMap { it.columnsSql(true) }
                append((mainTableColumns + joinedColumns).joinToString(", "))
            } else {
                append(selectExpr.sqlArgs(true).sql)
            }

            append(" FROM ")

            if (
                selectExpr !is CountSelectExpr ||
                joinExpressions.isNotEmpty() ||
                whereClause != null
            ) {
                append(table.tableName)
                append(" ")
                append(table.alias())
            }

            joinExpressions.forEach { expr ->
                append(" ")
                val fragment = expr.sqlArgs(true)
                append(fragment.sql)
            }

            whereClause?.buildWhere(true)?.let { fragment ->
                append(" WHERE ")
                append(fragment.sql)
            }

            if (
                selectExpr !is CountSelectExpr &&
                selectExpr !is ExistsSelectExpr
            ) {
                orderByExpr?.sqlArgs(true)?.let { fragment ->
                    append(" ORDER BY ")
                    append(fragment.sql)
                }

                limitExpr?.sqlArgs(true)?.let { fragment ->
                    append(" ")
                    append(fragment.sql)
                }

                offsetExpr?.sqlArgs(true)?.let { fragment ->
                    append(" ")
                    append(fragment.sql)
                }
            }

            if (selectExpr is ExistsSelectExpr) {
                append(")")
            }
        }

        val args = mutableListOf<Any?>()
        whereClause?.buildWhere(true)?.let { args.addAll(it.args) }
        if (selectExpr !is CountSelectExpr && selectExpr !is ExistsSelectExpr) {
            limitExpr?.sqlArgs(true)?.let { args.addAll(it.args) }
            offsetExpr?.sqlArgs(true)?.let { args.addAll(it.args) }
        }

        return Query(sql.toString(), args)
    }
}

sealed class SelectResult<T> {
    data class Success<T>(
        val value: T
    ) : SelectResult<T>()

    class Empty<T> : SelectResult<T>()

    data class DatabaseError<T>(
        val exception: Exception
    ) : SelectResult<T>()

    fun isSuccess() = this is Success
    fun isEmpty() = this is Empty
    fun isDatabaseError() = this is DatabaseError

    fun getOrNull() = (this as? Success<T>)?.value
    fun getExceptionOrNull() = (this as? DatabaseError)?.exception
}

inline fun <reified R> Select.get(
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

inline fun <reified R> Select.list(
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

fun Table.select(vararg columns: Column<*>): Select = Select(this).select(*columns)

fun Table.selectPrimary(value: Any, vararg columns: Column<*>): Select = Select(this).selectPrimary(value, *columns)

fun Table.selectAll(vararg except: Column<*>): Select = Select(this).selectAll(*except)

fun Table.recordsCount(conn: Connection, block: (Where.() -> Unit)? = null): Result<Int> {
    val select = Select(this).count {
        block?.let { where(it) }
    }

    return select.sqlArgs().execMapOne(conn) {
        it.get<Int>(1)
    }
}
