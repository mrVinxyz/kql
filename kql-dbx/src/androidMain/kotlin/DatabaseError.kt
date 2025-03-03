sealed class DatabaseError {
    data class ExecutionError(val sql: String, val args: Array<out Any?>, val message: String) : DatabaseError()
    data class BatchExecutionError(val sql: String, val message: String) : DatabaseError()
    data class QueryError(val sql: String, val args: Array<out Any?>, val message: String) : DatabaseError()
    data class TransactionError(val message: String, val cause: Throwable? = null) : DatabaseError()
    data object UnknownError : DatabaseError()

    override fun toString(): String {
        return when (this) {
            is ExecutionError -> "ExecutionError: $message for query: $sql with args: ${args.contentToString()}"
            is BatchExecutionError -> "BatchExecutionError: $message for query: $sql"
            is QueryError -> "QueryError: $message for query: $sql with args: ${args.contentToString()}"
            is TransactionError -> "TransactionError: $message ${cause?.let { "caused by: ${it.message}" } ?: ""}"
            is UnknownError -> "UnknownError: An unknown database error occurred"
        }
    }
}