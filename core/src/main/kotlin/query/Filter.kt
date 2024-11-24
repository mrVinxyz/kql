package query

import Select
import schema.Column
import schema.Table

data class FilterResult(val ok: Boolean, val err: String? = null, val field: String? = null)

data class FilterCheck(
    val select: Select,
    val errorCase: Boolean = true,
    val msg: String,
    val field: String? = null
)

abstract class BaseFilter(val table: Table) {
    protected val filterFunctions = mutableListOf<() -> FilterCheck>()

    fun predicate(
        msg: String,
        selectBuilder: Select.() -> Unit
    ) {
        filterFunctions.add {
            val select = Select(table)
                .apply(selectBuilder)

            FilterCheck(select, true, msg)
        }
    }

    fun <T : Any> checkExists(
        attachedColumn: Column<T>,
        foreignTable: Table,
        whereClause: Where,
        msg: String = "Referenced record doesn't exist"
    ) {
        filterFunctions.add {
            val select = Select(foreignTable)
                .exists { whereClause }

            FilterCheck(select, false, msg, attachedColumn.key)
        }
    }

    fun filters(): List<FilterCheck> = filterFunctions.map { it() }
}

class InsertFilter(table: Table) : BaseFilter(table) {
    fun <T : Any> Column<T>.unique(
        value: T?,
        msg: String = "Record already exists"
    ) {
        value?.let {
            val column = this
            filterFunctions.add {
                val select = Select(table)
                    .exists { where { column eq it } }

                FilterCheck(select, true, msg, key)
            }
        }
    }

    fun <T : Any> Column<T>.exists(
        foreignTable: Table,
        foreignColumn: Column<T>,
        value: T?,
        msg: String = "Referenced record doesn't exist"
    ) {
        value?.let {
            val whereClause = Where(this.table) { foreignColumn eq value }
            checkExists(this, foreignTable, whereClause, msg)
        }
    }
}

class UpdateFilter(table: Table) : BaseFilter(table) {
    fun <T : Any> Column<T>.unique(
        pk: Any,
        value: T?,
        msg: String = "Record already exists"
    ) {
        val tablePk = table.primaryKey<Any>()
        requireNotNull(tablePk) { "Table $table does not have a primary key" }

        value?.let {
            val column = this
            filterFunctions.add {
                val select = Select(table)
                    .exists {
                        where {
                            column eq it
                            tablePk neq pk
                        }
                    }

                FilterCheck(select, true, msg, key)
            }
        }
    }

    fun <T : Any> Column<T>.exists(
        pk: Any,
        foreignTable: Table,
        foreignColumn: Column<T>,
        value: T?,
        msg: String = "Referenced record doesn't exist"
    ) {
        value?.let {

            val tablePk = table.primaryKey<Any>()
            requireNotNull(tablePk) { "Table $table does not have a primary key" }

            filterFunctions.add {
                val select = Select(foreignTable)
                    .exists {
                        where {
                            foreignColumn eq value
                            tablePk neq pk
                        }
                    }

                FilterCheck(select, false, msg, key)
            }
        }
    }
}