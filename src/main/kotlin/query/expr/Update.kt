package query.expr

import query.Query
import query.exec
import query.schema.Column
import query.schema.Table
import java.sql.Connection

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
    private val nullableColumnsArgsValues = mutableListOf<Pair<Column<*>, Any?>>()
    private var conditionClauses: Where? = null
    internal var filter: Filter? = null

    fun update(block: Update.() -> Unit): Update {
        block(this)
        return this
    }

    fun updatePrimary(value: Any, init: (Update) -> Unit): Update {
        val primaryKey = table.primaryKey<Any>()
        val updateWithCondition = update {
            where { primaryKey eq value }
            init(this)
        }

        return updateWithCondition
    }

    operator fun <T : Any> set(column: Column<*>, value: T?): Update {
        nullableColumnsArgsValues.add(column to value)
        return this
    }

    infix fun <T : Any> Column<T>.to(value: T?) {
        nullableColumnsArgsValues.add(Pair(this, value))
    }

    fun where(init: Where.() -> Unit): Update {
        val where = Where().apply(init)
        conditionClauses = where
        return this
    }

    fun filter(block: Filter.() -> Unit): Update {
        filter = Filter(table).apply(block)
        return this
    }

    fun sqlArgs(): Query {
        require(nullableColumnsArgsValues.isNotEmpty()) { "No columns specified for update" }
        val sql = StringBuilder()

        sql.append("UPDATE ")
        sql.append(table.tableName)
        sql.append(" SET ")

        val args = mutableListOf<Any?>()
        nullableColumnsArgsValues.forEachIndexed { index, (column, value) ->
            if (value == null) {
                sql.append("${column.key()} = COALESCE(?, ${column.key()})")
            } else {
                sql.append("${column.key()} = ?")
            }
            args.add(value)

            if (index < nullableColumnsArgsValues.size - 1) {
                sql.append(", ")
            }
        }

        conditionClauses?.whereClauses()?.let { (clause, clauseArgs) ->
            sql.append(clause)
            args.addAll(clauseArgs)
        }

        return Query(sql.toString(), args)
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