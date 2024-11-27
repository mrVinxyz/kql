package query

import expr.BetweenExpr
import expr.ComparisonExpr
import expr.ComparisonOperator
import expr.GroupExpr
import expr.InListExpr
import expr.LogicalExpr
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

    fun nullable(init: NullableWhere.() -> Unit) {
        val nullableWhere = NullableWhere(table, blockOperator).apply(init)
        nullableWhere.expression?.let { addExpression(it) }
    }

    fun cond(predicate: Boolean, block: Where.() -> Unit) {
        if (predicate) block()
    }

    fun and(init: Where.() -> Unit) {
        val subWhere = Where(table, "AND").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun or(init: Where.() -> Unit) {
        val subWhere = Where(table, "OR").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun group(init: Where.() -> Unit) {
        val subWhere = Where(table, blockOperator).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun not(init: Where.() -> Unit) {
        val subWhere = Where(table).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("NOT", listOf(subExpr)))
        }
    }

    // Non-null comparison operators
    infix fun <T : Any> Column<T>.eq(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.EQUALS, value))
    }

    infix fun <T : Any> Column<T>.neq(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.NOT_EQUALS, value))
    }

    infix fun <T : Comparable<T>> Column<T>.lt(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN, value))
    }

    infix fun <T : Comparable<T>> Column<T>.lte(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN_OR_EQUAL, value))
    }

    infix fun <T : Comparable<T>> Column<T>.gt(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN, value))
    }

    infix fun <T : Comparable<T>> Column<T>.gte(value: T) {
        addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, value))
    }

    // Non-null list operations
    infix fun <T : Any> Column<T>.inList(values: List<T>) {
        require(values.isNotEmpty()) { "Cannot create IN expression with empty list" }
        addExpression(InListExpr(this, values, false))
    }

    infix fun <T : Any> Column<T>.notInList(values: List<T>) {
        require(values.isNotEmpty()) { "Cannot create NOT IN expression with empty list" }
        addExpression(InListExpr(this, values, true))
    }

    // Non-null between operation
    infix fun <T : Comparable<T>> Column<T>.between(range: Pair<T, T>) {
        addExpression(BetweenExpr(this, range))
    }

    // Explicit null checks
    fun <T : Any> Column<T>.isNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NULL, null))
    }

    fun <T : Any> Column<T>.isNotNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NOT_NULL, null))
    }

    // Non-null string operations
    infix fun Column<String>.like(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, pattern))
    }

    infix fun Column<String>.likeContains(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$pattern%"))
    }

    infix fun Column<String>.likeStarts(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "$pattern%"))
    }

    infix fun Column<String>.likeEnds(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$pattern"))
    }

    infix fun Column<String>.ilike(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.ILIKE, pattern))
    }

    infix fun Column<String>.notLike(pattern: String) {
        addExpression(ComparisonExpr(this, ComparisonOperator.NOT_LIKE, pattern))
    }
}

class NullableWhere(
    private val table: Table,
    private val blockOperator: String,
) {
    var expression: WhereExpression? = null
        private set
    private val expressions = mutableListOf<WhereExpression>()

    private fun addExpression(expr: WhereExpression) {
        expressions.add(expr)
        expression = when (expressions.size) {
            1 -> expr
            else -> LogicalExpr(blockOperator, expressions)
        }
    }

    // Comparison operators
    infix fun <T : Any> Column<T>.eq(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.EQUALS, it)) }
    }

    infix fun <T : Any> Column<T>.neq(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.NOT_EQUALS, it)) }
    }

    infix fun <T : Comparable<T>> Column<T>.lt(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN, it)) }
    }

    infix fun <T : Comparable<T>> Column<T>.lte(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LESS_THAN_OR_EQUAL, it)) }
    }

    infix fun <T : Comparable<T>> Column<T>.gt(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN, it)) }
    }

    infix fun <T : Comparable<T>> Column<T>.gte(value: T?) {
        value?.let { addExpression(ComparisonExpr(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, it)) }
    }

    // List operations
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
            when {
                min != null && max != null -> addExpression(BetweenExpr(this, min to max))
                min != null -> gte(min)
                max != null -> lte(max)
            }
        }
    }

    // Explicit null checks remain unchanged
    fun <T : Any> Column<T>.isNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NULL, null))
    }

    fun <T : Any> Column<T>.isNotNull() {
        addExpression(ComparisonExpr(this, ComparisonOperator.IS_NOT_NULL, null))
    }

    // String operations
    infix fun Column<String>.like(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, it)) }
    }

    infix fun Column<String>.likeContains(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$it%")) }
    }

    infix fun Column<String>.likeStarts(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "$it%")) }
    }

    infix fun Column<String>.likeEnds(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.LIKE, "%$it")) }
    }

    infix fun Column<String>.ilike(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.ILIKE, it)) }
    }

    infix fun Column<String>.notLike(pattern: String?) {
        pattern?.let { addExpression(ComparisonExpr(this, ComparisonOperator.NOT_LIKE, it)) }
    }


    // Logical grouping
    fun group(init: NullableWhere.() -> Unit) {
        val subWhere = NullableWhere(table, blockOperator).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun and(init: NullableWhere.() -> Unit) {
        val subWhere = NullableWhere(table, "AND").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun or(init: NullableWhere.() -> Unit) {
        val subWhere = NullableWhere(table, "OR").apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(GroupExpr(subExpr))
        }
    }

    fun not(init: NullableWhere.() -> Unit) {
        val subWhere = NullableWhere(table, blockOperator).apply(init)
        subWhere.expression?.let { subExpr ->
            addExpression(LogicalExpr("NOT", listOf(subExpr)))
        }
    }
}

// TODO - add support for correlated subqueries
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