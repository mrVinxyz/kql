package query.expr

import query.Query
import query.exec
import query.schema.Table
import java.sql.Connection

class Delete(private val table: Table) {
    private var condition: Where? = null
    private val args = mutableListOf<Any>()

    fun deleteWhere(init: Where.() -> Unit): Delete {
        val where = Where()
        init(where)

        condition = where

        return this
    }

    fun <T : Any> deletePrimary(value: T): Delete {
        val primaryKey = table.primaryKey<T>()
        return deleteWhere { primaryKey eq value }
    }

    fun sqlArgs(): Query {
        val sql = StringBuilder()

        sql.append("DELETE FROM ")
        sql.append(table.tableName)

        condition?.whereClauses()?.let {
            sql.append(it.first)
            args.addAll(it.second)
        }

        return Query(sql.toString(), args)
    }
}

fun Delete.persist(conn: Connection): Result<Unit> = sqlArgs().exec(conn)

fun Delete.persistOrThrow(conn: Connection): Unit = sqlArgs().exec(conn).getOrThrow()