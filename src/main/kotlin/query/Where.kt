package query

import expr.BetweenExpr
import expr.ComparisonExpr
import expr.ComparisonOperator
import expr.ExistsExpr
import expr.GroupExpr
import expr.InListExpr
import expr.LogicalExpr
import expr.SqlFragment
import expr.WhereExpression
import schema.Column
import schema.Table

class Where(private val table: Table, private val blockOperator: String = "AND") {
    var expression: WhereExpression? = null
        private set
    private val expressions = mutableListOf<WhereExpression>()

    companion object {
        operator fun invoke(table: Table, init: Where.() -> Unit): Where {
            return Where(table).apply(init)
        }
    }

    private fun addExpression(expr: WhereExpression) {
        expressions.add(expr)
        expression = when (expressions.size) {
            1 -> expr
            else -> LogicalExpr(blockOperator, expressions)
        }
    }

    fun or(init: Where.() -> Unit) {
        val subWhere = Where(table, "OR").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(subExpr)
        }
    }

    fun and(init: Where.() -> Unit) {
        val subWhere = Where(table, "AND").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(subExpr)
        }
    }

    infix fun <T : Any> Column<T>.eq(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.EQUALS, it))
        }
    }

    infix fun <T : Any> Column<T>.neq(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.NOT_EQUALS, it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.lt(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN, it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.lte(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN_OR_EQUAL, it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.gt(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN, it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.gte(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, it))
        }
    }

    fun not(init: Where.() -> Unit) {
        val subWhere = Where(table).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("NOT", listOf(subExpr)))
        }
    }

    infix fun <T : Any> Column<T>.inList(values: List<T>?) {
        values?.takeIf { it.isNotEmpty() }?.let {
            addExpression(InListExpr(this, it, false))
        }
    }

    infix fun <T : Any> Column<T>.notInList(values: List<T>?) {
        values?.takeIf { it.isNotEmpty() }?.let {
            addExpression(InListExpr(this, it, true))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.between(range: Pair<T?, T?>?) {
        range?.let { (min, max) ->
            if (min != null && max != null) {
                addExpression(BetweenExpr(this, min to max))
            }
        }
    }

    fun <T : Any> Column<T>.isNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NULL, null))
    }

    fun <T : Any> Column<T>.isNotNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NOT_NULL, null))
    }

    infix fun Column<String>.like(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, it))
        }
    }

    infix fun Column<String>.likeContains(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$it%"))
        }
    }

    infix fun Column<String>.likeStarts(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "$it%"))
        }
    }

    infix fun Column<String>.likeEnds(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$it"))
        }
    }

    infix fun Column<String>.ilike(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.ILIKE, it))
        }
    }

    infix fun Column<String>.notLike(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, ComparisonOperator.NOT_LIKE, it))
        }
    }

    fun group(init: Where.() -> Unit) {
        val subWhere = Where(table, blockOperator).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun withOr(init: Where.() -> Unit) {
        val subWhere = Where(table, "OR").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun withAnd(init: Where.() -> Unit) {
        val subWhere = Where(table, "AND").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    // TODO - add correlated subqueries support
//    fun exists(block: Select.() -> Unit) {
//        val select = Select(table)
//        select.block()
//        val query = select.sqlArgs()
//        addExpression(ExistsExpr(query.sql, query.args, false))
//    }
//
//    fun notExists(block: Select.() -> Unit) {
//        val select = Select(table)
//        select.block()
//        val query = select.sqlArgs()
//        addExpression(ExistsExpr(query.sql, query.args, true))
//    }
}