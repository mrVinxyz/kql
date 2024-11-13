package query

import schema.Column
import schema.Table
import java.sql.Connection

data class FilterResult(val ok: Boolean, val err: String? = null, val field: String? = null)

abstract class BaseFilter(val table: Table) {
    protected val filterFunctions = mutableListOf<(Connection) -> FilterResult>()

    protected fun checkExists(
        conn: Connection,
        select: Select,
        errorCase: Boolean = true,
        msg: String,
        field: String? = null
    ): FilterResult {
        val exists = select
            .sqlArgs()
            .execMapOne(conn) { it.get<Int>(1) }
            .getOrThrow()

        return if (exists == (if (errorCase) 1 else 0)) {
            FilterResult(false, msg, field)
        } else {
            FilterResult(true)
        }
    }

    fun predicate(
        msg: String,
        selectBuilder: Select.() -> Unit
    ) {
        filterFunctions.add { conn ->
            val select = Select(table)
                .apply(selectBuilder)

            checkExists(conn, select, true, msg)
        }
    }

    fun execute(conn: Connection): FilterResult {
        filterFunctions.forEach { filterFn ->
            val result = filterFn(conn)
            if (!result.ok) {
                return result
            }
        }
        return FilterResult(true)
    }
}

class InsertFilter(table: Table) : BaseFilter(table) {
    fun <T : Any> Column<T>.unique(
        value: T?,
        msg: String = "Record already exists"
    ) {
        value?.let {
            val column = this
            filterFunctions.add { conn ->
                val select = Select(table)
                    .exists { column eq it }

                checkExists(conn, select, true, msg, key())
            }
        }
    }

    fun <T : Any> uniqueComposite(
        vararg columns: Pair<Column<T>, T?>,
        msg: String = "Composite key already exists"
    ) {
        filterFunctions.add { conn ->
            val select = Select(table)
                .exists {
                    columns.forEach { (column, value) ->
                        column eq value
                    }
                }

            checkExists(
                conn,
                select,
                true,
                msg,
                columns.joinToString(",") { it.first.key() }
            )
        }
    }

    fun <T : Any> Column<T>.exists(
        foreignTable: Table,
        foreignColumn: Column<T>,
        value: T?,
        msg: String = "Referenced record doesn't exist"
    ) {
        filterFunctions.add { conn ->
            val select = Select(foreignTable)
                .exists { foreignColumn eq value }

            checkExists(conn, select, false, msg, key())
        }
    }
}

class UpdateFilter(table: Table) : BaseFilter(table) {
    fun <T : Any> Column<T>.unique(
        pk: Any,
        value: T?,
        msg: String = "Record already exists"
    ) {
        value?.let {
            val column = this
            filterFunctions.add { conn ->
                val select = Select(table)
                    .exists {
                        column eq it
                        table.primaryKey<Any>() neq pk
                    }

                checkExists(conn, select, true, msg, key())
            }
        }
    }

    fun <T : Any> uniqueComposite(
        pk: Any,
        vararg columns: Pair<Column<T>, T?>,
        msg: String = "Composite key already exists"
    ) {
        filterFunctions.add { conn ->
            val select = Select(table)
                .exists {
                    columns.forEach { (column, value) ->
                        column eq value
                        table.primaryKey<Any>() neq pk
                    }
                }

            checkExists(
                conn,
                select,
                true,
                msg,
                columns.joinToString(",") { it.first.key() }
            )
        }
    }

    fun <T : Any> Column<T>.exists(
        pk: Any,
        foreignTable: Table,
        foreignColumn: Column<T>,
        value: T?,
        msg: String = "Referenced record doesn't exist"
    ) {
        filterFunctions.add { conn ->
            val select = Select(foreignTable)
                .exists {
                    foreignColumn eq value
                    table.primaryKey<Any>() neq pk
                }

            checkExists(conn, select, false, msg, key())
        }
    }
}