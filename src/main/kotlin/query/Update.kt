package query

import expr.SetExpr
import expr.SetExpr.Assignment
import expr.UpdateQueryExpression
import java.sql.Connection
import schema.Column
import schema.Table

sealed class UpdateResult {
    data object Success : UpdateResult()

    data class FilterFailure(
        val error: FilterResult? = null,
    ) : UpdateResult()

    data class DatabaseError(
        val exception: Exception
    ) : UpdateResult()
}

class Update(private val table: Table) {
    private var setExpr: SetExpr? = null
    private var whereClause: Where? = null
    private val assignments = mutableListOf<Assignment>()
    internal var filter: UpdateFilter? = null

    fun update(block: Update.() -> Unit): Update {
        block(this)
        setExpr = SetExpr(assignments)
        return this
    }

    fun updatePrimary(value: Any, init: Update.() -> Unit): Update {
        val primaryKey = table.primaryKey<Any>()
        where { primaryKey eq value }
        return update(init)
    }

    operator fun <T : Any> set(column: Column<*>, value: T?): Update {
        assignments.add(Assignment(column, value))
        return this
    }

    infix fun <T : Any> Column<T>.to(value: T?) {
        assignments.add(Assignment(this, value))
    }

    fun where(init: Where.() -> Unit): Update {
        val where = Where(table)
        init(where)
        whereClause = where
        return this
    }

    fun filter(block: UpdateFilter.() -> Unit): Update {
        filter = UpdateFilter(table)
        block(filter!!)
        return this
    }

    fun sqlArgs(): Query {
        require(assignments.isNotEmpty()) { "No columns specified for update" }

        val expression = UpdateQueryExpression(
            table = table,
            setExpr = SetExpr(assignments),
            condition = whereClause?.expression
        )

        val fragment = expression.accept(table.dialect, false)
        return Query(fragment.sql, fragment.args)
    }
}

fun Update.persist(conn: Connection): UpdateResult {
    filter?.let { ft ->
        val result = ft.execute(conn)
        if (!result.ok) {
            return UpdateResult.FilterFailure(result)
        }
    }

    return try {
        val result = sqlArgs().exec(conn)
        when {
            result.isSuccess -> UpdateResult.Success
            else -> UpdateResult.DatabaseError(result.exceptionOrNull() as Exception)
        }
    } catch (e: Exception) {
        UpdateResult.DatabaseError(e)
    }
}

fun Table.update(block: (Update) -> Unit): Update = Update(this).apply(block)

fun Table.updatePrimary(value: Any, block: (Update) -> Unit) = Update(this).updatePrimary(value, block)
