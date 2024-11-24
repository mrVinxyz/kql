package expr

import schema.Column
import schema.Table

data class InsertValuesExpr(
    val assignments: List<ColumnAssignment>,
    val table: Table = assignments.first().column.table
) : SqlExpression {
    data class ColumnAssignment(
        val column: Column<*>,
        val value: Any?
    )

    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderInsert(this, useTableAlias)
}

data class SelectQueryExpr(
    val table: Table,
    val selectExpr: SelectClauseExpression,
    val joinExpressions: List<JoinExpression> = emptyList(),
    val condition: WhereExpression? = null,
    val orderByExpr: OrderByExpr? = null,
    val limitExpr: LimitExpr? = null,
    val offsetExpr: OffsetExpr? = null,
) : SqlExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderSelect(this, useTableAlias)
}

sealed interface SelectClauseExpression : SqlExpression

class SelectOneExpr : SelectClauseExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment {
        return renderer.renderSelectOne(this, useTableAlias)
    }
}

data class ColumnsSelectExpr(
    val columns: List<Column<*>>
) : SelectClauseExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderColumnsSelect(this, useTableAlias)
}

class CountSelectExpr : SelectClauseExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderCountSelect(this, useTableAlias)
}

class ExistsSelectExpr(
    val subquery: () -> SqlFragment
) : SelectClauseExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment {
        return subquery()
    }
}

sealed interface WhereExpression : SqlExpression

data class ComparisonExpr<T : Any>(
    val column: Column<T>,
    val operator: ComparisonOperator,
    val value: T?
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderComparison(this, useTableAlias)
}

data class LogicalExpr(
    val operator: String,
    val expressions: List<WhereExpression>
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderLogical(this, useTableAlias)
}

data class InListExpr<T : Any>(
    val column: Column<T>,
    val values: List<T>,
    val isNegated: Boolean = false
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderInList(this, useTableAlias)
}

data class BetweenExpr<T : Comparable<T>>(
    val column: Column<T>,
    val range: Pair<T, T>
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderBetween(this, useTableAlias)
}

data class ExistsExpr<T : Any>(
    val subquery: String,
    val args: List<T?>,
    val isNegated: Boolean = false
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderExists(this, useTableAlias)
}

data class GroupExpr(
    val expression: WhereExpression
) : WhereExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderGroup(this, useTableAlias)
}

enum class ComparisonOperator(val sql: String) {
    EQUALS("="),
    NOT_EQUALS("<>"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    LIKE("LIKE"),
    ILIKE("ILIKE"),
    NOT_LIKE("NOT LIKE")
}

sealed interface UpdateSetExpression : SqlExpression

data class UpdateQueryExpression(
    val table: Table,
    val setExpr: UpdateSetExpression,
    val condition: WhereExpression? = null
) : SqlExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderUpdate(this, useTableAlias)
}

data class SetExpr(
    val assignments: List<Assignment>
) : UpdateSetExpression {
    data class Assignment(
        val column: Column<*>,
        val value: Any?
    )

    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderSet(this, useTableAlias)
}

sealed interface JoinExpression : SqlExpression {
    val context: JoinContext?
}

interface JoinContext {
    val tableAliases: MutableMap<Table, String>

    fun addTable(table: Table) {
        if (!tableAliases.containsKey(table)) {
            tableAliases[table] = table.alias()
        }
    }
    fun tableAlias(table: Table): String?
}

data class TableJoinExpr(
    val table: Table,
    val columns: List<Column<*>>,
    val type: JoinType,
    val condition: JoinCondition,
    override val context: JoinContext?
) : JoinExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderJoin(this, useTableAlias)
}

data class JoinCondition(
    val leftColumn: Column<*>,
    val rightColumn: Column<*>
)

enum class JoinType(val sql: String) {
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    INNER("INNER"),
    OUTER("OUTER"),
    FULL("FULL")
}

data class OrderByExpr(
    val orders: List<OrderByColumn>
) : SqlExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderOrderBy(this, useTableAlias)
}

data class OrderByColumn(
    val column: Column<*>,
    val direction: OrderDirection
)

enum class OrderDirection(val sql: String) {
    ASC("ASC"),
    DESC("DESC")
}

sealed interface LimitOffsetExpression : SqlExpression

data class LimitExpr(
    val limit: Int
) : LimitOffsetExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderLimit(this, useTableAlias)
}

data class OffsetExpr(
    val offset: Int
) : LimitOffsetExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderOffset(this, useTableAlias)
}

data class DeleteQueryExpression(
    val table: Table,
    val condition: WhereExpression? = null
) : SqlExpression {
    override fun accept(renderer: SqlRenderer, useTableAlias: Boolean): SqlFragment =
        renderer.renderDelete(this, useTableAlias)
}