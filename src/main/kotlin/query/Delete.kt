package query

import expr.DeleteExpr
import java.sql.Connection
import schema.Table

class Delete(private val table: Table) {
    private var whereClause: Where? = null

    fun deleteWhere(init: Where.() -> Unit): Delete {
        val where = Where()
        init(where)
        whereClause = where
        return this
    }

    fun <T : Any> deletePrimary(value: T): Delete {
        val primaryKey = table.primaryKey<T>()
        return deleteWhere { primaryKey eq value }
    }

    fun sqlArgs(): Query {
        val sql = StringBuilder().apply {
            val deleteFragment = DeleteExpr(table).sqlArgs(false)
            append(deleteFragment.sql)

            whereClause?.buildWhere(false)?.let { fragment ->
                append(" WHERE ")
                append(fragment.sql)
            }
        }

        val args = mutableListOf<Any?>()
        whereClause?.buildWhere(false)?.let { args.addAll(it.args) }

        return Query(sql.toString(), args)
    }
}

fun Delete.persist(conn: Connection): Result<Unit> = sqlArgs().exec(conn)

fun Delete.persistOrThrow(conn: Connection): Unit = sqlArgs().exec(conn).getOrThrow()

fun Table.deleteWhere(init: Where.() -> Unit): Delete = Delete(this).deleteWhere(init)

fun Table.deletePrimary(value: Any): Delete = Delete(this).deletePrimary(value)