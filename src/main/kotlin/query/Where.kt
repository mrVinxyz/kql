package query

import expr.BetweenExpr
import expr.ComparisonExpr
import expr.ExistsExpr
import expr.GroupExpr
import expr.InListExpr
import expr.LogicalExpr
import expr.SqlFragment
import expr.WhereExpression
import schema.Column

class Where(private val blockOperator: String = "AND") {
    private var expression: WhereExpression? = null
    private val expressions = mutableListOf<WhereExpression>()

    private fun addExpression(expr: WhereExpression) {
        expressions.add(expr)
        expression = when (expressions.size) {
            1 -> expr
            else -> LogicalExpr(blockOperator, expressions)
        }
    }

    fun or(init: Where.() -> Unit) {
        val subWhere = Where("OR").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("OR", listOf(subExpr)))
        }
    }

    fun and(init: Where.() -> Unit) {
        val subWhere = Where("AND").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("AND", listOf(subExpr)))
        }
    }

    fun not(init: Where.() -> Unit) {
        val subWhere = Where("NOT").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("NOT", listOf(subExpr)))
        }
    }

    infix fun <T : Any> Column<T>.eq(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, "=", it))
        }
    }

    infix fun <T : Any> Column<T>.neq(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, "<>", it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.lt(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, "<", it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.lte(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, "<=", it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.gt(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ">", it))
        }
    }

    infix fun <T : Comparable<T>> Column<T>.gte(value: T?) {
        value?.let {
            addExpression(ComparisonExpr(this, ">=", it))
        }
    }

    infix fun <T : Any> Column<T>.inList(values: List<T>?) {
        values?.takeIf { it.isNotEmpty() }?.let {
            addExpression(InListExpr(this, it))
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
        addExpression(ComparisonExpr(this, "IS NULL", null))
    }

    fun <T : Any> Column<T>.isNotNull() {
        addExpression(ComparisonExpr(this, "IS NOT NULL", null))
    }

    infix fun Column<String>.like(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "LIKE", it))
        }
    }

    infix fun Column<String>.likeContains(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "LIKE", "%$it%"))
        }
    }

    infix fun Column<String>.likeStarts(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "LIKE", "$it%"))
        }
    }

    infix fun Column<String>.likeEnds(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "LIKE", "%$it"))
        }
    }

    infix fun Column<String>.ilike(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "ILIKE", it))
        }
    }

    infix fun Column<String>.notLike(pattern: String?) {
        pattern?.let {
            addExpression(ComparisonExpr(this, "NOT LIKE", it))
        }
    }

    fun group(init: Where.() -> Unit) {
        val subWhere = Where(blockOperator).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun exists(subquery: String, args: List<Any> = emptyList()) {
        addExpression(ExistsExpr(subquery, args))
    }

    fun notExists(subquery: String, args: List<Any> = emptyList()) {
        addExpression(ExistsExpr(subquery, args, true))
    }

    fun buildWhere(useTableAlias: Boolean = false): SqlFragment? {
        return expression?.sqlArgs(useTableAlias)?.let { fragment ->
            SqlFragment(fragment.sql, fragment.args)
        }
    }
}