package dialect

import expr.BetweenExpr
import expr.ColumnsSelectExpr
import expr.ComparisonExpr
import expr.CountSelectExpr
import expr.DeleteQueryExpression
import expr.ExistsExpr
import expr.ExistsSelectExpr
import expr.GroupExpr
import expr.InListExpr
import expr.InsertValuesExpr
import expr.LimitExpr
import expr.LogicalExpr
import expr.OffsetExpr
import expr.OrderByExpr
import expr.SelectClauseExpression
import expr.SelectOneExpr
import expr.SelectQueryExpr
import expr.SetExpr
import expr.SqlFragment
import expr.SqlRenderer
import expr.TableJoinExpr
import expr.UpdateQueryExpression
import expr.WhereExpression
import schema.Column
import schema.Table

class SQLiteDialect : SqlRenderer {
    private fun renderColumnRef(column: Column<*>, useTableAlias: Boolean): String {
        return if (useTableAlias) {
            "${column.table().alias()}.${column.key()}"
        } else {
            column.key()
        }
    }

    private fun renderTableRef(table: Table, useTableAlias: Boolean, customAlias: String? = null): String {
        return if (useTableAlias) {
            "${table.tableName} ${customAlias ?: table.alias()}"
        } else {
            table.tableName
        }
    }

    override fun renderInsert(expr: InsertValuesExpr, useTableAlias: Boolean): SqlFragment {
        val columnsSql = expr.assignments.joinToString(", ") { it.column.key() }
        val placeholders = expr.assignments.joinToString(", ") { "?" }

        val sql = StringBuilder().apply {
            append("INSERT INTO ")
            append(expr.table.tableName)
            append(" (")
            append(columnsSql)
            append(") VALUES (")
            append(placeholders)
            append(")")
        }

        return SqlFragment(sql.toString(), expr.assignments.map { it.value })
    }

    override fun renderSelect(expr: SelectQueryExpr, useTableAlias: Boolean): SqlFragment {
        if (expr.selectExpr is ExistsSelectExpr) {
            val subqueryFragment = expr.selectExpr.subquery()
            return SqlFragment(
                "SELECT EXISTS (${subqueryFragment.sql})",
                subqueryFragment.args
            )
        }

        val args = mutableListOf<Any?>()
        val sql = StringBuilder().apply {
            val selectFragment = expr.selectExpr.accept(this@SQLiteDialect, useTableAlias)
            append(selectFragment.sql)
            append(" FROM ")
            append(renderTableRef(expr.table, useTableAlias))

            expr.joinExpressions.forEach { join ->
                append(" ")
                val joinFragment = join.accept(this@SQLiteDialect, useTableAlias)
                append(joinFragment.sql)
                args.addAll(joinFragment.args)
            }

            expr.condition?.let { where ->
                append(" WHERE ")
                val whereFragment = where.accept(this@SQLiteDialect, useTableAlias)
                append(whereFragment.sql)
                args.addAll(whereFragment.args)
            }

            expr.orderByExpr?.let { orderBy ->
                append(" ORDER BY ")
                val orderByFragment = orderBy.accept(this@SQLiteDialect, useTableAlias)
                append(orderByFragment.sql)
            }

            expr.limitExpr?.let { limit ->
                append(" ")
                val limitFragment = limit.accept(this@SQLiteDialect, useTableAlias)
                append(limitFragment.sql)
                args.addAll(limitFragment.args)
            }

            expr.offsetExpr?.let { offset ->
                append(" ")
                val offsetFragment = offset.accept(this@SQLiteDialect, useTableAlias)
                append(offsetFragment.sql)
                args.addAll(offsetFragment.args)
            }
        }

        return SqlFragment(sql.toString(), args)
    }

    override fun renderSelectOne(expr: SelectOneExpr, useTableAlias: Boolean): SqlFragment {
        return SqlFragment("SELECT 1")
    }

    override fun renderWhere(expr: WhereExpression, useTableAlias: Boolean): SqlFragment =
        expr.accept(this, useTableAlias)

    override fun renderComparison(expr: ComparisonExpr<*>, useTableAlias: Boolean): SqlFragment {
        val columnRef = renderColumnRef(expr.column, useTableAlias)
        return when (expr.value) {
            null -> SqlFragment("$columnRef ${expr.operator.sql}")
            else -> SqlFragment("$columnRef ${expr.operator.sql} ?", listOf(expr.value))
        }
    }

    override fun renderLogical(expr: LogicalExpr, useTableAlias: Boolean): SqlFragment {
        val fragments = expr.expressions.map { it.accept(this, useTableAlias) }
        val sql = StringBuilder()
        val args = mutableListOf<Any?>()

        when (expr.operator) {
            "NOT" -> {
                sql.append("NOT (")
                sql.append(fragments[0].sql)
                sql.append(")")
                args.addAll(fragments[0].args)
            }

            else -> {
                fragments.forEachIndexed { index, fragment ->
                    if (index > 0) sql.append(" ${expr.operator} ")
                    sql.append(fragment.sql)
                    args.addAll(fragment.args)
                }
            }
        }
        return SqlFragment(sql.toString(), args)
    }

    override fun renderInList(expr: InListExpr<*>, useTableAlias: Boolean): SqlFragment {
        val columnRef = renderColumnRef(expr.column, useTableAlias)
        val operator = if (expr.isNegated) "NOT IN" else "IN"
        val placeholders = expr.values.joinToString(", ") { "?" }
        return SqlFragment("$columnRef $operator ($placeholders)", expr.values)
    }

    override fun renderBetween(expr: BetweenExpr<*>, useTableAlias: Boolean): SqlFragment {
        val columnRef = renderColumnRef(expr.column, useTableAlias)
        return SqlFragment(
            "$columnRef BETWEEN ? AND ?",
            listOf(expr.range.first, expr.range.second)
        )
    }

    override fun renderExists(expr: ExistsExpr<*>, useTableAlias: Boolean): SqlFragment {
        val operator = if (expr.isNegated) "NOT EXISTS" else "EXISTS"
        return SqlFragment("$operator (${expr.subquery})", expr.args)
    }

    override fun renderGroup(expr: GroupExpr, useTableAlias: Boolean): SqlFragment {
        val fragment = expr.expression.accept(this, useTableAlias)
        return SqlFragment("(${fragment.sql})", fragment.args)
    }

    override fun renderSelectClause(expr: SelectClauseExpression, useTableAlias: Boolean): SqlFragment {
        return when (expr) {
            is SelectOneExpr -> renderSelectOne(expr, useTableAlias)
            is ColumnsSelectExpr -> renderColumnsSelect(expr, useTableAlias)
            is CountSelectExpr -> renderCountSelect(expr, useTableAlias)
            is ExistsSelectExpr -> renderExistsSelect(expr, useTableAlias)
        }
    }

    override fun renderColumnsSelect(expr: ColumnsSelectExpr, useTableAlias: Boolean): SqlFragment =
        SqlFragment("SELECT " + expr.columns.joinToString(", ") { renderColumnRef(it, useTableAlias) })

    override fun renderCountSelect(expr: CountSelectExpr, useTableAlias: Boolean): SqlFragment =
        SqlFragment("SELECT COUNT(*)")

    override fun renderExistsSelect(expr: ExistsSelectExpr, useTableAlias: Boolean): SqlFragment =
        SqlFragment("SELECT EXISTS (SELECT 1")

    override fun renderJoin(expr: TableJoinExpr, useTableAlias: Boolean): SqlFragment {
        val leftRef = renderColumnRef(expr.condition.leftColumn, useTableAlias)
        val rightRef = renderColumnRef(expr.condition.rightColumn, useTableAlias)
        val tableRef = renderTableRef(expr.table, useTableAlias, expr.context?.tableAlias(expr.table))
        return SqlFragment("${expr.type.sql} JOIN $tableRef ON $leftRef = $rightRef")
    }

    override fun renderOrderBy(expr: OrderByExpr, useTableAlias: Boolean): SqlFragment {
        val orderSql = expr.orders.joinToString(", ") { order ->
            "${renderColumnRef(order.column, useTableAlias)} ${order.direction.sql}"
        }
        return SqlFragment(orderSql)
    }

    override fun renderLimit(expr: LimitExpr, useTableAlias: Boolean): SqlFragment =
        SqlFragment("LIMIT ?", listOf(expr.limit))

    override fun renderOffset(expr: OffsetExpr, useTableAlias: Boolean): SqlFragment =
        SqlFragment("OFFSET ?", listOf(expr.offset))

    override fun renderUpdate(expr: UpdateQueryExpression, useTableAlias: Boolean): SqlFragment {
        val sql = StringBuilder("UPDATE ")
        val args = mutableListOf<Any?>()

        sql.append(renderTableRef(expr.table, useTableAlias))

        sql.append(" SET ")
        val setFragment = expr.setExpr.accept(this, useTableAlias)
        sql.append(setFragment.sql)
        args.addAll(setFragment.args)

        expr.condition?.let { where ->
            sql.append(" WHERE ")
            val whereFragment = where.accept(this, useTableAlias)
            sql.append(whereFragment.sql)
            args.addAll(whereFragment.args)
        }

        return SqlFragment(sql.toString(), args)
    }

    override fun renderSet(expr: SetExpr, useTableAlias: Boolean): SqlFragment {
        val sql = expr.assignments.joinToString(", ") { assignment ->
            val columnRef = renderColumnRef(assignment.column, useTableAlias)
            if (assignment.value == null) {
                "$columnRef = COALESCE(?, $columnRef)"
            } else {
                "$columnRef = ?"
            }
        }
        return SqlFragment(sql, expr.assignments.map { it.value })
    }

    override fun renderDelete(expr: DeleteQueryExpression, useTableAlias: Boolean): SqlFragment {
        val sql = StringBuilder("DELETE FROM ")
        val args = mutableListOf<Any?>()

        sql.append(renderTableRef(expr.table, useTableAlias))

        expr.condition?.let { where ->
            sql.append(" WHERE ")
            val whereFragment = where.accept(this, useTableAlias)
            sql.append(whereFragment.sql)
            args.addAll(whereFragment.args)
        }

        return SqlFragment(sql.toString(), args)
    }
}
