package expr

sealed interface Expression {
    fun sqlArgs(useTableAlias: Boolean = false): SqlFragment
}

data class SqlFragment(
    val sql: String,
    val args: List<Any?> = emptyList()
)
