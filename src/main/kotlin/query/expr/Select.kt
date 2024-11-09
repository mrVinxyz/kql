package query.expr

import query.Query
import query.Row
import query.execMapList
import query.execMapOne
import query.schema.Column
import query.schema.Table
import java.sql.Connection

class Select(private val table: Table) {
    private val selectColumns = mutableListOf<Column<*>>()
    private var args = mutableListOf<Any>()
    private var whereClauses: Where? = null
    private var joinClauses: Join? = null
    private var orderBy: OrderBy? = null
    private var limit: Int? = null
    private var offset: Int? = null
    private var isExists = false

    fun select(vararg columns: Column<*>): Select {
        columns.takeIf { it.isNotEmpty() }?.let { selectColumns.addAll(columns) }
            ?: selectColumns.addAll(table.getColumnsList())

        return this
    }

    fun selectPrimary(value: Int?, vararg columns: Column<*>): Select {
        val primaryKey = table.primaryKey<Int>()
        val selectWithCondition = where { primaryKey eq value }
        selectWithCondition.select(*columns)
        return selectWithCondition
    }

    operator fun <T : Any> Column<T>.unaryPlus() {
        selectColumns.add(this)
    }

    fun where(init: Where.() -> Unit): Select {
        val where = if (whereClauses == null) Where() else whereClauses!!
        init(where)
        whereClauses = where

        return this
    }

    fun join(vararg attachColumns: Column<*>, init: Join.() -> Unit): Select {
        val join = Join(table)
        init(join)
        joinClauses = join

        selectColumns.addAll(attachColumns)

        return this
    }

    fun orderBy(init: OrderBy.() -> Unit): Select {
        val orderBy = OrderBy()
        init(orderBy)
        this.orderBy = orderBy

        return this
    }

    fun limit(value: Int?): Select {
        value?.takeIf { it > 0 }?.let { limit = it }
        return this
    }

    fun offset(value: Int?): Select {
        value?.takeIf { it > 0 }?.let { offset = it }
        return this
    }

    fun pagination(page: Int?, pageSize: Int?): Select {
        val limit = pageSize ?: 10
        val offset = if (page != null && page > 0) {
            (page - 1) * limit
        } else {
            0
        }

        limit(limit)
        offset(offset)

        return this
    }

    fun exists(block: Where.() -> Unit): Select {
        isExists = true
        selectColumns.clear()

        where(block)

        return this
    }

    fun sqlArgs(): Query {
        val sql = StringBuilder()

        when {
            isExists -> sql.append("SELECT EXISTS(SELECT 1")
            else -> {
                sql.append("SELECT ")
                selectColumns.forEachIndexed { index, column ->
                    sql.append("${column.table().tableNameShort()}.${column.key()}")
                    if (index < selectColumns.size - 1) sql.append(", ")
                }
            }
        }

        sql.append(" FROM ")
        sql.append(table.tableName)

        joinClauses?.joinClauses()?.let {
            sql.append(it)
        }

        whereClauses?.whereClauses()?.let {
            sql.append(it.first)
            args.addAll(it.second)
        }

        if (!isExists) {
            orderBy?.let {
                sql.append(" ORDER BY ")
                sql.append(it.orderByClause())
            }

            limit?.let {
                sql.append(" LIMIT ?")
                args.add(it)
            }

            offset?.let {
                sql.append(" OFFSET ?")
                args.add(it)
            }
        }

        if (isExists) {
            sql.append(")")
        }

        return Query(sql.toString(), args)
    }
}

inline fun <reified R> Select.get(
    conn: Connection,
    mapper: (Row) -> R
): Result<R> =
    sqlArgs().execMapOne(conn, mapper)

inline fun <reified R> Select.getOrThrow(
    conn: Connection,
    mapper: (Row) -> R
): R =
    sqlArgs().execMapOne(conn, mapper).getOrThrow()

inline fun <reified R> Select.list(
    conn: Connection,
    mapper: (Row) -> R
): Result<List<R>> =
    sqlArgs().execMapList(conn, mapper)

inline fun <reified R> Select.listOrThrow(
    conn: Connection,
    mapper: (Row) -> R
): List<R> =
    sqlArgs().execMapList(conn, mapper).getOrThrow()