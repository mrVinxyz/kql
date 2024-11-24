package query

import expr.DeleteQueryExpression
import schema.Table

class Delete(private val table: Table) {
    private var whereClause: Where? = null

    fun deleteWhere(init: Where.() -> Unit): Delete {
        val where = Where(table)
        init(where)
        whereClause = where
        return this
    }

    fun <T : Any> deletePrimary(value: T): Delete {
        val primaryKey = table.primaryKey<T>()
        requireNotNull(primaryKey) { "Table $table does not have a primary key" }

        return deleteWhere { primaryKey eq value }
    }

    fun sqlArgs(): Query {
        val deleteExpr = DeleteQueryExpression(
            table = table,
            condition = whereClause?.expression
        )
        val fragment = deleteExpr.accept(table.dialect, false)
        return Query(fragment.sql, fragment.args)
    }
}

fun Table.deleteWhere(init: Where.() -> Unit): Delete = Delete(this).deleteWhere(init)

fun Table.deletePrimary(value: Any): Delete = Delete(this).deletePrimary(value)