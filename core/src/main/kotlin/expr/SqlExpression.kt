package expr

sealed interface SqlExpression {
    fun accept(renderer: SqlRenderer, useTableAlias: Boolean = false): SqlFragment
}

interface SqlRenderer {
    fun renderInsert(expr: InsertValuesExpr, useTableAlias: Boolean): SqlFragment

    fun renderSelect(expr: SelectQueryExpr, useTableAlias: Boolean): SqlFragment

    fun renderWhere(expr: WhereExpression, useTableAlias: Boolean): SqlFragment
    fun renderComparison(expr: ComparisonExpr<*>, useTableAlias: Boolean): SqlFragment
    fun renderLogical(expr: LogicalExpr, useTableAlias: Boolean): SqlFragment
    fun renderInList(expr: InListExpr<*>, useTableAlias: Boolean): SqlFragment
    fun renderBetween(expr: BetweenExpr<*>, useTableAlias: Boolean): SqlFragment
    fun renderExists(expr: ExistsExpr<*>, useTableAlias: Boolean): SqlFragment
    fun renderGroup(expr: GroupExpr, useTableAlias: Boolean): SqlFragment

    fun renderSelectClause(expr: SelectClauseExpression, useTableAlias: Boolean): SqlFragment
    fun renderSelectOne(expr: SelectOneExpr, useTableAlias: Boolean): SqlFragment
    fun renderColumnsSelect(expr: ColumnsSelectExpr, useTableAlias: Boolean): SqlFragment
    fun renderCountSelect(expr: CountSelectExpr, useTableAlias: Boolean): SqlFragment
    fun renderExistsSelect(expr: ExistsSelectExpr, useTableAlias: Boolean): SqlFragment

    fun renderJoin(expr: TableJoinExpr, useTableAlias: Boolean): SqlFragment

    fun renderOrderBy(expr: OrderByExpr, useTableAlias: Boolean): SqlFragment
    fun renderLimit(expr: LimitExpr, useTableAlias: Boolean): SqlFragment
    fun renderOffset(expr: OffsetExpr, useTableAlias: Boolean): SqlFragment

    fun renderUpdate(expr: UpdateQueryExpression, useTableAlias: Boolean): SqlFragment
    fun renderSet(expr: SetExpr, useTableAlias: Boolean): SqlFragment

    fun renderDelete(expr: DeleteQueryExpression, useTableAlias: Boolean): SqlFragment
}

data class SqlFragment(
    val sql: String,
    val args: List<Any?> = emptyList()
)
