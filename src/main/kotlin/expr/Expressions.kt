package expr

import schema.Column
import schema.Table

sealed interface InsertExpression : Expression

data class InsertValuesExpr(
    private val assignments: List<ColumnAssignment>
) : InsertExpression {
    data class ColumnAssignment(val column: Column<*>, val value: Any)

    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val columnsSql = assignments.joinToString(", ") { it.column.key() }
        val placeholders = assignments.joinToString(", ") { "?" }
        return SqlFragment("($columnsSql) VALUES ($placeholders)", assignments.map { it.value })
    }
}

sealed interface DeleteExpression : Expression

data class DeleteExpr(private val table: Table) : DeleteExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment("DELETE FROM ${table.tableName}")
    }
}

sealed interface WhereExpression : Expression

data class ComparisonExpr<T : Any>(
    private val column: Column<T>,
    private val operator: String,
    private val value: T?
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val columnRef = if (useTableAlias) {
            "${column.table().alias()}.${column.key()}"
        } else {
            column.key()
        }

        return when (value) {
            null -> SqlFragment("$columnRef $operator")
            else -> SqlFragment("$columnRef $operator ?", listOf(value))
        }
    }
}

data class LogicalExpr(
    private val operator: String,
    private val expressions: List<WhereExpression>
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val fragments = expressions.map { it.sqlArgs(useTableAlias) }
        val sql = StringBuilder()
        val args = mutableListOf<Any?>()

        when (operator) {
            "NOT" -> {
                sql.append("NOT (")
                sql.append(fragments[0].sql)
                sql.append(")")
                args.addAll(fragments[0].args)
            }
            else -> {
                fragments.forEachIndexed { index, fragment ->
                    if (index > 0) sql.append(" $operator ")
                    sql.append(fragment.sql)
                    args.addAll(fragment.args)
                }
            }
        }

        return SqlFragment(sql.toString(), args)
    }
}

data class InListExpr<T : Any>(
    private val column: Column<T>,
    private val values: List<T>,
    private val isNegated: Boolean = false
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val columnRef = if (useTableAlias) {
            "${column.table().alias()}.${column.key()}"
        } else {
            column.key()
        }

        val operator = if (isNegated) "NOT IN" else "IN"
        val placeholders = values.joinToString(", ") { "?" }
        return SqlFragment(
            "$columnRef $operator ($placeholders)",
            values
        )
    }
}

data class BetweenExpr<T : Comparable<T>>(
    private val column: Column<T>,
    private val range: Pair<T, T>
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val columnRef = if (useTableAlias) {
            "${column.table().alias()}.${column.key()}"
        } else {
            column.key()
        }

        return SqlFragment(
            "$columnRef BETWEEN ? AND ?",
            listOf(range.first, range.second)
        )
    }
}

data class ExistsExpr<T : Any>(
    private val subquery: String,
    private val args: List<T>,
    private val isNegated: Boolean = false
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val operator = if (isNegated) "NOT EXISTS" else "EXISTS"
        return SqlFragment("$operator ($subquery)", args)
    }
}

data class GroupExpr(
    private val expression: WhereExpression
) : WhereExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val fragment = expression.sqlArgs(useTableAlias)
        return SqlFragment("(${fragment.sql})", fragment.args)
    }
}

sealed interface UpdateExpression : Expression

data class SetExpr(private val assignments: List<Assignment>) : UpdateExpression {
    data class Assignment(val column: Column<*>, val value: Any?)

    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val sql = assignments.joinToString(", ") { assignment ->
            val columnRef = assignment.column.key()

            if (assignment.value == null) {
                "$columnRef = COALESCE(?, $columnRef)"
            } else {
                "$columnRef = ?"
            }
        }
        return SqlFragment(sql, assignments.map { it.value })
    }
}

sealed interface LimitOffsetExpression : Expression

data class LimitExpr(private val limit: Int) : LimitOffsetExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment("LIMIT ?", listOf(limit))
    }
}

data class OffsetExpr(private val offset: Int) : LimitOffsetExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment("OFFSET ?", listOf(offset))
    }
}

sealed interface OrderByExpression : Expression

data class OrderByExpr(private val columns: List<Pair<Column<*>, OrderDirection>>) : OrderByExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val orderSql = columns.joinToString(", ") { (column, direction) ->
            if (useTableAlias) {
                "${column.table().alias()}.${column.key()} ${direction.sql}"
            } else {
                "${column.key()} ${direction.sql}"
            }
        }
        return SqlFragment(orderSql)
    }
}

sealed class OrderDirection(val sql: String) {
    data object ASC : OrderDirection("ASC")
    data object DESC : OrderDirection("DESC")

    companion object {
        fun fromString(value: String?): OrderDirection = when (value?.uppercase()) {
            "DESC" -> DESC
            else -> ASC
        }
    }
}

sealed interface SelectClauseExpression : Expression

data class CountSelectExpr(private val isCount: Boolean = true) : SelectClauseExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment("SELECT COUNT(*)")
    }
}

data class ExistsSelectExpr(private val isExists: Boolean = true) : SelectClauseExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment("SELECT EXISTS (SELECT 1")
    }
}

data class ColumnsSelectExpr(private val columns: List<Column<*>>) : SelectClauseExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return SqlFragment(columns.joinToString(", ") { it.keyWithTableAlias() })
    }
}

sealed interface JoinExpression : Expression {
    val table: Table
    val columns: List<Column<*>>
    fun columnsSql(useTableAlias: Boolean): List<String>
}

data class TableJoinExpr(
    override val table: Table,
    override val columns: List<Column<*>>,
    private val type: JoinType,
    private val condition: JoinCondition
) : JoinExpression {
    override fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        val tableRef = if (useTableAlias) {
            "${table.tableName} ${table.alias()}"
        } else {
            table.tableName
        }

        val conditionFragment = condition.sqlArgs(useTableAlias)
        return SqlFragment(
            "${type.sql} JOIN $tableRef ON ${conditionFragment.sql}",
            conditionFragment.args
        )
    }

    override fun columnsSql(useTableAlias: Boolean): List<String> {
        return columns.map { column ->
            if (useTableAlias) {
                "${column.table().alias()}.${column.key()}"
            } else {
                column.key()
            }
        }
    }
}

enum class JoinType(val sql: String) {
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    INNER("INNER"),
    OUTER("OUTER"),
    FULL("FULL")
}

data class JoinCondition(
    val leftColumn: Column<*>,
    val rightColumn: Column<*>
) {
    fun sqlArgs(useTableAlias: Boolean): SqlFragment {
        return if (useTableAlias) {
            SqlFragment(
                "${leftColumn.table().alias()}.${leftColumn.key()} = " +
                        "${rightColumn.table().alias()}.${rightColumn.key()}"
            )
        } else {
            SqlFragment(
                "${leftColumn.key()} = ${rightColumn.key()}"
            )
        }
    }
}
