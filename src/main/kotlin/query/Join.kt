package query

import expr.JoinCondition
import expr.JoinContext
import expr.JoinExpression
import expr.JoinType
import expr.JoinType.FULL
import expr.JoinType.INNER
import expr.JoinType.LEFT
import expr.JoinType.OUTER
import expr.JoinType.RIGHT
import expr.TableJoinExpr
import schema.Column
import schema.Table

class TableJoinContext : JoinContext {
    override val tableAliases = mutableMapOf<Table, String>()

    override fun addTable(table: Table) {
        if (!tableAliases.containsKey(table)) {
            tableAliases[table] = table.alias()
        }
    }

    override fun tableAlias(table: Table): String? {
        return tableAliases[table]
    }
}

class Join private constructor(
    private val mainTable: Table,
    private val context: JoinContext? = null,
    private val columns: Array<out Column<*>>
) {
    var currentJoin: JoinExpression? = null

    companion object {
        operator fun invoke(
            mainTable: Table,
            context: JoinContext? = null,
            vararg columns: Column<*>,
            block: Join.() -> Unit
        ): Join? {
            return Join(mainTable, context ?: TableJoinContext(), columns).apply(block)
        }
    }

    private fun createJoin(left: Column<*>, right: Column<*>, type: JoinType) {
        val leftTable = left.table()
        val rightTable = right.table()

        val tableToJoin = when {
            leftTable == mainTable -> rightTable
            rightTable == mainTable -> leftTable
            context?.tableAlias(rightTable) == null -> rightTable
            else -> leftTable
        }

        context?.addTable(tableToJoin)

        currentJoin = TableJoinExpr(
            table = tableToJoin,
            columns = columns.toList(),
            type = type,
            condition = JoinCondition(left, right),
            context = context
        )
    }

    infix fun <T : Any> Column<T>.left(other: Column<*>) =
        createJoin(this, other, LEFT)

    infix fun <T : Any> Column<T>.inner(other: Column<*>) =
        createJoin(this, other, INNER)

    infix fun <T : Any> Column<T>.right(other: Column<*>) =
        createJoin(this, other, RIGHT)

    infix fun <T : Any> Column<T>.outer(other: Column<*>) =
        createJoin(this, other, OUTER)

    infix fun <T : Any> Column<T>.full(other: Column<*>) =
        createJoin(this, other, FULL)
}