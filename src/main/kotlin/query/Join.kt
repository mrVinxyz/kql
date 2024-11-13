package query

import expr.JoinCondition
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

class Join(private val mainTable: Table, private val columns: List<Column<*>>) {
    private var currentJoin: JoinExpression? = null

    operator fun invoke(block: Join.() -> Unit): JoinExpression? {
        block()
        return build()
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

    private fun createJoin(left: Column<*>, right: Column<*>, type: JoinType) {
        val (leftCol, rightCol) = if (left.table() == mainTable) {
            left to right
        } else {
            right to left
        }

        currentJoin = TableJoinExpr(
            table = rightCol.table(),
            columns = columns,
            type = type,
            condition = JoinCondition(leftCol, rightCol)
        )
    }

    fun build(): JoinExpression? {
        require(currentJoin != null) { "No join condition specified" }
        return currentJoin
    }
}