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

class Join private constructor(private val columns: Array<out Column<*>>) {
    private var currentJoin: JoinExpression? = null

    companion object {
        operator fun invoke(vararg columns: Column<*>, block: Join.() -> Unit): JoinExpression? {
            return Join(columns).apply(block).build()
        }
    }

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
        currentJoin = TableJoinExpr(
            table = right.table(),
            columns = columns.toList(),
            type = type,
            condition = JoinCondition(left, right)
        )
    }

    fun build(): JoinExpression? {
        require(currentJoin != null) { "No join condition specified" }
        return currentJoin
    }
}