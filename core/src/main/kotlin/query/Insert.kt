package query

import expr.InsertValuesExpr
import schema.Column
import schema.Table

class Insert(private val table: Table) {
    private val assignments = mutableListOf<InsertValuesExpr.ColumnAssignment>()
    private var insertFilter: InsertFilter? = null

    fun insert(block: Insert.() -> Unit): Insert = apply(block)

    operator fun <T : Any> set(column: Column<T>, value: T?) {
        value?.let {
            assignments.add(InsertValuesExpr.ColumnAssignment(column, value))
        }
    }

    infix fun <T : Any> Column<T>.to(value: T?) {
        value?.let {
            assignments.add(InsertValuesExpr.ColumnAssignment(this, value))
        }
    }

    fun filter(block: InsertFilter.() -> Unit): Insert {
        insertFilter = InsertFilter(table)
        block(insertFilter!!)
        return this
    }

    fun filters() = insertFilter

    fun sqlArgs(): Query {
        require(assignments.isNotEmpty()) { "No columns specified for insert" }
        val fragment = InsertValuesExpr(assignments, table).accept(table.dialect, false)
        return Query(fragment.sql, fragment.args)
    }
}

sealed class InsertResult<T> {
    data class Success<T>(
        val generatedId: T? = null
    ) : InsertResult<T>()

    data class FilterFailure<T>(
        val error: FilterResult? = null,
    ) : InsertResult<T>()

    data class DatabaseError<T>(
        val exception: Exception
    ) : InsertResult<T>()

    fun isSuccess() = this is Success
    fun isFilterFailure() = this is FilterFailure
    fun isDatabaseError() = this is DatabaseError

    fun getGeneratedIdOrNull() = (this as? Success)?.generatedId
    fun getErrorOrNull() = (this as? FilterFailure)?.error
    fun getExceptionOrNull() = (this as? DatabaseError)?.exception
}

fun Table.insert(init: Insert.() -> Unit): Insert = Insert(this).apply(init)