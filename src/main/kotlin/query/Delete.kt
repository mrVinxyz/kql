package query

import expr.DeleteQueryExpression
import java.sql.Connection
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

fun Delete.persist(conn: Connection): Result<Unit> = sqlArgs().exec(conn)

fun Delete.persistOrThrow(conn: Connection): Unit = sqlArgs().exec(conn).getOrThrow()

fun Table.deleteWhere(init: Where.() -> Unit): Delete = Delete(this).deleteWhere(init)

fun Table.deletePrimary(value: Any): Delete = Delete(this).deletePrimary(value)