package query

import java.sql.Connection
import expr.InsertValuesExpr
import schema.Column
import schema.Table

class Insert(private val table: Table) {
    private val assignments = mutableListOf<InsertValuesExpr.ColumnAssignment>()
    internal var filter: InsertFilter? = null

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
        filter = InsertFilter(table)
        block(filter!!)
        return this
    }

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

fun <T> Insert.persist(conn: Connection): InsertResult<T> {
    filter?.let { ft ->
        val result = ft.execute(conn)
        if (!result.ok) {
            return InsertResult.FilterFailure(result)
        }
    }

    return try {
        val result = sqlArgs().execReturnKey(conn)
        when {
            result.isSuccess -> {
                @Suppress("UNCHECKED_CAST")
                val id = result.getOrThrow() as? T
                InsertResult.Success(id)
            }

            else -> InsertResult.DatabaseError(result.exceptionOrNull() as java.lang.Exception)
        }
    } catch (e: Exception) {
        InsertResult.DatabaseError(e)
    }
}

fun Table.insert(init: Insert.() -> Unit): Insert = Insert(this).apply(init)